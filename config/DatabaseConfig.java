package oxygen.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import oxygen.config.management.ManagementTool;
import oxygen.entity.EntityProvider;
import oxygen.entity.EntityReadFailedException;
import oxygen.io.EntityProxy;
import oxygen.io.IndexManager;
import oxygen.io.LookupTable;
import oxygen.io.transaction.RollbackFileNameFilter;
import oxygen.io.transaction.TransactionRecoverer;
import oxygen.util.i18n.ResourceIO;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;

/**
 * <p>�������ݿ����ü�����</p>
 * <p>���ݿ�������������¹�����</p>
 * <p>1.�������ļ��ж������ݿ����ò�ע����һ��ȫ�ֿ��Է��ʵ��������С�</p>
 * <p>2.�����ݿ�Ŀ¼�½��������ļ���ȷ����ʹ�ڲ�ͬ��JVM�����ݿ�Ҳ�����ظ�����</p>
 * <p>3.��������ļ��У�data - ��������  log - ��¼��־  transaction - ����ר�ã�</p>
 * <p>4.�����û���ʵ���ļ������г�ʼ��</p>
 * <p>5.ע��JVM�رչ��ӣ�ȷ����JVM������ֹʱϵͳҲ�������ر�</p>
 * <p>6.�������ݿ��������ӹ�����ϵͳ���̣���ѡ��ͨ��{@link #setStartTools(boolean)}���ã�</p>
 * @author ����
 * @since 1.0, 2006-12-10
 * @version 1.0
 */
public class DatabaseConfig {
	
	/**
	 * <p>���ݿ����ļ�</p>
	 * <p>�����ļ���Ϊȷ�����ݿ⼴ʹ���ڲ�ͬ�������Ҳ�����ظ����������õġ�λ�������ݿ��Ŀ¼�£�
	 * �������쳣ԭ�����ݿ��ǰ���˳�û��ɾ�����ļ�����ɴ˴����ݿ�����ʧ�ܣ����ֶ�ɾ�������ļ���</p>
	 */ 
	public static final String LOCK_FILE_NAME = ".lock";
	
	// ���ݿ����б�־
	private static volatile boolean running = false;
	
	// ���ݿ������ļ�URL
	private static URL configFileURL;
	
	// ��ǰ��־�ļ�
	private static File logFile;
	
	// �ļ�������ϵͳ�����������ݿ�Ŀ¼���ڣ��ѱ�֤���ݿⲻ�ظ�������
	private static FileLock lock;
	
	// ���ļ�
	private static File lockFile;
	
	// ���ݿ������������־
	private static boolean isStartTools = true;
	
	private static final Logger rootLogger = Logger.getLogger( "oxygen" );
	
