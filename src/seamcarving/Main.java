package seamcarving;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

public class Main {
	
	private static final String debugFilePath = "./images/surfing_small.png";
	
	public static void main(String[] args) {
		File file = null;
		if(debugFilePath != null) {
			file = new File(debugFilePath);
		} else {
			JFileChooser filechooser = new JFileChooser();
			if (filechooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				file = filechooser.getSelectedFile();
			}
		}
		if(file != null) {
	        SeamCarver carver = new SeamCarver();
	        BufferedImage image = null;
	        try {
	        	image = ImageIO.read(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
	        carver.setImage(image);
	        carver.showImage();
	        carver.shrinkTo(100);
		}
	}

}
