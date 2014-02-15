package oxygen.io.standardizer;

import java.lang.reflect.Field;

import oxygen.entity.Entity;


/**
 * �������ݱ�׼����
 * @author ����
 * @since 1.0, 2007-04-12
 * @version 1.0
 * @param <E> ʵ��
 */
public class BooleanStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {

	BooleanStandardizer( E entity, Field field ) {
		super( entity, field );
	}
	
	// ���Լ������׼����������
	byte[] standardize() throws DataFormatException {
		
		// ����Լ�����
		checkConstraint();

		// ��׼������ֵ������0��ʾ������1��ʾ������-1��ʾ��
		byte[] b = new byte[1];
		if ( o != null && (Boolean) o .equals( true ) ) {
			b[0] = (byte) 1;
		} else if ( o != null && (Boolean) o .equals( false ) ) {
			b[0] = (byte) -1;
		}
		return b;
	}

}
