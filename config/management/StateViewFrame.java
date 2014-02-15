package oxygen.config.management;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;

import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 数据库状态窗体。在此窗体中可以浏览数据库的内部运行信息，如线程、内存使用、缓冲使用等。
 * @author 赖昆
 * @since 1.0, 2007-05-15
 * @version 1.0
 */
public class StateViewFrame extends JDialog {

	// 唯一的实例
	private static StateViewFrame frame;
	
	// 窗体宽
	static final int DIALOG_WIDTH = 700;
	
	// 窗体高
	static final int DIALOG_HEIGHT = 360;
	
	// 间距
	static final int GAP = 12;
	
	// 衬边
	static final int PADDING = GAP / 2;
	
	// 按钮面板宽
	static final int BUTTON_PANEL_WIDTH = DIALOG_WIDTH;
	
	// 按钮面板高
	static final int BUTTON_PANEL_HEIGHT = 50;
	
	// 线程显示面板宽
	static final int THREAD_PANEL_WIDTH = 150;
	
	// 状态更新频率（单位：毫秒）
	private static final long UPDATE_FREQUENCY = 1000L;
	
	private Timer timer;
	
	private ThreadTreeScrollPane threadPane;
	
	private MemoryPanel memoryPanel;
	
	private CachePanel cachePanel;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( StateViewFrame.class );
	
	// 内容面板（主要面板）
	private class ContentPanel extends JPanel {
		private ContentPanel() {
			super();
			
			SpringLayout layout = new SpringLayout();
			setLayout( layout );
			
			threadPane = new ThreadTreeScrollPane();
			memoryPanel = new MemoryPanel();
			cachePanel = new CachePanel();
			
			JScrollPane scroll = threadPane.getScrollPane();
			add( scroll );
			add( memoryPanel );
			add( cachePanel );
			
			layout.putConstraint( SpringLayout.WEST, scroll, GAP, SpringLayout.WEST,  this );
			layout.putConstraint( SpringLayout.EAST, scroll, THREAD_PANEL_WIDTH, SpringLayout.WEST, scroll );
			layout.putConstraint( SpringLayout.NORTH, scroll, GAP, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.SOUTH, scroll, 0, SpringLayout.SOUTH, this );
			
			layout.putConstraint( SpringLayout.NORTH, memoryPanel, GAP, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, memoryPanel, GAP, SpringLayout.EAST, scroll );
			layout.putConstraint( SpringLayout.EAST, memoryPanel, -GAP, SpringLayout.EAST, this );
			
			layout.putConstraint( SpringLayout.SOUTH, cachePanel, 0, SpringLayout.SOUTH, this );
			layout.putConstraint( SpringLayout.WEST, cachePanel, GAP, SpringLayout.EAST, scroll );
			layout.putConstraint( SpringLayout.EAST, cachePanel, -GAP, SpringLayout.EAST, this );
		}
	}
	
	// 按钮面板
	private class ButtonPanel extends JPanel {
		private ButtonPanel( Dimension perferredDimension ) {
			super();
			SpringLayout layout = new SpringLayout();
			setLayout( layout );
			setPreferredSize( perferredDimension );
			JButton okButton = new JButton( res.getResource( "StateViewFrame.label.ok" ) );
			okButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent event ) {
					StateViewFrame.getStateViewFrame().disappear();
				}
			} );
			add( okButton );
			layout.putConstraint( SpringLayout.EAST, okButton, -GAP, SpringLayout.EAST, this );
			layout.putConstraint( SpringLayout.NORTH, okButton, GAP, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.SOUTH, okButton, -GAP, SpringLayout.SOUTH, this );
		}
	}
	
	private StateViewFrame() {
		super();
		setTitle( res.getResource( "StateViewFrame.label.title" ) );
		setResizable( false );
		// 得到屏幕的大小，将其设置到屏幕正中
		int screenWidth = getToolkit().getScreenSize().width;
		int screenHeight = getToolkit().getScreenSize().height;
		setBounds( ( screenWidth - DIALOG_WIDTH ) >> 1, ( screenHeight - DIALOG_HEIGHT ) >> 1, DIALOG_WIDTH, DIALOG_HEIGHT );
		Container rootPane = getContentPane();
		// 加入内容面板（主要面板）
		rootPane.add( new ContentPanel(), BorderLayout.CENTER );
		// 加入按钮面板
		rootPane.add( new ButtonPanel( new Dimension( BUTTON_PANEL_WIDTH, BUTTON_PANEL_HEIGHT ) ), BorderLayout.SOUTH );
		validate();
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent event ) {
				StateViewFrame.getStateViewFrame().disappear();
			}
		} );
	}
	
	// 获取唯一的实例，这里使用lazy initialition
	static synchronized StateViewFrame getStateViewFrame() {
		if ( frame == null ) {
			frame = new StateViewFrame();
		}
		return frame;
	}
	
	// 关闭此窗体
	static void close() {
		if ( frame != null ) {
			frame.dispose();
			frame = null;
		}
	}

	// 显示窗体
	void display() {
				
		// 开启计时器，定时更新数据
		timer = new Timer( true );
		timer.schedule( new TimerTask() {
			public void run() {
				threadPane.repaint();
				memoryPanel.repaint();
				cachePanel.repaint();
			}
		}, 0L, UPDATE_FREQUENCY );
		// 显示
		setVisible( true );
	}
	
	// 隐藏窗体
	void disappear() {
		setVisible( false );
		timer.cancel();
		memoryPanel.reset();
	}
}