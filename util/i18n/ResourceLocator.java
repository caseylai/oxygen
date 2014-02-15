package oxygen.util.i18n;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * <p>��Դ��λ��</p>
 * <p>����Դ��λ������ȷ��ϵͳ����Դ����ЧURL��˵����������ĳЩ����£���Ӧ�ñ�����Ϊjar����Java EE ������
 * ������������������޸Ķ�������Դ�޷���λ��ʹ�ø���Դ��λ�����Ի����Դ����ЧURL��</p>
 * @author ����
 * @since 1.0, 2007-05-28
 * @version 1.0
 */
public class ResourceLocator {

	/**
	 * ���Ҿ���ָ������·������Դ�����磺
	 * oxygen/config/resource.properties��ʾoxygen.config�����resource.properties��Դ�ļ�
	 * @param path ָ������·������Դ
	 * @return ���ܹ��ҵ���Ч����Դ���򷵻���Դ��URL�������Ҳ�����Դ������null
	 * @see #getResourceAsStream(String)
	 */
	public static URL getResource( String path ) {
		
		// ����ʹ��ϵͳ�����������
		URL url = ClassLoader.getSystemResource( path );
		// û���ҵ��Ļ������Թ���URL jar:file:<jar�ļ�·��>!/<��Դ�ڰ��ڵ�·��>
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
	 * ���Ҿ���ָ������·������Դ������������
	 * @param path ָ������·������Դ
	 * @return ���ܹ��ҵ���Ч����Դ���򷵻���Դ���������Ҳ�����Դ������null
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
