package oxygen.io;

import java.math.BigInteger;

/**
 * 实体字段字符串值的hash计算器，计算出的hash值用于索引的查询
 * @author 赖昆
 * @since 1.0, 2007-05-24
 * @version 1.0
 * @see IndexManager 索引管理器
 */
public class IndexHasher {
	
	// 魔数。当待hash的字符串为null时返回
	private static final long MAGIC = 0x70C796871D272L;

	// 返回字符串的hash值
	public static long hash( String str ) {
		
		// 若为null值，返回一个“魔数”
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
