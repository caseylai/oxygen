package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.annotation.constraint.SizeConstraint;


/**
 * ʵ���ֶ����ͳ��ȶ�������ȷ��ʵ���ֶε���󴢴泤�ȡ�
 * @author ����
 * @since 1.0,2007-04-12
 * @version 1.0
 */
public class TypeSizeDefiner {
	
	// UTF-8�������ռ�õ��ֽ���
	static final int UTF_MAX_BYTES = 3;

	/**
	 * ����ָ��ʵ����ָ���ֶε���󴢴泤��
	 * @param entity ʵ��
	 * @param field �ֶ�
	 * @return ����
	 */
	public static int define( Field field ) {
		
		int size = 0;
		
		try {			
			field.setAccessible( true );
			Class<?> clazz = (Class<?>) field.getGenericType();
			if ( String.class.equals( clazz ) ) {
				// �ַ����ֶεĳ����ɱ�ע���������ȵ�λΪ�ַ�����
				size = field.getAnnotation( SizeConstraint.class ).value() * UTF_MAX_BYTES;
			} else if ( Number.class.equals( clazz.getSuperclass() ) ) {
				// ��ֵ�ֶεĳ�������ֵ���;��������ȵ�λΪ�ֽ�����
				// ��ͨ�������ҵ����SIZE�ֶλ�ȡ������λ����������3λ�õ��ֽ�����
				size = (int) ( ( (Class<?>) field.getGenericType() ).getField( "SIZE" ).getInt( null ) ) >> 3;
			} else if ( Boolean.class.equals( clazz ) ) {
				// �����ֶεĳ��ȹ̶�Ϊ1�ֽڡ�ֵΪ1��ʾ�棬-1��ʾ��
				size = 1;
			}
		} catch ( IllegalAccessException e ) {
		} catch ( NoSuchFieldException e ) {
		}
		
		return size;
	}
}
