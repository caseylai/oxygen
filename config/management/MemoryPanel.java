package oxygen.config.management;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 内存面板。包括内存使用面板和内存使用记录面板。
 * @author 赖昆
 * @since 1.0, 2007-05-19
 * @version 1.0
 */
public class MemoryPanel extends JPanel {
	
	// 内存面板高
	static final int MEMORY_PANEL_HEIGHT = 160;
	
	// 内存使用面板宽
	static final int MEMORY_USAGE_PANEL_WIDTH = 100;
	
	private final MemoryUsagePanel memoryUsagePanel;
	
	private final MemoryChartPanel memoryChartPanel;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( MemoryPanel.class );

	MemoryPanel() {
		super( new BorderLayout() );

		memoryUsagePanel = new MemoryUsagePanel();
		memoryUsagePanel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), res.getResource( "StateViewFrame.label.memoryUsageTitle" ) ) );
		memoryUsagePanel.setPreferredSize( new Dimension( MEMORY_USAGE_PANEL_WIDTH, MEMORY_PANEL_HEIGHT ) );
		
		memoryChartPanel = new MemoryChartPanel();
		memoryChartPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), res.getResource( "StateViewFrame.label.memoryChartTitle" ) ) );
		
		add( memoryUsagePanel, BorderLayout.WEST );
		add( memoryChartPanel, BorderLayout.CENTER );
	}
	
	// 覆盖父类的方法，完成内部组件的状态更新
	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		memoryUsagePanel.repaint();
		memoryChartPanel.repaint();
	}
	
	// 重置面板状态
	void reset() {
		memoryChartPanel.reset();
	}
}