	private static final Logger logger = Logger.getLogger( DatabaseConfig.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( DatabaseConfig.class );
	
	private DatabaseConfig() {}
	
	/**
	 * �������ݿ��Ƿ���������
	 */
	public static boolean isRunning() {
		return running;
	}
	
	/**
	 * ����ϵͳ���õ������ļ���URL
	 */
	public static URL getConfigFileURL() {
		return configFileURL;
	}
	
	/**
	 * ����ϵͳ��ǰʹ�õ���־�ļ�
	 */
	public static File getLogFile() {
		return logFile;
	}
	
	/**
	 * �����Ƿ��������ݿ�����ߣ�ϵͳ���̴������˷��������ݿ�����֮ǰ��������Ч�ģ������ݿ��Ѿ�������
	 * ��÷����������κ����á�
	 * @param flag ��Ϊtrue���������ݿ�����ߣ�Ĭ�ϣ�����Ϊfalse�����������ݿ������
	 */
	public static void setStartTools( boolean flag ) {
		if ( !running ) {
			isStartTools = flag;
		}
	}
	
	/**
	 * ��ʼ�����ݿ����ã��������ݿ�
	 * @param configFileURL �����ļ�URL
	 * @return ���������ݿ�ɹ�����true����֮false
	 */
	public static synchronized boolean config( URL configFileURL ) {
		
		// ����Ѿ���������ֱ�ӷ���
		if ( running ) return true;
		
		DatabaseConfig.configFileURL = configFileURL;

		try {
			// ����ָ���������ļ�
			Map<String, String> map = ResourceIO.load( configFileURL );
			// ������ע�ᵽ���ݿ�������
			try {
				DatabaseContext.register( map );
			} catch ( IllegalArgumentException e ) {
				logger.severe( res.getResource( "DatabaseConfig.config.severe.ConfigFileMissingNecessaryKey", e.getMessage() ) );
				return false;
			}
			logger.info( res.getResource( "DatabaseConfig.config.info.RegisterPropertiesSuccess", configFileURL.toString() ) );
		} catch ( FileNotFoundException e ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.NotFoundConfigFile",  configFileURL.toString() ) );
			return false;
		} catch ( IOException e ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.DiskIOError",  configFileURL.toString() ) );
			return false;
		} catch ( IllegalArgumentException e ) {
			logger.severe( e.getMessage() );
			return false;
		}
		
		// ��֤���õĺϷ���
		File root = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ) );
		if ( root.isFile() ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.DatabaseRootCannotBeFile", ConfigKey.DATABASE_ROOT.key(), DatabaseContext.get( ConfigKey.DATABASE_ROOT ) ) );
			return false;
		} else if ( !root.exists() && !root.mkdirs() ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.DatabaseRootNotExists", ConfigKey.DATABASE_ROOT.key(), DatabaseContext.get( ConfigKey.DATABASE_ROOT ) ) );
			return false;
		} else {
						
			// �������ļ�
			lockFile = new File( root, LOCK_FILE_NAME );
			boolean isDBLocked = lockFile.exists() && !lockFile.delete();
			try {
				FileChannel fc = new RandomAccessFile( lockFile, "rw" ).getChannel();
				/* 
				 * �����ļ��Ѿ����ڣ��ұ���������ʾ���ܴ����ݿ����ڱ���һ��������ʹ�á�
				 * �������ļ��ĵ�һ���ֽڡ������ֽ�Ϊ1����ʾǰ�����ݿ�������ֹͣ��ֻ��ɾ�����ļ�ʧ�ܣ�
				 * ����������������ݿ���������֮�����������ݿ�����
				 */
				if ( isDBLocked ) {
					ByteBuffer bb = ByteBuffer.allocate( 1 );
					try {
						fc.read( bb );
					} catch ( IOException e ) {
						logger.severe( res.getResource( "DatabaseConfig.config.severe.AnotherInstanceIsRunning" ) );
						return false;
					}
					if ( bb.get( 0 ) != (byte) 1  ) {
						logger.severe( res.getResource( "DatabaseConfig.config.severe.AnotherInstanceIsRunning" ) );
						return false;
					}
				}
				try {
					lock = fc.lock();
					fc.truncate( 0 );
				} catch ( IOException e ) {
					logger.severe( res.getResource( "DatabaseConfig.config.severe.CannotCreateLockFile" ) );
					return false;
				}
			} catch ( FileNotFoundException e ) {
			}
			
			// ������־Ŀ¼
			File logRoot = new File( root, "log" );
			if ( !logRoot.exists() ) logRoot.mkdir();
			/*
			 * ����ϵͳ��־�ļ����ļ�����"oxygen_"�������Ļ����µ��еȳ���������ɡ�֮���������ģ�����ΪĳЩ���ҵ�
			 * ���ڸ�ʽ�е��ַ��������ļ�������"/"�ַ���
			 */
			String logFileName = "oxygen_" + DateFormat.getDateInstance( DateFormat.MEDIUM, Locale.CHINESE ).format( Calendar.getInstance().getTime() ) + ".log";
			logFile = new File( logRoot, logFileName );
			try {
				FileHandler handler = new FileHandler( logFile.getAbsolutePath(), true );
				handler.setFormatter( new SimpleFormatter() );
				rootLogger.addHandler( handler );
			} catch ( IOException e ) {
				// ��ΪI/O��������ܴ��ļ��������ļ���־д��
				logger.warning( res.getResource( "DatabaseConfig.config.warning.CannotOpenLogFile", logFileName ) );
			}
			// ��������Ŀ¼
			File dataRoot =  new File( root, "data" );
			if ( !dataRoot.exists() ) dataRoot.mkdir();
			// ��������Ŀ¼
			File indexRoot = new File( dataRoot, "index" );
			if ( !indexRoot.exists() ) indexRoot.mkdir();
		}
		
