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
 * �ع��ļ���¼������������������̵��У����ع���Ϣ��¼��ع��ļ��С�
 * @author ����
 * @since 1.0, 2007-04-26
 * @version 1.0
 */
public class RollbackFileWriter implements Runnable {
	
	// �ع��ļ�
	private final File rollbackFile;
	
	// �ü�¼����ѭ���߳�
	private final Thread thread;
	
	// �ع��ļ�ͨ��
	private FileChannel channel;
	
	// ��д��Ļع���¼
	private RollbackRecord record;
	
	// ���������ⲿʹ�ô˼�¼���̵߳���
	private volatile Object lock;
	
	// �ع���¼�ɹ���־
	private volatile boolean successful = true;
	
	// �˳�ʱ�Ƿ�ɾ���ع��ļ���־
	private volatile boolean delete = true;
	
	private static final Logger logger = Logger.getLogger( RollbackFileWriter.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( RollbackFileWriter.class );
	
	RollbackFileWriter() {
		
		// ����һ���ջع��ļ�
		File file;
		do {
			File transactionRoot = new File( DatabaseContext.get( ConfigKey.DATABASE_ROOT ), "transaction" );
			String fileName = String.valueOf( System.nanoTime() ) + ".rb";
			file = new File( transactionRoot, fileName );
		} while ( file.exists() );
		
		rollbackFile = file;
		try {
			// ���ʹ���ˡ�rwd��ģʽȷ��ÿһ��I/O������ͬ��д�뵽���
			channel = new RandomAccessFile( rollbackFile, "rwd" ).getChannel();
		} catch ( FileNotFoundException e ) {
		}

		// ����ѭ���̣߳����ջع���Ϣ������д��ع��ļ�
		thread = new Thread( this );
		thread.start();
	}
	
	File getRollbackFile() {
		return rollbackFile;
	}
	
	/**
	 * �رոûع���¼��
	 * @param delete �ر�ǰ�Ƿ�ɾ���ع��ļ�
	 */
	public void close( boolean delete ) {
		this.delete = delete;
		thread.interrupt();
	}
	
	/**
	 * ��ع��ļ����һ���ع���¼���ڼ�¼��ȷ��д��ع��ļ�ǰ���˷�����һֱ������
	 * @param entityName ʵ����
	 * @param address ʵ���ļ���ƫ����
	 * @param bytes ���޸ĵ��ֽ���
	 * @return ����ӳɹ��򷵻��棬��֮���ؼ�
	 */
	public boolean write( String entityName, long address, byte[] bytes ) {

		lock = new byte[0];
		synchronized ( lock ) {
			
			synchronized ( this ) {
				record = new RollbackRecord( entityName, address, bytes );
				notify();
			}
			
			// �����������ֱ�����֪ͨ��¼��д���ļ�
			try {
				lock.wait();
			} catch ( InterruptedException e ) {
			}
		}
		
		return successful;
	}
	
	public synchronized void run() {

		try {
			// ֱ���˳���־Ϊ���̲߳��˳�
			while ( !thread.isInterrupted() ) {
				// ����ʱ�߳�����������
				while ( record == null ) {
					wait();
				}
				// д�ع���¼
				ByteBuffer bb = ByteBuffer.wrap( record.toBytes() );
				try {
					channel.write( bb );
				} catch ( IOException e ) {
					logger.warning( res.getResource( "RollbackFileWriter.run.warning.WriteRollbackFileFailed", rollbackFile.getAbsolutePath() ) );
					successful = false;
				}
				// ֪ͨ�ⲿ�̻߳ع���¼��д�����
				synchronized ( lock ) {
					lock.notify();
				}
				// ���д���Ļع���¼
				record = null;
			}
		} catch ( InterruptedException e ) {
		}
		
		// �ر�ͨ��
		try {
			channel.close();
		} catch ( IOException e ) {
		}
		// ����Ҫɾ���ع��ļ�
		if ( delete ) {
			rollbackFile.delete();
		}
	}
}
