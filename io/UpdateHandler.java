package oxygen.io;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.standardizer.DataFormatException;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>ʵ���޸Ĵ�����</p>
 * <p>ʵ���޸Ĵ���������ʵ�弰������������޸Ĳ�����ֻ��ʵ���Լ��������޸ĳɹ���һ���޸Ĳ����ǳɹ��ġ������޸�
 * �м�������쳣���޸�ʧ�ܵĻ������޸Ĵ����������������޸Ĳ�����������ʵ���ļ��������ļ����޸Ļع����޸�֮ǰ��
 * ״̬��</p>
 * @author ����
 * @since 1.0, 2007-04-06
 * @version 1.0
 * @param <E> ʵ��
 */
public class UpdateHandler<E extends Entity<E>> extends Handler<E> {
	
	// Ҫ���µ�ʵ��
	private final E entitySrc;
	
	// ���µ���ʵ��
	private final E entityDest;
	
	// �������
	private final Response<Boolean> response;
	
	UpdateHandler( E entitySrc, E entityDest, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entitySrc = entitySrc;
		this.entityDest = entityDest;
		this.response = response;
	}
	
	void handle() {
		
		try {
		
			// ��ô��޸�Ŀ��ʵ��ĵ�ַ
			HashSet<Long> address = new Querist<E>( entitySrc, table ).query();
			// ���Ҳ������޸�Ŀ��ʵ�壬��ֱ�ӷ���
			if ( address == null ) {
				response.setResponse( Boolean.FALSE );
				return;
			} else if ( address.size() == 0 ) {
				response.setResponse( Boolean.TRUE );
				return;
			}
			
			// ���в�Ϊnull���ֶκ����⴦���nullֵ�ֶζ��Ǵ��޸ĵ��ֶ�
			Map<Field, Integer> offsetMap = new HashMap<Field, Integer>();
			Map<Field, String> indexMap = new HashMap<Field, String>();
			Collection<Field> validFieldSet = new HashSet<Field>();
			for ( Field field : entityDest.getClass().getDeclaredFields() ) {
				field.setAccessible( true );
				offsetMap.put( field, table.offset( field ) );
				Object o = entityDest.valueOf( field );
				if ( o != null || entityDest.isSpecialNull( field.getName() ) ) {
					validFieldSet.add( field );
					// ��ȡ���޸��ֶ��е������ֶΣ���Щ�������Ǽ������޸ĵ�
					if ( field.getAnnotation( Index.class ) != null ) {
						indexMap.put( field, o == null ? null : o.toString() );
					}
				}
			}
	
			// �޸�Ŀ��ʵ��
			ByteBuffer old = ByteBuffer.allocate( table.size() );
			Collection<Long> protectedPosition = new HashSet<Long>();
			try {
				table.lock( address );
				FileChannel channel = table.channel();
				for ( long position : address ) {
					table.protect( position );
					protectedPosition.add( position );
					try {
						// ����Ŀ��ʵ������ݣ���¼��ع�����
						channel.read( old, position );
						transaction.addRollback( new EntityRollback<E>( table, old.array(), position ) );
						// �����޸Ĳ�������һ������������ʵ���ļ�д������ǰ����д��ع���¼
						if ( writer != null ) {
							writer.write( table.name(), position, old.array() );
						}
						// ��дĿ��ʵ������ݣ���д�ص�ʵ���ļ�
						for ( Field field : validFieldSet ) {
							old.position( offsetMap.get( field ) );
							// ������Ҫ�Դ��޸ĵ�ֵ���б�׼������
							try {
								byte[] b = StandardizerProxy.standardize( entityDest, field );
								old.put( b, 0, b.length );
							} catch ( DataFormatException e  ) {
								response.setResponse( Boolean.FALSE );
								transaction.rollback();
								for ( Long pos : protectedPosition ) {
									table.unprotect( pos );
								}
								logger.warning( e.getMessage() );
								return;
							}
						}
						old.rewind();
						channel.write( old, position );
						old.clear();
					} catch ( IOException e ) {
						response.setResponse( Boolean.FALSE );
						transaction.rollback();
						for ( Long pos : protectedPosition ) {
							table.unprotect( pos );
						}
						logger.warning( res.getResource( "UpdateHandler.handle.warning.UpdateFromEntityFileFailed", table.name(), String.valueOf( position ) ) );
						return;
					}
				}
			} finally {
				table.unlock();
			}
			
			// �޸�ʵ����ص�����
			try {
				// ö�ٴ��������ֶ�
				for ( long position : address ) {
					for ( Field field : indexMap.keySet() ) {
						if ( !IndexManager.update( field, indexMap.get( field ), position, transaction ) ) {
							response.setResponse( Boolean.FALSE );
							IndexManager.unprotect( transaction, false );
							transaction.rollback();
							logger.warning( res.getResource( "UpdateHandler.handle.warning.UpdateIndexFailed", table.name(), field.getName() ) );
							return;
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
			logger.warning( res.getResource( "UpdateHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
