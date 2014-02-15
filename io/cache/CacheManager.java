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
 * <p>缓冲管理器<p>
 * <p>缓冲管理器是按实体类型划分的，一个实体类型的缓冲管理器负责处理一种类型实体的缓冲。每个缓冲管理器都维护了
 * 一张实体-缓冲的映射表，当查询的实体存在于该映射表中时，就可以直接返回实体的查询结果集而不需要到外存中查询，
 * 极大地提高了查询性能。但是当实体文件改变时，映射表中部分或全部缓冲将过期，这时候缓冲将被废弃，以保持缓冲
 * 中数据和外存的同步。</p>
 * <p>当实体经常被查询而较少被修改时，应该使用缓冲以提高查询性能；由于维护缓冲也有一定性能开销，当实体较少查询
 * 而经常被修改时，不应使用缓冲。</p>
 * @author 赖昆
 * @since 1.0, 2007-05-12
 * @version 1.0
 * @param <E> 实体
 */
public class CacheManager<E extends Entity<E>> {
	
	// 实体类类型-缓冲管理器映射表
	private static final Map<Class<? extends Entity>, CacheManager<? extends Entity>> managerMap = new HashMap<Class<? extends Entity>, CacheManager<? extends Entity>>();
	
	// 实体-缓冲映射表
	private final Map<Entity<E>, Cache<E>> cacheMap = new HashMap<Entity<E>, Cache<E>>();
	
	private static final Logger logger = Logger.getLogger( CacheManager.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( CacheManager.class );
	
	private CacheManager() {}
	
	/**
	 * 得到与指定实体类型匹配的缓冲管理器。若指定的实体类型找不到匹配的管理器，就新创建一个
	 * @param <E> 实体
	 * @param entity 指定实体
	 * @return 与指定实体类型相匹配的缓冲管理器
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
	 * 返回Cache类型数
	 */
	public static int getCacheManagerCount() {
		return managerMap.size();
	}
	
	/**
	 * 返回Cache中缓存的结果集总数
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
	
	// 对内存的使用进行监视。返回true表示内存没有出现紧张
	private static boolean testMemory() {
		Runtime rt = Runtime.getRuntime();
		// 空闲内存大于最大内存的1/4时内存没有出现紧张
		return rt.freeMemory() + ( rt.maxMemory() - rt.totalMemory() ) > .25 * rt.maxMemory();
	}
	
	// 克隆指定的列表（深层克隆，同时克隆其中的实体）
	private static <E extends Entity<E>> List<E> cloneList( List<E> list ) {
		List<E> cloneList = new LinkedList<E>();
		for ( E entity : list ) {
			cloneList.add( entity.clone() );
		}
		return cloneList;
	}
	
	/**
	 * 清理Cache。该方法将清空所有缓存并执行垃圾收集，可能在一定程度上影响性能，需要谨慎使用。
	 */
	public static void reset() {
		synchronized ( CacheManager.class ) {
			managerMap.clear();
		}
		System.gc();
		logger.info( res.getResource( "CacheManager.reset.info.CacheManagerReset" ) );
	}
	
	/**
	 * 查询缓冲中指定实体
	 * @param entity 指定实体
	 * @return 若缓冲中没有指定实体（未命中），返回null；反之返回实体的查询结果
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
		// 克隆结果列表，必须深层克隆，以防止用户修改实体带来的结果列表更改
		return cloneList( resultList );
	}
	
	/**
	 * 向缓冲中增加一个实体
	 * 注：若内存紧张，则插入可能会被放弃
	 * @param entity 指定实体
	 */
	public void add( E entity, List<E> resultList, HashSet<Long> addressSet ) {
		
		if ( !testMemory() ) {
			// 若内存紧张，则移除查询频率最低的实体-缓冲映射，并建议进行垃圾回收，若内存仍然紧张，则放弃插入
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
	 * 废弃集合中的地址，含有这些地址的缓冲都已过期，将从映射表中移除
	 * @param addressSet 已被废弃的地址
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
	 * 废弃所有缓冲（当插入了新实体时）
	 */
	public synchronized void disuseAll() {
		cacheMap.clear();
	}
}
