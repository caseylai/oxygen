package oxygen.entity.annotation.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数值类型（Byte、Short、Integer、Long、Float、Double）的范围约束
 * @author 赖昆
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface RangeConstraint {

	/**
	 * 最小值
	 */
	String min() default "undefined";
	
	/**
	 * 最大值
	 */
	String max() default "undefined";
}
