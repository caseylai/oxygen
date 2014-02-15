package oxygen.entity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 实体文件加载器。将指定的实体文件从外存以映射表的形式加载到内存。
 * @author 赖昆
 * @since 1.0, 2007-05-05
 * @version 1.0
 */
public class EntityReader {
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntityReader.class );
	
	private EntityReader() {}
	
	/**
	 * 加载指定的实体文件，并对其进行分析，返回实体名-实体类对象映射表
	 * @return 实体文件的实体名-实体类对象映射表
	 */
	public static Map<String, Class<? extends Entity>> read( File jar ) throws EntityReadFailedException {
		
		if ( !jar.isFile() ) {
			throw new EntityReadFailedException( res.getResource( "EntityReader.read.throw.ReadingEntityJarError", jar.getAbsolutePath() ) );
		}
		
		// 保存字节码
		final Map<String, byte[]> byteMap = new HashMap<String, byte[]>();
		// 实体类加载器，负责将实体类字节码加载到内存。这里将Entity的类加载器作为父类加载器。
		ClassLoader loader = new ClassLoader( Entity.class.getClassLoader() ) {
			// 寻找指定名字的类。覆盖了父类ClassLoader的方法。
			@Override
			public Class<?> findClass( String name ) {
				// 根据类名，在外部类的byteMap中寻找字节码
				byte[] b = byteMap.get( name );
				return defineClass( name, b, 0, b.length );
			}
		};

		// 解析jar文件。将其中实体类的类名和字节码放入映射表byteMap中，待实体类加载器寻找字节码使用
		try {
			JarFile jf = new JarFile( jar );
			for( Enumeration<JarEntry> e = jf.entries() ; e.hasMoreElements() ; ) {
				JarEntry je = e.nextElement();
				String name = je.getName();
				if ( name.endsWith( ".class" ) ) {
					name = name.substring( 0, name.lastIndexOf( "." ) ).replaceAll( "/", "." );
					InputStream is = jf.getInputStream( je );
					byte[] buf = new byte[(int) je.getSize()];
					is.read( buf );
					byteMap.put( name, buf );
					is.close();
				}
			}
		} catch ( IOException e ) {
			throw new EntityReadFailedException( res.getResource( "EntityReader.read.throw.DiskIOError", jar.getAbsolutePath() ) );
		}

		Map<String, Class<? extends Entity>> entityMap = new HashMap<String, Class<? extends Entity>>();		
		// 加载byteMap中的所有类，成功后放入entityMap
		for( String name : byteMap.keySet() ) {
			try {
				Class<? extends Entity> clazz = loader.loadClass( name ).asSubclass( Entity.class );
				String simpleName = name.substring( name.lastIndexOf( '.' ) + 1 );
				entityMap.put( simpleName, clazz );
			} catch ( ClassNotFoundException e ) {
				throw new EntityReadFailedException( res.getResource( "EntityReader.read.throw.CannotFindClass", name ) );
			}
		}
		
		return entityMap;
	}

}
