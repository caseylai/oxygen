package oxygen.entity;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * ʵ�幩Ӧ�ߡ���ϵͳ�����󣬴˹�Ӧ�߻��ȡʵ��jar�ļ��е��ಢ�������ڴ��С�����Ҫʹ��ʵ�壬
 * ֻ������������provide�������ɡ�
 * @author ����
 * @since 1.0, 2006-12-22
 * @version 1.0
 */
public class EntityProvider {
	
	// ʵ����-�����ӳ���
	private static Map<String, Class<? extends Entity>> entityMap;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntityProvider.class );
	
	/**
	 * <p>��ʼ��ʵ���ṩ��</p>
	 * <p>�˷��������ݿ�ϵͳ������ʱ���ã��û�<b>��Ӧ��</b>�������������</p>
	 * @throws EntityReadFailedException ����ʼ��ʧ��
	 */
	public static void init() throws EntityReadFailedException {
		entityMap = EntityReader.read( new File( DatabaseContext.get( ConfigKey.ENTITY_JAR ) ) );
	}
	
	/**
	 * <p>�ṩָ���������������ʵ������Щ�඼��Entity�������</p>
	 * <p>���磺{@code User user = EntityProvider.provide("User");}
	 * �����User��һ��ʵ�壬��չ��Entity&lt;User&gt;</p>
	 * @param <E> ʵ��
	 * @param name ��������
	 * @return ָ�����ֵ���ʵ��
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Entity<E>> E provide( String name ) {
		
		// ����������ֲ����û�ʵ�������
		Class<? extends Entity> clazz = entityMap.get( name );
		// û���ҵ�������null
		if ( clazz == null ) return null;
		try {
			return (E) clazz.newInstance();
		} catch ( Exception e ){
			throw new IllegalStateException( res.getResource( "EntityProvider.provide.throw.CannotInstantiationEntity", name ) );
		}
	}
	
	/**
	 * ����ȫ��ʵ����
	 */
	public static Collection<String> names() {
		return entityMap.keySet();
	}
	
	/**
	 * ����ȫ����ʵ��������
	 */
	public static Collection<Class<? extends Entity>> entities() {
		return entityMap.values();
	}
}
