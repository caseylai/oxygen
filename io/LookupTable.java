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
 * <p>ʵ�����ݷ��ʴ���������ʵ�������ļ��Ĳ��ұ�</p>
 * <p>�ò��ұ���ʵ�������ļ�ͨ����ʵ�������ļ����������е��м�״̬�����ڲ�ѯ�����롢ɾ�����޸Ĳ���������ʵ�������ļ�����Ӱ�죬
 * �����Ҳ����Э����Щ������˳����ɣ����ṩ��ʵ�屣����������֧�֡�</p>
 * @author ����
 * @since 1.0, 2007-01-12
 * @param <E> ʵ��
 */
@SuppressWarnings("unchecked")
public class LookupTable<E extends Entity<E>> {
	
	// ʵ����
	private final String name;
	
	// ���ݿ��ļ���ͨ��
	private final FileChannel channel;
		
	// ʵ���ֶ�ƫ����ӳ���
	private final Map<Field, Integer> offsetMap = new HashMap<Field, Integer>();
	
	// ʵ���С
	private final int size;
	
	// �ֶ�ֵȫ��Ϊnull��ʵ����б�
	private final List<E> emptyEntityList;
	
	// ʵ���ļ��Ŀ�λ����
	private final NavigableSet<Long> freeSet = new ConcurrentSkipListSet<Long>();
	
	// ��������ʵ���ַ���ϣ���Щ��ַ��ʵ�����ڽ��в������ǲ��ȶ��ġ������߳���ʱ���ܸ�����Щʵ�壬���Խ����Ǳ���������
	private final HashSet<Long> protectedSet = new HashSet<Long>();
	
	// ʹ�øò��ұ��ʵ�����ݷ��ʴ�������Ŀ
	private volatile int user = 0;
	
	// �Ƿ�ͨ������ʹ����
	private volatile boolean busy = false;
	
