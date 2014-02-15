package oxygen.entity;

/**
 * 若读实体文件的过程中出现错误，抛出此异常
 * @author 赖昆
 * @since 1.0, 2007-05-05
 * @version 1.0
 */
public class EntityReadFailedException extends Exception {

	EntityReadFailedException( String msg ) {
		super( msg );
	}
}
