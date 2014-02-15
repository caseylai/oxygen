package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;
import oxygen.entity.EntityProvider;
import oxygen.io.LookupTable;


/**
 * <p>数据标准化器代理</p>
 * <p>数据标准化器用于在数据写入数据库前对其约束进行检查，并将数据格式化为可以写入数据库的格式。此代理隐藏了
 * 各类型数据标准化器，以一个统一的接口来实现数据的标准化。因此不必调用具体的数据标准化器，只需使用此代理即可。</p>
 * @author 赖昆
 * @since 1.0, 2007-04-09
 * @version 1.0
 */
public class StandardizerProxy {
	
	/**
	 * 对指定实体进行标准化（约束检查和格式化）
	 * @param <E> 实体
	 * @param entity 指定的实体
	 * @return 标准化后的字节数组
	 * @throws DataFormatException 若约束检查失败 
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Entity<E>> byte[] standardize( E entity ) throws DataFormatException {
		
		LookupTable<E> table = LookupTable.getLookupTable( entity.getClass().getSimpleName() );
		byte[] bytes = new byte[ table.size() ];
		for ( Field field : table.getFields() ) {
			byte b[] = standardize( entity, field );
			System.arraycopy( b, 0, bytes, table.offset( field ), b.length );
		}
		
		return bytes;
	}

	/**
	 * 对指定实体的指定字段的值进行标准化（约束检查和格式化）
	 * @param <E> 实体
	 * @param entity 指定的实体
	 * @param field 指定的实体字段
	 * @return 标准化后的字节数组
	 * @throws DataFormatException 若约束检查失败 
	 */
	public static <E extends Entity<E>> byte[] standardize( E entity, Field field ) throws DataFormatException {

		field.setAccessible( true );
		Class<?> clazz = (Class<?>) field.getGenericType();
		// 不同的数据类型，使用不同的数据标准化器
		if ( String.class.equals( clazz ) ) {
			return new StringStandardizer<E>( entity, field ).standardize();
		} else if ( Number.class.equals( clazz.getSuperclass() ) ) {
			return new NumberStandardizer<E>( entity, field ).standardize();
		} else if ( Boolean.class.equals( clazz ) ) {
			return new BooleanStandardizer<E>( entity, field ).standardize();
		} else {
			return new byte[0];
		}
	}
	
	/**
	 * 实体标准化的逆操作。将实体标准化后的字节数组重新转化为实体
	 * @param <E> 实体
	 * @param b 实体标准化后的字节数组
	 * @param entityName 实体名
	 * @return 逆标准化后产生的实体
	 */
	public static <E extends Entity<E>> E unstandardize( byte[] b, String entityName ) {

		E entity = EntityProvider.provide( entityName );
		LookupTable<E> table = LookupTable.getLookupTable( entityName );
		for ( Field field : table.getFields() ) {
			field.setAccessible( true );
			int offset = table.offset( field );
			int size = TypeSizeDefiner.define( field );
			byte[] byteCode = new byte[ size ];
			System.arraycopy( b, offset, byteCode, 0, size );
			Class<?> clazz = (Class<?>) field.getGenericType();
			Object o = unstandardize( byteCode, clazz );
			try {
				field.set( entity, o );
			} catch ( IllegalAccessException e ) {
			}
		}
		
		return entity;
	}
	
	/**
	 * 实体字段标准化的逆操作。将标准化后的实体字段重新转化为实体字段的值
	 * @param b 实体字段标准化后产生的字节数组
	 * @param clazz 实体字段的类对象
	 * @return 实体字段的值对象
	 */
	public static Object unstandardize( byte[] b, Class<?> clazz ) {
		return ObjectGenerator.generate( clazz, b );
	}
}
