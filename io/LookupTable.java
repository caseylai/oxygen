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
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.entity.Entity;
import oxygen.entity.EntityProvider;
import oxygen.io.standardizer.TypeSizeDefiner;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * <p>实体数据访问处理器操作实体数据文件的查找表</p>
 * <p>该查找表保存实体数据文件通道和实体数据文件操作过程中的中间状态。由于查询、插入、删除、修改操作都将对实体数据文件产生影响，
 * 这个类也用于协调这些操作的顺利完成，如提供了实体保护，锁定等支持。</p>
 * @author 赖昆
 * @since 1.0, 2007-01-12
 * @param <E> 实体
 */
@SuppressWarnings("unchecked")
public class LookupTable<E extends Entity<E>> {
	
	// 实体名
	private final String name;
	
	// 数据库文件的通道
	private final FileChannel channel;
		
	// 实体字段偏移量映射表
	private final Map<Field, Integer> offsetMap = new HashMap<Field, Integer>();
	
	// 实体大小
	private final int size;
	
	// 字段值全部为null的实体的列表
	private final List<E> emptyEntityList;
	
	// 实体文件的空位集合
	private final NavigableSet<Long> freeSet = new ConcurrentSkipListSet<Long>();
	
	// 被保护的实体地址集合（这些地址的实体正在进行操作，是不稳定的。其他线程暂时不能干扰这些实体，所以将它们保护起来）
	private final HashSet<Long> protectedSet = new HashSet<Long>();
	
	// 使用该查找表的实体数据访问处理器数目
	private volatile int user = 0;
	
	// 是否通道正在使用中
	private volatile boolean busy = false;
	
	private static final Logger logger = Logger.getLogger( LookupTable.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( LookupTable.class );
	
	// 实体类对象-查找表映射表
	private static final Map<String, LookupTable<? extends Entity>> tableMap = new HashMap<String, LookupTable<? extends Entity>>();
	
	// 活动的查找表数目
	private static volatile int active = 0;
	
	// 预加载所有实体的查找表
	static {
		for ( Class<? extends Entity> clazz : EntityProvider.entities() ) {
			addTable( clazz );
		}
	}
	
	// 建立实体类与查找表的映射
	private static <E extends Entity<E>> void addTable( Class<E> clazz ) {
		try {
			tableMap.put( clazz.getSimpleName(), new LookupTable<E>( clazz ) );
			active++;
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 用实体类对象获得适合实体的查找表
	 * @param <E> 实体
	 * @param clazz 实体类对象
	 * @return 对应实体类的查找表
	 * @see #getLookupTable(String)
	 */
	public static <E extends Entity<E>> LookupTable<E> getLookupTable( Class<E> clazz ) {
		return getLookupTable( clazz.getSimpleName() );
	}
	
	/**
	 * 用实体名获得适合实体的查找表
	 * @param <E> 实体
	 * @param className 实体名
	 * @return 对应实体类的查找表
	 * @see #getLookupTable(Class)
	 */
	public static <E extends Entity<E>> LookupTable<E> getLookupTable( String className ) {
		LookupTable<E> table = (LookupTable<E>) tableMap.get( className );
		if ( table == null && EntityProvider.names().contains( className ) ) {
			E entity = EntityProvider.provide( className );
			addTable( entity.getClass() );
			table = (LookupTable<E>) tableMap.get( className );
		}
		return table;
	}
	
	// 默认无修饰符构造器
	LookupTable( Class<? extends Entity> clazz ) throws IOException {
		
		name = clazz.getSimpleName();
		E entity = EntityProvider.provide( name );
		emptyEntityList = Collections.singletonList( entity );
		
		// 打开文件通道
		File file = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "data/" + name + ".dat" );
		channel = new RandomAccessFile( file, "rwd" ).getChannel();
		
		// 加载空位集合文件
		file = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "data/" + name + ".fre" );
		if ( file.exists() ) {
			FileChannel fc = new RandomAccessFile( file, "r" ).getChannel();
			int length = (int) fc.size();
			if ( length > 0 ) {
				ByteBuffer bb = ByteBuffer.allocate( length );
				while ( bb.position() < bb.limit() ) {
					fc.read( bb );
				}
				bb.rewind();
				while ( bb.position() < bb.limit() ) {
					freeSet.add( bb.getLong() );
				}
				fc.close();
			}
			file.delete();
		}
		
		// 分析实体结构，确定各字段偏移量
		SortedMap<String, Field> sm = new TreeMap<String, Field>();
		Field[] fields = clazz.getDeclaredFields();
		for ( Field field : fields ) {
			sm.put( field.getName(), field );
		}
		int offset = 0;
		for ( String name : sm.keySet() ) {
			Field field = sm.get( name );
			offsetMap.put( field, offset );
			offset += TypeSizeDefiner.define( field );
		}
		size = offset;
	}
	
	/**
	 * 获取当前各实体进行数据操作的线程数，以实体名-线程数的映射表形式返回
	 */
	public static Map<String, Integer> getUserThreadCountMap() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for ( LookupTable<? extends Entity> table : tableMap.values() ) {
			map.put( table.name, table.user );
		}
		return map;
	}
	
	// 检查两个集合是否有共同的元素（交集是否不为空）
	private static boolean isCrossSetNull( HashSet<Long> set1, HashSet<Long> set2 ) {
		if ( set1 == null || set2 == null ) return true;
		HashSet<Long> temp = (HashSet<Long>) set1.clone();
		temp.retainAll( set2 );
		return temp.size() == 0;
	}
	
