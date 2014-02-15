package oxygen.entity.annotation.constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ��ֵ���ͣ�Byte��Short��Integer��Long��Float��Double���ķ�ΧԼ��
 * @author ����
 * @since 1.0, 2006-12-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface RangeConstraint {

	/**
	 * ��Сֵ
	 */
	String min() default "undefined";
	
	/**
	 * ���ֵ
	 */
	String max() default "undefined";
}
