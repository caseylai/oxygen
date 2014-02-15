package oxygen.entity.annotation.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * String类型的最大长度约束（单位char）
 * @author 赖昆
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface SizeConstraint {

	/**
	 * 最大长度
	 */
	int value();
}
