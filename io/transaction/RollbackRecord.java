package oxygen.io.transaction;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.io.standardizer.NumberStandardizer;


/**
 * �ع���¼�ࡣ�ع��ļ��е���Ϣ�ɴ˼�¼����ֽ���ʽ��ʾ��
 * @author ����
 * @since 1.0, 2007-04-26
 * @version 1.0
 */
public class RollbackRecord {
	
	// ʵ����
	private final String entityName;
	
	// ʵ���ļ��е�ƫ����
	private final long address;
	
	// ���޸ĵ��ֽ���
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
	 * ���ػع���¼���ֽ�������ʽ������ʽҲ�Ǳ����ڻع��ļ��е���ʽ��
	 * ��ʽ��
	 * 		1 int				ʵ�������ȣ�byte���ȣ�
	 * 		ʵ��������			ʵ������byte��ʽ
	 * 		1 long				��ʵ���ļ��е�ƫ����
	 * 		1 int				���޸ĵ��ֽ��鳤��
	 * 		ʵ���byte��ʾ����		���޸ĵ��ֽ���
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
	
	// ���ӻع��ļ��ж������ֽ�����ת��Ϊ�ع���¼
	static Deque<RollbackRecord> toRecords( byte[] bytes ) {
		
		ByteBuffer bb = ByteBuffer.wrap( bytes );
		// ��������Ϊ˫�ζ��е�Ŀ����������лع�����
		Deque<RollbackRecord> deque = new LinkedList<RollbackRecord>();
		
		while ( bb.position() < bb.limit() ) {
			try {
				int entityNameLength = bb.getInt();
				byte[] entityNameBytes = new byte[ entityNameLength ];
				bb.get( entityNameBytes );
				// �õ�ʵ����
				String entityName = new String( entityNameBytes, ENCODING );
				// �õ�ƫ����
				long address = bb.getLong();
				int bytesLength = bb.getInt();
				byte[] b = new byte[ bytesLength ];
				// �õ����޸��ֽ�����
				bb.get( b );
				// ���������������б����һ����¼����ͷ����ӣ�Ϊʵ������
				deque.addFirst( new RollbackRecord( entityName, address, b ) );
			} catch ( UnsupportedEncodingException e ) {
			}
		}
		
		return deque;
	}

}
