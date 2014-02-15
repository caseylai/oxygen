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
 * ʵ���ļ�����������ָ����ʵ���ļ��������ӳ������ʽ���ص��ڴ档
 * @author ����
 * @since 1.0, 2007-05-05
 * @version 1.0
 */
public class EntityReader {
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( EntityReader.class );
	
	private EntityReader() {}
	
	/**
	 * ����ָ����ʵ���ļ�����������з���������ʵ����-ʵ�������ӳ���
	 * @return ʵ���ļ���ʵ����-ʵ�������ӳ���
	 */
	public static Map<String, Class<? extends Entity>> read( File jar ) throws EntityReadFailedException {
		
		if ( !jar.isFile() ) {
			throw new EntityReadFailedException( res.getResource( "EntityReader.read.throw.ReadingEntityJarError", jar.getAbsolutePath() ) );
		}
		
		// �����ֽ���
		final Map<String, byte[]> byteMap = new HashMap<String, byte[]>();
		// ʵ���������������ʵ�����ֽ�����ص��ڴ档���ｫEntity�����������Ϊ�����������
		ClassLoader loader = new ClassLoader( Entity.class.getClassLoader() ) {
			// Ѱ��ָ�����ֵ��ࡣ�����˸���ClassLoader�ķ�����
			@Override
			public Class<?> findClass( String name ) {
				// �������������ⲿ���byteMap��Ѱ���ֽ���
				byte[] b = byteMap.get( name );
				return defineClass( name, b, 0, b.length );
			}
		};

		// ����jar�ļ���������ʵ������������ֽ������ӳ���byteMap�У���ʵ���������Ѱ���ֽ���ʹ��
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
		// ����byteMap�е������࣬�ɹ������entityMap
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
