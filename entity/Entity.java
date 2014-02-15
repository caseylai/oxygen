package oxygen.entity;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oxygen.io.EntityProxy;


/**
 * <p>所有实体的公共父类，提供实体以数据库支持的能力。</p>
 * <p>这也是一个泛型类，所有其子类必须扩展其子类类型的本实体类。</p>
 * @author 赖昆
 * @since 1.0, 2006-12-10
 * @version 1.0
 * @param <E> 实体
 * @see EntityProvider 实体提供者
 */
@SuppressWarnings("unchecked")
public abstract class Entity<E extends Entity<E>> implements Cloneable {
	
	// 用于保存特殊需要特殊处理的null值：如作为查询条件、作为待插入的数据等
	private final Set<Field> specialNullSet = Collections.synchronizedSet( new HashSet<Field>() );
	
	/**
	 * 开始一个事务
	 */
	public static final void beginTransaction() {
		EntityProxy.beginTransaction();
	}
	
	/**
	 * 提交一个事务
	 */
	public static final void commit() {
		EntityProxy.commit();
	}
	
	/**
	 * 撤销一个事务
	 */
	public static final void rollback() {
		EntityProxy.rollback();
	}
	
	/**
	 * 检查指定的字段是否是特殊处理null的字段
	 * @param name 字段名
	 * @return 是特殊处理的返回true，反之返回false
	 */
	public final boolean isSpecialNull( String name ) {
		try {
			return specialNullSet.contains( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
			return false;
		}
	}
	
	/**
	 * 设置字段的null值需要特殊处理。若实体中没有指定的字段，则不进行任何操作
	 * @param name 字段名
	 */
	public final void setSpecialNull( String name ) {
		try {
			specialNullSet.add( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
		}
	}
	
	/**
	 * 取消字段null值的特殊性。若实体中没有指定的字段，则不进行任何操作
	 * @param name 字段名
	 */
	public final void removeSpecialNull( String name ) {
		try {
			specialNullSet.remove( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
		}
	}
	
	/**
	 * 取消所有字段null值的特殊性
	 */
	public final void removeSpecialNullAll() {
		specialNullSet.clear();
	}
	
	/**
	 * 将实体作为条件，在数据库中搜索满足条件的实体
	 * @return 满足条件的实体的列表
	 */
	public final List<E> query() {
		return EntityProxy.query( (E) this );
	}

	/**
	 * 数据库中插入实体
	 * @return 若插入成功，返回true；反之返回false
	 */
	public final boolean insert() {
		return EntityProxy.insert( (E) this );
	}
	
	/**
	 * 将实体作为条件，在数据库中删除满足条件的实体
	 * @return 若删除成功，返回true；反之返回false
	 */
	public final boolean delete() {
		return EntityProxy.delete( (E) this );
	}
	
	/**
	 * 将实体作为条件，在数据库中更新满足条件的实体
	 * @param entity 更新的目标实体
	 * @return 若更新成功，返回true；反之返回false
	 */
	public final boolean update( E entity ) {
		return EntityProxy.update( (E) this, entity );
	}
	
	/**
	 * 克隆此实体
	 */
	@Override
	public final E clone() {
		Class<E> clazz = (Class<E>) getClass();
		E entity = EntityProvider.provide( clazz.getSimpleName() );
		try {
			for ( Field field : clazz.getDeclaredFields() ) {
				field.setAccessible( true );
				field.set( entity, field.get( this ) );
	
			}
		} catch ( IllegalAccessException e ) {
		}
		return entity;
	}
	
	/**
	 * 获得指定字段的值
	 * @param name 指定字段的名称
	 * @return 指定字段的值
	 * @throws NoSuchFieldException 若实体内没有指定名称的字段
	 */
	public final Object valueOf( String name ) throws NoSuchFieldException {
		Field field = getClass().getDeclaredField( name );
		return valueOf( field );
	}
	
	/**
	 * 获取指定字段的值
	 * @param field 指定的属于该实体的字段
	 * @return 指定字段的值
	 */
	public final Object valueOf( Field field ) {
		field.setAccessible( true );
		try {
			return field.get( this );
		} catch ( IllegalAccessException e ) {
			return null;
		}
	}
	
	/**
	 * <p>返回实体的字符串形式</p>
	 * <p>该形式为: <b>EntityName(field1 = value1, field2 = value2, ... , fieldN = valueN)</b></p>
	 */
	@Override
	public final String toString() {
		
		StringBuilder builder = new StringBuilder( getClass().getSimpleName() + "(" );
		for ( Field field : getClass().getDeclaredFields() ) {
			field.setAccessible( true );
			try {
				builder.append( field.getName() + "=" + field.get( this ) + ", " );
			} catch ( IllegalAccessException e ) {
			}			
		}
		builder.delete( builder.length() - 2, builder.length() ).append( ")" );
		
		return builder.toString();
	}
	
	/**
	 * <p>判断实体是否相等</p>
	 * <p>相等的条件是实体中所有字段的值相等（不包括实体ID和实体的创建时间）</p>
	 */
	@Override
	public final boolean equals( Object object ) {
		
		if ( object == null ) return false;
		return toString().equals( object.toString() );
	}
	
	/**
	 * <p>得到实体的散列码</p>
	 */
	@Override
	public final int hashCode() {

		String str = toString();
		int sgn = ( str.length() & 1 ) == 0 ? 1 : -1;
		StringBuilder builder = new StringBuilder( "0" );
		for ( char c : str.toCharArray() ) {
			builder.append( (int) c );
		}
		while ( builder.length() < 10 ) {
			builder.append( builder );
		}
		BigInteger bi = new BigInteger( builder.toString() );
		BigInteger limit = new BigInteger( String.valueOf( Integer.MAX_VALUE ) );
		bi = bi.multiply( limit.subtract( bi ) );
		
		return sgn * bi.remainder( limit ).intValue();
	}
	
}
