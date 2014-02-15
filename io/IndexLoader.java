package oxygen.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * <p>索引加载器</p>
 * <p>索引加载器是完成索引操作最主要的类。如果说{@link IndexManager 索引管理器}是索引操作的协调者和代理，那么
 * 索引管理器就是完成索引操作的执行者。实体的每一个索引字段都有一个索引加载器相对应，当一个索引操作的请求被索引管理器
 * 所收到时，索引管理器首先在映射表中查找该索引字段对应的索引加载器。若没有找到与之对应的加载器则创建一个。得到
 * 索引加载器后，就能通过加载器执行指定的索引操作。</p>
 * <p>索引加载器是多线程安全的。</p>
 * @see IndexManager 索引管理器
 * @author 赖昆
 * @since 1.0, 2007-01-16
 * @version 1.0
 */
public class IndexLoader {
	
	/* 
	 * 索引保护类。这个类用于记录一条被保护的索引，这条被保护索引已经插入到索引文件，但是由于微事务，它不能被立刻移入
	 * 内存生效，暂时需要“保护”起来。在“保护”期间，这条索引不能被其他线程所使用，任何试图操作在“保护”状态下的索引的线程
	 * 都将被阻塞，直到微事务的完成。若微事务完成成功，那么该保护索引将被移入内存生效；否则该保护索引将被删除，同时该保护
	 * 索引在索引文件的部分也将由回滚操作所删除。
	 */
	private static class Protection {
		
		final int position;
		final long keySrc;
		final long addressSrc;
		final long keyDest;
		final long addressDest;
		
		Protection( int position, long keySrc, long addressSrc, long keyDest, long addressDest ) {
			this.position = position;
			this.keySrc = keySrc;
			this.addressSrc = addressSrc;
			this.keyDest = keyDest;
			this.addressDest = addressDest;
		}
	}
	
	// 索引字段
	private final Field field;
	
	// 索引文件通道
	private final FileChannel channel;
	
	// 保护索引用锁
	private final byte[] lock = new byte[0];
	
	// 索引-实体地址映射表
	private final Map<Long, Object> entityMap = Collections.synchronizedMap( new HashMap<Long, Object>() );
	
	// 实体地址-索引地址映射表
	private final Map<Long, Integer> indexMap = Collections.synchronizedMap( new HashMap<Long, Integer>() );
	
	// 空位集合
	private final NavigableSet<Integer> freeSet = new ConcurrentSkipListSet<Integer>();
	
	// 微事务信物-受保护地址集合映射表：保存与该微事务相关联的受保护地址
	private final Map<Object, Collection<Protection>> protectMap = new HashMap<Object, Collection<Protection>>();
	
	private static final Logger logger = Logger.getLogger( IndexLoader.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( IndexLoader.class );
	
	// 此构造函数由IndexManager调用
	IndexLoader( Field field ) throws IOException {
		
		this.field = field;
		// 得到索引文件的通道
		File root = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ) );
		File dir = new File( root, "data/index/" + field.getDeclaringClass().getSimpleName() );
		if ( !dir.exists() ) dir.mkdirs();
		File file = new File( dir, field.getName() + ".idx" );
		channel = new RandomAccessFile( file, "rwd" ).getChannel();
		
