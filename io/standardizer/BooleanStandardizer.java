package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;


/**
 * 布尔数据标准化器
 * @author 赖昆
 * @since 1.0, 2007-04-12
 * @version 1.0
 * @param <E> 实体
 */
public class BooleanStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {

	BooleanStandardizer( E entity, Field field ) {
		super( entity, field );
	}
	
	// 检查约束并标准化布尔数据
	byte[] standardize() throws DataFormatException {
		
		// 公共约束检查
		checkConstraint();

		// 标准化布尔值（空用0表示，真用1表示，假用-1表示）
		byte[] b = new byte[1];
		if ( o != null && (Boolean) o .equals( true ) ) {
			b[0] = (byte) 1;
		} else if ( o != null && (Boolean) o .equals( false ) ) {
			b[0] = (byte) -1;
		}
		return b;
	}

}
