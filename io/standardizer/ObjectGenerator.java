package oxygen.io.standardizer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import oxygen.config.ConfigKey;
import oxygen.config.DatabaseContext;


/**
 * 对象生成器。根据对象类型和字节数组构建对象。
 * @author 赖昆
 * @since 1.0, 2007-04-12
 * @version 1.0
 */
public class ObjectGenerator {
	
	// 数据存储编码方式
	private static final String ENCODING = DatabaseContext.get( ConfigKey.ENCODE );
	
	// 检查字节数组表示的对象是否为null（全0表示null）
	private static boolean isEmpty( byte[] bytes ) {
		for ( byte b : bytes ) {
			if ( b != (byte) 0 ) return false;
		}
		return true;
	}
	
	// 生成字符串
	private static String generateString( byte[] b ) {
		
		try {
			return new String( b, ENCODING ).trim();
		} catch ( UnsupportedEncodingException e ) {
			return null;
		}
	}
	
	// 生成数值
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
	
	// 生成布尔值
	private static Boolean generateBoolean( byte[] b ) {
		
		if ( b[0] == (byte) 1 ) {
			return Boolean.TRUE;
		} else if ( b[0] == (byte) -1 ) {
			return Boolean.FALSE;
		} else {
			return null;
		}
	}

	// 根据对象类型和字节数组构建对象
	static Object generate( Class<?> clazz, byte[] b ) {

		// 若字节数组为全0，则返回null
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
