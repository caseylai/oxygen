package oxygen.io.standardizer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;
import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.ContentConstraint;


/**
 * 字符串数据标准化器
 * @author 赖昆
 * @since 1.0, 2007-04-11
 * @version 1.0
 * @param <E> 实体
 */
public class StringStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {
	
	// 数据存储编码方式
	private static final String encoding = DatabaseContext.get( ConfigKey.ENCODE );
	
	// 字段的字符串值
	private final String str;
	
	StringStandardizer( E entity, Field field ) {
		super( entity, field );
		str = o == null ? null : o.toString();
	}
	
	// 检查字符串约束
	@Override
	void checkConstraint() throws DataFormatException {
		
		// 公共约束检查
		super.checkConstraint();
		
		if ( str != null ) {
			// 检查长度约束
			if ( str.length() > size / TypeSizeDefiner.UTF_MAX_BYTES ) {
				throw new DataFormatException( res.getResource( "StringStandardizer.checkConstraint.exception.SizeConstraintFailed" , entity.getClass().getSimpleName(), field.getName(), str, String.valueOf( str.length() ), String.valueOf( size / TypeSizeDefiner.UTF_MAX_BYTES ) ) );
			}
			// 检查内容约束
			ContentConstraint cc = field.getAnnotation( ContentConstraint.class );
			if ( cc != null && !str.matches( cc.value() ) ) {
				throw new DataFormatException( res.getResource( "StringStandardizer.checkConstraint.exception.ContentConstraintFailed", entity.getClass().getSimpleName(), field.getName(), str, cc.value() ) );
			}
		}
	}
	
	// 检查约束并标准化字符串数据
	byte[] standardize() throws DataFormatException {

		// 约束检查
		checkConstraint();
		// 若字符串为null，则直接返回全零的字节数组
		if ( str == null ) {
			return new byte[ size ];
		}
		
		// 标准化字符串
		try {
			
			StringBuilder sb = new StringBuilder( str );
			int zeroNum = size - str.getBytes( encoding ).length;
			for ( int i = 0 ; i < zeroNum ; i++ ) {
				sb.append( '\u0000' );
			}
			
			return sb.toString().getBytes( encoding );
			
		} catch ( UnsupportedEncodingException e ) {
			return new byte[0];
		}
	}
}