	/**
	 * <p>获取文件通道独占锁定。在使用通道进行I/O操作前应调用这个方法以得到锁定，确保通道操作安全</p>
	 * <p>获取锁定前对指定地址集合进行保护地址检测，若该地址集合有至少一个地址处于保护状态，线程将阻塞</p>
	 * @param addressSet 要操作的地址集合
	 * @see #lock(Long)
	 * @see #lock()
	 */
	public synchronized void lock( HashSet<Long> addressSet ) {
		// 若通道忙，则等待
		while ( busy && isCrossSetNull( addressSet, protectedSet ) ) {
			try {
				wait();
			} catch ( InterruptedException e ) {
			}
		}
		busy = true;
	}
	
	/**
	 * <p>获取文件通道独占锁定。在使用通道进行I/O操作前应调用这个方法以得到锁定，确保通道操作安全</p>
	 * <p>获取锁定前对指定地址进行保护地址检测，若该地址处于保护状态，线程将阻塞</p>
	 * @param address 要操作的地址
	 * @see #lock(HashSet)
	 * @see #lock()
	 */
	public void lock( Long address ) {
		HashSet<Long> addressSet = new HashSet<Long>();
		addressSet.add( address );
		lock( addressSet );
	}
	
	/**
	 * <p>获取文件通道独占锁定。在使用通道进行I/O操作前应调用这个方法以得到锁定，确保通道操作安全</p>
	 * <p>获取锁定前不进行保护地址检测</p>
	 * @see #lock(HashSet)
	 * @see #lock(Long)
	 */
	public void lock() {
		lock( (HashSet<Long>) null );
	}
	
	/**
	 * 解除文件通道的独占锁定。在不使用通道时应调用这个方法以释放锁定
	 */
	public synchronized void unlock() {
		busy = false;
		notifyAll();
	}
	
	/**
	 * 获取实体名
	 */
	public String name() {
		return name;
	}
	
	/**
	 * 获得空实体实例的列表
	 */
	public List<E> getEmptyEntityList() {
		return emptyEntityList;
	}

	/**
	 * 获取字段在实体中的偏移量
	 */
	public int offset( Field field ) {
		return offsetMap.get( field );
	}
	
	/**
	 * 得到实体的大小（单位byte）
	 */
	public int size() {
		return size;
	}
	
	/**
	 * 得到所有实体字段
	 */
	public Collection<Field> getFields() {
		return offsetMap.keySet();
	}
	
	/**
	 * 获取实体文件的文件通道
	 */
	public FileChannel channel() {
		return channel;
	}
	
	/**
	 * 从空位集合得到一个空位，同时将这个空位移除
	 */
	public Long getFree() {
		return freeSet.pollFirst();
	}
	
	/**
	 * 将一个空位放入空位集合
	 * @param address 空位的地址
	 */
	public void putFree( long address ) {
		freeSet.add( address );
	}
	
	/**
	 * 从空位集合移除指定的地址
	 * @param address 空位地址
	 */
	public void removeFree( long address ) {
		freeSet.remove( address );
	}
	
	/**
	 * 保护指定的地址
	 */
	synchronized void protect( long address ) {
		while ( protectedSet.contains( address ) ) {
			try {
				wait();
			} catch ( InterruptedException e ) {
			}
		}
		protectedSet.add( address );
	}
	
	/**
	 * 解除指定地址的保护
	 */
	synchronized void unprotect( long address ) {
		protectedSet.remove( address );
		notifyAll();
	}
	
	/**
	 * 该查找表的使用者数量增加一个。记录该数量的目的是确保安全地关闭查找表
	 */
	public void use() {
		user++;
	}
	
	/**
	 * 该查找表的使用者数量减少一个
	 */
	public void away() {
		user--;
		// 通知close方法
		synchronized ( this ) {
			notifyAll();
		}
	}

	// 关闭此查找表。关闭后该查找表将不可使用
	void close() {
		
		try {
			synchronized ( this ) {
				// 当还有实体数据访问处理器在使用该查找表时，暂时等候。当通道空闲时，关闭通道
				while ( user > 0 ) {
					try {
						wait();
					} catch ( InterruptedException e ) {
					}
				}
				channel.close();
			}
		} catch ( IOException e ) {
		}
		
		// 保存空位集合到空位集合文件
		File file = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "data/" + name + ".fre" );
		Long[] frees = (Long[]) freeSet.toArray( new Long[0] );
		if ( frees.length > 0 ) {
			try {
				FileChannel fc = new RandomAccessFile( file, "rw" ).getChannel();
				ByteBuffer bb = ByteBuffer.allocate( frees.length * 8 );
				for ( Long free : frees ) {
					bb.putLong( free );
				}
				bb.flip();
				fc.write( bb );
				fc.close();
			} catch ( IOException e ) {
				logger.warning( res.getResource( "LookupTable.close.warning.SaveEmptyPositionFileFailed", name ) );
			}
		}
		
		// 关闭查找表后通知
		active--;
		synchronized ( LookupTable.class ) {
			LookupTable.class.notifyAll();
		}
	}
	
	/**
	 * 关闭全部查找表
	 */
	public static void closeAll() {
		
		// 枚举查找表并进行关闭
		for ( final LookupTable<? extends Entity> table : tableMap.values() ) {
			new Thread( new Runnable() {
				public void run() {
					table.close();
				}
			}).start();
		}
		
		// 阻塞等待全部查找表关闭
		while ( active > 0 ) {
			try {
				synchronized ( LookupTable.class ) {
					LookupTable.class.wait();
				}
			} catch ( InterruptedException e ) {
			}
		}
		
		tableMap.clear();
	}
}
