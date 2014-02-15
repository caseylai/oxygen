package oxygen.io;

/*
 * ��������ģ�鼼�����˵���� version 1.0
 * ��������ģ���ʵ��Ŀ���ʵ�ַ����Լ��������
 * 
 * һ��ʵ��Ŀ��
 * �������ݿ��ļ����Ӵ��������������ܽ���ȫ�������ڴ档�����ڴ��̴�ȡ���ڴ��ȡ�ٶȵľ޴���죬ȫ��ʹ�ô���I/O��ȡ����
 * ������Ӱ�����ܡ�Ϊ�ˣ�����������������������Ŀ���������ܵļ��ٴ���I/O������������I/O�����ܵ�Ӱ�콵����͡�
 * ��һ��ʵ����ԣ��������ֶ�Ӧ�������ڲ�ѯ���ֶΡ�����ѯ���ֶα�עΪ�����ֶΣ���ô���Ը��ֶ�Ϊ��������ʵ���ѯ��ʱ��
 * ���ȸ��ֶλ�ͨ��ɢ�к����γ�һ��long����ֵ��ͨ������ֵ���Դ�ӳ���ֱ�ӵõ�ʵ����ʵ���ļ��е�ƫ��������û�жԲ�ѯ�ֶ�
 * ��ע��������ô�������ò���������ʵ���ļ��е�ÿ��ʵ�壬�����Ƿǳ����µġ�
 * 
 * 
 * ����Ŀ¼�ṹ
 * �����ļ��Ǻ�׺��Ϊ".idx"���ļ���λ�����ݿ�data/index����Ŀ¼�¡�����·��������ʵ�������ֶ���������
 * ���磺ʵ��User�е�name�ֶ�Ϊһ�������ֶΣ���·��Ϊdata/index/User/name.idx
 * ��User�����ڰ�xxx.yyy.zzz��·��Ϊdata/index/xxx.yyy.zzz.User/name.idx
 * 
 * 
 * �����ļ��ṹ
 * �����ļ����ļ�ȫ���ɵ�������Ϊ16�ֽڵ�����(index)���ɣ���ˣ���һ��������ƫ����Ϊ0���ڶ���Ϊ16��������Ϊ32����
 * ��N��Ϊ16*(N-1)��ÿһ�������ַ�Ϊ��(key)��ֵ(value)�������������ĸ�8λ��ֵ���������ĵ�8λ��
 * ÿһ������ֵ�����Ա���Ϊһ��Java��long�ͻ����������Ͷ����������������ַ�������ɢ�к�������ó���
 * ��ֵ�ɴ�������ʵ����ʵ���ļ��е�ƫ���������������������������ַ��������ܿ��ٵ��ҵ�ʵ����ʵ���ļ��е�λ�ã�
 * ��������������ʵ���ļ����������˴���I/O������
 * ���磺
 * ʵ��User��name�ֶ�Ϊһ�������ֶΣ���һ��ʵ��(entity)��name�ֶ�ֵΪ"Susan"������ɢ�к�����
 * �������Ϊ"349082820474922L"������ʵ����ʵ���ļ��еĴ洢λ����3832920238L����ô��
 * ��ֵ��"349082820474922L,3832920238L"������һ��������ÿ����Ҫ��ѯ"Susan"ʱ�������ɢ��ֵ��
 * Ҳ���Ǽ�Ϊ"349082820474922L"����ѯ���ü���Ӧ��ֵΪ"3832920238L"��������ʵ���ļ���3832920238Lλ�ô��ҵ���Ӧ��ʵ�塣
 * ע�⣺
 * 1�����������ֶ��ǿ����ظ��ģ���ʵ���ļ��п����ж��ʵ���name�ֶζ�Ϊ"Susan"������������£�
 * ���ж��������ļ�����Ҫ�󡣷��ص�ֵ����һ�����ϡ������뿴���Ĳ��֡����ݽṹ����
 * 2�����൱���������£�����һ���ַ���������Ϊ"Julie"������ɢ��ֵ��"Susan"��ͬ����Ϊ349082820474922L��
 * ��ô������ѯ"Susan"ʱ����ͬʱ��ѯ��"Julie"�ĵ�ַ�����ص�ֵ����һ��ͬʱ����"Susan"��"Julie"�ļ��ϣ�
 * ��ˣ����ݵ�ַ��ʵ���ļ��в�ѯʱ��Ӧ�ö�ʵ��������ַ���������֤�������ڷ���������ĸ��ʷǳ��ǳ�С�����汾������������ķ�����
 * 
 * 
 * �ġ����ݽṹ
 * ��Ŀǰ����汾�У������ļ���׼��ȫ�����뵽�ڴ�ģ��������������ǣ�����¼�ǳ��࣬����Ҫ������ڴ�ռ䣬
 * 1MB�������ļ��ܹ����ɵ�������Ŀ��65536������ʵ���ļ�ƫ����ӳ���entityMap<Long, Object>�����ɼ��������ַ�����hashֵ����
 * ֵ��ʵ���ļ��е�ƫ��������ӳ���ϵ������ֵ�����ǵ�����һ��ֵ��Ҳ������һ�����ϣ����Է��ص�Object������
 * һ��Long����Ҳ������һ��HashSet<Long>����
 * ���磺
 * ��ѯһ�������ֶ�nameΪ"Susan"��ʵ�壬������һ��ʵ���name�ֶ�Ϊ"Susan"����ô�����صĽ������ʵ����ʵ���ļ���
 * ƫ������Long�������ж��ʵ������������򷵻�һ��HashSet<Long>���������еķ�������ʵ����ʵ���ļ��е�ƫ������
 * ��ǿ��һ�㣺
 * ����ɢ�г�ͻ�Ĵ��ڣ����ص�ֵ���߼��϶����Ǿ��Կɿ��ģ���Ҫ��ʵ���ļ��ж������ֶν�����֤��
 * 
 * 
 * �塢��ȡ����
 * ʵ��������ֶκ������ļ���һһ��Ӧ�ġ���User���name�ֶ�Ϊ�����ֶΣ���ô���Ի��ڸ���������ɾ�����Ϊ����
 * data/index/User/name.idx�Ͻ��У��μ���һ���֡�Ŀ¼�ṹ�������ļ�data/index/User/name.idxҲ�Ǹ�������Ψһ���ݡ�
 * ��һ�δ�ȡ����ʧ��ʱ��Ӧ����������Ϣд�뵽��־�ļ������ṩ��������ϸ����Ϣ��ÿһ��������Ӧ�÷���һ����־��
 * �����жϲ����Ƿ�ɹ���ɡ�
 * 
 * 1�������ļ��Ķ�ȡ
 * ��Ŀǰ����汾�У������ļ�����ȫ��װ���ڴ棬��ˣ��������ڴ���������⡣����I/O����������ļ��������ֳ�16�ֽڵ�һ����������
 * ������ʵ���ļ�ƫ����ӳ���entityMap<Long, Object>�У��������������û���ظ�����ô�������ͽ���<Long, Long>��ʽ����֮��
 * �������ͽ���<Long, HashSet<Long>>����ʽ��Ϊ���ڽ�������ɾ��Ĺ���������һ�������ļ�ƫ����ӳ���
 * indexMap<Long, Integer>������֮����û��ʹ��Long����Integer���ǻ��������ļ�������4GB��һ�����裬
 * 4GB�������ļ��ܹ�����Լ2.7������¼����������汾�������ļ�ȫ��װ���ڴ棬��ʹ�ڸ߶˷������ϣ�һ��װ��4GB����������Դ��
 * ����Ҳ�Ǿ��˵ģ�����ʹ��Integer�㹻�������ӳ������ڱ���ʵ���ļ���ʵ��ƫ������ʵ���и��ֶ������������ļ���ƫ������ӳ�䡣
 * ���磺
 * ʵ��User����һ�������ֶ�name���������ļ�data/index/User/name.idx��ĳһ��ƫ����Ϊ7485948������Ϊ
 * ��ֵ��"349082820474922L,3832920238L"����ô�����˽��ü�ֵ�Լ��뵽entityMap�У����⻹��Ҫ��
 * ��ֵ��"3832920238L, 7485948"���뵽indexMap�С��������ĺô��ǣ���ɾ��ʵ���ļ���ƫ����Ϊ3832920238L��ʵ��ʱ��
 * ���Է�����������ļ�ƫ����Ϊ7485948�ĵط�ɾ�����������������ǰ����������ļ���Ѱ�Ҹ�ʵ���Ӧ��������
 * ���⣬�ڶ�������ʱ��������ļ���ֵ��Ϊ0L����ô������������һ����������������������ɾ����û������������������ġ�
 * ������������һ����λ����freeSet<Integer>�У���ִ����Ӳ���ʱ������ѡ����뵽��λ�����еĿ�λ�У�
 * ɾ�������У���ɾ�������������ļ��е�ƫ����Ҳ������ӵ���λ�����С�
 * 
 * 2������(insert)����
 * ����Ѱ�������ļ����Ƿ���ɾ�����������µĿ�λ������һ�������������ļ����м�ĳ��������ɾ�������µĿ�λ����
 * �����λ�ɿ�λ����freeSet<Integer>�ṩ������λ�����ܹ��ṩһ����λ����ô����������д�������λ���У�
 * ����λ�����޷��ṩ��λ����ô����������д�뵽�ļ�ĩβ����д�������������ļ��ɹ�����entityMap��indexMap�������µ�����ӳ�䡣
 * �������κ�ԭ��д���������ļ��Ĳ���ʧ�ܣ���ô������entityMap��indexMap�����µ�����ӳ�䡣
 * 
 * 3����ѯ(query)����
 * ֱ����entityMap�в��ҡ����ؿյ�HashSet<Long>���󣬱�ʾû��������������ݿ��в����ڴ����������ʵ�壨��һ����ȷ���ģ���һ��ʵ�屻����
 * �����ݿ�ʱ�������е������ֶζ�������Ϊ���������뵽�����ļ��У������������ݵ�HashSet<Long>����
 * ��ʾ�ж��������ѯ���������HashSet<Long>��������������������ʵ����ʵ���ļ��е�ƫ������
 * 
 * 4��ɾ��(delete)����
 * ɾ�������Ĳ�����������ʵ���ɾ��ʱ�������ڶ�ȡ����ʱ������������ƫ����ӳ���indexMap����˶Բ�ѯ����ƫ�����ṩ�˺ܴ�ķ��㡣
 * ɾ��ʵ��ʱ����Ȼ��ͨ����ѯ�����õ�ʵ����ʵ���ļ��е�ƫ��������ƫ����ͨ��indexMapӳ�䵽ʵ�������������ļ���ƫ������
 * �ҵ���������ƫ���������ܷ����ɾ��������������ֵ��Ϊ0L�������ļ���ɾ�������ɹ��󣬽�������ƫ������ӵ���λ����
 * freeSet�У��ȴ���������ʱ���Բ��뵽�����λ��
 * 
 * 5���޸�(update)����
 * Ҫ�޸���������Ҫ��ѯ����������ȷ���������������ļ��е�ƫ������Ȼ�����ƫ����д���µļ���ֵ�������ļ��޸ĳɹ���Ҫ����
 * entityMap��indexMap���������ļ��޸�ʧ�ܣ��򲻶�entityMap��indexMap�����κ��޸ġ�
 * 
 * 
 * �������̡߳��ع������ͱ�������
 * �������Ĳ���������������������ɵģ����Ҷ������ͬһ�������߳�ʹ��ͬһ����������������ˣ����������������Ƕ��̰߳�ȫ�ġ�
 * �����������б��������ļ��м�״̬��ӳ���ͼ��ϵ�ҲӦ�����̰߳�ȫ�ġ�
 * ����΢����Ĵ��ڣ��������Ҳ����ʹ�ûع������������й�΢����ͻع������ĸ����ο�Handler���еġ�ʵ�����ݷ��ʴ���ģ��
 * �������˵���顱����������΢������ʹ�ö�����ʵ�����ݷ��ʴ������еĻع����������ģ�һ���ع����б�ʾһ��΢���񡣵�����
 * ���뵽�����ļ��ɹ�������������������һ��΢����Ļ�����ô��������������Ч����Ϊ����û�н�������������������һ��
 * ���Իָ����������������ع��ಢ���뵽�ع������У�Ȼ������һ������������ŵ������������ı�����������ӳ����У��ɻع�
 * ������ӳ�䡣��΢����ɹ���ɣ������������������������Ч����ʧ�ܣ�������������ɾ������������ļ����޸�Ҳ��ͨ���ع�
 * ���ָ�������������������һ��΢����ʱ����ô��������ǡ����ɡ��ģ�ֻҪ�ɹ�д�뵽�����ļ�����������Ч��
 * �����ᵽ��һ���������������ĸ����������д�뵽�ļ�����΢������δ���������Ա����д���ļ����������б������Ա�������
 * �߳���΢����û�н���������¾Ͷ����������޸ģ�������ݵĲ�һ�¡����������б���������������һ��΢�����ǰ���½��еģ�
 * �������޸������ļ�֮��΢�������֮ǰ�����������ڱ���״̬������������ȷ�����������ᱻ�κ��߳����޸ģ�����������
 * ���б������̣߳������������в���ǰ��Ӧ�õ���������������waitIfProtected����������������ڱ���״̬����ô�͵ȴ���
 * ����״̬������
 * �����Ļع������ǻ���ʵ��ع������ģ��ο�Handler��ġ�ʵ�����ݷ��ʴ���ģ�鼼�����˵���顱�Ļع��������ֺͶ��̲߳��ֿ���
 * ��ø���������⡣
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * <p>����������</p>
 * <p>�����������ṩ��{@link Handler ʵ�����ݷ��ʴ�����}���������Ľӿڡ�ʵ���������������൱��
 * {@link IndexLoader ����������}�Ĵ���ÿһ���������Ĳ������󶼽�����������������ɡ�</p>
 * @author ����
 * @since 1.0, 2007-01-10
 * @version 1.0
 * @see Handler ʵ�����ݷ��ʴ�����
 * @see IndexLoader ����������
 */
