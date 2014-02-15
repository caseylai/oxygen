package oxygen.util.i18n;

import java.util.Map;
import java.util.HashMap;

/**
 * <p>资源加载器工厂</p>
 * <p>------------ 命名空间：</p>
 * <p>资源加载器工厂用以提供带命名空间支持的资源加载器。通常，资源加载器有一个名字（匿名资源加载器除外），
 * 此名字是以"/"字符隔开，有和类所在包相同层次的划分的字符串。</p>
 * <p>例如：</p>
 * <p>类oxygen.util.i18n.ResourceLoader持有的资源加载器名字为"oxygen/util/i18n"。</p>
 * <p>若类没有包定义，如类Test没有声明包，那么它持有的资源加载器名字为""（空字符串）。</p>
 * <p>资源加载器是有层次的，它们靠名字来区分。前缀相同，较短的名字的资源加载器是较长名字的父资源加载器，
 * 例如：</p>
 * <p>名字为"oxygen/util"的加载器是名字为"oxygen/util/i18n"的加载器的父加载器，
 * 而名字为"oxygen"的加载器又是"oxygen/util"的父加载器；最后""（空字符串）是所有加载器的父加载器。</p>
 * <p>有一类资源加载器是比较特殊的，匿名资源加载器的名字为null，它们不受命名空间的约束。</p>
 * <p>------------ 继承特性：</p>
 * <p>资源加载器是可以继承的，子加载器拥有父加载器的所有资源。所以在某些场合，可以将父加载器的一部分作一些小修改，
 * 再作为子加载器覆盖父加载器。这样，子加载器回被优先访问，当被要求子加载器所不具有的资源时，回到父加载器中寻找，
 * 一直到根加载器</p>
 * <p>------------ 使用方法：</p>
 * <p>假设要为类oxygen.config.DatabaseConfig加入国际化支持，首先将资源加载器定义为DatabaseConfig的最终静态类成员：</p>
 * <p>{@code private static final ResourceLoader res = ResourceLoaderProvider.provide(DatabaseConfig.class);}</p>
 * <p>然后在包oxygen.config下撰写国际化文件resource.properties。这个properties属性文件也使用"key=value"的形式书写，
 * 一行一个记录，与Java标准properties文件不同的是，可以在文件中使用本地默认的编码格式。只有等号'='是特殊字符，若键key中需要含有等号，
 * 则必须在等号前加反斜杠字符'\'表示转义。如希望key="if.a=b"，那么，在文件中必须写为"if.a\=b"。有效等号后面的全部字符串都被认为是键值，
 * 如一条记录：</p>
 * <p>hello\=world\=!=Hello,\==World=!</p>
 * <p>其中key="hello=world=!"，value="Hello,\==World=!"</p>
 * <p>若文件中某条记录缺少等号或多条记录的键名重复，解析过程会抛出IllegalArgumentException。</p>
 * <p>资源文件的名字也可以改变，若资源文件名改为language.properties，那么引用此资源的类中将资源加载器的声明改为（还是以DatabaseConfig为例）：</p>
 * <p>{@code private static final ResourceLoader res = ResourceLoaderProvider.provide(DatabaseConfig.class, "language.properties");}</p>
 * <p>"resource.properties"是默认资源文件名</p>
 * <p>要引用资源，使用ResourceLoader的getResource()方法，这个方法的签名是getResource(String key, String... args)，
 * 所以可以在资源中引入参数：</p>
 * <p>在资源文件中有这样一条记录</p>
 * <p>application.hello=%s好，%s！</p>
 * <p>执行getResource("application.hello","早上","Susan")，将会返回"早上好，Susan！"。</p>
 * <p>'%s'表示一个参数的占位符（s必须小写）。</p>
 * <p>'%%'是对'%'的转义。<b><font color=red>注意：若键值串中含有单独的百分号'%'，而不与后面的字符结合解释为转义，
 * 那么必须改写为'%%'，否则可能引起参数匹配异常。</font></b></p>
 * <p>例如：</p>
 * <p>某条记录：you.are.right=you're 100%% right (不能写为you're 100% right)</p>
 * <p></p>
 * <p>若想表达'%%s'（第一个百分号表示单独的百分号，后面的'%s'表示参数，那么必须写为'%%%s'）</p>
 * @author 赖昆
 * @since 1.0, 2006-12-17
 * @version 1.0
 */
