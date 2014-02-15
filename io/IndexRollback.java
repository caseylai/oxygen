package oxygen.io;

/**
 * <p>�����ļ������ع���</p>
 * <p>��һ��ʵ���ļ��Ĵ�ȡ���������롢ɾ�����޸ģ����м䲽��ʧ��ʱ�����뽫��ع�����ȡ����ǰ��״̬�������������ڼ�¼�����ļ�
 * �Ļع������ġ�</p>
 * @see Handler ʵ�����ݷ��ʴ�����
 * @see Rollback �ع���ӿ�
 * @see EntityRollback ʵ���ļ������ع���
 * @author ����
 * @since 1.0, 2007-01-15
 * @version 1.0
 */
public class IndexRollback implements Rollback {
	
	// �����ļ�����
	private final IndexLoader loader;
	
	// �����������ļ��еĵ�ַ
	private final int position;
	
	// ������
	private final long key;
	
	// ����ֵ
	private final long address;
	
	IndexRollback( IndexLoader loader, int position, long key, long address ) {
		this.loader = loader;
		this.position = position;
		this.key = key;
		this.address = address;
	}

	/**
	 * ���ݴ˻ع���ʵ����״̬��ִ�лع�
	 */
	public void rollback() {
		loader.rollback( position, key, address );		
	}

}
