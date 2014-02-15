package oxygen.io.transaction;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import oxygen.entity.Entity;
import oxygen.io.EntityProxy;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * ����ʵ����
 * @author ����
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public class Transaction {
	
	// �������
	private final Queue<EntityOperation> transaction = new LinkedList<EntityOperation>();
		
	private static final Logger logger = Logger.getLogger( Transaction.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( Transaction.class );
	
	// ���λ�ȡʵ�����
	EntityOperation getNextEntityOperation() {
		return transaction.poll();
	}

	/**
	 * ��������������һ��ʵ�����
	 * @param type ��������
	 * @param entities �����漰��ʵ��
	 * @return ��ӳɹ������棬��֮���ؼ�
	 */
	public <E extends Entity<E>> boolean addOperation( OperationType type, E... entities ) {
		
		// ��֤�����Ƿ�Ϸ�
		try {
			switch ( type ) {
				case INSERT:
				case DELETE:
					if ( entities.length != 1 || entities[0] == null ) {
						throw new IllegalArgumentException();
					}
					break;
				case UPDATE:
					if ( entities.length != 2 || entities[0] == null || entities[1] == null ) {
						throw new IllegalArgumentException();
					}
			}
		} catch ( IllegalArgumentException e ) {
			logger.warning( res.getResource( "Transaction.addOperation.warning.IllegalArgumentInAddOpertion" ) );
			rollback();
			return false;
		}
		
		// ����ʵ��������������
		if ( !transaction.offer( new EntityOperation<E>( type, entities ) ) ) {
			logger.warning( res.getResource( "Transaction.addOperation.warning.AddEntityOperationFailed" ) );
			rollback();
			return false;
		}
		
		return true;
	}
	
	/**
	 * �ύ����
	 */
	public void commit() {
		if ( transaction.size() != 0 ) {
			new TransactionExecutor( this ).execute();
		}
	}
	
	/**
	 * ��������
	 */
	public void rollback() {
		transaction.clear();
		EntityProxy.rollback();
	}
}
