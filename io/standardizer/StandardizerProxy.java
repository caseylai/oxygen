package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;
import oxygen.entity.EntityProvider;
import oxygen.io.LookupTable;


/**
 * <p>���ݱ�׼��������</p>
 * <p>���ݱ�׼��������������д�����ݿ�ǰ����Լ�����м�飬�������ݸ�ʽ��Ϊ����д�����ݿ�ĸ�ʽ���˴���������
 * ���������ݱ�׼��������һ��ͳһ�Ľӿ���ʵ�����ݵı�׼������˲��ص��þ�������ݱ�׼������ֻ��ʹ�ô˴����ɡ�</p>
 * @author ����
 * @since 1.0, 2007-04-09
 * @version 1.0
 */
public class StandardizerProxy {
	
	/**
	 * ��ָ��ʵ����б�׼����Լ�����͸�ʽ����
	 * @param <E> ʵ��
	 * @param entity ָ����ʵ��
	 * @return ��׼������ֽ�����
	 * @throws DataFormatException ��Լ�����ʧ�� 
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
	 * ��ָ��ʵ���ָ���ֶε�ֵ���б�׼����Լ�����͸�ʽ����
	 * @param <E> ʵ��
	 * @param entity ָ����ʵ��
	 * @param field ָ����ʵ���ֶ�
	 * @return ��׼������ֽ�����
	 * @throws DataFormatException ��Լ�����ʧ�� 
	 */
	public static <E extends Entity<E>> byte[] standardize( E entity, Field field ) throws DataFormatException {

		field.setAccessible( true );
		Class<?> clazz = (Class<?>) field.getGenericType();
		// ��ͬ���������ͣ�ʹ�ò�ͬ�����ݱ�׼����
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
	 * ʵ���׼�������������ʵ���׼������ֽ���������ת��Ϊʵ��
	 * @param <E> ʵ��
	 * @param b ʵ���׼������ֽ�����
	 * @param entityName ʵ����
	 * @return ���׼���������ʵ��
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
	 * ʵ���ֶα�׼���������������׼�����ʵ���ֶ�����ת��Ϊʵ���ֶε�ֵ
	 * @param b ʵ���ֶα�׼����������ֽ�����
	 * @param clazz ʵ���ֶε������
	 * @return ʵ���ֶε�ֵ����
	 */
	public static Object unstandardize( byte[] b, Class<?> clazz ) {
		return ObjectGenerator.generate( clazz, b );
	}
}
