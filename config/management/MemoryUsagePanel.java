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
 * �ڴ�ʹ����塣�������ͼ����ʾ���ڴ�ʹ���ʺ�������ʾ�ڴ��ʹ��������
 * @author ����
 * @since 1.0, 2007-05-18
 * @version 1.0
 */
public class MemoryUsagePanel extends JPanel {
	
	// ����������ʾ�����ڴ棩
	private Paint darkGreenTexture;
	
	// ����������ʾʹ���ڴ棩
	private Paint lightGreenTexture;
	
	MemoryUsagePanel() {
		super();
	}

	// ���Ǹ���ķ������Ի����ڴ�ʹ���ʵ�ͼ�α�ʾ
	@Override
	public void paintComponent( Graphics g ) {
		
		super.paintComponent( g );
		Graphics2D g2d = (Graphics2D) g;
		Dimension size = getSize();
		
		// ���ڱ���
		int blackHeight = size.height - 3 * GAP;
		g2d.setColor( Color.BLACK );
		g2d.fillRect( GAP, 2 * GAP, size.width - 2 * GAP, blackHeight );
		
		// ����ɫ�������߶ȱ�ʾ����ڴ�����
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
		
		// ����ɫ�������߶ȱ�ʾ��ʹ���ڴ���(MB)��
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
		
		// ���м䴹ֱ����
		g2d.setColor( Color.BLACK );
		g2d.drawLine( size.width / 2, 2 * GAP, size.width / 2, 3 * GAP + darkHeight );
		
		// ����
		g2d.setColor( Color.GREEN );
		String usedMemoryText = String.valueOf( usedMemory ) + " MB";
		int textWidth = (int) g2d.getFontMetrics().getStringBounds( String.valueOf( usedMemoryText ), g2d ).getWidth();
		g2d.drawString( usedMemoryText, ( size.width - textWidth ) / 2, blackHeight + GAP + PADDING );
	}
}
