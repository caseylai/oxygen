package oxygen.config;

/**
 * <p>Oxygen数据库配置文件的键枚举</p>
 * <p>键名是枚举常量名替换所有的"_"为"."并转为小写，
 * 如：DATABASE_ROOT的键名为"database.root"</p>
 * @author 赖昆
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
public enum ConfigKey {
	
	/**
	 * 数据库根目录位置
	 */
	DATABASE_ROOT( null ),
	
	/**
	 * 用户的实体jar文件
	 */
	ENTITY_JAR( null ),
	
	/**
	 * 数据存储的编码方式，默认为UTF-8
	 */
	ENCODE( "UTF-8" );

	// 配置文件中的键
	private String key;
	
	// 配置文件中键的默认值
	private String defaultValue;
	
	private ConfigKey( String defaultValue ) {
		this.defaultValue = defaultValue;
		key = name().replaceAll( "_", "." ).toLowerCase();
	}
	
	/**
	 * 得到配置文件中的键
	 * @return 配置文件中的键
	 */
	public String key() {
		return key;
	}
	
	/**
	 * 得到配置文件中键的默认值
	 * @return 配置文件中键的默认值
	 */
	public String defaultValue() {
		return defaultValue;
	}
}
