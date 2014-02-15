package oxygen.config.management;

import static oxygen.config.management.MemoryPanel.MEMORY_PANEL_HEIGHT;
import static oxygen.config.management.StateViewFrame.BUTTON_PANEL_HEIGHT;
import static oxygen.config.management.StateViewFrame.DIALOG_HEIGHT;
import static oxygen.config.management.StateViewFrame.DIALOG_WIDTH;
import static oxygen.config.management.StateViewFrame.GAP;
import static oxygen.config.management.StateViewFrame.THREAD_PANEL_WIDTH;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import oxygen.io.cache.CacheManager;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * Cache��塣���ڶ�̬��ʾCache��״̬
 * @author ����
 * @since 1.0, 2007-05-19
 * @version 1.0
 */
public class CachePanel extends JPanel {
	
	// Cache����
	static final int CACHE_PANEL_WIDTH = DIALOG_WIDTH - THREAD_PANEL_WIDTH;
	
	// Cache����
	static final int CACHE_PANEL_HEIGHT = DIALOG_HEIGHT - BUTTON_PANEL_HEIGHT - MEMORY_PANEL_HEIGHT - 4 * GAP;

	// ����ʹ�õ�Cache������
	private final JLabel cacheType;
	
	// Cache�л���Ľ������
	private final JLabel cacheSum;
	
	// ����Cache��ť
	private final JButton clearCacheButton;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( CachePanel.class );
			
	CachePanel() {
		
		super();
		SpringLayout layout = new SpringLayout();
		setLayout( layout );
		setPreferredSize( new Dimension( CACHE_PANEL_WIDTH, CACHE_PANEL_HEIGHT ) );
		setBorder( BorderFactory.createTitledBorder( BorderFactory.createEtchedBorder(), res.getResource( "StateViewFrame.label.cacheTitle" ) ) );
		
		cacheType = new JLabel( res.getResource( "StateViewFrame.label.cacheType" ) );
		cacheSum = new JLabel( res.getResource( "StateViewFrame.label.cacheSum" ) );
		clearCacheButton = new JButton( res.getResource( "StateViewFrame.label.clearCache" ) );
		
		clearCacheButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent event ) {
				CacheManager.reset();
			}
		} );
		
		add( cacheType );
		add( cacheSum );
		add( clearCacheButton );
			
		layout.putConstraint( SpringLayout.SOUTH, cacheSum, -GAP, SpringLayout.SOUTH, this );
		layout.putConstraint( SpringLayout.WEST, cacheSum, GAP, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.SOUTH, cacheType, -GAP, SpringLayout.NORTH, cacheSum );
		layout.putConstraint( SpringLayout.WEST, cacheType, GAP, SpringLayout.WEST, this );
		layout.putConstraint( SpringLayout.SOUTH, clearCacheButton, -GAP, SpringLayout.SOUTH, this );
		layout.putConstraint( SpringLayout.EAST, clearCacheButton, -GAP, SpringLayout.EAST, this );
	}
	
	// ���Ǹ��෽�����Զ�̬��ʾCache��Ϣ
	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );
		cacheType.setText( res.getResource( "StateViewFrame.label.cacheType" ) + CacheManager.getCacheManagerCount() );
		cacheSum.setText( res.getResource( "StateViewFrame.label.cacheSum" ) + CacheManager.getAllCacheCount() );
	}
}
