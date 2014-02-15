package oxygen.io;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>ʵ��ɾ��������</p>
 * <p>ʵ��ɾ����������ɾ��ʵ���Լ����ʵ���������ֻ��ɾ��ʵ����������ɹ���ɾ�������ǳɹ��ġ���ɾ�����м�ĳ������ʧ�ܣ�
 * ��ô���������������ɾ��������������ʵ���ļ��������ļ����޸Ļع���ɾ��֮ǰ��������ʧ�ܡ�</p>
 * @author ����
 * @since 1.0, 2007-04-03
 * @version 1.0
 * @param <E> ʵ��
 */
public class DeleteHandler<E extends Entity<E>> extends Handler<E> {
	
	// Ҫɾ����ʵ��
	private final E entity;
	
	// �������
	private final Response<Boolean> response;
	
	DeleteHandler( E entity, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entity = entity;
		this.response = response;
	}
	
	void handle() {
		
		try {
		
			// ���Ŀ��ʵ��ĵ�ַ
			HashSet<Long> address = new Querist<E>( entity, table ).query();
			// ���Ҳ������޸�Ŀ��ʵ�壬��ֱ�ӷ���
			if ( address == null ) {
				response.setResponse( Boolean.FALSE );
				return;
			} else if ( address.size() == 0 ) {
				response.setResponse( Boolean.TRUE );
				return;
			}
			
			// ɾ��Ŀ���ַ����ʵ��
			ByteBuffer bb = ByteBuffer.allocate( table.size() );
			bb.putLong( 0L ).putLong( 0L ).rewind();
			ByteBuffer old = ByteBuffer.allocate( table.size() );
			Collection<Long> protectedPosition = new HashSet<Long>();
			try {
				table.lock( address );
				FileChannel channel = table.channel();
				for ( long position : address ) {
					// �����������ĵ�ַ
					table.protect( position );
					protectedPosition.add( position );
					try {
						// ������ַ��ԭ��ʵ��,֮����ӵ��ع�����
						channel.read( old, position );
						transaction.addRollback( new EntityRollback<E>( table, old.array(), position ) );
						// ����ɾ����������һ������������ʵ���ļ�д������ǰ����д��ع���¼
						if ( writer != null ) {
							writer.write( table.name(), position, old.array() );
						}
						old.clear();
						// ɾ��ʵ��
						channel.write( bb, position );
						bb.rewind();
					} catch ( IOException e ) {
						response.setResponse( Boolean.FALSE );
						transaction.rollback();
						for ( Long pos : protectedPosition ) {
							table.unprotect( pos );
						}
						logger.warning( res.getResource( "DeleteHandler.handle.warning.DeleteFromEntityFileFailed", table.name(), String.valueOf( position ) ) );
						return;
					}
					// ��ɾ�����ʵ���ַ��¼����ұ�Ŀ�λ����
					table.putFree( position );
				}
			} finally {
				table.unlock();
			}
			
			// ɾ����ʵ����ص�����
			try {
				// ö�ٴ��������ֶ�
				for ( Field field : entity.getClass().getDeclaredFields() ) {
					field.setAccessible( true );
					if ( field.getAnnotation( Index.class ) != null ) {
						// ö�ٸ�����ַ
						for ( long position : address ) {
							if ( !IndexManager.delete( field, position, transaction ) ) {
								response.setResponse( Boolean.FALSE );
								IndexManager.unprotect( transaction, false );
								transaction.rollback();
								logger.warning( res.getResource( "DeleteHandler.handle.warning.DeleteIndexFailed", table.name(), field.getName() ) );
								return;
							}
						}
					}
				}
				// ���������ַ�ı�����������Ч
				IndexManager.unprotect( transaction, true );
			} finally {
				// �������ʵ��ı���
				for ( long pos : protectedPosition ) {
					table.unprotect( pos );
				}
			}

			response.setAddressSet( address );
			response.setResponse( Boolean.TRUE );
			
		} catch ( Exception e ) {
			logger.warning( res.getResource( "DeleteHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
