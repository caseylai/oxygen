package oxygen.util.i18n;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * <p>资源定位器</p>
 * <p>该资源定位器用于确定系统内资源的有效URL。说明：由于在某些情况下（如应用被部署为jar包随Java EE 服务器
 * 启动），类加载器被修改而导致资源无法定位，使用该资源定位器可以获得资源的有效URL。</p>
 * @author 赖昆
 * @since 1.0, 2007-05-28
 * @version 1.0
 */
public class ResourceLocator {

	/**
	 * 查找具有指定包内路径的资源，例如：
	 * oxygen/config/resource.properties表示oxygen.config包里的resource.properties资源文件
	 * @param path 指定包内路径的资源
	 * @return 若能够找到有效的资源，则返回资源的URL对象；若找不到资源，返回null
	 * @see #getResourceAsStream(String)
	 */
	public static URL getResource( String path ) {
		
		// 首先使用系统类加载器查找
		URL url = ClassLoader.getSystemResource( path );
		// 没有找到的话，尝试构造URL jar:file:<jar文件路径>!/<资源在包内的路径>
		if ( url == null ) {
			String jarPath = ResourceLocator.class.getProtectionDomain().getCodeSource().getLocation().toString();
			if ( jarPath.endsWith( ".jar" ) ) {
				try {
					StringBuilder builder = new StringBuilder( "jar:" );
					builder.append( jarPath );
					builder.append( "!/" );
					builder.append( path );
					url = new URL( builder.toString() );
				} catch ( Exception e ) {
				}
			}
		}
		
		return url;
	}
	
	/**
	 * 查找具有指定包内路径的资源，并按流返回
	 * @param path 指定包内路径的资源
	 * @return 若能够找到有效的资源，则返回资源的流；若找不到资源，返回null
	 * @see #getResource(String)
	 */
	public static InputStream getResourceAsStream( String path ) {
		URL url = getResource( path );
		try {
			if ( url != null ) {
				return url.openStream();
			}
		} catch ( IOException e ) {
		}
		return null;
	}
}
