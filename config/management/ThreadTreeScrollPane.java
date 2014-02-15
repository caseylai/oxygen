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
 * 线程情况显示面板。
 * @author 赖昆
 * @since 1.0, 2007-05-19
 * @version 1.0
 */
public class ThreadTreeScrollPane {
	
	// 委托的JScrollPane
	private final JScrollPane scroll;
	
	// 显示线程情况的树
	private JTree tree;
	
	private final Map<String, MutableTreeNode> nodeMap = new HashMap<String, MutableTreeNode>();
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( ThreadTreeScrollPane.class );
	
	private static final String title = res.getResource( "StateViewFrame.label.threadTitle" ) + ": ";
	
	ThreadTreeScrollPane() {
		TreeNode root = new DefaultMutableTreeNode( title );
		tree = new JTree( root );
		scroll = new JScrollPane( tree );
	}
	
	// 返回委托的JScrollPane
	JScrollPane getScrollPane() {
		return scroll;
	}
	
	// 更新树
	void repaint() {
		// 得到各个线程的名字与使用其线程数量的映射表
		Map<String, Integer> map = LookupTable.getUserThreadCountMap();
		// 按映射表构建树
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
