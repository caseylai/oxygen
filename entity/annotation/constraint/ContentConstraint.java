package oxygen.entity.annotation.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * String���͵�����Լ������������ʽʵ�֣�
 * @author ����
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface ContentConstraint {

	/**
	 * ����Լ����������ʽ
	 */
	String value() default ".*";
}
