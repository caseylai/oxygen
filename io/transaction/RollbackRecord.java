package oxygen.io.transaction;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.io.standardizer.NumberStandardizer;


/**
 * 回滚记录类。回滚文件中的信息由此记录类的字节形式表示。
 * @author 赖昆
 * @since 1.0, 2007-04-26
 * @version 1.0
 */
public class RollbackRecord {
	
	// 实体名
	private final String entityName;
	
	// 实体文件中的偏移量
	private final long address;
	
	// 待修改的字节组
	private final byte[] bytes;
	
	private static final String ENCODING = DatabaseContext.get( ConfigKey.ENCODE );
	
	RollbackRecord( String entityName, long address, byte[] bytes ) {
		this.entityName = entityName;
		this.address = address;
		this.bytes = bytes;
	}
	
	long getAddress() {
		return address;
	}

	byte[] getBytes() {
		return bytes;
	}

	String getEntityName() {
		return entityName;
	}

	/*
	 * 返回回滚记录的字节数组形式。该形式也是保存在回滚文件中的形式。
	 * 格式：
	 * 		1 int				实体名长度（byte长度）
	 * 		实体名长度			实体名的byte形式
	 * 		1 long				在实体文件中的偏移量
	 * 		1 int				待修改的字节组长度
	 * 		实体的byte表示长度		待修改的字节组
	 */
	byte[] toBytes() {
		
		try {
			
			byte[] entityNameBytes = entityName.getBytes( ENCODING );
			byte[] entityNameLength = NumberStandardizer.numberToBytes( entityNameBytes.length );
			byte[] offset = NumberStandardizer.numberToBytes( address );
			byte[] bytesLength = NumberStandardizer.numberToBytes( bytes.length );
			
			int length = 4 + entityNameBytes.length + 8 + 4 + bytes.length;
			byte[] result = new byte[ length ];
			
			System.arraycopy( entityNameLength, 0, result, 0, 4 );
			System.arraycopy( entityNameBytes, 0, result, 4, entityNameBytes.length );
			System.arraycopy( offset, 0, result, 4 + entityNameBytes.length, 8 );
			System.arraycopy( bytesLength, 0, result, 4 + entityNameBytes.length + 8, 4 );
			System.arraycopy( bytes, 0, result, 4 + entityNameBytes.length + 8 + 4, bytes.length );
			
			return result;
			
		} catch ( UnsupportedEncodingException e ) {
			return new byte[0];
		}
	}
	
	// 将从回滚文件中读出的字节数组转化为回滚记录
	static Deque<RollbackRecord> toRecords( byte[] bytes ) {
		
		ByteBuffer bb = ByteBuffer.wrap( bytes );
		// 这里声名为双段队列的目的是逆向进行回滚操作
		Deque<RollbackRecord> deque = new LinkedList<RollbackRecord>();
		
		while ( bb.position() < bb.limit() ) {
			try {
				int entityNameLength = bb.getInt();
				byte[] entityNameBytes = new byte[ entityNameLength ];
				bb.get( entityNameBytes );
				// 得到实体名
				String entityName = new String( entityNameBytes, ENCODING );
				// 得到偏移量
				long address = bb.getLong();
				int bytesLength = bb.getInt();
				byte[] b = new byte[ bytesLength ];
				// 得到待修改字节数组
				bb.get( b );
				// 分析结束，向结果列表添加一个记录（向头部添加，为实现逆序）
				deque.addFirst( new RollbackRecord( entityName, address, b ) );
			} catch ( UnsupportedEncodingException e ) {
			}
		}
		
		return deque;
	}

}
