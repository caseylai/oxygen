package oxygen.io.transaction;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import oxygen.entity.Entity;
import oxygen.io.EntityProxy;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 事务实现类
 * @author 赖昆
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public class Transaction {
	
	// 事务队列
	private final Queue<EntityOperation> transaction = new LinkedList<EntityOperation>();
		
	private static final Logger logger = Logger.getLogger( Transaction.class.getName() );
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( Transaction.class );
	
	// 依次获取实体操作
	EntityOperation getNextEntityOperation() {
		return transaction.poll();
	}

	/**
	 * 向事务队列中添加一个实体操作
	 * @param type 操作类型
	 * @param entities 操作涉及的实体
	 * @return 添加成功返回真，反之返回假
	 */
	public <E extends Entity<E>> boolean addOperation( OperationType type, E... entities ) {
		
		// 验证参数是否合法
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
		
		// 增加实体操作到事务队列
		if ( !transaction.offer( new EntityOperation<E>( type, entities ) ) ) {
			logger.warning( res.getResource( "Transaction.addOperation.warning.AddEntityOperationFailed" ) );
			rollback();
			return false;
		}
		
		return true;
	}
	
	/**
	 * 提交事务
	 */
	public void commit() {
		if ( transaction.size() != 0 ) {
			new TransactionExecutor( this ).execute();
		}
	}
	
	/**
	 * 撤销事务
	 */
	public void rollback() {
		transaction.clear();
		EntityProxy.rollback();
	}
}