		// ����ʵ��jar�ļ�
		try {
			EntityProvider.init();
			logger.info( res.getResource( "DatabaseConfig.config.info.EntityJarLoadedSuccessfully" ) );
		} catch ( EntityReadFailedException e ) {
			logger.severe( e.getMessage() );
			logger.severe( res.getResource( "DatabaseConfig.config.severe.EntityJarLoadingFailed" ) );
			return false;
		}
		
		// ��������Ŀ¼
		File transactionRoot = new File( root, "transaction" );
		if ( !transactionRoot.exists() ) {
			transactionRoot.mkdir();
		} else {
			// ��������ʱ���������������µĻع��ļ����������û������������������������ָ������лع�
			File[] rollbackFiles = transactionRoot.listFiles( new RollbackFileNameFilter() );
			for ( File rollbackFile : rollbackFiles ) {
				new TransactionRecoverer( rollbackFile ).recover();
			}
			if ( rollbackFiles.length > 0 ) {
				logger.info( res.getResource( "DatabaseConfig.config.info.TransactionRollbackSuccessfully", String.valueOf( rollbackFiles.length ) ) );
			}
		}
		
		// ���ϵͳ�رչ��ӡ����������������ر�ʱ�������ر����ݿ�
		Runtime.getRuntime().addShutdownHook( new Thread() {
			public void run() {
				close();
			}
		});
		
		running = true;
		// ���¹�����״̬
		if ( isStartTools ) {
			ManagementTool.getManagementTool().update( running );
		}
		
		logger.info( res.getResource( "DatabaseConfig.config.info.DBStartSuccessfully" ) );
		return true;
	}
	
	/**
	 * ��ȫ�ر����ݿ�
	 */
	public static synchronized void close() {
		// �����ݿ��ѹرգ���ֱ�ӷ���
		if ( !running ) return;
		// �ر�ʵ�����ݴ���
		EntityProxy.close();
		// �رղ��ұ�
		LookupTable.closeAll();
		// �ر�����������
		IndexManager.close();
		// �ͷ��ļ�����ɾ�����ļ�
		try {
			lock.release();
		} catch ( IOException e ) {
			logger.warning( res.getResource( "DatabaseConfig.close.warning.FileLockReleaseFailed" ) );
		} finally {
			if ( !lockFile.delete() ) {
				// ������ɾ�����ļ������������ļ���д��һ��\u0001��ʾ���ݿ����ļ����ͷ�
				ByteBuffer bb = ByteBuffer.allocate( 1 );
				bb.put( (byte) 1 );
				bb.flip();
				try {
					lock.channel().write( bb, 0 );
				} catch ( IOException e ) {
					logger.warning( res.getResource( "DatabaseConfig.close.warning.FileLockDeleteFailed" ) );
				} finally {
					lock = null;
				}
			}
		}
		running = false;
		// �������ݿ������״̬
		if ( isStartTools ) {
			ManagementTool.getManagementTool().update( running );
		}
		logger.info( res.getResource( "DatabaseConfig.close.info.DBStopSuccessfully" ) );
	}
}