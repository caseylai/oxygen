package oxygen.config.management;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import static oxygen.config.management.StateViewFrame.GAP;
import static oxygen.config.management.StateViewFrame.PADDING;

/**
 * 内存使用面板。该面板以图形显示了内存使用率和文字显示内存的使用数量。
 * @author 赖昆
 * @since 1.0, 2007-05-18
 * @version 1.0
 */
public class MemoryUsagePanel extends JPanel {
	
	// 暗绿纹理（表示空闲内存）
	private Paint darkGreenTexture;
	
	// 亮绿纹理（表示使用内存）
	private Paint lightGreenTexture;
	
	MemoryUsagePanel() {
		super();
	}

	// 覆盖父类的方法，以画出内存使用率的图形表示
	@Override
	public void paintComponent( Graphics g ) {
		
		super.paintComponent( g );
		Graphics2D g2d = (Graphics2D) g;
		Dimension size = getSize();
		
		// 画黑背景
		int blackHeight = size.height - 3 * GAP;
		g2d.setColor( Color.BLACK );
		g2d.fillRect( GAP, 2 * GAP, size.width - 2 * GAP, blackHeight );
		
		// 画暗色绿条（高度表示最大内存数）
		int darkHeight = blackHeight - 3 * GAP;
		if ( darkGreenTexture == null ) {
			BufferedImage bi = new BufferedImage( 2, 3, BufferedImage.TYPE_INT_RGB );
			Graphics2D big2d = (Graphics2D) bi.getGraphics();
			big2d.setColor( Color.GREEN.darker() );
			big2d.fillRect( 0, 0, 1, 1 );
			big2d.fillRect( 1, 1, 1, 1 );
			darkGreenTexture = new TexturePaint( bi, new Rectangle( 2, 3 ) );
		}
		g2d.setPaint( darkGreenTexture );
		g2d.fillRect( 2 * GAP + PADDING, 3 * GAP, size.width - 5 * GAP + 1, darkHeight );
		
		// 画亮色绿条（高度表示已使用内存数(MB)）
		Runtime rt = Runtime.getRuntime();
		int maxMemory = (int) ( rt.maxMemory() / 1048576 );
		int usedMemory = (int) ( ( rt.totalMemory() - rt.freeMemory() ) / 1048576 );
		int lightHeight = (int) ( usedMemory * darkHeight / maxMemory );
		if ( lightHeight < 2 ) lightHeight = 2;
		if ( lightGreenTexture == null ) {
			BufferedImage bi = new BufferedImage( 2, 3, BufferedImage.TYPE_INT_RGB );
			Graphics2D big2d = (Graphics2D) bi.getGraphics();
			big2d.setColor( Color.GREEN );
			big2d.fillRect( 0, 0, 2, 2 );
			lightGreenTexture = new TexturePaint( bi, new Rectangle( 2, 3 ) );
		}
		g2d.setPaint( lightGreenTexture );
		g2d.fillRect( 2 * GAP + PADDING, 3 * GAP + ( darkHeight - lightHeight ), size.width - 5 * GAP + 1, lightHeight );
		
		// 画中间垂直黑条
		g2d.setColor( Color.BLACK );
		g2d.drawLine( size.width / 2, 2 * GAP, size.width / 2, 3 * GAP + darkHeight );
		
		// 画字
		g2d.setColor( Color.GREEN );
		String usedMemoryText = String.valueOf( usedMemory ) + " MB";
		int textWidth = (int) g2d.getFontMetrics().getStringBounds( String.valueOf( usedMemoryText ), g2d ).getWidth();
		g2d.drawString( usedMemoryText, ( size.width - textWidth ) / 2, blackHeight + GAP + PADDING );
	}
}
