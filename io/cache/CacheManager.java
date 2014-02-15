package oxygen.io.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import oxygen.entity.Entity;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * <p>���������<p>
 * <p>����������ǰ�ʵ�����ͻ��ֵģ�һ��ʵ�����͵Ļ��������������һ������ʵ��Ļ��塣ÿ�������������ά����
 * һ��ʵ��-�����ӳ�������ѯ��ʵ������ڸ�ӳ�����ʱ���Ϳ���ֱ�ӷ���ʵ��Ĳ�ѯ�����������Ҫ������в�ѯ��
 * ���������˲�ѯ���ܡ����ǵ�ʵ���ļ��ı�ʱ��ӳ����в��ֻ�ȫ�����彫���ڣ���ʱ�򻺳彫���������Ա��ֻ���
 * �����ݺ�����ͬ����</p>
 * <p>��ʵ�徭������ѯ�����ٱ��޸�ʱ��Ӧ��ʹ�û�������߲�ѯ���ܣ�����ά������Ҳ��һ�����ܿ�������ʵ����ٲ�ѯ
 * ���������޸�ʱ����Ӧʹ�û��塣</p>
 * @author ����
 * @since 1.0, 2007-05-12
 * @version 1.0
 * @param <E> ʵ��
 */
public class CacheManager<E extends Entity<E>> {
	
	// ʵ��������-���������ӳ���
	private static final Map<Class<? extends Entity>, CacheManager<? extends Entity>> managerMap = new HashMap<Class<? extends Entity>, CacheManager<? extends Entity>>();
	
	// ʵ��-����ӳ���
	private final Map<Entity<E>, Cache<E>> cacheMap = new HashMap<Entity<E>, Cache<E>>();
	
	private static final Logger logger = Logger.getLogger( CacheManager.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( CacheManager.class );
	
	private CacheManager() {}
	
	/**
	 * �õ���ָ��ʵ������ƥ��Ļ������������ָ����ʵ�������Ҳ���ƥ��Ĺ����������´���һ��
	 * @param <E> ʵ��
	 * @param entity ָ��ʵ��
	 * @return ��ָ��ʵ��������ƥ��Ļ��������
	 */
	@SuppressWarnings("unchecked")
	public synchronized static <E extends Entity<E>> CacheManager<E> getCacheManager( E entity ) {
		Object o = managerMap.get( entity.getClass() );
		if ( o == null ) {
			CacheManager<E> manager = new CacheManager<E>();
			managerMap.put( entity.getClass(), manager );
			return manager;
		} else {
			return (CacheManager<E>) o;
		}
	}
	
	/**
	 * ����Cache������
	 */
	public static int getCacheManagerCount() {
		return managerMap.size();
	}
	
	/**
	 * ����Cache�л���Ľ��������
	 */
	public static int getAllCacheCount() {
		int count = 0;
		synchronized ( CacheManager.class ) {
			for ( CacheManager<? extends Entity> manager : managerMap.values() ) {
				count += manager.cacheMap.size();
			}
		}
		return count;
	}
	
	// ���ڴ��ʹ�ý��м��ӡ�����true��ʾ�ڴ�û�г��ֽ���
	private static boolean testMemory() {
		Runtime rt = Runtime.getRuntime();
		// �����ڴ��������ڴ��1/4ʱ�ڴ�û�г��ֽ���
		return rt.freeMemory() + ( rt.maxMemory() - rt.totalMemory() ) > .25 * rt.maxMemory();
	}
	
	// ��¡ָ�����б�����¡��ͬʱ��¡���е�ʵ�壩
	private static <E extends Entity<E>> List<E> cloneList( List<E> list ) {
		List<E> cloneList = new LinkedList<E>();
		for ( E entity : list ) {
			cloneList.add( entity.clone() );
		}
		return cloneList;
	}
	
	/**
	 * ����Cache���÷�����������л��沢ִ�������ռ���������һ���̶���Ӱ�����ܣ���Ҫ����ʹ�á�
	 */
	public static void reset() {
		synchronized ( CacheManager.class ) {
			managerMap.clear();
		}
		System.gc();
		logger.info( res.getResource( "CacheManager.reset.info.CacheManagerReset" ) );
	}
	
	/**
	 * ��ѯ������ָ��ʵ��
	 * @param entity ָ��ʵ��
	 * @return ��������û��ָ��ʵ�壨δ���У�������null����֮����ʵ��Ĳ�ѯ���
	 */
	@SuppressWarnings("unchecked")
	public List<E> query( E entity ) {
		List<E> resultList;
		Cache<E> cache;
		synchronized ( this ) {
			if ( !cacheMap.containsKey( entity ) ) return null;
			cache = cacheMap.get( entity );
			resultList = cache.getResultList();
		}
		cache.count();
		// ��¡����б���������¡���Է�ֹ�û��޸�ʵ������Ľ���б����
		return cloneList( resultList );
	}
	
	/**
	 * �򻺳�������һ��ʵ��
	 * ע�����ڴ���ţ��������ܻᱻ����
	 * @param entity ָ��ʵ��
	 */
	public void add( E entity, List<E> resultList, HashSet<Long> addressSet ) {
		
		if ( !testMemory() ) {
			// ���ڴ���ţ����Ƴ���ѯƵ����͵�ʵ��-����ӳ�䣬����������������գ����ڴ���Ȼ���ţ����������
			synchronized ( this ) {
				cacheMap.remove( Collections.min( cacheMap.values() ).getKeyEntity() );
			}
			System.gc();
			if ( !testMemory() ) {
				logger.info( res.getResource( "CacheManager.add.info.MemoryIsNotEnough" ) );
				return;
			}
		}
		synchronized ( this ) {
			cacheMap.put( entity, new Cache<E>( entity, resultList, addressSet ) );
		}
	}
	
	/**
	 * ���������еĵ�ַ��������Щ��ַ�Ļ��嶼�ѹ��ڣ�����ӳ������Ƴ�
	 * @param addressSet �ѱ������ĵ�ַ
	 */
	public void disuse( HashSet<Long> addressSet ) {
		Set<E> disuseSet = new HashSet<E>();
		synchronized ( this ) {
			for ( Cache<E> cache : cacheMap.values() ) {
				if ( !Collections.disjoint( addressSet, cache.getAddressSet() ) ) {
					disuseSet.add( cache.getKeyEntity() );
				}
			}
			for ( E entity : disuseSet ) {
				cacheMap.remove( entity );
			}
		}
	}
	
	/**
	 * �������л��壨����������ʵ��ʱ��
	 */
	public synchronized void disuseAll() {
		cacheMap.clear();
	}
}
