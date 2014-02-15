package oxygen.io.transaction;

/**
 * <p>����ִ����</p>
 * <p>����ִ�е����̴������£�</p>
 * <p>1.����һ���յĻع��ļ���.rb����</p>
 * <p>2.������Ĳ���ִ�����е�ʵ����������޸�ʵ���ļ�֮ǰ�����ȶ�ȡ�޸Ĳ��ֵ�ֵ������д�뵽�ع��ļ���Ȼ��
 * ���޸ı�Ҫ��ʵ���ļ����䲽�費�ɵߵ���</p>
 * <p>3.��ʵ�����ȫ���ɹ���ɣ�ɾ���ع��ļ������������</p>
 * <p>4.����;����ʧ�ܣ��׳��쳣�������Զ��ع����ع�������ɾ���ع��ļ��������ֵ���ȳ��򲻿ɼ���ִ�еĹ��ϣ�
 * ��������һ�����ݿ�����ʱ���ݻع��ļ��ļ�¼���лع����ɴ�ȷ�����ݵĿɿ��ԡ�</p>
 * @author ����
 * @since 1.0, 2007-04-25
 * @version 1.0
 */
public class TransactionExecutor {

	// Ҫִ�е�����
	private final Transaction transaction;
	
	TransactionExecutor( Transaction transaction ) {
		this.transaction = transaction;
	}
	
	// ��ʼ�ύ����
	void execute() {
		
		// �����ع��ļ���¼��
		RollbackFileWriter writer = new RollbackFileWriter();
		
		// ����ִ�������е�ʵ�����
		EntityOperation operation;
		while ( ( operation = transaction.getNextEntityOperation() ) != null ) {
			// ��ʵ�����ִ��ʧ�ܣ����������лָ�
			if ( !operation.invoke( writer ) ) {
				writer.close( false );
				new TransactionRecoverer( writer ).recover();
				return;
			}
		}
		// �رջع��ļ���¼��
		writer.close( true );
	}
}
