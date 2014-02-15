package oxygen.config.management;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import oxygen.io.LookupTable;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * �߳������ʾ��塣
 * @author ����
 * @since 1.0, 2007-05-19
 * @version 1.0
 */
public class ThreadTreeScrollPane {
	
	// ί�е�JScrollPane
	private final JScrollPane scroll;
	
	// ��ʾ�߳��������
	private JTree tree;
	
	private final Map<String, MutableTreeNode> nodeMap = new HashMap<String, MutableTreeNode>();
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( ThreadTreeScrollPane.class );
	
	private static final String title = res.getResource( "StateViewFrame.label.threadTitle" ) + ": ";
	
	ThreadTreeScrollPane() {
		TreeNode root = new DefaultMutableTreeNode( title );
		tree = new JTree( root );
		scroll = new JScrollPane( tree );
	}
	
	// ����ί�е�JScrollPane
	JScrollPane getScrollPane() {
		return scroll;
	}
	
	// ������
	void repaint() {
		// �õ������̵߳�������ʹ�����߳�������ӳ���
		Map<String, Integer> map = LookupTable.getUserThreadCountMap();
		// ��ӳ�������
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		int sum = 0;
		for ( String name : map.keySet() ) {
			int count = map.get( name );
			sum += count;
			StringBuilder builder = new StringBuilder( name );
			builder.append( ": " );
			builder.append( count );
			MutableTreeNode node = nodeMap.get( name );
			if ( node == null ) {
				node = new DefaultMutableTreeNode();
				nodeMap.put( name, node );
				root.add( node );
			}
			node.setUserObject( builder.toString() );
		}
		root.setUserObject( title + sum );
		model.reload();
	}

}