public class IndexManager {
	
	// ������������ӳ���
	private static final Map<Field, IndexLoader> loaderMap = new ConcurrentHashMap<Field, IndexLoader>();
	
	// ΢����-��������������ӳ�����ʾ΢�����漰��������������ÿ��΢������һ����֮��Ӧ�Ļع����б�ʾ��
	private static final Map<MicroTransaction, Collection<IndexLoader>> transactionMap = new ConcurrentHashMap<MicroTransaction, Collection<IndexLoader>>();
	
	private static final Logger logger = Logger.getLogger( IndexManager.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( IndexManager.class );
	
	// ��������ֶζ�Ӧ������������
	private synchronized static IndexLoader getLoader( Field field ) {
		
		// ���������ֶ�û�ж�Ӧ����������������������һ��������ŵ�����������ӳ���
		IndexLoader loader = loaderMap.get( field );
		if ( loader == null ) {
			try {
				loader = new IndexLoader( field );
				loaderMap.put( field, loader );
			} catch ( IOException e ) {
				logger.warning( res.getResource( "IndexManager.getLoader.warning.CannotOpenIndexFile", field.getDeclaringClass().getSimpleName(), field.getName() ) );
				return null;
			}
		}

		return loader;
	}
	
	// ��������������һ��΢����ʱ����¼��΢�����漰�����������������Ա�΢����������ɹ���ʧ�ܣ�ʱ�ͷ�
	private static void preHandleTransaction( MicroTransaction transaction, IndexLoader loader ) {

		if ( transaction != null ) {
			Collection<IndexLoader> c = transactionMap.get( transaction );
			if ( c == null ) {
				c = new HashSet<IndexLoader>();
				transactionMap.put( transaction, c );
			}
			if ( !c.contains( loader ) ) {
				c.add( loader );
			}
		}
	}

	/**
	 * ����һ������
	 * @param field ��������ʵ���ֶ�
	 * @param key ʵ���ֶε��ַ���ֵ
	 * @param address ʵ���ַ
	 * @param transaction ΢������������������������һ��΢������Ϊnull��
	 * @return �ɹ���������������true����֮����false
	 */
	public static boolean insert( Field field, String key, long address, MicroTransaction transaction ) {

		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.insert( IndexHasher.hash( key ), address, transaction );
	}
	
	/**
	 * ��ѯ����
	 * @param field ��������ʵ���ֶ�
	 * @param key ʵ���ֶε��ַ���ֵ
	 * @return ����ѯ�ɹ����������������ѯ������ʵ���ַ��û�в�ѯ������������ʵ�巵��һ���ռ��ϣ�����ѯʧ�ܣ�����null
	 */
	public static HashSet<Long> query( Field field, String key ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) {
			logger.warning( res.getResource( "IndexManager.query.warning.CannotLoadIndex", field.getDeclaringClass().getSimpleName(), field.getName() ) );
			throw new IllegalStateException( res.getResource( "IndexManager.query.warning.CannotLoadIndex", field.getDeclaringClass().getSimpleName(), field.getName() ) );
		}
		return loader.query( IndexHasher.hash( key ) );
	}
	
