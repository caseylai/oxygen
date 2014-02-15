package oxygen.io.transaction;

/**
 * <p>事务执行器</p>
 * <p>事务执行的流程大致如下：</p>
 * <p>1.建立一个空的回滚文件（.rb）。</p>
 * <p>2.按事务的步骤执行其中的实体操作。当修改实体文件之前，总先读取修改部分的值，将其写入到回滚文件，然后
 * 再修改必要的实体文件。其步骤不可颠倒。</p>
 * <p>3.若实体操作全部成功完成，删除回滚文件。事务结束。</p>
 * <p>4.若中途发生失败，抛出异常，事务将自动回滚。回滚结束，删除回滚文件。若出现掉电等程序不可继续执行的故障，
 * 事务将在下一次数据库启动时根据回滚文件的记录进行回滚，由此确保数据的可靠性。</p>
 * @author 赖昆
 * @since 1.0, 2007-04-25
 * @version 1.0
 */
public class TransactionExecutor {

	// 要执行的事务
	private final Transaction transaction;
	
	TransactionExecutor( Transaction transaction ) {
		this.transaction = transaction;
	}
	
	// 开始提交事务
	void execute() {
		
		// 建立回滚文件记录器
		RollbackFileWriter writer = new RollbackFileWriter();
		
		// 依次执行事务中的实体操作
		EntityOperation operation;
		while ( ( operation = transaction.getNextEntityOperation() ) != null ) {
			// 若实体操作执行失败，则对事务进行恢复
			if ( !operation.invoke( writer ) ) {
				writer.close( false );
				new TransactionRecoverer( writer ).recover();
				return;
			}
		}
		// 关闭回滚文件记录器
		writer.close( true );
	}
}
