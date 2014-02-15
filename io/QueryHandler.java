package oxygen.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import oxygen.entity.Entity;
import oxygen.io.standardizer.StandardizerProxy;


/**
 * <p>ʵ���ѯ��</p>
 * <p>ʵ���ѯ�����ж�ʵ��Ĳ�ѯ��������ѯ������ط���������ʵ��ļ��ϡ���û���ҵ�����������ʵ�壬�򷵻�һ��
 * �ռ��ϡ�����ѯ�������쳣��ʧ�ܣ��򷵻ؿձ�ǣ�null����</p>
 * @author ����
 * @since 1.0, 2007-04-01
 * @version 1.0
 * @param <E> ʵ��
 */
public class QueryHandler<E extends Entity<E>> extends Handler<E> {
	
	// Ҫ��ѯ��ʵ��
	private final E entity;
	
	// �������
	private final Response<List<E>> response;
	
	QueryHandler( E entity, LookupTable<E> table, Response<List<E>> response ) {
		super( table, null );
		this.entity = entity;
		this.response = response;
	}

	void handle() {
		
		try {

			// ��ѯĿ��ʵ��ĵ�ַ
			HashSet<Long> addressSet = new Querist<E>( entity, table ).query();
			// �����ѯ����ʵ��
			List<E> resultList = new LinkedList<E>();
			
			try {
				table.lock( addressSet );
				FileChannel channel = table.channel();
				ByteBuffer bb = ByteBuffer.allocate( table.size() );
				// ����ѯ���ĵ�ַ����ʵ��ʵ����
				for ( Long position : addressSet ) {
					channel.read( bb, position );
					E entity = StandardizerProxy.unstandardize( bb.array(), table.name() );
					resultList.add( entity );
					bb.clear();
				}
				// ���˵���ʵ�壨�ֶ�ֵȫ��Ϊnull��ʵ�壩
				resultList.removeAll( table.getEmptyEntityList() );
				
				response.setAddressSet( addressSet );
				response.setResponse( resultList );
			} catch ( IOException e ) {
				e.printStackTrace();
				response.setResponse( null );
			} finally {
				table.unlock();
			}
		
		} catch ( Exception e ) {
			logger.warning( res.getResource( "QueryHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( null );
		}
	}
}
