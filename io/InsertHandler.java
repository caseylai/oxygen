package oxygen.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.standardizer.DataFormatException;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>ʵ����봦����</p>
 * <p>ʵ����봦�����ܹ���ʵ����뵽ʵ���ļ���������Ӧ��������ֻ�в���ʵ�嵽ʵ���ļ��ͽ����������ɹ���һ�β�����������ǳɹ���
 * �����سɹ���ǡ�������������м�ĳ����ʧ�ܣ���ô������������˴β��������������ʵ���ļ����������޸Ļع�������ǰ��״̬��
 * ������ʧ�ܱ�ǡ�</p>
 * @author ����
 * @since 1.0, 2007-01-15
 * @version 1.0
 * @param <E> ʵ��
 */
public class InsertHandler<E extends Entity<E>> extends Handler<E> {
	
	// Ҫ�����ʵ��
	private final E entity;
	
	// �������
	private final Response<Boolean> response;
	
	InsertHandler( E entity, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entity = entity;
		this.response = response;
	}
	
	// ִ�в������
	void handle() {
		
		try {
		
			Map<Field, String> indexMap = new HashMap<Field, String>();
			// ��׼��ʵ��
			byte[] entityBytes;
			try {
				entityBytes = StandardizerProxy.standardize( entity );
			} catch ( DataFormatException e ) {
				response.setResponse( Boolean.FALSE );
				logger.warning( e.getMessage() );
				return;
			}
			// ��¼�����ֶ�
			for ( Field field : table.getFields() ) {
				field.setAccessible( true );
				if ( field.getAnnotation( Index.class ) != null ) {
					Object o = entity.valueOf( field );
					indexMap.put( field, o == null ? null : o.toString() );
				}
			}
			ByteBuffer bb = ByteBuffer.wrap( entityBytes );
			
			// ���һ����λ��ַ����û�п�λ����д���ļ�ĩβ��
			Long address = table.getFree();
			try {
				// ʹ���ļ�ͨ��ǰ��Ҫ�Ȼ��ͨ����������������ʵ���ļ��Ĳ��������Ի������ʵ���ļ�����������
				// ��ȡ������ͬʱ���Ҫд��ĵ�ַ�Ƿ��ڱ���״̬
				table.lock( address );
				FileChannel channel = table.channel();
				if ( address == null ) {
					address = channel.size();
				}
				// ���˲����������һ������������ʵ���ļ�д������ǰ����д��ع���¼
				if ( writer != null ) {
					writer.write( table.name(), address, new byte[ table.size() ] );
				}
				// ��������ʵ�屣�������������������ȫ���������ǰ�������̲߳��ܸ������ʵ��
				table.protect( address );
				// ��¼ʵ��Ļع�
				transaction.addRollback( new EntityRollback<E>( table, new byte[ table.size() ], address ) );
				// дʵ���ļ���ʵ��ʵ�����
				channel.write( bb, address );
			} catch ( IOException e ) {
				if ( address != null ) {
					table.unprotect( address );
				}
				response.setResponse( Boolean.FALSE );
				logger.warning( res.getResource( "InsertHandler.handle.warning.CannotWriteEntityFile", table.name() ) );
				return;
			} finally {
				table.unlock();
			}
	
			try {
				// ͳ�������ֶΣ���Щ�����������뵽�����ļ���ʵ��ӳ���
				for ( Field field : indexMap.keySet() ) {
					if ( !IndexManager.insert( field, indexMap.get( field ), address, transaction ) ) {
						response.setResponse( Boolean.FALSE );
						// ���������ַ�ı���������������Ч
						IndexManager.unprotect( transaction, false );
						// �ع�
						transaction.rollback();
						logger.warning( res.getResource( "InsertHandler.handle.warning.InsertIndexFailed", table.name(), field.getName() ) );
						return;
					}
				}
				// ���������ַ�ı�����������Ч
				IndexManager.unprotect( transaction, true );
			} finally {
				// ���ʵ��ı���
				table.unprotect( address );
			}
	
			response.setResponse( Boolean.TRUE );
			
		} catch ( Exception e ) {
			logger.warning( res.getResource( "InsertHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
