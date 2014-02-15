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
 * <p>����������</p>
 * <p>���������������������������Ҫ���ࡣ���˵{@link IndexManager ����������}������������Э���ߺʹ�����ô
 * ���������������������������ִ���ߡ�ʵ���ÿһ�������ֶζ���һ���������������Ӧ����һ��������������������������
 * ���յ�ʱ������������������ӳ����в��Ҹ������ֶζ�Ӧ����������������û���ҵ���֮��Ӧ�ļ������򴴽�һ�����õ�
 * �����������󣬾���ͨ��������ִ��ָ��������������</p>
 * <p>�����������Ƕ��̰߳�ȫ�ġ�</p>
 * @see IndexManager ����������
 * @author ����
 * @since 1.0, 2007-01-16
 * @version 1.0
 */
public class IndexLoader {
	
	/* 
	 * ���������ࡣ��������ڼ�¼һ�������������������������������Ѿ����뵽�����ļ�����������΢���������ܱ���������
	 * �ڴ���Ч����ʱ��Ҫ���������������ڡ��������ڼ䣬�����������ܱ������߳���ʹ�ã��κ���ͼ�����ڡ�������״̬�µ��������߳�
	 * ������������ֱ��΢�������ɡ���΢������ɳɹ�����ô�ñ����������������ڴ���Ч������ñ�����������ɾ����ͬʱ�ñ���
	 * �����������ļ��Ĳ���Ҳ���ɻع�������ɾ����
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
	
	// �����ֶ�
	private final Field field;
	
	// �����ļ�ͨ��
	private final FileChannel channel;
	
	// ������������
	private final byte[] lock = new byte[0];
	
	// ����-ʵ���ַӳ���
	private final Map<Long, Object> entityMap = Collections.synchronizedMap( new HashMap<Long, Object>() );
	
	// ʵ���ַ-������ַӳ���
	private final Map<Long, Integer> indexMap = Collections.synchronizedMap( new HashMap<Long, Integer>() );
	
	// ��λ����
	private final NavigableSet<Integer> freeSet = new ConcurrentSkipListSet<Integer>();
	
	// ΢��������-�ܱ�����ַ����ӳ����������΢������������ܱ�����ַ
	private final Map<Object, Collection<Protection>> protectMap = new HashMap<Object, Collection<Protection>>();
	
	private static final Logger logger = Logger.getLogger( IndexLoader.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( IndexLoader.class );
	
	// �˹��캯����IndexManager����
	IndexLoader( Field field ) throws IOException {
		
		this.field = field;
		// �õ������ļ���ͨ��
		File root = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ) );
		File dir = new File( root, "data/index/" + field.getDeclaringClass().getSimpleName() );
		if ( !dir.exists() ) dir.mkdirs();
		File file = new File( dir, field.getName() + ".idx" );
		channel = new RandomAccessFile( file, "rwd" ).getChannel();
		
		// ���������뵽����-ʵ���ַӳ����ٳ�ʼ��ʵ���ַ-������ַӳ���Ϳ�λ����
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
	
	// ��������ʵ���ַ���뵽ӳ��������������ڣ���ֱ����ӣ���������Ӧһ����ַ���򽫵�ַ�滻Ϊ��ַ���ϣ���������Ӧ���Ѿ���
	// ��ַ���ϣ��򽫵�ַ�����ַ����
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
	
	// ��ָ��������ʵ���ַ��ӳ������Ƴ�
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
	
	// ���뵽΢��������-������ַ����ӳ�������ַ��������
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
	
	// ���΢������ص��ܱ�����ַ�ı���
	void unprotect( Object keepsake, boolean successful ) {
		// ��΢��������ɹ�����������������ӳ���ʹ����Ч
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
	
	// ��������ļ�λ���Ƿ����ܵ�����
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
	
	// ��ָ����ַ��Ӧ�������ܵ���������ȴ���ֱ���õ�ַ�ı������
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
	
	// ��ѯ����
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
	
	// ��������
	boolean insert( long key, long address, MicroTransaction transaction ) {
		
		// �ȴӿ�λ��������һ�����Բ���Ŀ�λ�����û�У���ֱ�Ӳ��뵽�ļ�ĩβ
		Integer position = freeSet.pollFirst();
		
		try {
			if ( position == null ) {
				synchronized ( this ) {
					position = (int) channel.size();
				}
			}			
			ByteBuffer bb = ByteBuffer.allocate( 16 );
			bb.putLong( key ).putLong( address ).flip();
			// ���õ�ַ���������ܵ���������ȴ�ֱ���������
			waitIfProtected( position );
			synchronized ( this ) {
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			logger.warning( res.getResource( "IndexLoader.insert.warning.InsertIndexFailed", field.getDeclaringClass().getName(), field.getName(), String.valueOf( position ), String.valueOf( key ), String.valueOf( address ) ) );
			return false;
		}
		// ������������һ��΢��������ӻع�ʵ�����Ѹ���������������ֱ�����������Ч
		if ( transaction != null ) {
			// ��������һ�����������Ĳ��������Իع��൱��ɾ�����ʼ���ֵ����ֵΪ0
			transaction.addRollback( new IndexRollback( this, position, 0L, 0L ) );
			// �����������������������������������Ч
			protect( transaction.getKeepsake(), position, 0L, 0L, key, address );
		} else {
			// ��������������-ʵ���ַӳ���
			addEntity( key, address );
			// ����������ʵ���ַ-������ַӳ���
			indexMap.put( address, position );
		}
		return true;
	}
	
	// ɾ������
	boolean delete( long address, MicroTransaction transaction ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( 0L ).putLong( 0L ).flip();
		// ����ȡ������ǰ��hash����ֵ���־��ǲ����е�ʵ���ַ
		ByteBuffer old = ByteBuffer.allocate( 8 );
		// ��ȡҪɾ���������ĵ�ַ
		int position = indexMap.get( address );
		// ����ַ���ڱ���״̬����ȴ����������
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
		// ����ɾ����������һ��΢��������ӻع��������Ա���
		if ( transaction != null ) {
			transaction.addRollback( new IndexRollback( this, position, key, address ) );
			protect( transaction.getKeepsake(), position, key, address, 0L, 0L );
		} else {
			removeEntity( key, address );
			indexMap.remove( address );
		}
		
		return true;
	}
	
	// �޸�����
	boolean update( long key, long address, MicroTransaction transaction ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( key ).putLong( address ).flip();
		// ��¼������ԭʼֵ
		ByteBuffer old = ByteBuffer.allocate( 8 );
		// ��ȡҪɾ���������ĵ�ַ
		int position = indexMap.get( address );
		// ����ַ���ڱ���״̬����ȴ����������
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
		// ����ɾ����������һ��΢��������ӻع��������Ա���
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
	
	// �ڴ�ʵ����ʾ��������ִ�лع�
	void rollback( int position, long key, long address ) {
		
		ByteBuffer bb = ByteBuffer.allocate( 16 );
		bb.putLong( key ).putLong( address ).flip();
		try {
			synchronized ( this ) {
				channel.write( bb, position );
			}
		} catch ( IOException e ) {
			// �ع�����������IOException����������
		}
		// ������ֵ��Ϊ0���ʾ����һ��ɾ����������λ�ü��뵽��λ����
		if ( key == 0L && address == 0L ) {
			freeSet.add( position );
		} else {
			freeSet.remove( position );
		}
	}
	
	// �ر��������������ͷ���Դ
	void close() {
		// �ر�ͨ��
		try {
			synchronized ( this ) {
				if ( channel.isOpen() ) channel.close();
			}
		} catch ( IOException e ) {
		}
	}
}
