package oxygen.io;

import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>΢�����Ƕ�һ��ʵ����������еĲ������Եģ�����Щ��������һ������ֻ����Щ����ȫ��˳����ɣ�΢���������ɣ�������
 * һ������ʧ�ܣ�������ʵ�����ʧ�ܣ�΢����Ҳʧ�ܡ�</p>
 * @author ����
 * @since 1.0, 2007-01-17
 * @version 1.0
 */
public class MicroTransaction {

	// �ع�����
	private final Queue<Rollback> queue = new LinkedList<Rollback>();
	
	// ����
	private final byte[] keepsake = new byte[0];
	
	MicroTransaction() {}
	
	// ���΢��������
	Object getKeepsake() {
		return keepsake;
	}
	
	// ����һ���ع�ʵ��
	void addRollback( Rollback rollback ) {
		queue.offer( rollback );
	}
	
	// ִ�лع�
	void rollback() {
		for ( Rollback r : queue ) {
			r.rollback();
		}
	}
}
