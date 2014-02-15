package oxygen.web;

import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import oxygen.config.DatabaseConfig;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;

/**
 * <p>此类负责在web应用程序启动时启动数据库。</p>
 * <p>此类是一个Servlet，要在应用程序启动时启动该Servlet，需要在应用程序的web部署文件中进行相关的配置，
 * 例如，一个可能的部署如下：</p>
 * <p>……</p>
 * <code>
 * <pre>
 * <servlet>
 *		<servlet-name>DatabaseStartServlet</servlet-name>
 *		<servlet-class>oxygen.web.DatabaseStartServlet</servlet-class>
 *		<init-params>
 *			<!-- 数据库配置文件的位置（必须配置的初始化参数） -->
 *			<init-param>
 *				<param-name>configLocation</param-name>
 *				<param-value>/WEB-INF/oxygen-config.properties</param-value>
 *			</init-param>
 *			<!-- 是否启动数据库管理工具（可选配置的初始化参数） -->
 *			<init-param>
 *				<param-name>startManagementTool</param-name>
 *				<param-value>true</param-value>
 *			</init-param>
 *		</init-params>
 *		<load-on-startup>1</load-on-startup>
 * </servlet>
 * </pre>
 * <code>
 * <p>……</p>
 * @author 赖昆
 * @since 1.0, 2007-05-27
 * @version 1.0
 */
public class DatabaseStartServlet extends HttpServlet {
	
	//private static final String ENCODING = "UTF-8";
	
	/**
	 * 数据库配置文件位置的配置参数名
	 */
	public static final String CONFIG_LOCATION = "configLocation";
	
	/**
	 * 是否启动数据库管理工具的配置参数名
	 */
	public static final String START_MANAGEMENT_TOOL = "startManagementTool";
	
	private static final Logger logger = Logger.getLogger( DatabaseStartServlet.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( DatabaseStartServlet.class );

	@Override
	public void init() throws ServletException {
		
		// 获取web部署文件中数据库配置文件位置的配置参数
		String configLocation = getInitParameter( CONFIG_LOCATION );
		if ( configLocation == null ) {
			throw new ServletException( res.getResource( "DatabaseStartServlet.init.throw.ConfigFileIsNull" ) );
		}
		
		// 获取web部署文件中是否启动数据库管理工具的配置参数
		String startManagementTool = getInitParameter( START_MANAGEMENT_TOOL );
		boolean isStartManagementTool = !"false".equalsIgnoreCase( startManagementTool );
		
		try {
			// 解析配置文件资源
			URL url = getServletContext().getResource( configLocation );
			// 启动数据库
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
