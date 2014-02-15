package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.annotation.constraint.SizeConstraint;


/**
 * 实体字段类型长度定义器。确定实体字段的最大储存长度。
 * @author 赖昆
 * @since 1.0,2007-04-12
 * @version 1.0
 */
public class TypeSizeDefiner {
	
	// UTF-8编码最大占用的字节数
	static final int UTF_MAX_BYTES = 3;

	/**
	 * 定义指定实体内指定字段的最大储存长度
	 * @param entity 实体
	 * @param field 字段
	 * @return 长度
	 */
	public static int define( Field field ) {
		
		int size = 0;
		
		try {			
			field.setAccessible( true );
			Class<?> clazz = (Class<?>) field.getGenericType();
			if ( String.class.equals( clazz ) ) {
				// 字符串字段的长度由标注决定（长度单位为字符数）
				size = field.getAnnotation( SizeConstraint.class ).value() * UTF_MAX_BYTES;
			} else if ( Number.class.equals( clazz.getSuperclass() ) ) {
				// 数值字段的长度由数值类型决定（长度单位为字节数）
				// （通过反射找到类的SIZE字段获取二进制位数，再右移3位得到字节数）
				size = (int) ( ( (Class<?>) field.getGenericType() ).getField( "SIZE" ).getInt( null ) ) >> 3;
			} else if ( Boolean.class.equals( clazz ) ) {
				// 布尔字段的长度固定为1字节。值为1表示真，-1表示假
				size = 1;
			}
		} catch ( IllegalAccessException e ) {
		} catch ( NoSuchFieldException e ) {
		}
		
		return size;
	}
}
