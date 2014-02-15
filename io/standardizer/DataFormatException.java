package oxygen.io.standardizer;

/**
 * 当即将写入数据库的数据不满足规定的约束时，抛出此异常
 * @author 赖昆
 * @since 1.0, 2007-04-11
 * @version 1.0
 */
public class DataFormatException extends Exception {
	
	DataFormatException ( String msg ) {
		super( msg );
	}
}
