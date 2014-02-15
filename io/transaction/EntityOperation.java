package oxygen.io.transaction;

import oxygen.entity.Entity;
import oxygen.io.EntityProxy;

/**
 * <p>实体操作类</p>
 * <p>实体操作是事务的组成部分。多个互相关联的实体操作组成了一个事务。一个事务的成功需要每个实体操作的成功</p>
 * @param <E> 实体
 * @author 赖昆
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public class EntityOperation<E extends Entity<E>> {
	
	// 实体操作的类型
	private OperationType type;
	
	// 实体参数1
	private E entity1;
	
	// 实体参数2
	private E entity2;
	
	EntityOperation( OperationType type, E[] entities ) {
		this.type = type;
		entity1 = entities[0];
		if ( entities.length == 2 ) {
			entity2 = entities[1];
		}
	}
	
	// 执行此实体操作（带有回滚文件记录）
	boolean invoke( RollbackFileWriter writer ) {
		
		boolean result = false;
		
		switch ( type ) {
		case INSERT:
			result = EntityProxy.insert( entity1, writer );
			break;
		case DELETE:
			result = EntityProxy.delete( entity1, writer );
			break;
		case UPDATE:
			result = EntityProxy.update( entity1, entity2, writer );
		}
		return result;
	}
}
