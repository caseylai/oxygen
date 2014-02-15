package oxygen.web;

import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import oxygen.config.DatabaseConfig;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;

/**
 * <p>���ฺ����webӦ�ó�������ʱ�������ݿ⡣</p>
 * <p>������һ��Servlet��Ҫ��Ӧ�ó�������ʱ������Servlet����Ҫ��Ӧ�ó����web�����ļ��н�����ص����ã�
 * ���磬һ�����ܵĲ������£�</p>
 * <p>����</p>
 * <code>
 * <pre>
 * <servlet>
 *		<servlet-name>DatabaseStartServlet</servlet-name>
 *		<servlet-class>oxygen.web.DatabaseStartServlet</servlet-class>
 *		<init-params>
 *			<!-- ���ݿ������ļ���λ�ã��������õĳ�ʼ�������� -->
 *			<init-param>
 *				<param-name>configLocation</param-name>
 *				<param-value>/WEB-INF/oxygen-config.properties</param-value>
 *			</init-param>
 *			<!-- �Ƿ��������ݿ�����ߣ���ѡ���õĳ�ʼ�������� -->
 *			<init-param>
 *				<param-name>startManagementTool</param-name>
 *				<param-value>true</param-value>
 *			</init-param>
 *		</init-params>
 *		<load-on-startup>1</load-on-startup>
 * </servlet>
 * </pre>
 * <code>
 * <p>����</p>
 * @author ����
 * @since 1.0, 2007-05-27
 * @version 1.0
 */
public class DatabaseStartServlet extends HttpServlet {
	
	//private static final String ENCODING = "UTF-8";
	
	/**
	 * ���ݿ������ļ�λ�õ����ò�����
	 */
	public static final String CONFIG_LOCATION = "configLocation";
	
	/**
	 * �Ƿ��������ݿ�����ߵ����ò�����
	 */
	public static final String START_MANAGEMENT_TOOL = "startManagementTool";
	
	private static final Logger logger = Logger.getLogger( DatabaseStartServlet.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( DatabaseStartServlet.class );

	@Override
	public void init() throws ServletException {
		
		// ��ȡweb�����ļ������ݿ������ļ�λ�õ����ò���
		String configLocation = getInitParameter( CONFIG_LOCATION );
		if ( configLocation == null ) {
			throw new ServletException( res.getResource( "DatabaseStartServlet.init.throw.ConfigFileIsNull" ) );
		}
		
		// ��ȡweb�����ļ����Ƿ��������ݿ�����ߵ����ò���
		String startManagementTool = getInitParameter( START_MANAGEMENT_TOOL );
		boolean isStartManagementTool = !"false".equalsIgnoreCase( startManagementTool );
		
		try {
			// ���������ļ���Դ
			URL url = getServletContext().getResource( configLocation );
			// �������ݿ�
			DatabaseConfig.setStartTools( isStartManagementTool );
			if ( !DatabaseConfig.config( url ) ) {
				logger.info( res.getResource( "DatabaseStartServlet.init.warning.DatabaseStartFailed" ) );
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			throw new ServletException( res.getResource( "DatabaseStartServlet.init.throw.ConfigFileIsNull" ) );
		}
	}
}