	/**
	 * ɾ��һ������
	 * @param field ��������ʵ���ֶ�
	 * @param address ʵ���ַ
	 * @param transaction ΢������ɾ����������������һ��΢������Ϊnull��
	 * @return �ɹ�ɾ������������true����֮����false
	 */
	public static boolean delete( Field field, long address, MicroTransaction transaction ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.delete( address, transaction );
	}
	
	/**
	 * �޸�һ������
	 * @param field ��������ʵ���ֶ�
	 * @param key ʵ���ֶε��ַ���ֵ
	 * @param address ʵ���ַ
	 * @param transaction ΢�������޸���������������һ��΢������Ϊnull��
	 * @return �ɹ��޸�����������true����֮����false
	 */
	public static boolean update( Field field, String key, long address, MicroTransaction transaction ) {
		
		IndexLoader loader = getLoader( field );
		if ( loader == null ) return false;
		
		preHandleTransaction( transaction, loader );
		
		return loader.update( IndexHasher.hash( key ), address, transaction );
	}
	
	// ���΢�����ж������ı���
	static void unprotect( MicroTransaction transaction, boolean isSuccessful ) {
		
		Collection<IndexLoader> c = transactionMap.get( transaction );
		if ( c != null ) {
			for ( IndexLoader loader : c ) {
				loader.unprotect( transaction.getKeepsake(), isSuccessful );
			}
			transactionMap.remove( transaction );
		}
	}
	
	/**
	 * ��ȫ�ر���������������
	 */ 
	public synchronized static void close() {

		// ���ιر�����������
		for ( IndexLoader loader : loaderMap.values() ) {
			loader.close();
		}
		loaderMap.clear();
	}
}
