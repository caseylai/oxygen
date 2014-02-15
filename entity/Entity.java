package oxygen.entity;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import oxygen.io.EntityProxy;


/**
 * <p>����ʵ��Ĺ������࣬�ṩʵ�������ݿ�֧�ֵ�������</p>
 * <p>��Ҳ��һ�������࣬���������������չ���������͵ı�ʵ���ࡣ</p>
 * @author ����
 * @since 1.0, 2006-12-10
 * @version 1.0
 * @param <E> ʵ��
 * @see EntityProvider ʵ���ṩ��
 */
@SuppressWarnings("unchecked")
public abstract class Entity<E extends Entity<E>> implements Cloneable {
	
	// ���ڱ���������Ҫ���⴦���nullֵ������Ϊ��ѯ��������Ϊ����������ݵ�
	private final Set<Field> specialNullSet = Collections.synchronizedSet( new HashSet<Field>() );
	
	/**
	 * ��ʼһ������
	 */
	public static final void beginTransaction() {
		EntityProxy.beginTransaction();
	}
	
	/**
	 * �ύһ������
	 */
	public static final void commit() {
		EntityProxy.commit();
	}
	
	/**
	 * ����һ������
	 */
	public static final void rollback() {
		EntityProxy.rollback();
	}
	
	/**
	 * ���ָ�����ֶ��Ƿ������⴦��null���ֶ�
	 * @param name �ֶ���
	 * @return �����⴦��ķ���true����֮����false
	 */
	public final boolean isSpecialNull( String name ) {
		try {
			return specialNullSet.contains( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
			return false;
		}
	}
	
	/**
	 * �����ֶε�nullֵ��Ҫ���⴦����ʵ����û��ָ�����ֶΣ��򲻽����κβ���
	 * @param name �ֶ���
	 */
	public final void setSpecialNull( String name ) {
		try {
			specialNullSet.add( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
		}
	}
	
	/**
	 * ȡ���ֶ�nullֵ�������ԡ���ʵ����û��ָ�����ֶΣ��򲻽����κβ���
	 * @param name �ֶ���
	 */
	public final void removeSpecialNull( String name ) {
		try {
			specialNullSet.remove( getClass().getDeclaredField( name ) );
		} catch ( NoSuchFieldException e ) {
		}
	}
	
	/**
	 * ȡ�������ֶ�nullֵ��������
	 */
	public final void removeSpecialNullAll() {
		specialNullSet.clear();
	}
	
	/**
	 * ��ʵ����Ϊ�����������ݿ�����������������ʵ��
	 * @return ����������ʵ����б�
	 */
	public final List<E> query() {
		return EntityProxy.query( (E) this );
	}

	/**
	 * ���ݿ��в���ʵ��
	 * @return ������ɹ�������true����֮����false
	 */
	public final boolean insert() {
		return EntityProxy.insert( (E) this );
	}
	
	/**
	 * ��ʵ����Ϊ�����������ݿ���ɾ������������ʵ��
	 * @return ��ɾ���ɹ�������true����֮����false
	 */
	public final boolean delete() {
		return EntityProxy.delete( (E) this );
	}
	
	/**
	 * ��ʵ����Ϊ�����������ݿ��и�������������ʵ��
	 * @param entity ���µ�Ŀ��ʵ��
	 * @return �����³ɹ�������true����֮����false
	 */
	public final boolean update( E entity ) {
		return EntityProxy.update( (E) this, entity );
	}
	
	/**
	 * ��¡��ʵ��
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
	 * ���ָ���ֶε�ֵ
	 * @param name ָ���ֶε�����
	 * @return ָ���ֶε�ֵ
	 * @throws NoSuchFieldException ��ʵ����û��ָ�����Ƶ��ֶ�
	 */
	public final Object valueOf( String name ) throws NoSuchFieldException {
		Field field = getClass().getDeclaredField( name );
		return valueOf( field );
	}
	
	/**
	 * ��ȡָ���ֶε�ֵ
	 * @param field ָ�������ڸ�ʵ����ֶ�
	 * @return ָ���ֶε�ֵ
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
	 * <p>����ʵ����ַ�����ʽ</p>
	 * <p>����ʽΪ: <b>EntityName(field1 = value1, field2 = value2, ... , fieldN = valueN)</b></p>
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
	 * <p>�ж�ʵ���Ƿ����</p>
	 * <p>��ȵ�������ʵ���������ֶε�ֵ��ȣ�������ʵ��ID��ʵ��Ĵ���ʱ�䣩</p>
	 */
	@Override
	public final boolean equals( Object object ) {
		
		if ( object == null ) return false;
		return toString().equals( object.toString() );
	}
	
	/**
	 * <p>�õ�ʵ���ɢ����</p>
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
