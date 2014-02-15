package oxygen.config.management;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * �ڴ���塣�����ڴ�ʹ�������ڴ�ʹ�ü�¼��塣
 * @author ����
 * @since 1.0, 2007-05-19
 * @version 1.0
 */
public class MemoryPanel extends JPanel {
	
	// �ڴ�����
	static final int MEMORY_PANEL_HEIGHT = 160;
	
	// �ڴ�ʹ������
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
	
	// ���Ǹ���ķ���������ڲ������״̬����
	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		memoryUsagePanel.repaint();
		memoryChartPanel.repaint();
	}
	
	// �������״̬
	void reset() {
		memoryChartPanel.reset();
	}
}
