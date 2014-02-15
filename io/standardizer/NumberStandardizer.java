package oxygen.io.standardizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.RangeConstraint;


/**
 * 数值数据标准化器（允许的数值数据类型有Byte、Short、Integer、Long、Float和Double）
 * @author 赖昆
 * @since 1.0, 2007-04-11
 * @version 1.0
 * @param <E> 实体
 */
public class NumberStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {
	
	NumberStandardizer( E entity, Field field ) {
		super( entity, field );
	}
	
	// 检查数值约束
	@Override
	void checkConstraint() throws DataFormatException {
		
		// 公共约束检查
		super.checkConstraint();
		
		if ( o != null ) {	
			Class<? extends Number> clazz = ( (Class<?>) field.getGenericType() ).asSubclass( Number.class );
			// 检查范围约束（利用反射特性调用数值类中的compareTo方法）
			try {
				RangeConstraint rc = field.getAnnotation( RangeConstraint.class );
				if ( rc != null ) {
					Constructor<? extends Number> c = clazz.getConstructor( String.class );
					Method m = clazz.getMethod( "compareTo", clazz );
					boolean biggerThanMin = rc.min().equals( "undefined" ) || (Integer) m.invoke( c.newInstance( rc.min() ), o ) <= 0;
					boolean smallerThanMax = rc.max().equals( "undefined" ) || (Integer) m.invoke( c.newInstance( rc.max() ), o ) >= 0;
					if ( !biggerThanMin || !smallerThanMax ) {
						throw new DataFormatException( res.getResource( "NumberStandardizer.checkConstraint.exception.RangeConstraintFailed" , entity.getClass().getSimpleName(), field.getName(), o.toString(), rc.min(), rc.max() ) );				
					}
				}
			} catch ( Exception e ) {
				if ( e instanceof DataFormatException ) {
					throw (DataFormatException) e;
				}
			}
		}
	}
	
	// 检查约束并标准化数值数据
	byte[] standardize() throws DataFormatException {
		
		// 检查约束
		checkConstraint();
		
		// 数值为空，直接返回全零的字节数组
		if ( o == null ) {
			return new byte[ size ];
		}		
		
		// 标准化数值
		return numberToBytes( (Number) o );
	}
	
	/**
	 * 将数值（基本数据类型包装类的形式，限Byte、Short、Integer、Long、Float、Double）转化为可以存储在外存的字节数组形式
	 * @param number 数值
	 * @return 字节数组
	 */
	public static byte[] numberToBytes( Number number ) {
		
		// 通过SIZE字段获得其字节长度
		int size;
		try {
			size = (int) ( number.getClass().getField( "SIZE" ).getInt( null ) ) >> 3;
		} catch ( IllegalAccessException e ) {
			return new byte[0];
		} catch ( NoSuchFieldException e ) {
			return new byte[0];
		}
		
		// 通过ByteBuffer转化，这儿应该使用其他更好的转化方法
		ByteBuffer bb = ByteBuffer.allocate( size );
		if ( number instanceof Byte ) {
			bb.put( (Byte) number );
		} else if ( number instanceof Short ) {
			bb.putShort( (Short) number );
		} else if ( number instanceof Integer ) {
			bb.putInt( (Integer) number );
		} else if ( number instanceof Long ) {
			bb.putLong( (Long) number );
		} else if ( number instanceof Float ) {
			bb.putFloat( (Float) number );
		} else if ( number instanceof Double ) {
			bb.putDouble( (Double) number );
		}
		
		return bb.array();
	}
}
