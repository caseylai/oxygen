package oxygen.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ���иñ�ע��ʵ�彫��ʵ�ֻ��塣�����ע��Ƶ����ѯ�������޸ĵ�ʵ�壬���԰����ϴ����߲�ѯ����
 * @author ����
 * @since 1.0, 2007-05-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface Cachable {
}