package oxygen.io.transaction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 回滚文件记录器。用于在事务处理过程当中，将回滚信息记录入回滚文件中。
 * @author 赖昆
 * @since 1.0, 2007-04-26
 * @version 1.0
 */
public class RollbackFileWriter implements Runnable {
	
	// 回滚文件
	private final File rollbackFile;
	
	// 该记录器的循环线程
	private final Thread thread;
	
	// 回滚文件通道
	private FileChannel channel;
	
	// 待写入的回滚记录
	private RollbackRecord record;
	
	// 用于阻塞外部使用此记录器线程的锁
	private volatile Object lock;
	
	// 回滚记录成功标志
	private volatile boolean successful = true;
	
	// 退出时是否删除回滚文件标志
	private volatile boolean delete = true;
	
	private static final Logger logger = Logger.getLogger( RollbackFileWriter.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( RollbackFileWriter.class );
	
	RollbackFileWriter() {
		
		// 建立一个空回滚文件
		File file;
		do {
			File transactionRoot = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "transaction" );
			String fileName = String.valueOf( System.nanoTime() ) + ".rb";
			file = new File( transactionRoot, fileName );
		} while ( file.exists() );
		
		rollbackFile = file;
		try {
			// 这儿使用了“rwd”模式确保每一个I/O操作都同步写入到外存
			channel = new RandomAccessFile( rollbackFile, "rwd" ).getChannel();
		} catch ( FileNotFoundException e ) {
		}

		// 启动循环线程，接收回滚信息，将其写入回滚文件
		thread = new Thread( this );
		thread.start();
	}
	
	File getRollbackFile() {
		return rollbackFile;
	}
	
	/**
	 * 关闭该回滚记录器
	 * @param delete 关闭前是否删除回滚文件
	 */
	public void close( boolean delete ) {
		this.delete = delete;
		thread.interrupt();
	}
	
	/**
	 * 向回滚文件添加一条回滚记录。在记录被确保写入回滚文件前，此方法将一直阻塞。
	 * @param entityName 实体名
	 * @param address 实体文件内偏移量
	 * @param bytes 待修改的字节组
	 * @return 若添加成功则返回真，反之返回假
	 */
	public boolean write( String entityName, long address, byte[] bytes ) {

		lock = new byte[0];
		synchronized ( lock ) {
			
			synchronized ( this ) {
				record = new RollbackRecord( entityName, address, bytes );
				notify();
			}
			
			// 阻塞在这儿，直到获得通知记录已写入文件
			try {
				lock.wait();
			} catch ( InterruptedException e ) {
			}
		}
		
		return successful;
	}
	
	public synchronized void run() {

		try {
			// 直到退出标志为真线程才退出
			while ( !thread.isInterrupted() ) {
				// 空闲时线程在这里阻塞
				while ( record == null ) {
					wait();
				}
				// 写回滚记录
				ByteBuffer bb = ByteBuffer.wrap( record.toBytes() );
				try {
					channel.write( bb );
				} catch ( IOException e ) {
					logger.warning( res.getResource( "RollbackFileWriter.run.warning.WriteRollbackFileFailed", rollbackFile.getAbsolutePath() ) );
					successful = false;
				}
				// 通知外部线程回滚记录已写入完成
				synchronized ( lock ) {
					lock.notify();
				}
				// 清空写过的回滚记录
				record = null;
			}
		} catch ( InterruptedException e ) {
		}
		
		// 关闭通道
		try {
			channel.close();
		} catch ( IOException e ) {
		}
		// 若需要删除回滚文件
		if ( delete ) {
			rollbackFile.delete();
		}
	}
}
