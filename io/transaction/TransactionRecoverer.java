package oxygen.io.transaction;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.EntityProvider;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.IndexManager;
import oxygen.io.LookupTable;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.standardizer.TypeSizeDefiner;


/**
 * <p>����ָ���</p>
 * <p>����ָ������Ը�������Ļع���־��������������ļ��ĸ��Ļָ���������֮ǰ��</p>
 * @author ����
 * @since 1.0, 2007-05-05
 * @version 1.0
 */
public class TransactionRecoverer {

	// �ع��ļ�
	private final File rollbackFile;
		
	// ������ǿ���жϣ���Դ���ϡ�������Ӳ�жϣ������ݿ����������������лָ�ʱ��ʹ���ļ�����ָ���
	public TransactionRecoverer( File rollbackFile ) {
		this.rollbackFile = rollbackFile;
	}
	
	// ������ִ����;�����쳣�����жϣ���ʹ�ûع��ļ���¼������ָ���
	TransactionRecoverer( RollbackFileWriter writer ) {
		rollbackFile = writer.getRollbackFile();
	}
	
	// �Ƿ��ǿ�ʵ�壨��׼������ֽ�����Ϊȫ\u0000��
	private static boolean isEmptyEntity( byte[] bytes ) {
		for ( byte b : bytes ) {
			if ( b != (byte) 0 ) return false;
		}
		return true;
	}
	
	// ��ʵ����ֽ������л���ֶε��ַ���ֵ
	private static String getFieldString( Field field, byte[] b, Map<Field, Integer> offsetMap ) {
		
		int fieldSize = TypeSizeDefiner.define( field );
		byte[] byteCode = new byte[ fieldSize ];
		System.arraycopy( b, offsetMap.get( field ), byteCode, 0, fieldSize );
		Object o = StandardizerProxy.unstandardize( byteCode, (Class<?>) field.getGenericType() );
		return o == null ? null : o.toString();
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * ���лָ������ָ��ɹ����ع��ļ�����ɾ������֮�ع��ļ�����������
	 */
	public void recover() {
		
		try {
			// �Ȱ��ֽ�������ʽ����ع��ļ�
			FileChannel channel = new RandomAccessFile( rollbackFile, "rw" ).getChannel();
			ByteBuffer bb = ByteBuffer.allocate( (int) rollbackFile.length() );
			while ( bb.position() < bb.limit() ) {
				channel.read( bb );
			}
			channel.close();
			
			// ���ֽ�����⿪Ϊ�ع���¼��������лָ�
			Collection<LookupTable<? extends Entity>> lockedTableSet = new HashSet<LookupTable<? extends Entity>>();
			try {
				for ( RollbackRecord record : RollbackRecord.toRecords( bb.array() ) ) {

					String entityName = record.getEntityName();
					long address = record.getAddress();
					byte[] bytes = record.getBytes();
					
					
					// ����ʵ������ȡ���ұ�����ȡ���ұ������
					LookupTable<? extends Entity> table = LookupTable.getLookupTable( entityName );
					if ( !lockedTableSet.contains( table ) ) {
						table.use();
						lockedTableSet.add( table );
						table.lock( address );
					}
					
					FileChannel fc = table.channel();
					// �������������ǵ�ʵ��
					ByteBuffer old = ByteBuffer.allocate( table.size() );
					fc.read( old, address );
					byte[] oldBytes = old.array();
					// ��дʵ��
					ByteBuffer buffer = ByteBuffer.wrap( bytes );
					fc.write( buffer, address );

					// ׼������
					Map<Field, Integer> indexMap = new HashMap<Field, Integer>();
					Entity entity = EntityProvider.provide( entityName );
					for ( Field field : entity.getClass().getDeclaredFields() ) {
						if ( field.getAnnotation( Index.class ) != null ) {
							indexMap.put( field, table.offset( field ) );
						}
					}

					// �����λ���Ϻ�����
					if ( isEmptyEntity( bytes ) ) {
						// �����ع���ʵ���ֽ�����Ϊ�գ���������һ����Ӳ�����ʧ�ܻع�����ӵ�ַ����λ����
						table.putFree( address );
						// ɾ������
						for ( Field field : indexMap.keySet() ) {
							if ( !IndexManager.delete( field, address, null ) ) {
								// ��ɾ��ʧ�ܣ�ֱ�ӷ��أ�������ɾ���ع��ļ�
								return;
							}
						}
					} else if ( isEmptyEntity( oldBytes ) ) {
						// ��ԭʼʵ����ֽ�����Ϊ�գ���������һ��ɾ��������ʧ�ܻع����ӿ�λ����ɾ���˵�ַ
						table.removeFree( address );
						// �������
						for ( Field field : indexMap.keySet() ) {
							String key = getFieldString( field, bytes, indexMap );
							if ( !IndexManager.insert( field, key, address, null ) ) {
								return;
							}
						}
					} else {
						// �޸Ĳ��������޸Ŀ�λ���ϣ�����������
						for ( Field field : indexMap.keySet() ) {
							String key = getFieldString( field, bytes, indexMap );
							if ( !IndexManager.update( field, key, address, null ) ) {
								return;
							}
						}
					}
				}
			} finally {
				// ͳһ����
				for ( LookupTable<? extends Entity> table : lockedTableSet ) {
					table.unlock();
					table.away();
				}
			}
			
			// �ָ�˳����ɣ�ɾ���ع��ļ�
			rollbackFile.delete();
			
		} catch ( IOException e ) {
			// ���쳣�׳�����ʾ�ָ�ʧ�ܣ�����������û��ɾ���ع��ļ�
		}
	}
}
