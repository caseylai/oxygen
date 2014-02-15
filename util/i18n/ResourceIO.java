package oxygen.util.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 资源文件I/O类
 * @author 赖昆
 * @since 1.0, 2006-12-19
 * @version 1.0
 */
public class ResourceIO {

	private ResourceIO() {}
	
	/**
	 * <p>将<b>自定义</b>属性文件加载为映射表</p>
	 * <p>在该属性文件中，只有'='是特殊字符。'='前的字符串被认为是键名，'='后的字符串被认为是键值。
	 * 若键名中含有'='，可以使用'\='进行转义。第一个有效的'='后的全部字符串被认为是键值</p>
	 * @param file 属性文件
	 * @return 加载完属性键值的Map
	 * @throws FileNotFoundException 在指定的路径上没有找到属性文件
	 * @throws IOException 读文件I/O错误
	 * @throws IllegalArgumentException 属性文件格式有误
	 * @see #load(URL)
	 * @see #load(InputStream)
	 */
	public static Map<String, String> load( File file ) throws FileNotFoundException, IOException, IllegalArgumentException {
		return read( new BufferedReader( new FileReader( file ) ) );
	}
	
	/**
	 * 从URL描述的配置文件中读取属性数据并加载为映射表
	 * @param url 配置文件的URL
	 * @return 加载完属性键值的Map
	 * @throws IOException 发生I/O错误
	 * @throws IllegalArgumentException 属性文件格式有误
	 * @see #load(File)
	 * @see #load(InputStream)
	 */
	public static Map<String, String> load( URL url ) throws IOException, IllegalArgumentException {
		return load( url.openStream() );
	}
	
	/**
	 * 从流中读取属性数据并加载为映射表
	 * @param is 流
	 * @return 加载完属性键值的Map
	 * @throws IOException 发生I/O错误
	 * @throws IllegalArgumentException 属性文件格式有误
	 * @see #load(File)
	 * @see #load(URL)
	 */
	public static Map<String, String> load( InputStream is ) throws IOException, IllegalArgumentException {
		return read( new BufferedReader( new InputStreamReader( is ) ) );
	}
	
	// 从reader中读取字符，并分析转换为映射表
	private static Map<String, String> read( BufferedReader reader ) throws IOException, IllegalArgumentException {
		
		Map<String, String> map = new HashMap<String, String>();
		
		String line;
		int lineCount = 0;
		while ( ( line = reader.readLine() ) != null ) {
			
			lineCount++;
			// 读到空行忽略掉，继续读下一行
			if ( ( line = line.trim() ).equals( "" ) ) continue;
			
			int pos, lastPos = 0;		
			
			do {
				pos = line.indexOf( "=", lastPos );
				lastPos = pos + 1;
			} while ( pos != -1 && line.charAt( pos - 1 ) == '\\' );
			
			// 没有找到'='
			if ( pos == -1 ) throw new IllegalArgumentException( "Missing '=' in line " + lineCount + ". Parsing failed." );
			
			String key = line.substring( 0, pos ).replaceAll( "\\\\=", "=" );
			String value = line.substring( pos + 1 );
			
			// 键名重复
			if ( map.containsKey( key ) ) throw new IllegalArgumentException( "Duplicate key '" + key + "' in line " + lineCount + ". Parsing failed." );
			
			map.put( key, value );
		}
		
		reader.close();
		return map;
	}
	
	/*
	 * 查找类clazz的包的路径
	 * 例如：
	 * pathOf( java.lang.Integer.class ) 返回"java/lang"
	 * pathOf( Test.class ) 返回"" （Test类没有包定义）
	 */
	static String pathOf( Class clazz ) {
		
		if ( clazz == null ) throw new NullPointerException( "The class for searching package path cannot be null" );
		String packageName = clazz.getPackage().getName();
		if ( packageName == null ) packageName = "";
		
		return packageName.replace( ".", "/" );
	}
	
	/*
	 * 搜索指定路径上存在的资源。参数由一个相对路径（例如："java/util"或"java"或""，必须不以"/"开始和结束）
	 * 和资源文件名组成。本方法首先在指定的路径目录上搜索资源，若其存在，则返回一个资源流；若不存在，则在其父目录
	 * 重试搜索……一直到根目录（相对路径）都没有找到，则返回null。
	 */
	static InputStream search( String path, String resourceFileName ) {
		
		if ( path == null ) throw new NullPointerException( "The path cannot be null." );
		if ( resourceFileName == null ) throw new NullPointerException( "The resource file name cannot be null." );

		// 若path不是空串且不是以斜杠结尾的话，在路径和文件名之间添加斜杠并连接成完整文件路径
		StringBuilder builder = new StringBuilder( path );
		if ( !"".equals( path ) && !path.endsWith( "/" ) ) {
			builder.append( '/' );
		}
		builder.append( resourceFileName );
		InputStream is = ResourceLocator.getResourceAsStream( builder.toString() );
		if ( is != null ) {
			return is;
		} else if ( "".equals( path ) ) {
			return null;
		} else {
			return search( ( path.indexOf( '/' ) == -1 ) ? "" : path.substring( 0, path.lastIndexOf( '/' ) ), resourceFileName );
		}
	}
}
