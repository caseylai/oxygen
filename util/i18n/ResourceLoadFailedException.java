package oxygen.util.i18n;

/**
 * <p>资源文件调用失败异常</p>
 * <p>资源文件调用失败的原因可能是资源文件路径不正确、磁盘I/O问题或者资源文件格式不正确。</p>
 * @author 赖昆
 * @since 1.0, 2006-12-16
 * @version 1.0
 */
public class ResourceLoadFailedException extends RuntimeException {

	ResourceLoadFailedException( String msg ) {
		super( msg );
	}
}
