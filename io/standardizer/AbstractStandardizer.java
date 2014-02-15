package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.NotNullConstraint;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 抽象数据标准化器
 * @author 赖昆
 * @since 1.0, 2007-04-12
 * @version 1.0
 * @param <E> 实体
 */
public abstract class AbstractStandardizer<E extends Entity<E>> {
	
	// 指定的实体
	protected final E entity;
	
	// 指定的实体字段
	protected final Field field;
	
	// 实体字段的值
	protected Object o;
	
	// 实体字段值的长度
	protected int size;
	
	protected static final ResourceLoader res = ResourceLoaderProvider.provide( AbstractStandardizer.class );

	AbstractStandardizer( E entity, Field field ) {
		
		this.entity = entity;
		this.field = field;
		o = entity.valueOf( field );
		size = TypeSizeDefiner.define( field );
	}
	
	// 公用的约束检查
	void checkConstraint() throws DataFormatException {
		
		// 非空检查
		if ( field.getAnnotation( NotNullConstraint.class ) != null && o == null ) {
			throw new DataFormatException( res.getResource( "AbstractStandardizer.checkConstraint.exception.NotNullConstraintFailed", entity.getClass().getSimpleName(), field.getName() ) );
		}
	}
	
	// 执行约束检查以及标准化数据
	abstract byte[] standardize() throws DataFormatException;
}
