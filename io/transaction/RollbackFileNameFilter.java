package oxygen.io.transaction;

import java.io.File;
import java.io.FilenameFilter;

/**
 * �ع��ļ�������������׺��Ϊ".rb"���ļ�����Ϊ�ǻع��ļ���
 * @author ����
 * @since 1.0, 2007-05-07
 * @version 1.0
 */
public class RollbackFileNameFilter implements FilenameFilter {

	public boolean accept( File dir, String name ) {
		return name.endsWith( ".rb" );
	}

}