public final class ResourceLoaderProvider {
	
	/**
	 * 资源加载器映射表
	 */
	private static final Map<String, ResourceLoader> map = new HashMap<String, ResourceLoader>();
	
	/**
	 * 按参数类的包和资源文件名构造带命名空间的资源加载器。若该加载器的命名空间已经被注册，则直接返回该加载器
	 * @param clazz 参数类
	 * @param resourceFileName 资源文件名
	 * @return 带命名空间的资源加载器
	 */
	public static ResourceLoader provide( Class clazz, String resourceFileName ) {
		
		String resourceLoaderName = ResourceIO.pathOf( clazz );
		
		if ( map.containsKey( resourceLoaderName ) ) {
			return map.get( resourceLoaderName );
		}
		
		ResourceLoader resourceLoader = ResourceLoader.newInstance( clazz, resourceFileName );
		map.put( resourceLoaderName, resourceLoader );
		
		return resourceLoader;
	}
	
	/**
	 * 按参数类的包和{@linkplain ResourceLoader#defaultResourceBundle 默认资源文件名}构造带命名空间的资源加载器。若该加载器的命名空间已经被注册，则直接返回该加载器
	 * @param clazz 参数类
	 * @return 带命名空间的资源加载器
	 */
	public static ResourceLoader provide( Class clazz ) {
		return provide( clazz, ResourceLoader.defaultResourceBundle );
	}	

	/**
	 * <p>在子资源加载器的父资源加载器中寻找键，若找到则返回该键值对应的资源（包含参数），未找到返回null</p>
	 * <p>此方法将在子资源加载器<b>所有</b>的父资源加载器中查找键，若一直到根资源加载器也未查找到键，则返回null</p>
	 * @param child 子资源加载器
	 * @param key 键
	 * @param args 参数
	 * @return 若找到返回键值，未找到返回null
	 * @throws NullPointerException 若子资源加载器child或键key为null
	 */
	static String searchResourceInParent( ResourceLoader child, String key, String... args ) {
		
		if ( child == null ) throw new NullPointerException( "The child resource loader cannot be null." );
		if ( key == null ) throw new NullPointerException( "The key for searching in parent resource bundle cannot be null." );
		
		ResourceLoader parent = getParent( child );
		if ( parent == null ) {
			return null;
		} else if ( parent.contains( key ) ){
			return parent.getResource( key, args );
		} else {
			return searchResourceInParent( parent, key, args );
		}
	}
		
	/**
	 * <p>从资源加载器得到其最近的父资源加载器</p>
	 * <p>以下情况返回null：</p>
	 * <ul><li>子资源加载器是匿名的。匿名的资源加载器没有自己的命名空间，不划分到资源加载器的分级范围</li>
	 * <li>子资源加载器是当前命名空间中最高层的资源加载器。当子资源加载器拥有""空字符串作为名字时，
	 * 它居于命名空间的最高层，是所有其它资源加载器的父资源加载器，但其本身没有父资源加载器</li>
	 * <li>父资源加载器尚未注册到此ResourceLoaderProvider</li>
	 * </ul>
	 * @param child 子资源加载器
	 * @return 查找成功返回父资源加载器，失败返回null
	 */
	private static ResourceLoader getParent( ResourceLoader child ) {
	
		String childName = child.getName();
		
		if ( childName == null || "".equals( childName ) ) return null;
		
		return searchParent( childName );
	}
	
	/**
	 * <p>在命名空间中寻找名为childName的资源加载器的父加载器</p>
	 * <p>若找到，则返回此资源加载器；若没有找到，则查找其更高层的资源加载器……一直到根资源加载器。
	 * 若根资源加载器也没有找到，则返回null</p>
	 * @param childName 资源加载器名字
	 * @return 父资源加载器
	 */
	private static ResourceLoader searchParent( String childName ) {

		if ( childName.indexOf( "/" ) == -1 ) {
			return map.get( "" );
		} else {			
			String parentName = childName.substring( 0, childName.lastIndexOf( "/" ) );
			ResourceLoader parent = map.get( parentName );
			if ( parent != null ) {
				return parent;
			} else {
				return searchParent( parentName );
			}
		}		
	}
}
