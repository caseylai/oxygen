package oxygen.io;

/**
 * <p>回滚类接口</p>
 * <p>当实体存取发生错误，中途失败时，需要将实体文件的状态恢复（回滚）到存取前的状态。</p>
 * <p>所有的{@link oxygen.io.Handler 实体数据访问处理器}都带有一个回滚队列，每一次对文件的修改都将同时生成一个回滚类
 * 并加入到回滚队列中。若一次文件操作失败，则按照回滚队列中的回滚类进行恢复，保证每一次实体存取工作的安全性。</p>
 * @author 赖昆
 * @since 1.0, 2007-01-15
 * @version 1.0
 * @see EntityRollback 实体文件回滚类
 * @see IndexRollback 索引文件回滚类
 * @see oxygen.io.Handler 实体请求处理器
 */
public interface Rollback {
	
	/**
	 * 以实例中记录的状态进行回滚操作
	 */
	void rollback();
}
