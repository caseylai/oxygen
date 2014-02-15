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
 * <p>ʵ�����ݷ��ʴ���</p>
 * <p>�˴���ʵ������һ��Facade�����ڹ�ͨʵ������ݲ���������֮�䣬�˴�������Cache����ز�����</p>
 * @author ����
 * @since 1.0, 2006-01-18
 * @version 1.0
 * @see Entity ʵ��
 * @see Handler ʵ�����ݷ��ʴ�����
 * @see QueryHandler ʵ���ѯ������
 * @see InsertHandler ʵ����봦����
 * @see DeleteHandler ʵ��ɾ��������
 * @see UpdateHandler ʵ���޸Ĵ�����
 * @see CacheManager Cache������
 * @see Transaction ����
 */
@SuppressWarnings("unchecked")
public class EntityProxy {
	
	// ��¼��Ҫ��������߳�
	private static final Map<Thread, Transaction> transactionThreadPool = new HashMap<Thread, Transaction>();

	// �˴���Ĺرձ�־
	private static volatile boolean close = false;
	
	private EntityProxy() {}
	
	/**
	 * ����ʵ���ѯ����ʵ�岻Ϊnull���ֶν���Ϊ��ѯ������null���ֶβ���Ϊ���ơ�
	 * ���⣬Ҳ����ͨ��ʵ���{@link Entity#setSpecialNull(String)}����������Ϊ��ѯ������null�ֶΡ�
	 * @param <E> ʵ��
	 * @param entity Ҫ��ѯ��ʵ��
	 * @return ��ѯ����������ݿ��������ѯ��ͬ��ʵ���б������ݿ���û�������ѯ������ʵ�壬�򷵻�һ�����б�����ѯ�����׳��쳣��ʧ�ܣ�
	 * ����null
	 */
	public static <E extends Entity<E>> List<E> query( E entity ) {
		if ( close ) return null;
		// ��ͼ��Cache�еõ���ѯ���
		boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
		if ( isCachable ) {
			List<E> result = CacheManager.getCacheManager( entity ).query( entity );
			if ( result != null ) return result;
		}
		// Cacheû�����У����ڴ����ϲ�ѯ
		Response<List<E>> response = new Response<List<E>>();
		new Thread( new QueryHandler( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response ) ).start();
		List<E> resultList = response.getResponse();
		// Cache���¼ӱ��β�ѯ
		if ( isCachable ) {
			HashSet<Long> addressSet = response.getAddressSet();
			if ( addressSet != null ) {
				CacheManager.getCacheManager( entity ).add( entity, resultList, addressSet );
			}
		}
		return resultList;
	}
	
	/**
	 * ����ʵ�����
	 * @param <E> ʵ��
	 * @param entity Ҫ���뵽���ݿ��ʵ��
	 * @return ����ɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean insert( E entity ) {
		// �жϸò����Ƿ�����һ������
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.INSERT, entity );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new InsertHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// ��Ϊ��������ʵ�壬��Cache�е�ӳ����ȫ�����ڡ���Cache��ȥ�����е�ַ
			boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
			if ( result && isCachable ) {
				CacheManager.getCacheManager( entity ).disuseAll();
			}
			return result;
		}
	}

	/**
	 * ����ʵ����루������֧�֣�
	 * @param <E> ʵ��
	 * @param entity Ҫ���뵽���ݿ��ʵ��
	 * @param writer �ع��ļ���¼��
	 * @return ����ɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean insert( E entity, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new InsertHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// ��Ϊ��������ʵ�壬��Cache�е�ӳ����ȫ�����ڡ���Cache��ȥ������ӳ��
		boolean isCachable = entity.getClass().getAnnotation( Cachable.class ) != null;
		if ( result && isCachable ) {
			CacheManager.getCacheManager( entity ).disuseAll();
		}
		return result;
	}
	
	/**
	 * ����ʵ��ɾ��
	 * @param <E> ʵ��
	 * @param entity Ҫɾ����ʵ��
	 * @return ɾ���ɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean delete( E entity ) {
		// �жϸò����Ƿ�����һ������
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.DELETE, entity );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new DeleteHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// ��Ϊ�޸���ʵ���ļ�����Cache����ص�ַ��ӳ���ѹ��ڣ������ⲿ��ӳ��
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
	 * ����ʵ��ɾ����������֧�֣�
	 * @param <E> ʵ��
	 * @param entity Ҫɾ����ʵ��
	 * @param writer �ع��ļ���¼��
	 * @return ɾ���ɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean delete( E entity, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new DeleteHandler<E>( entity, LookupTable.getLookupTable( (Class<E>) entity.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// ��Ϊ�޸���ʵ���ļ�����Cache����ص�ַ��ӳ���ѹ��ڣ������ⲿ��ӳ��
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
	 * ����ʵ���޸�
	 * @param <E> ʵ��
	 * @param entitySrc Ҫ�޸ĵ�ʵ��
	 * @param entityDest �޸ĺ��ʵ��
	 * @return �޸ĳɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean update( E entitySrc, E entityDest ) {
		// �жϸò����Ƿ�����һ������
		Thread thread = Thread.currentThread();
		if ( transactionThreadPool.containsKey( thread ) ) {
			return transactionThreadPool.get( thread ).addOperation( OperationType.UPDATE, entitySrc, entityDest );
		} else {
			if ( close ) return false;
			Response<Boolean> response = new Response<Boolean>();
			new Thread( new UpdateHandler<E>( entitySrc, entityDest, LookupTable.getLookupTable( (Class<E>) entityDest.getClass() ), response, null ) ).start();
			boolean result = response.getResponse();
			// ��Ϊ�޸���ʵ���ļ�����Cache����ص�ַ��ӳ���ѹ��ڣ������ⲿ��ӳ��
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
	 * ����ʵ���޸ģ�������֧�֣�
	 * @param <E> ʵ��
	 * @param entitySrc Ҫ�޸ĵ�ʵ��
	 * @param entityDest �޸ĺ��ʵ��
	 * @return �޸ĳɹ�����true����֮����false
	 */
	public static <E extends Entity<E>> boolean update( E entitySrc, E entityDest, RollbackFileWriter writer ) {
		Response<Boolean> response = new Response<Boolean>();
		new Thread( new UpdateHandler<E>( entitySrc, entityDest, LookupTable.getLookupTable( (Class<E>) entityDest.getClass() ), response, writer ) ).start();
		boolean result = response.getResponse();
		// ��Ϊ�޸���ʵ���ļ�����Cache����ص�ַ��ӳ���ѹ��ڣ������ⲿ��ӳ��
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
	 * ��ʼһ������
	 */
	public static void beginTransaction() {
		// ���ô����ѹرգ��򲻽�������
		if ( close ) return;
		Thread thread = Thread.currentThread();
		if ( !transactionThreadPool.containsKey( thread ) ) {
			transactionThreadPool.put( thread, new Transaction() );
		}
	}
	
	/**
	 * �ύһ������
	 */
	public static void commit() {
		Thread thread = Thread.currentThread();
		// �ύ�����Ƴ�
		Transaction transaction = transactionThreadPool.remove( thread );
		if ( transaction != null ) {
			transaction.commit();
		}
	}
	
	/**
	 * ����һ�����񣨵�������δ�ύ������ʹ��������������������������ύ������ʧ�ܣ�����ʹ���������������
	 */
	public static void rollback() {
		transactionThreadPool.remove( Thread.currentThread() );
	}
	
	/**
	 * �ر�ʵ�����
	 */
	public static void close() {
		close = true;
	}
}
