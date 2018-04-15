package seamcarving;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class SeamCarver {

	public static final byte TRACEBACK_NONE = 0;
	public static final byte TRACEBACK_LEFT = 1;
	public static final byte TRACEBACK_RIGHT = 2;
	public static final byte TRACEBACK_STRAIGHT = 3;
	
	public static final int MAX_DISPLAY_WIDTH = 500;
	public static final int MAX_DISPLAY_HEIGHT = 500;
	
	public static final int SLEEP_DELAY_MS = 5;
	
	private BufferedImage image;
	private boolean tablesUpdated = false;
	private int[][] energies;
	private int[][] bestPathEnergies;
	private byte[][] traceback;
	
	private PixelEnergyFunction pixelEnergyFunction = new PixelEnergyFunction.DualGradient.Wraparound();
	
	private JFrame jframe;
	private BufferedImage displayImage;
	private Graphics2D displayGraphics;
	private ImageIcon displayImageIcon;
	private double displayImageScale;
	
	private boolean shrinking = false;
	
	public void setImage(BufferedImage image){
		if(shrinking){
			return;
		}
		this.image = image;
		tablesUpdated = false;
		energies = null;
		bestPathEnergies = null;
		traceback = null;
	}
	
	public void showImage() {
		if((image == null) || (jframe != null)){
			return;
		}
		jframe = new JFrame();
		displayImageIcon = new ImageIcon();
		JLabel jlabel = new JLabel(displayImageIcon);
        jframe.add(jlabel);
		generateDisplayImage();
        jframe.setVisible(true);
	}
	
	private void generateDisplayImage() {
		if(image == null) {
			return;
		}
		displayImageScale = Math.min(1, Math.min((double)MAX_DISPLAY_WIDTH / image.getWidth(), (double)MAX_DISPLAY_HEIGHT / image.getHeight()));				
		int newWidth = (int)(image.getWidth() * displayImageScale);
		int newHeight = (int)(image.getHeight() * displayImageScale);
		displayImage = new BufferedImage(newWidth, newHeight,  image.getType());
		displayGraphics = displayImage.createGraphics();
		displayGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		displayGraphics.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(), image.getHeight(), null);
		displayImageIcon.setImage(displayImage);
		jframe.pack();
	}
	
	public int[] getLowEnergySeam(){
		if(image == null){
			return null;
		}

		updateTables();
		int[] result = new int[image.getHeight()];
		int bestIndex = 0;
		for(int x=1;x<image.getWidth();++x) {
			if(bestPathEnergies[x][image.getHeight()-1] < bestPathEnergies[bestIndex][image.getHeight()-1]){
				bestIndex = x;
			}
		}

		int x = bestIndex;
		for(int y=image.getHeight()-1;y>0;--y) {
			result[y] = x;
			switch(traceback[x][y]) {
			case TRACEBACK_LEFT:
				--x;
				break;
			case TRACEBACK_RIGHT:
				++x;
				break;
			case TRACEBACK_STRAIGHT:
				break;
			}
		}
		result[0] = x;
		return result;
	}
	
	public void shrinkTo(int newWidth) {
		if(shrinking || (image == null) || (newWidth < 0) || (newWidth >= image.getWidth())) {
			return;
		}
		shrinking = true;
		while(image.getWidth() > newWidth) {
			int[] seam = getLowEnergySeam();
			highlightSeam(seam);
			try {
				Thread.sleep(SLEEP_DELAY_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			trimSeam(seam);
		}
		shrinking = false;

	}
	
	private void highlightSeam(int[] seam) {
		displayGraphics.setColor(new Color(255, 0, 255));
		for(int i=0;i<displayImage.getHeight();++i){
			displayGraphics.fillRect(getDisplayImageX(seam[i]), i, 1, 1);
		}
		
		jframe.repaint();
	}
	
	private void trimSeam(int[] seam) {
		if(image == null) {
			return;
		}
		BufferedImage newImage = new BufferedImage(image.getWidth()-1, image.getHeight(), image.getType());
		for(int y=0;y<image.getHeight();++y){
			for(int x=0;x<seam[y];++x) {
				newImage.setRGB(x, y, image.getRGB(x, y));
			}
			for(int x=seam[y]+1;x<newImage.getWidth();++x) {
				newImage.setRGB(x-1, y, image.getRGB(x, y));
			}
		}
		image = newImage;
		if(displayImage != null){
			generateDisplayImage();
		}
		
		tablesUpdated = false;
		updateTables();
	}
	
	private int getDisplayImageX(int x) {
		return (int)(x * displayImageScale);
	}
	
	private int getDisplayImageY(int y) {
		return (int)(y * displayImageScale);
	}
	
	private int getImageX(int x) {
		return (int)(x / displayImageScale);
	}
	
	private int getImageY(int y) {
		return (int)(y / displayImageScale);
	}
	
	private void updateTables() {
		if(tablesUpdated || (image == null)){
			return;
		}
		energies = new int[image.getWidth()][image.getHeight()];
		bestPathEnergies = new int[image.getWidth()][image.getHeight()];
		traceback = new byte[image.getWidth()][image.getHeight()];
		
		//pixel energy buffer
		for(int y=0;y<image.getHeight();++y) {
			for(int x=0;x<image.getWidth();++x) {
				energies[x][y] = pixelEnergyFunction.getPixelEnergy(image, x, y);
			}
		}
		
		//first row of path bestPathEnergies and traceback
		for(int x=0;x<image.getWidth();++x) {
			bestPathEnergies[x][0] = energies[x][0];
			traceback[x][0] = TRACEBACK_STRAIGHT;
		}

		//all the other rows of path bestPathEnergies and traceback
		for(int y=1;y<image.getHeight();++y) {
			for(int x=0;x<image.getWidth();++x) {
				int best = bestPathEnergies[x][y-1];
				traceback[x][y] = TRACEBACK_STRAIGHT;
				if(x > 0) {
					int leftPathEnergy = bestPathEnergies[x-1][y-1];
					if(leftPathEnergy < best) {
						best = leftPathEnergy;
						traceback[x][y] = TRACEBACK_LEFT;
					}
				}
				if(x < (image.getWidth()-1)) {
					int rightPathEnergy = bestPathEnergies[x+1][y-1];
					if(rightPathEnergy < best) {
						best = rightPathEnergy;
						traceback[x][y] = TRACEBACK_RIGHT;
					}
				}
				bestPathEnergies[x][y] = best + energies[x][y];
			}
		}
		tablesUpdated = true;
	}


}
