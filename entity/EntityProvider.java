package oxygen.entity;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 实体供应者。当系统启动后，此供应者会读取实体jar文件中的类并保存在内存中。如需要使用实体，
 * 只需以类名调用provide方法即可。
 * @author 赖昆
 * @since 1.0, 2006-12-22
 * @version 1.0
 */
public class EntityProvider {
	
	// 实体名-类对象映射表
	private static Map<String, Class<? extends Entity>> entityMap;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntityProvider.class );
	
	/**
	 * <p>初始化实体提供者</p>
	 * <p>此方法由数据库系统在启动时调用，用户<b>不应该</b>调用这个方法。</p>
	 * @throws EntityReadFailedException 若初始化失败
	 */
	public static void init() throws EntityReadFailedException {
		entityMap = EntityReader.read( new File( DatabaseContext.get( ConfigKey.ENTITY_JAR ) ) );
	}
	
	/**
	 * <p>提供指定完整类名的类的实例。这些类都是Entity类的子类</p>
	 * <p>例如：{@code User user = EntityProvider.provide("User");}
	 * 这里的User是一个实体，扩展了Entity&lt;User&gt;</p>
	 * @param <E> 实体
	 * @param name 完整类名
	 * @return 指定名字的类实例
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Entity<E>> E provide( String name ) {
		
		// 按传入的名字查找用户实体类对象
		Class<? extends Entity> clazz = entityMap.get( name );
		// 没有找到，返回null
		if ( clazz == null ) return null;
		try {
			return (E) clazz.newInstance();
		} catch ( Exception e ){
			throw new IllegalStateException( res.getResource( "EntityProvider.provide.throw.CannotInstantiationEntity", name ) );
		}
	}
	
	/**
	 * 返回全部实体名
	 */
	public static Collection<String> names() {
		return entityMap.keySet();
	}
	
	/**
	 * 返回全部的实体类类型
	 */
	public static Collection<Class<? extends Entity>> entities() {
		return entityMap.values();
	}
}
