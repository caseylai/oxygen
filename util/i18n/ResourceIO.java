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
 * ��Դ�ļ�I/O��
 * @author ����
 * @since 1.0, 2006-12-19
 * @version 1.0
 */
public class ResourceIO {

	private ResourceIO() {}
	
	/**
	 * <p>��<b>�Զ���</b>�����ļ�����Ϊӳ���</p>
	 * <p>�ڸ������ļ��У�ֻ��'='�������ַ���'='ǰ���ַ�������Ϊ�Ǽ�����'='����ַ�������Ϊ�Ǽ�ֵ��
	 * �������к���'='������ʹ��'\='����ת�塣��һ����Ч��'='���ȫ���ַ�������Ϊ�Ǽ�ֵ</p>
	 * @param file �����ļ�
	 * @return ���������Լ�ֵ��Map
	 * @throws FileNotFoundException ��ָ����·����û���ҵ������ļ�
	 * @throws IOException ���ļ�I/O����
	 * @throws IllegalArgumentException �����ļ���ʽ����
	 * @see #load(URL)
	 * @see #load(InputStream)
	 */
	public static Map<String, String> load( File file ) throws FileNotFoundException, IOException, IllegalArgumentException {
		return read( new BufferedReader( new FileReader( file ) ) );
	}
	
	/**
	 * ��URL�����������ļ��ж�ȡ�������ݲ�����Ϊӳ���
	 * @param url �����ļ���URL
	 * @return ���������Լ�ֵ��Map
	 * @throws IOException ����I/O����
	 * @throws IllegalArgumentException �����ļ���ʽ����
	 * @see #load(File)
	 * @see #load(InputStream)
	 */
	public static Map<String, String> load( URL url ) throws IOException, IllegalArgumentException {
		return load( url.openStream() );
	}
	
	/**
	 * �����ж�ȡ�������ݲ�����Ϊӳ���
	 * @param is ��
	 * @return ���������Լ�ֵ��Map
	 * @throws IOException ����I/O����
	 * @throws IllegalArgumentException �����ļ���ʽ����
	 * @see #load(File)
	 * @see #load(URL)
	 */
	public static Map<String, String> load( InputStream is ) throws IOException, IllegalArgumentException {
		return read( new BufferedReader( new InputStreamReader( is ) ) );
	}
	
	// ��reader�ж�ȡ�ַ���������ת��Ϊӳ���
	private static Map<String, String> read( BufferedReader reader ) throws IOException, IllegalArgumentException {
		
		Map<String, String> map = new HashMap<String, String>();
		
		String line;
		int lineCount = 0;
		while ( ( line = reader.readLine() ) != null ) {
			
			lineCount++;
			// �������к��Ե�����������һ��
			if ( ( line = line.trim() ).equals( "" ) ) continue;
			
			int pos, lastPos = 0;		
			
			do {
				pos = line.indexOf( "=", lastPos );
				lastPos = pos + 1;
			} while ( pos != -1 && line.charAt( pos - 1 ) == '\\' );
			
			// û���ҵ�'='
			if ( pos == -1 ) throw new IllegalArgumentException( "Missing '=' in line " + lineCount + ". Parsing failed." );
			
			String key = line.substring( 0, pos ).replaceAll( "\\\\=", "=" );
			String value = line.substring( pos + 1 );
			
			// �����ظ�
			if ( map.containsKey( key ) ) throw new IllegalArgumentException( "Duplicate key '" + key + "' in line " + lineCount + ". Parsing failed." );
			
			map.put( key, value );
		}
		
		reader.close();
		return map;
	}
	
	/*
	 * ������clazz�İ���·��
	 * ���磺
	 * pathOf( java.lang.Integer.class ) ����"java/lang"
	 * pathOf( Test.class ) ����"" ��Test��û�а����壩
	 */
	static String pathOf( Class clazz ) {
		
		if ( clazz == null ) throw new NullPointerException( "The class for searching package path cannot be null" );
		String packageName = clazz.getPackage().getName();
		if ( packageName == null ) packageName = "";
		
		return packageName.replace( ".", "/" );
	}
	
	/*
	 * ����ָ��·���ϴ��ڵ���Դ��������һ�����·�������磺"java/util"��"java"��""�����벻��"/"��ʼ�ͽ�����
	 * ����Դ�ļ�����ɡ�������������ָ����·��Ŀ¼��������Դ��������ڣ��򷵻�һ����Դ�����������ڣ������丸Ŀ¼
	 * ������������һֱ����Ŀ¼�����·������û���ҵ����򷵻�null��
	 */
	static InputStream search( String path, String resourceFileName ) {
		
		if ( path == null ) throw new NullPointerException( "The path cannot be null." );
		if ( resourceFileName == null ) throw new NullPointerException( "The resource file name cannot be null." );

		// ��path���ǿմ��Ҳ�����б�ܽ�β�Ļ�����·�����ļ���֮�����б�ܲ����ӳ������ļ�·��
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
