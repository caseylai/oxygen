package oxygen.util.i18n;

/**
 * <p>没有相关资源异常</p>
 * <p>发生的原因是在资源加载器及所有的父加载器中都找不到所需的资源（键名），
 * 抛出这个异常后需要确认资源文件是否正确配置和其中是否有这个资源（键名）</p>
 * @author 赖昆
 * @since 1.0, 2006-12-16
 * @version 1.0
 */
public class NoSuchResourceException extends RuntimeException {

	NoSuchResourceException( String msg ) {
		super( msg );
	}
}
