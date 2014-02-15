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
 * ���ݿ�״̬���塣�ڴ˴����п���������ݿ���ڲ�������Ϣ�����̡߳��ڴ�ʹ�á�����ʹ�õȡ�
 * @author ����
 * @since 1.0, 2007-05-15
 * @version 1.0
 */
public class StateViewFrame extends JDialog {

	// Ψһ��ʵ��
	private static StateViewFrame frame;
	
	// �����
	static final int DIALOG_WIDTH = 700;
	
	// �����
	static final int DIALOG_HEIGHT = 360;
	
	// ���
	static final int GAP = 12;
	
	// �ı�
	static final int PADDING = GAP / 2;
	
	// ��ť����
	static final int BUTTON_PANEL_WIDTH = DIALOG_WIDTH;
	
	// ��ť����
	static final int BUTTON_PANEL_HEIGHT = 50;
	
	// �߳���ʾ����
	static final int THREAD_PANEL_WIDTH = 150;
	
	// ״̬����Ƶ�ʣ���λ�����룩
	private static final long UPDATE_FREQUENCY = 1000L;
	
	private Timer timer;
	
	private ThreadTreeScrollPane threadPane;
	
	private MemoryPanel memoryPanel;
	
	private CachePanel cachePanel;
	
	private static final ResourceLoader res = ResourceLoaderProvider.provide( StateViewFrame.class );
	
	// ������壨��Ҫ��壩
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
	
	// ��ť���
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
		// �õ���Ļ�Ĵ�С���������õ���Ļ����
		int screenWidth = getToolkit().getScreenSize().width;
		int screenHeight = getToolkit().getScreenSize().height;
		setBounds( ( screenWidth - DIALOG_WIDTH ) >> 1, ( screenHeight - DIALOG_HEIGHT ) >> 1, DIALOG_WIDTH, DIALOG_HEIGHT );
		Container rootPane = getContentPane();
		// ����������壨��Ҫ��壩
		rootPane.add( new ContentPanel(), BorderLayout.CENTER );
		// ���밴ť���
		rootPane.add( new ButtonPanel( new Dimension( BUTTON_PANEL_WIDTH, BUTTON_PANEL_HEIGHT ) ), BorderLayout.SOUTH );
		validate();
		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent event ) {
				StateViewFrame.getStateViewFrame().disappear();
			}
		} );
	}
	
	// ��ȡΨһ��ʵ��������ʹ��lazy initialition
	static synchronized StateViewFrame getStateViewFrame() {
		if ( frame == null ) {
			frame = new StateViewFrame();
		}
		return frame;
	}
	
	// �رմ˴���
	static void close() {
		if ( frame != null ) {
			frame.dispose();
			frame = null;
		}
	}

	// ��ʾ����
	void display() {
				
		// ������ʱ������ʱ��������
		timer = new Timer( true );
		timer.schedule( new TimerTask() {
			public void run() {
				threadPane.repaint();
				memoryPanel.repaint();
				cachePanel.repaint();
			}
		}, 0L, UPDATE_FREQUENCY );
		// ��ʾ
		setVisible( true );
	}
	
	// ���ش���
	void disappear() {
		setVisible( false );
		timer.cancel();
		memoryPanel.reset();
	}
}