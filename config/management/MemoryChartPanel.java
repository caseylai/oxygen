package oxygen.config.management;

import static oxygen.config.management.StateViewFrame.GAP;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JPanel;

/**
 * 内存图形面板。该面板以图形方式显示了内存的历史使用记录。
 * @author 赖昆
 * @since 1.0, 2007-05-18
 * @version 1.0
 */
public class MemoryChartPanel extends JPanel {
	
	static final int MEMORY_CHART_GRID_SIZE = 12;
	
	static final int MEMORY_CHART_SCALE_WIDTH = 2;
	
	// 用一个双端队列保存内存使用历史记录
	private final Deque<Integer> deque = new LinkedList<Integer>();
	
	// 保存背景渲染的起始偏移量
	private int startOffset = 0;
	
	private BufferedImage backgroundGrid;
	
	MemoryChartPanel() {
		super();
	}
	
	// 更新下一次开始渲染背景的位置
	private void updateStartOffset() {
		startOffset -= 2;
		if ( startOffset <= -MEMORY_CHART_GRID_SIZE ) {
			startOffset = 0;
		}
	}
	
	// 覆盖父类的方法，以动态显示内存的历史使用情况记录
	@Override
	public void paintComponent( Graphics g ) {
		
		super.paintComponent( g );
		Graphics2D g2d = (Graphics2D) g;
		Dimension size = getSize();
		int viewWidth = size.width - 2 * GAP;
		int viewHeight = size.height - 3 * GAP;
		
		// 动态画方格背景
		if ( backgroundGrid == null ) {
			backgroundGrid = new BufferedImage( viewWidth, viewHeight, BufferedImage.TYPE_INT_RGB );
			Graphics2D big2d = (Graphics2D) backgroundGrid.getGraphics();
			big2d.setColor( Color.GREEN.darker() );
			for ( int y = MEMORY_CHART_GRID_SIZE ; y < viewHeight ; y += MEMORY_CHART_GRID_SIZE  ) {
				big2d.drawLine( 0, y, viewWidth + GAP, y );
			}
		}
		g2d.drawImage( backgroundGrid, GAP, 2 * GAP, null );
		g2d.setColor( Color.GREEN.darker() );
		for ( int x = MEMORY_CHART_GRID_SIZE + startOffset + GAP ; x < viewWidth + GAP; x += MEMORY_CHART_GRID_SIZE ) {
			g2d.drawLine( x, 2 * GAP, x, viewHeight + 2 * GAP );
		}
		updateStartOffset();
		
		// 开始画历史折线
		g2d.setColor( Color.YELLOW );
		Runtime rt = Runtime.getRuntime();
		int usedMemory = (int) ( ( rt.totalMemory() - rt.freeMemory() ) / 1048576 );
		int maxMemory = (int) ( rt.maxMemory() / 1048576 );
		deque.offer( new Integer( usedMemory ) );
		int maxScale = viewWidth / MEMORY_CHART_SCALE_WIDTH;
		if ( deque.size() > maxScale ) {
			deque.poll();
		}
		int lastY = -1;
		int x = viewWidth + GAP;
		Iterator<Integer> iterator = deque.descendingIterator();
		while ( iterator.hasNext() ) {
			int y = 2 * GAP + viewHeight - iterator.next() * viewHeight / maxMemory;
			if ( lastY == -1 ) {
				g2d.fillRect( x, y, 1, 1 );
			} else {
				g2d.drawLine( x, y, x + MEMORY_CHART_SCALE_WIDTH, lastY );
			}
			x -= MEMORY_CHART_SCALE_WIDTH;
			lastY = y;
		}
	}

	// 重置面板。清除所有历史数据
	void reset() {
		deque.clear();
		startOffset = 0;
	}
}
