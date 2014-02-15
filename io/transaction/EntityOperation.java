package oxygen.io.transaction;

import oxygen.entity.Entity;
import oxygen.io.EntityProxy;

/**
 * <p>ʵ�������</p>
 * <p>ʵ��������������ɲ��֡�������������ʵ����������һ������һ������ĳɹ���Ҫÿ��ʵ������ĳɹ�</p>
 * @param <E> ʵ��
 * @author ����
 * @since 1.0, 2007-04-23
 * @version 1.0
 */
public class EntityOperation<E extends Entity<E>> {
	
	// ʵ�����������
	private OperationType type;
	
	// ʵ�����1
	private E entity1;
	
	// ʵ�����2
	private E entity2;
	
	EntityOperation( OperationType type, E[] entities ) {
		this.type = type;
		entity1 = entities[0];
		if ( entities.length == 2 ) {
			entity2 = entities[1];
		}
	}
	
	// ִ�д�ʵ����������лع��ļ���¼��
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
