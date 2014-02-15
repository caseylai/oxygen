package oxygen.util.i18n;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;

/**
 * <p>资源加载器，为类提供国际化支持</p>
 * @see ResourceLoaderProvider 资源加载器提供者
 * @author 赖昆
 * @since 1.0, 2006-12-16
 * @version 1.0
 */
public class ResourceLoader {
	
	// 资源映射表
	private final Map<String, String> map;
	
	// 资源文件名字
	private final String resourceFileName;
	
	// 资源加载器名字
	private final String name;
	
	/**
	 * 默认资源文件名
	 */
	public static final String defaultResourceBundle = "resource.properties";
	
	// 以指定的资源文件和名字构造资源加载器实例
	private ResourceLoader( InputStream is, String name, String resourceFileName ) throws ResourceLoadFailedException {

		Map<String, String> res = null;

		try {
			res = ResourceIO.load( is );
		} catch ( FileNotFoundException e ) {
			throw new ResourceLoadFailedException( "Cannot find resource file '" + resourceFileName + "'." );
		} catch ( IOException e ) {
			throw new ResourceLoadFailedException( "Failed to load resource file '" + resourceFileName + "', disk I/O error." );
		} catch ( IllegalArgumentException e ) {
			throw new ResourceLoadFailedException( e.getMessage() );
		}
				
		map = res;
		this.name = name;
		this.resourceFileName = resourceFileName;
	}
		
	/**
	 * 得到指定的资源，参数将嵌入资源并一起返回
	 * @param key 资源名
	 * @param args 参数
	 * @return 资源值
	 * @throws NoSuchResourceException 若资源（键名）在当前资源加载器及其全部父加载器中都没有找到
	 * @throws IllegalArgumentException 若参数个数少于资源要求的参数个数或转义符号'%'误用
	 */
	public String getResource( String key, String... args ) {
		
		String value = map.get( key );
		if ( value == null ) value = ResourceLoaderProvider.searchResourceInParent( this, key, args );
		if ( value == null ) throw new NoSuchResourceException( "Not found the resource '" + key + "' in the resource file '" + resourceFileName + "'." );
		
		try {
			value = format( value, args );
		} catch ( MissingFormatArgumentException e ) {
			throw new IllegalArgumentException( "The number of parameters is not enough for the key '" + key + "' in the resource file '" + resourceFileName + "'." );
		} catch ( UnknownFormatConversionException e ) {
			throw new IllegalArgumentException( "There is invalid '%' in the value '" + value + "' of the key '" + key + "' in the resource file '" + resourceFileName + "'." );
		}
		
		return value;
	}
	
	// 返回是否该资源加载器包含了指定的键
	boolean contains( String key ) {
		return map.containsKey( key );
	}
	
	// 返回资源加载器的名字
	String getName() {
		return name;
	}
	
	// 得到指定类名、指定资源文件的资源加载器实例
	static ResourceLoader newInstance( Class clazz, String resourceFileName ) {
		
		InputStream is = ResourceIO.search( ResourceIO.pathOf( clazz ), resourceFileName );
		if ( is == null ) throw new ResourceLoadFailedException( "Class '" + clazz.getName() + "' cannot find the resource file '" + resourceFileName + "'." );
		return new ResourceLoader( is, ResourceIO.pathOf( clazz ), resourceFileName );
	}
	
	// 得到指定类名的资源加载器实例
	static ResourceLoader newInstance( Class clazz ) {
		return newInstance( clazz, defaultResourceBundle );
	}

	// 将参数嵌入资源当中
	private static String format( String str, String... args ) {				
		return String.format( str, (Object[]) args );
	}
}
