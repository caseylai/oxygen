package oxygen.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 带有该标注的实体将被实现缓冲。建议标注被频繁查询而较少修改的实体，可以帮助较大地提高查询性能
 * @author 赖昆
 * @since 1.0, 2007-05-12
 * @version 1.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
public @interface Cachable {
}