		// 将索引读入到索引-实体地址映射表，再初始化实体地址-索引地址映射表和空位集合
		int length = (int) channel.size();
		if ( length > 0 ) {
			ByteBuffer bb = ByteBuffer.allocate( length );
			while ( bb.position() < bb.limit() ) {
				channel.read( bb );
			}
			bb.rewind();
			while ( bb.position() < bb.limit() ) {
				long key = bb.getLong();
				long value = bb.getLong();
				if ( key == 0L && value == 0L ) {
					freeSet.add( bb.position() - 16 );
				} else {
					addEntity( key, value );
					indexMap.put( value, bb.position() - 16 );
				}
			}
		}
	}
	
	// 将索引和实体地址加入到映射表。若索引不存在，则直接添加；若索引对应一个地址，则将地址替换为地址集合；若索引对应的已经是
	// 地址集合，则将地址加入地址集合
	@SuppressWarnings("unchecked")
	private void addEntity( long key, long value ) {
		Object o = entityMap.get( key );
		if ( o == null ) {
			entityMap.put( key, value );
		} else if ( o instanceof Long ) {
			HashSet<Long> set = new HashSet<Long>();
			set.add( (Long) o );
			set.add( value );
			entityMap.put( key, set );
		} else {
			( (HashSet<Long>) o ).add( value );
		}
	}
	
	// 将指定索引和实体地址从映射表中移除
	@SuppressWarnings("unchecked")
	private void removeEntity( long key, long value ) {
		Object o = entityMap.get( key );
		if ( o == null ) return;
		if ( o instanceof Long ) {
			entityMap.remove( key );
		} else {
			( (HashSet<Long>) entityMap.get( key ) ).remove( value );
		}
	}
	
	// 加入到微事务信物-保护地址集合映射表，将地址保护起来
	private void protect( Object keepsake, int position, long keySrc, long addressSrc, long keyDest, long addressDest ) {
		synchronized ( lock ) {
			Collection<Protection> c = protectMap.get( keepsake );
			if ( c == null ) {
				c = new HashSet<Protection>();
				protectMap.put( keepsake, c );
			}
			c.add( new Protection( position, keySrc, addressSrc, keyDest, addressDest ) );
		}
	}
	
	// 解除微事务相关的受保护地址的保护
	void unprotect( Object keepsake, boolean successful ) {
		// 若微事务操作成功，加入索引到两个映射表，使其生效
		synchronized ( lock ) {
			if ( successful ) {
				for ( Protection p : protectMap.get( keepsake ) ) {
					if ( p.keySrc == 0L && p.addressSrc == 0L ) {
						addEntity( p.keyDest, p.addressDest );
						indexMap.put( p.addressDest, p.position );
						freeSet.remove( p.position );
					} else if ( p.keyDest == 0L && p.addressDest == 0L ) {
						removeEntity( p.keySrc, p.addressSrc );
						indexMap.remove( p.addressSrc );
						freeSet.add( p.position );
					} else {
						removeEntity( p.keySrc, p.addressSrc );
						indexMap.remove( p.addressSrc );
						addEntity( p.keyDest, p.addressDest );
						indexMap.put( p.addressDest, p.position );
					}
				}
			}
			protectMap.remove( keepsake );
			lock.notifyAll();
		}
	}
	
	// 检查索引文件位置是否正受到保护
	private boolean isProtected( int position ) {
		for ( Collection<Protection> c : protectMap.values() ) {
			for ( Protection p : c ) {
				if ( p.position == position ) {
					return true;
				}
			}
		}
		return false;
	}
	
	// 若指定地址对应的索引受到保护，则等待，直到该地址的保护解除
	private void waitIfProtected( int position ) {
		synchronized ( lock ) {
			while ( isProtected( position ) ) {
				try {
					lock.wait();
				} catch ( InterruptedException e ) {
				}
			}
		}
	}
	
	// 查询索引
	@SuppressWarnings("unchecked")
	HashSet<Long> query( long key ) {
		
		Object o = entityMap.get( key );
		if ( o == null ) {
			return new HashSet<Long>();
		} else if ( o instanceof Long ) {
			HashSet<Long> set = new HashSet<Long>();
			set.add( (Long) o );
			return set;
		} else {
			return (HashSet<Long>) ( (HashSet<Long>) o ).clone();
		}
	}
	
	// 插入索引
	boolean insert( long key, long address, MicroTransaction transaction ) {
		
		// 先从空位集合中找一个可以插入的空位。如果没有，就直接插入到文件末尾
		Integer position = freeSet.pollFirst();
		
		try {
			if ( position == null ) {
				synchronized ( this ) {
					position = (int) channel.size();
				}
			}			
			ByteBuffer bb = ByteBuffer.allocate( 16 );
			bb.putLong( key ).putLong( address ).flip();
			// 若该地址处的索引受到保护，则等待直到保护解除
			waitIfProtected( position );
			synchronized ( this ) {
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			logger.warning( res.getResource( "IndexLoader.insert.warning.InsertIndexFailed", field.getDeclaringClass().getName(), field.getName(), String.valueOf( position ), String.valueOf( key ), String.valueOf( address ) ) );
			return false;
		}
		// 若该索引属于一个微事务，则添加回滚实例并把该索引保护；否则，直接令该索引生效
		if ( transaction != null ) {
			// 由于这是一个插入索引的操作，所以回滚相当于删除。故键和值都赋值为0
			transaction.addRollback( new IndexRollback( this, position, 0L, 0L ) );
			// 将这个新索引“保护”起来，但不立刻生效
			protect( transaction.getKeepsake(), position, 0L, 0L, key, address );
		} else {
			// 加入索引到索引-实体地址映射表
			addEntity( key, address );
			// 加入索引到实体地址-索引地址映射表
			indexMap.put( address, position );
		}
		return true;
	}
	
	// 删除索引
	boolean delete( long address, MicroTransaction transaction ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( 0L ).putLong( 0L ).flip();
		// 仅获取索引的前端hash键，值部分就是参数中的实体地址
		ByteBuffer old = ByteBuffer.allocate( 8 );
		// 获取要删除的索引的地址
		int position = indexMap.get( address );
		// 若地址处于保护状态，则等待保护被解除
		waitIfProtected( position );
		try {
			synchronized ( this ) {
				channel.read( old, position );
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			logger.warning( res.getResource( "IndexLoader.delete.warning.DeleteIndexFailed", field.getDeclaringClass().getName(), field.getName(), String.valueOf( position ) ) );
			return false;
		}
		old.rewind();
		long key = old.getLong();
		// 若该删除操作属于一个微事务，则添加回滚，并加以保护
		if ( transaction != null ) {
			transaction.addRollback( new IndexRollback( this, position, key, address ) );
			protect( transaction.getKeepsake(), position, key, address, 0L, 0L );
		} else {
			removeEntity( key, address );
			indexMap.remove( address );
		}
		
		return true;
	}
	
	// 修改索引
	boolean update( long key, long address, MicroTransaction transaction ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( key ).putLong( address ).flip();
		// 记录索引的原始值
		ByteBuffer old = ByteBuffer.allocate( 8 );
		// 获取要删除的索引的地址
		int position = indexMap.get( address );
		// 若地址处于保护状态，则等待保护被解除
		waitIfProtected( position );
		try {
			synchronized ( this ) {
				channel.read( old, position );
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			logger.warning( res.getResource( "IndexLoader.update.warning.UpdateIndexFailed", field.getDeclaringClass().getName(), field.getName(), String.valueOf( position ) ) );
			return false;
		}
		old.rewind();
		long oldKey = old.getLong();
		// 若该删除操作属于一个微事务，则添加回滚，并加以保护
		if ( transaction != null ) {
			transaction.addRollback( new IndexRollback( this, position, oldKey, address ) );
			protect( transaction.getKeepsake(), position, oldKey, address, key, address );
		} else {
			removeEntity( oldKey, address );
			indexMap.remove( address );
			addEntity( key, address );
			indexMap.put( address, position );
		}
		
		return true;		
	}
	
	// 在此实例表示的索引上执行回滚
	void rollback( int position, long key, long address ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( key ).putLong( address ).flip();
		try {
			synchronized ( this ) {
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			// 回滚操作发生的IOException都不作处理
		}
		// 若键和值都为0则表示这是一个删除操作，将位置加入到空位集合
		if ( key == 0L && address == 0L ) {
			freeSet.add( position );
		} else {
			freeSet.remove( position );
		}
	}
	
	// 关闭索引加载器，释放资源
	void close() {
		// 关闭通道
		try {
			synchronized ( this ) {
				if ( channel.isOpen() ) channel.close();
			}
		} catch ( IOException e ) {
		}
	}
}
