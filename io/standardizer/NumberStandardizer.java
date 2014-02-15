package oxygen.io.standardizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.RangeConstraint;


/**
 * ��ֵ���ݱ�׼�������������ֵ����������Byte��Short��Integer��Long��Float��Double��
 * @author ����
 * @since 1.0, 2007-04-11
 * @version 1.0
 * @param <E> ʵ��
 */
public class NumberStandardizer<E extends Entity<E>> extends AbstractStandardizer<E> {
	
	NumberStandardizer( E entity, Field field ) {
		super( entity, field );
	}
	
	// �����ֵԼ��
	@Override
	void checkConstraint() throws DataFormatException {
		
		// ����Լ�����
		super.checkConstraint();
		
		if ( o != null ) {	
			Class<? extends Number> clazz = ( (Class<?>) field.getGenericType() ).asSubclass( Number.class );
			// ��鷶ΧԼ�������÷������Ե�����ֵ���е�compareTo������
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
	
	// ���Լ������׼����ֵ����
	byte[] standardize() throws DataFormatException {
		
		// ���Լ��
		checkConstraint();
		
		// ��ֵΪ�գ�ֱ�ӷ���ȫ����ֽ�����
		if ( o == null ) {
			return new byte[ size ];
		}		
		
		// ��׼����ֵ
		return numberToBytes( (Number) o );
	}
	
	/**
	 * ����ֵ�������������Ͱ�װ�����ʽ����Byte��Short��Integer��Long��Float��Double��ת��Ϊ���Դ洢�������ֽ�������ʽ
	 * @param number ��ֵ
	 * @return �ֽ�����
	 */
	public static byte[] numberToBytes( Number number ) {
		
		// ͨ��SIZE�ֶλ�����ֽڳ���
		int size;
		try {
			size = (int) ( number.getClass().getField( "SIZE" ).getInt( null ) ) >> 3;
		} catch ( IllegalAccessException e ) {
			return new byte[0];
		} catch ( NoSuchFieldException e ) {
			return new byte[0];
		}
		
		// ͨ��ByteBufferת�������Ӧ��ʹ���������õ�ת������
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
