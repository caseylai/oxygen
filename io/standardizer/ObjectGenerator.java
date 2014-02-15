package oxygen.io.standardizer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;


/**
 * ���������������ݶ������ͺ��ֽ����鹹������
 * @author ����
 * @since 1.0, 2007-04-12
 * @version 1.0
 */
public class ObjectGenerator {
	
	// ���ݴ洢���뷽ʽ
	private static final String ENCODING = DatabaseContext.get( ConfigKey.ENCODE );
	
	// ����ֽ������ʾ�Ķ����Ƿ�Ϊnull��ȫ0��ʾnull��
	private static boolean isEmpty( byte[] bytes ) {
		for ( byte b : bytes ) {
			if ( b != (byte) 0 ) return false;
		}
		return true;
	}
	
	// �����ַ���
	private static String generateString( byte[] b ) {
		
		try {
			return new String( b, ENCODING ).trim();
		} catch ( UnsupportedEncodingException e ) {
			return null;
		}
	}
	
	// ������ֵ
	private static Number generateNumber( Class<?> clazz, byte[] b ) {
		
		Number n = null;
		ByteBuffer bb = ByteBuffer.wrap( b );
		if ( Byte.class.equals( clazz ) ) {
			n = bb.get();
		} else if ( Short.class.equals( clazz ) ) {
			n = bb.getShort();
		} else if ( Integer.class.equals( clazz ) ) {
			n = bb.getInt();
		} else if ( Long.class.equals( clazz ) ) {
			n = bb.getLong();
		} else if ( Float.class.equals( clazz ) ) {
			n = bb.getFloat();
		} else if ( Double.class.equals( clazz ) ) {
			n = bb.getDouble();
		}
		
		return n;
	}
	
	// ���ɲ���ֵ
	private static Boolean generateBoolean( byte[] b ) {
		
		if ( b[0] == (byte) 1 ) {
			return Boolean.TRUE;
		} else if ( b[0] == (byte) -1 ) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

	// ���ݶ������ͺ��ֽ����鹹������
	static Object generate( Class<?> clazz, byte[] b ) {

		// ���ֽ�����Ϊȫ0���򷵻�null
		if ( isEmpty( b ) ) return null;
		
		Object o = null;
		if ( String.class.equals( clazz ) ) {
			o = generateString( b );
		} else if ( Number.class.equals( clazz.getSuperclass() ) ) {
			o = generateNumber( clazz, b );
		} else if ( Boolean.class.equals( clazz ) ) {
			o = generateBoolean( b );
		}
		
		return o;
	}
}
