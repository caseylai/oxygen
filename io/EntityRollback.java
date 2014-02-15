package oxygen.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import oxygen.entity.Entity;


/**
 * <p>实体文件操作回滚类</p>
 * <p>当一个实体文件的存取操作（插入、删除、修改）在中间步骤失败时，必须将其回滚到存取操作前的状态。这个类就是用于记录实体文件
 * 的回滚操作的，其初始化和调用都在{@link Handler 实体数据访问处理器}中完成。</p>
 * @see Handler 实体数据访问处理器
 * @see Rollback 回滚类接口
 * @see IndexRollback 索引文件操作回滚类
 * @author 赖昆
 * @since 1.0, 2007-01-15
 * @version 1.0
 * @param <E> 实体
 */
public class EntityRollback<E extends Entity<E>> implements Rollback {
	
	// 实体文件通道
	private final LookupTable<E> table;
	
	// 要回滚至的实体，用字节数组描述
	private final byte[] content;
	
	// 实体地址
	private final long address;
	
	EntityRollback( LookupTable<E> table, byte[] content, long address ) {
		this.table = table;
		this.content = content;
		this.address = address;
	}

	/**
	 * 依据此回滚类实例的状态，执行回滚
	 */
	public void rollback() {
		
		try {
			table.lock();
			table.channel().write( ByteBuffer.wrap( content ), address );
		} catch ( IOException e ) {
			// 回滚操作发生的IOException都不作处理
		} finally {
			table.unlock();
		}
		// 若content全为\u0000则表示一个删除操作，将实体地址添加到空位集合。否则从空位集合中移除地址
		for ( byte b : content ) {
			if ( b != 0 ) {
				table.removeFree( address );
				return;
			}
		}
		table.putFree( address );
	}

}
