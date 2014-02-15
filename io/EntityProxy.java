package oxygen.io;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.annotation.Cachable;
import oxygen.io.cache.CacheManager;
import oxygen.io.transaction.OperationType;
import oxygen.io.transaction.RollbackFileWriter;
import oxygen.io.transaction.Transaction;


/**
 * <p>实体数据访问代理</p>
 * <p>此代理实际上是一个Facade，用于沟通实体和数据操作。在这之间，此代理还处理Cache的相关操作。</p>
 * @author 赖昆
 * @since 1.0, 2006-01-18
 * @version 1.0
 * @see Entity 实体
 * @see Handler 实体数据访问处理器
 * @see QueryHandler 实体查询处理器
 * @see InsertHandler 实体插入处理器
 * @see DeleteHandler 实体删除处理器
 * @see UpdateHandler 实体修改处理器
 * @see CacheManager Cache管理器
 * @see Transaction 事务
 */
@SuppressWarnings("unchecked")
public class EntityProxy {
	
	// 记录需要事务处理的线程
	private static final Map<Thread, Transaction> transactionThreadPool = new HashMap<Thread, Transaction>();

	// 此代理的关闭标志
	private static volatile boolean close = false;
	
	private EntityProxy() {}
	
	/**
	 * 代理实体查询。该实体不为null的字段将作为查询条件，null的字段不作为限制。
	 * 另外，也可以通过实体的{@link Entity#setSpecialNull(String)}方法设置作为查询条件的null字段。
	 * @param <E> 实体
	 * @param entity 要查询的实体
	 * @return 查询结果，即数据库中满足查询的同类实体列表；若数据库中没有满足查询条件的实体，则返回一个空列表；若查询由于抛出异常而失败，
	 * 返回null
	 */
	public static <E extends Entity<E>> List<E> query( E entity ) {
		if ( close ) return null;
		// 试图从Cache中得到查询结果
		boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
		if ( isCachable ) {
			List<E> result = CacheManager.getCacheManager( entity ).query( entity );
			if ( result != null ) return result;
		}
		// Cache没有命中，则在磁盘上查询
		Response<List<E>> response = new Response<List<E>>();
		new Thread( new QueryHandler( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response ) ).start();
		List<E> resultList = response.getResponse();
		// Cache中新加本次查询
		if ( isCachable ) {
			HashSet<Long> addressSet = response.getAddressSet();
			if ( addressSet != null ) {
				CacheManager.getCacheManager( entity ).add( entity, resultList, addressSet );
			}
		}
		return resultList;
	}
	
	/**
	 * 代理实体插入
	 * @param <E> 实体
	 * @param entity 要插入到数据库的实体
	 * @return 插入成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean insert( E entity ) {
		// 判断该操作是否属于一个事务
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.INSERT, entity );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new InsertHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// 因为新增加了实体，此Cache中的映射已全部过期。从Cache中去掉所有地址
			boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
			if ( result && isCachable ) {
				CacheManager.getCacheManager( entity ).disuseAll();
			}
			return result;
		}
	}

	/**
	 * 代理实体插入（带事务支持）
	 * @param <E> 实体
	 * @param entity 要插入到数据库的实体
	 * @param writer 回滚文件记录器
	 * @return 插入成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean insert( E entity, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new InsertHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// 因为新增加了实体，此Cache中的映射已全部过期。从Cache中去掉所有映射
		boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
		if ( result && isCachable ) {
			CacheManager.getCacheManager( entity ).disuseAll();
		}
		return result;
	}
	
	/**
	 * 代理实体删除
	 * @param <E> 实体
	 * @param entity 要删除的实体
	 * @return 删除成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean delete( E entity ) {
		// 判断该操作是否属于一个事务
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.DELETE, entity );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new DeleteHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// 因为修改了实体文件，此Cache中相关地址的映射已过期，废弃这部分映射
			boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
			if ( result && isCachable ) {
				HashSet<Long> addressSet = response.getAddressSet();
				if ( addressSet != null && addressSet.size() > 0 ) {
					CacheManager.getCacheManager( entity ).disuse( addressSet );
				}
			}
			return result;
		}
	}
	
	/**
	 * 代理实体删除（带事务支持）
	 * @param <E> 实体
	 * @param entity 要删除的实体
	 * @param writer 回滚文件记录器
	 * @return 删除成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean delete( E entity, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new DeleteHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// 因为修改了实体文件，此Cache中相关地址的映射已过期，废弃这部分映射
		boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
		if ( result && isCachable ) {
			HashSet<Long> addressSet = response.getAddressSet();
			if ( addressSet != null && addressSet.size() > 0 ) {
				CacheManager.getCacheManager( entity ).disuse( addressSet );
			}
		}
		return result;
	}
	
	/**
	 * 代理实体修改
	 * @param <E> 实体
	 * @param entitySrc 要修改的实体
	 * @param entityDest 修改后的实体
	 * @return 修改成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean update( E entitySrc, E entityDest ) {
		// 判断该操作是否属于一个事务
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.UPDATE, entitySrc, entityDest );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new UpdateHandler<E>( entitySrc, entityDest, LookupTable.getLookupTable( (Class<E>) entityDest.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// 因为修改了实体文件，此Cache中相关地址的映射已过期，废弃这部分映射
			boolean isCachable = entitySrc.getClass().getAnnotation( Cachable.class ) != null;
			if ( result && isCachable ) {
				HashSet<Long> addressSet = response.getAddressSet();
				if ( addressSet != null && addressSet.size() > 0 ) {
					CacheManager.getCacheManager( entitySrc ).disuse( addressSet );
				}
			}
			return result;
		}
	}
	
	/**
	 * 代理实体修改（带事务支持）
	 * @param <E> 实体
	 * @param entitySrc 要修改的实体
	 * @param entityDest 修改后的实体
	 * @return 修改成功返回true，反之返回false
	 */
	public static <E extends Entity<E>> boolean update( E entitySrc, E entityDest, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new UpdateHandler<E>( entitySrc, entityDest, LookupTable.getLookupTable( (Class<E>) entityDest.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// 因为修改了实体文件，此Cache中相关地址的映射已过期，废弃这部分映射
		boolean isCachable = entitySrc.getClass().getAnnotation( Cachable.class ) != null;
		if ( result && isCachable ) {
			HashSet<Long> addressSet = response.getAddressSet();
			if ( addressSet != null && addressSet.size() > 0 ) {
				CacheManager.getCacheManager( entitySrc ).disuse( addressSet );
			}
		}
		return result;
	}
	
	/**
	 * 开始一个事务
	 */
	public static void beginTransaction() {
		// 若该代理已关闭，则不接受事务
		if ( close ) return;
		Thread thread = Thread.currentThread();
		if ( !transactionThreadPool.containsKey( thread ) ) {
			transactionThreadPool.put( thread, new Transaction() );
		}
	}
	
	/**
	 * 提交一个事务
	 */
	public static void commit() {
		Thread thread = Thread.currentThread();
		// 提交后将其移除
		Transaction transaction = transactionThreadPool.remove( thread );
		if ( transaction != null ) {
			transaction.commit();
		}
	}
	
	/**
	 * 撤销一个事务（当事务尚未提交，可以使用这个方法撤销事务。若事务在提交过程中失败，不能使用这个方法撤销）
	 */
	public static void rollback() {
		transactionThreadPool.remove( Thread.currentThread() );
	}
	
	/**
	 * 关闭实体代理
	 */
	public static void close() {
		close = true;
	}
}
