package oxygen.util.i18n;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.UnknownFormatConversionException;

/**
 * <p>��Դ��������Ϊ���ṩ���ʻ�֧��</p>
 * @see ResourceLoaderProvider ��Դ�������ṩ��
 * @author ����
 * @since 1.0, 2006-12-16
 * @version 1.0
 */
public class ResourceLoader {
	
	// ��Դӳ���
	private final Map<String, String> map;
	
	// ��Դ�ļ�����
	private final String resourceFileName;
	
	// ��Դ����������
	private final String name;
	
	/**
	 * Ĭ����Դ�ļ���
	 */
	public static final String defaultResourceBundle = "resource.properties";
	
	// ��ָ������Դ�ļ������ֹ�����Դ������ʵ��
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
	 * �õ�ָ������Դ��������Ƕ����Դ��һ�𷵻�
	 * @param key ��Դ��
	 * @param args ����
	 * @return ��Դֵ
	 * @throws NoSuchResourceException ����Դ���������ڵ�ǰ��Դ����������ȫ�����������ж�û���ҵ�
	 * @throws IllegalArgumentException ����������������ԴҪ��Ĳ���������ת�����'%'����
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
	
	// �����Ƿ����Դ������������ָ���ļ�
	boolean contains( String key ) {
		return map.containsKey( key );
	}
	
	// ������Դ������������
	String getName() {
		return name;
	}
	
	// �õ�ָ��������ָ����Դ�ļ�����Դ������ʵ��
	static ResourceLoader newInstance( Class clazz, String resourceFileName ) {
		
		InputStream is = ResourceIO.search( ResourceIO.pathOf( clazz ), resourceFileName );
		if ( is == null ) throw new ResourceLoadFailedException( "Class '" + clazz.getName() + "' cannot find the resource file '" + resourceFileName + "'." );
		return new ResourceLoader( is, ResourceIO.pathOf( clazz ), resourceFileName );
	}
	
	// �õ�ָ����������Դ������ʵ��
	static ResourceLoader newInstance( Class clazz ) {
		return newInstance( clazz, defaultResourceBundle );
	}

	// ������Ƕ����Դ����
	private static String format( String str, String... args ) {				
		return String.format( str, (Object[]) args );
	}
}
