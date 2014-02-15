package oxygen.config.management;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Logger;

import oxygen.config.DatabaseConfig;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;
import oxygen.util.i18n.ResourceLocator;


/**
 * ���ݿ���ⲿ�����ߣ��˹�����ϵͳ�ڽ�����һ��ʵ����ͨ��ϵͳ���̽������ݿ�����ά��������
 * @author ����
 * @since 1.0, 2007-05-14
 * @version 1.0
 */
public class ManagementTool {
	
	private SystemTray tray;
	
	private TrayIcon ti;
	
	private PopupMenu popup;
	
	private MenuItem startItem;
	
	private MenuItem stopItem;
	
	private MenuItem stateItem;
	
	private MenuItem logItem;
	
	private MenuItem exitItem;
	
	// ����ʱͼ���ַ
	private static final String RUNNING_IMAGE = "images/system_running.gif";
	
	// ����ʱͼ���ַ
	private static final String SUSPENDING_IMAGE = "images/system_suspending.gif";
	
	// ����ʱ��ʾ
	private static final String RUNNING_HINT = "Oxygen - running";
	
	// ����ʱ��ʾ
	private static final String SUSPENDING_HINT = "Oxygen - suspending";
	
	// ����ʱͼ��
	private static final Image RUNNING_ICON = Toolkit.getDefaultToolkit().getImage( ResourceLocator.getResource( RUNNING_IMAGE ) );
	
	// ����ʱͼ��
	private static final Image SUSPENDING_ICON = Toolkit.getDefaultToolkit().getImage( ResourceLocator.getResource( SUSPENDING_IMAGE ) );
	
	private static final Logger logger = Logger.getLogger( ManagementTool.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( ManagementTool.class );
		
	// Ψһ�Ĺ�����ʵ��
	private static final ManagementTool tool = new ManagementTool();

	private ManagementTool() {

		// Swing�н��ô�����
		System.setProperty( "swing.boldMetal", "false" );
		
		// ����ǰϵͳ֧��ϵͳ���̵�ʹ�ã���������������
		if ( SystemTray.isSupported() ) {
			tray = SystemTray.getSystemTray();
			popup = new PopupMenu();
			startItem = new MenuItem( res.getResource( "ManagementTool.label.startItem" ) );
			stopItem = new MenuItem( res.getResource( "ManagementTool.label.stopItem" ) );
			stateItem = new MenuItem( res.getResource( "ManagementTool.label.stateItem" ) );
			logItem = new MenuItem( res.getResource( "ManagementTool.label.logItem" ) );
			exitItem = new MenuItem( res.getResource( "ManagementTool.label.exitItem" ) );
			startItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					DatabaseConfig.config( DatabaseConfig.getConfigFileURL() );
				}
			});
			stopItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					DatabaseConfig.close();
				}
			});
			stateItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					StateViewFrame.getStateViewFrame().display();
				}
			});
			logItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					try {
						Desktop.getDesktop().open( DatabaseConfig.getLogFile() );
					} catch ( IOException e ) {
					}
				}
			});
			exitItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					DatabaseConfig.close();
					StateViewFrame.close();
					tray.remove( ti );
				}
			});
			popup.add( startItem );
			popup.add( stopItem );
			popup.addSeparator();
			popup.add( stateItem );
			popup.addSeparator();
			popup.add( logItem );
			popup.addSeparator();
			popup.add( exitItem );
			if ( DatabaseConfig.isRunning() ) {
				ti = new TrayIcon( RUNNING_ICON, RUNNING_HINT, popup );
				startItem.setEnabled( false );
			} else {
				ti = new TrayIcon( SUSPENDING_ICON, SUSPENDING_HINT, popup );
				stopItem.setEnabled( false );
			}
			ti.setImageAutoSize( true );
			ti.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					StateViewFrame.getStateViewFrame().display();
				}
			});
			try {
				tray.add( ti );
			} catch ( AWTException e ) {
				logger.warning( res.getResource( "ManagementTool.warning.SystemTrayCannotBeUsed" ) );
			}
		} else {
			logger.warning( res.getResource( "ManagementTool.warning.SystemTrayIsNotSupported" ) );
		}
	}
	
	/**
	 * ���¹�������ʾ״̬
	 * @param running ���ݿ��Ƿ���������
	 */
	public void update( boolean running ) {
		if ( running ) {
			ti.setImage( RUNNING_ICON );
			ti.setToolTip( RUNNING_HINT );
			startItem.setEnabled( false );
			stopItem.setEnabled( true );
		} else {
			ti.setImage( SUSPENDING_ICON );
			ti.setToolTip( SUSPENDING_HINT );
			startItem.setEnabled( true );
			stopItem.setEnabled( false );
		}
	}
	
	/**
	 * ����ϵͳΨһ�Ĺ�����ʵ��
	 */
	public static ManagementTool getManagementTool() {
		return tool;
	}
}
