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
 * <p>管理数据库配置及启动</p>
 * <p>数据库启动将完成以下工作：</p>
 * <p>1.从配置文件中读入数据库配置并注册在一个全局可以访问的上下文中。</p>
 * <p>2.在数据库目录下建立锁定文件，确保即使在不同的JVM中数据库也不能重复启动</p>
 * <p>3.建立相关文件夹（data - 保存数据  log - 记录日志  transaction - 事务专用）</p>
 * <p>4.读入用户的实体文件并进行初始化</p>
 * <p>5.注册JVM关闭钩子，确保在JVM意外终止时系统也能正常关闭</p>
 * <p>6.启动数据库管理与监视工具在系统托盘（可选，通过{@link #setStartTools(boolean)}设置）</p>
 * @author 赖昆
 * @since 1.0, 2006-12-10
 * @version 1.0
 */
public class DatabaseConfig {
	
	/**
	 * <p>数据库锁文件</p>
	 * <p>该锁文件是为确保数据库即使是在不同的虚拟机也不能重复启动而设置的。位置在数据库根目录下，
	 * 若由于异常原因，数据库的前次退出没有删除锁文件而造成此次数据库启动失败，请手动删除此锁文件。</p>
	 */ 
	public static final String LOCK_FILE_NAME = ".lock";
	
	// 数据库运行标志
	private static volatile boolean running = false;
	
	// 数据库配置文件URL
	private static URL configFileURL;
	
	// 当前日志文件
	private static File logFile;
	
	// 文件锁（当系统运行中在数据库目录存在，已保证数据库不重复启动）
	private static FileLock lock;
	
	// 锁文件
	private static File lockFile;
	
	// 数据库管理工具启动标志
	private static boolean isStartTools = true;
	
	private static final Logger rootLogger = Logger.getLogger( "oxygen" );
	
	private static final Logger logger = Logger.getLogger( DatabaseConfig.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( DatabaseConfig.class );
	
	private DatabaseConfig() {}
	
	/**
	 * 返回数据库是否正在运行
	 */
	public static boolean isRunning() {
		return running;
	}
	
	/**
	 * 返回系统所用的配置文件的URL
	 */
	public static URL getConfigFileURL() {
		return configFileURL;
	}
	
	/**
	 * 返回系统当前使用的日志文件
	 */
	public static File getLogFile() {
		return logFile;
	}
	
	/**
	 * 设置是否启动数据库管理工具（系统托盘处）。此方法在数据库启动之前调用是有效的；若数据库已经启动，
	 * 则该方法将不起任何作用。
	 * @param flag 设为true将启动数据库管理工具（默认），设为false将不启动数据库管理工具
	 */
	public static void setStartTools( boolean flag ) {
		if ( !running ) {
			isStartTools = flag;
		}
	}
	
	/**
	 * 初始化数据库配置，启动数据库
	 * @param configFileURL 配置文件URL
	 * @return 若启动数据库成功返回true，反之false
	 */
	public static synchronized boolean config( URL configFileURL ) {
		
		// 如果已经启动，则直接返回
		if ( running ) return true;
		
		DatabaseConfig.configFileURL = configFileURL;

		try {
			// 加载指定的配置文件
			Map<String, String> map = ResourceIO.load( configFileURL );
			// 将配置注册到数据库上下文
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
		
		// 验证配置的合法性
		File root = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ) );
		if ( root.isFile() ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.DatabaseRootCannotBeFile", ConfigKey.DATABASE_ROOT.key(), DatabaseContext.get( ConfigKey.DATABASE_ROOT ) ) );
			return false;
		} else if ( !root.exists() && !root.mkdirs() ) {
			logger.severe( res.getResource( "DatabaseConfig.config.severe.DatabaseRootNotExists", ConfigKey.DATABASE_ROOT.key(), DatabaseContext.get( ConfigKey.DATABASE_ROOT ) ) );
			return false;
		} else {
						
			// 建立锁文件
			lockFile = new File( root, LOCK_FILE_NAME );
			boolean isDBLocked = lockFile.exists() && !lockFile.delete();
			try {
				FileChannel fc = new RandomAccessFile( lockFile, "rw" ).getChannel();
				/* 
				 * 若锁文件已经存在，且被锁定，表示可能此数据库正在被另一个进程所使用。
				 * 读出锁文件的第一个字节。若该字节为1，表示前次数据库已正常停止，只是删除锁文件失败，
				 * 这种情况下允许数据库启动；反之，不允许数据库启动
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
			
			// 建立日志目录
			File logRoot = new File( root, "log" );
			if ( !logRoot.exists() ) logRoot.mkdir();
			/*
			 * 建立系统日志文件。文件名由"oxygen_"加上中文环境下的中等长度日期组成。之所以用中文，是因为某些国家的
			 * 日期格式中的字符不能作文件名，如"/"字符。
			 */
			String logFileName = "oxygen_" + DateFormat.getDateInstance( DateFormat.MEDIUM, Locale.CHINESE ).format( Calendar.getInstance().getTime() ) + ".log";
			logFile = new File( logRoot, logFileName );
			try {
				FileHandler handler = new FileHandler( logFile.getAbsolutePath(), true );
				handler.setFormatter( new SimpleFormatter() );
				rootLogger.addHandler( handler );
			} catch ( IOException e ) {
				// 因为I/O问题而不能打开文件，放弃文件日志写入
				logger.warning( res.getResource( "DatabaseConfig.config.warning.CannotOpenLogFile", logFileName ) );
			}
			// 建立数据目录
			File dataRoot =  new File( root, "data" );
			if ( !dataRoot.exists() ) dataRoot.mkdir();
			// 建立索引目录
			File indexRoot = new File( dataRoot, "index" );
			if ( !indexRoot.exists() ) indexRoot.mkdir();
		}
		
		// 加载实体jar文件
		try {
			EntityProvider.init();
			logger.info( res.getResource( "DatabaseConfig.config.info.EntityJarLoadedSuccessfully" ) );
		} catch ( EntityReadFailedException e ) {
			logger.severe( e.getMessage() );
			logger.severe( res.getResource( "DatabaseConfig.config.severe.EntityJarLoadingFailed" ) );
			return false;
		}
		
		// 建立事务目录
		File transactionRoot = new File( root, "transaction" );
		if ( !transactionRoot.exists() ) {
			transactionRoot.mkdir();
		} else {
			// 若在启动时发现有事务遗留下的回滚文件，则表明有没有正常结束的事务，启动事务恢复器进行回滚
			File[] rollbackFiles = transactionRoot.listFiles( new RollbackFileNameFilter() );
			for ( File rollbackFile : rollbackFiles ) {
				new TransactionRecoverer( rollbackFile ).recover();
			}
			if ( rollbackFiles.length > 0 ) {
				logger.info( res.getResource( "DatabaseConfig.config.info.TransactionRollbackSuccessfully", String.valueOf( rollbackFiles.length ) ) );
			}
		}
		
		// 添加系统关闭钩子。这样在虚拟机意外关闭时能正常关闭数据库
		Runtime.getRuntime().addShutdownHook( new Thread() {
			public void run() {
				close();
			}
		});
		
		running = true;
		// 更新管理工具状态
		if ( isStartTools ) {
			ManagementTool.getManagementTool().update( running );
		}
		
		logger.info( res.getResource( "DatabaseConfig.config.info.DBStartSuccessfully" ) );
		return true;
	}
	
	/**
	 * 安全关闭数据库
	 */
	public static synchronized void close() {
		// 若数据库已关闭，则直接返回
		if ( !running ) return;
		// 关闭实体数据代理
		EntityProxy.close();
		// 关闭查找表
		LookupTable.closeAll();
		// 关闭索引管理器
		IndexManager.close();
		// 释放文件锁，删除锁文件
		try {
			lock.release();
		} catch ( IOException e ) {
			logger.warning( res.getResource( "DatabaseConfig.close.warning.FileLockReleaseFailed" ) );
		} finally {
			if ( !lockFile.delete() ) {
				// 若不能删除锁文件。则尝试在锁文件中写入一个\u0001表示数据库锁文件已释放
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
		// 更新数据库管理工具状态
		if ( isStartTools ) {
			ManagementTool.getManagementTool().update( running );
		}
		logger.info( res.getResource( "DatabaseConfig.close.info.DBStopSuccessfully" ) );
	}
}