	private static final Logger logger = Logger.getLogger( LookupTable.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( LookupTable.class );
	
	// ʵ�������-���ұ�ӳ���
	private static final Map<String, LookupTable<? extends Entity>> tableMap = new HashMap<String, LookupTable<? extends Entity>>();
	
	// ��Ĳ��ұ���Ŀ
	private static volatile int active = 0;
	
	// Ԥ��������ʵ��Ĳ��ұ�
	static {
		for ( Class<? extends Entity> clazz : EntityProvider.entities() ) {
			addTable( clazz );
		}
	}
	
	// ����ʵ��������ұ��ӳ��
	private static <E extends Entity<E>> void addTable( Class<E> clazz ) {
		try {
			tableMap.put( clazz.getSimpleName(), new LookupTable<E>( clazz ) );
			active++;
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ʵ����������ʺ�ʵ��Ĳ��ұ�
	 * @param <E> ʵ��
	 * @param clazz ʵ�������
	 * @return ��Ӧʵ����Ĳ��ұ�
	 * @see #getLookupTable(String)
	 */
	public static <E extends Entity<E>> LookupTable<E> getLookupTable( Class<E> clazz ) {
		return getLookupTable( clazz.getSimpleName() );
	}
	
	/**
	 * ��ʵ��������ʺ�ʵ��Ĳ��ұ�
	 * @param <E> ʵ��
	 * @param className ʵ����
	 * @return ��Ӧʵ����Ĳ��ұ�
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
	
	// Ĭ�������η�������
	LookupTable( Class<? extends Entity> clazz ) throws IOException {
		
		name = clazz.getSimpleName();
		E entity = EntityProvider.provide( name );
		emptyEntityList = Collections.singletonList( entity );
		
		// ���ļ�ͨ��
		File file = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "data/" + name + ".dat" );
		channel = new RandomAccessFile( file, "rwd" ).getChannel();
		
		// ���ؿ�λ�����ļ�
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
		
		// ����ʵ��ṹ��ȷ�����ֶ�ƫ����
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
	 * ��ȡ��ǰ��ʵ��������ݲ������߳�������ʵ����-�߳�����ӳ�����ʽ����
	 */
	public static Map<String, Integer> getUserThreadCountMap() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for ( LookupTable<? extends Entity> table : tableMap.values() ) {
			map.put( table.name, table.user );
		}
		return map;
	}
	
	// ������������Ƿ��й�ͬ��Ԫ�أ������Ƿ�Ϊ�գ�
	private static boolean isCrossSetNull( HashSet<Long> set1, HashSet<Long> set2 ) {
		if ( set1 == null || set2 == null ) return true;
		HashSet<Long> temp = (HashSet<Long>) set1.clone();
		temp.retainAll( set2 );
		return temp.size() == 0;
	}
	
	/**
	 * <p>��ȡ�ļ�ͨ����ռ��������ʹ��ͨ������I/O����ǰӦ������������Եõ�������ȷ��ͨ��������ȫ</p>
	 * <p>��ȡ����ǰ��ָ����ַ���Ͻ��б�����ַ��⣬���õ�ַ����������һ����ַ���ڱ���״̬���߳̽�����</p>
	 * @param addressSet Ҫ�����ĵ�ַ����
	 * @see #lock(Long)
	 * @see #lock()
	 */
	public synchronized void lock( HashSet<Long> addressSet ) {
		// ��ͨ��æ����ȴ�
		while ( busy && isCrossSetNull( addressSet, protectedSet ) ) {
			try {
				wait();
			} catch ( InterruptedException e ) {
			}
		}
		busy = true;
	}
	
	/**
	 * <p>��ȡ�ļ�ͨ����ռ��������ʹ��ͨ������I/O����ǰӦ������������Եõ�������ȷ��ͨ��������ȫ</p>
	 * <p>��ȡ����ǰ��ָ����ַ���б�����ַ��⣬���õ�ַ���ڱ���״̬���߳̽�����</p>
	 * @param address Ҫ�����ĵ�ַ
	 * @see #lock(HashSet)
	 * @see #lock()
	 */
	public void lock( Long address ) {
		HashSet<Long> addressSet = new HashSet<Long>();
		addressSet.add( address );
		lock( addressSet );
	}
	
	/**
	 * <p>��ȡ�ļ�ͨ����ռ��������ʹ��ͨ������I/O����ǰӦ������������Եõ�������ȷ��ͨ��������ȫ</p>
	 * <p>��ȡ����ǰ�����б�����ַ���</p>
	 * @see #lock(HashSet)
	 * @see #lock(Long)
	 */
	public void lock() {
		lock( (HashSet<Long>) null );
	}
	
	/**
	 * ����ļ�ͨ���Ķ�ռ�������ڲ�ʹ��ͨ��ʱӦ��������������ͷ�����
	 */
	public synchronized void unlock() {
		busy = false;
		notifyAll();
	}
	
	/**
	 * ��ȡʵ����
	 */
	public String name() {
		return name;
	}
	
	/**
	 * ��ÿ�ʵ��ʵ�����б�
	 */
	public List<E> getEmptyEntityList() {
		return emptyEntityList;
	}

	/**
	 * ��ȡ�ֶ���ʵ���е�ƫ����
	 */
	public int offset( Field field ) {
		return offsetMap.get( field );
	}
	
	/**
	 * �õ�ʵ��Ĵ�С����λbyte��
	 */
	public int size() {
		return size;
	}
	
	/**
	 * �õ�����ʵ���ֶ�
	 */
	public Collection<Field> getFields() {
		return offsetMap.keySet();
	}
	
	/**
	 * ��ȡʵ���ļ����ļ�ͨ��
	 */
	public FileChannel channel() {
		return channel;
	}
	
	/**
	 * �ӿ�λ���ϵõ�һ����λ��ͬʱ�������λ�Ƴ�
	 */
	public Long getFree() {
		return freeSet.pollFirst();
	}
	
	/**
	 * ��һ����λ�����λ����
	 * @param address ��λ�ĵ�ַ
	 */
	public void putFree( long address ) {
		freeSet.add( address );
	}
	
	/**
	 * �ӿ�λ�����Ƴ�ָ���ĵ�ַ
	 * @param address ��λ��ַ
	 */
	public void removeFree( long address ) {
		freeSet.remove( address );
	}
	
	/**
	 * ����ָ���ĵ�ַ
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
	 * ���ָ����ַ�ı���
	 */
	synchronized void unprotect( long address ) {
		protectedSet.remove( address );
		notifyAll();
	}
	
	/**
	 * �ò��ұ��ʹ������������һ������¼��������Ŀ����ȷ����ȫ�عرղ��ұ�
	 */
	public void use() {
		user++;
	}
	
	/**
	 * �ò��ұ��ʹ������������һ��
	 */
	public void away() {
		user--;
		// ֪ͨclose����
		synchronized ( this ) {
			notifyAll();
		}
	}

	// �رմ˲��ұ��رպ�ò��ұ�����ʹ��
	void close() {
		
		try {
			synchronized ( this ) {
				// ������ʵ�����ݷ��ʴ�������ʹ�øò��ұ�ʱ����ʱ�Ⱥ򡣵�ͨ������ʱ���ر�ͨ��
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
		
		// �����λ���ϵ���λ�����ļ�
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
		
		// �رղ��ұ��֪ͨ
		active--;
		synchronized ( LookupTable.class ) {
			LookupTable.class.notifyAll();
		}
	}
	
	/**
	 * �ر�ȫ�����ұ�
	 */
	public static void closeAll() {
		
		// ö�ٲ��ұ����йر�
		for ( final LookupTable<? extends Entity> table : tableMap.values() ) {
			new Thread( new Runnable() {
				public void run() {
					table.close();
				}
			}).start();
		}
		
		// �����ȴ�ȫ�����ұ�ر�
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
