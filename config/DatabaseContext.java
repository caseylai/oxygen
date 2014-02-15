package oxygen.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * 数据库上下文，维持数据库所需的配置信息
 * @author 赖昆
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
public class DatabaseContext {
	
	// 数据库上下文的映射表
	private static Map<ConfigKey, String> propertiesMap;

	private DatabaseContext() {}

	/**
	 * 取上下文中键为key的值
	 * @param key 键
	 * @return key对应的值。若key不在上下文中，返回null
	 */
	public static String get( ConfigKey key ) {
		return propertiesMap.get( key );
	}
	
	// 将配置文件中读出的属性映射表注册到上下文
	static void register( Map<String, String> configMap ) throws IllegalArgumentException {
		
		Map<ConfigKey, String> map = new HashMap<ConfigKey, String>();
		
		for( ConfigKey key : ConfigKey.values() ) {
			String value = configMap.get( key.key() );
			if ( value == null ) value = key.defaultValue();
			if ( value == null ) {
				throw new IllegalArgumentException( key.key() );
			} else {
				map.put( key, value );
			}
		}
		
		// 返回一个不可修改的映射表
		propertiesMap = Collections.unmodifiableMap( map );
	}
}
