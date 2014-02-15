package oxygen.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import oxygen.entity.Entity;


/**
 * <p>ʵ���ļ������ع���</p>
 * <p>��һ��ʵ���ļ��Ĵ�ȡ���������롢ɾ�����޸ģ����м䲽��ʧ��ʱ�����뽫��ع�����ȡ����ǰ��״̬�������������ڼ�¼ʵ���ļ�
 * �Ļع������ģ����ʼ���͵��ö���{@link Handler ʵ�����ݷ��ʴ�����}����ɡ�</p>
 * @see Handler ʵ�����ݷ��ʴ�����
 * @see Rollback �ع���ӿ�
 * @see IndexRollback �����ļ������ع���
 * @author ����
 * @since 1.0, 2007-01-15
 * @version 1.0
 * @param <E> ʵ��
 */
public class EntityRollback<E extends Entity<E>> implements Rollback {
	
	// ʵ���ļ�ͨ��
	private final LookupTable<E> table;
	
	// Ҫ�ع�����ʵ�壬���ֽ���������
	private final byte[] content;
	
	// ʵ���ַ
	private final long address;
	
	EntityRollback( LookupTable<E> table, byte[] content, long address ) {
		this.table = table;
		this.content = content;
		this.address = address;
	}

	/**
	 * ���ݴ˻ع���ʵ����״̬��ִ�лع�
	 */
	public void rollback() {
		
		try {
			table.lock();
			table.channel().write( ByteBuffer.wrap( content ), address );
		} catch ( IOException e ) {
			// �ع�����������IOException����������
		} finally {
			table.unlock();
		}
		// ��contentȫΪ\u0000���ʾһ��ɾ����������ʵ���ַ��ӵ���λ���ϡ�����ӿ�λ�������Ƴ���ַ
		for ( byte b : content ) {
			if ( b != 0 ) {
				table.removeFree( address );
				return;
			}
		}
		table.putFree( address );
	}

}
