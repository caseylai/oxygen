package oxygen.io;

import java.math.BigInteger;

/**
 * ʵ���ֶ��ַ���ֵ��hash���������������hashֵ���������Ĳ�ѯ
 * @author ����
 * @since 1.0, 2007-05-24
 * @version 1.0
 * @see IndexManager ����������
 */
public class IndexHasher {
	
	// ħ��������hash���ַ���Ϊnullʱ����
	private static final long MAGIC = 0x70C796871D272L;

	// �����ַ�����hashֵ
	public static long hash( String str ) {
		
		// ��Ϊnullֵ������һ����ħ����
		if ( str == null ) return MAGIC;
		
		long sgn = ( str.length() & 1 ) == 0 ? 1L : -1L;
		StringBuilder builder = new StringBuilder( "0" );
		for ( char c : str.toCharArray() ) {
			builder.append( (int) c );
		}
		while ( builder.length() < 20 ) {
			builder.append( builder );
		}
		BigInteger bi = new BigInteger( builder.toString() );
		BigInteger limit = new BigInteger( String.valueOf( Long.MAX_VALUE ) );
		bi = bi.multiply( limit.subtract( bi ) );
		
		return sgn * bi.remainder( limit ).longValue();
	}
}
