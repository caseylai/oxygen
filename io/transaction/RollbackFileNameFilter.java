package oxygen.io.transaction;

import java.io.File;
import java.io.FilenameFilter;

/**
 * 回滚文件名过滤器。后缀名为".rb"的文件被认为是回滚文件。
 * @author 赖昆
 * @since 1.0, 2007-05-07
 * @version 1.0
 */
public class RollbackFileNameFilter implements FilenameFilter {

	public boolean accept( File dir, String name ) {
		return name.endsWith( ".rb" );
	}

}
