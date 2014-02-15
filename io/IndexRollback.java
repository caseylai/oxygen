package oxygen.io;

/**
 * <p>索引文件操作回滚类</p>
 * <p>当一个实体文件的存取操作（插入、删除、修改）在中间步骤失败时，必须将其回滚到存取操作前的状态。这个类就是用于记录索引文件
 * 的回滚操作的。</p>
 * @see Handler 实体数据访问处理器
 * @see Rollback 回滚类接口
 * @see EntityRollback 实体文件操作回滚类
 * @author 赖昆
 * @since 1.0, 2007-01-15
 * @version 1.0
 */
public class IndexRollback implements Rollback {
	
	// 索引的加载器
	private final IndexLoader loader;
	
	// 索引在索引文件中的地址
	private final int position;
	
	// 索引键
	private final long key;
	
	// 索引值
	private final long address;
	
	IndexRollback( IndexLoader loader, int position, long key, long address ) {
		this.loader = loader;
		this.position = position;
		this.key = key;
		this.address = address;
	}

	/**
	 * 依据此回滚类实例的状态，执行回滚
	 */
	public void rollback() {
		loader.rollback( position, key, address );		
	}

}
