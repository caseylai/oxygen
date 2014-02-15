package oxygen.io;

import java.util.LinkedList;
import java.util.Queue;

/**
 * <p>微事务是对一个实体操作内所有的操作而言的，将这些操作看作一个事务：只有这些操作全部顺利完成，微事务才能完成；若其中
 * 一个操作失败，则整个实体操作失败，微事务也失败。</p>
 * @author 赖昆
 * @since 1.0, 2007-01-17
 * @version 1.0
 */
public class MicroTransaction {

	// 回滚队列
	private final Queue<Rollback> queue = new LinkedList<Rollback>();
	
	// 信物
	private final byte[] keepsake = new byte[0];
	
	MicroTransaction() {}
	
	// 获得微事务信物
	Object getKeepsake() {
		return keepsake;
	}
	
	// 加入一个回滚实例
	void addRollback( Rollback rollback ) {
		queue.offer( rollback );
	}
	
	// 执行回滚
	void rollback() {
		for ( Rollback r : queue ) {
			r.rollback();
		}
	}
}
