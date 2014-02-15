package oxygen.entity.annotation.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * String类型的内容约束（用正则表达式实现）
 * @author 赖昆
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface ContentConstraint {

	/**
	 * 用于约束的正则表达式
	 */
	String value() default ".*";
}
