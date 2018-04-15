package seamcarving;

import java.awt.image.BufferedImage;

public interface PixelEnergyFunction {
	
	public int getPixelEnergy(BufferedImage image, int x, int y);
	
	
	public static abstract class DualGradient implements PixelEnergyFunction {
		
		private static int getSquaredRGBGradient(int rgb1, int rgb2) {
			final int BYTEMASK = 255;
			
			final int RED1 = (rgb1 >> 16) & BYTEMASK;
			final int GREEN1 = (rgb1 >> 8) & BYTEMASK;
			final int BLUE1 = (rgb1) & BYTEMASK;

			final int RED2 = (rgb2 >> 16) & BYTEMASK;
			final int GREEN2 = (rgb2 >> 8) & BYTEMASK;
			final int BLUE2 = (rgb2) & BYTEMASK;
			
			final int REDDIFF = Math.abs(RED1 - RED2);
			final int GREENDIFF = Math.abs(GREEN1 - GREEN2);
			final int BLUEDIFF = Math.abs(BLUE1 - BLUE2);
			
			return REDDIFF*REDDIFF + GREENDIFF*GREENDIFF + BLUEDIFF*BLUEDIFF;
		}
		
		
		public static class EdgeLocked extends DualGradient {
			
			@Override
			public int getPixelEnergy(BufferedImage image, int x, int y){
				int bottom = image.getRGB(x, Math.max(0, y-1));
				int top = image.getRGB(x, Math.min(image.getHeight()-1, y+1));
				int left = image.getRGB(Math.max(0, x), y);
				int right = image.getRGB(Math.min(image.getWidth()-1, x+1), y);
				
				return getSquaredRGBGradient(bottom, top) + getSquaredRGBGradient(left, right);
			}
			
		}
		
		public static class Wraparound extends DualGradient {
			
			@Override
			public int getPixelEnergy(BufferedImage image, int x, int y){
				int bottom = image.getRGB(x, (y == 0) ? image.getHeight()-1 : y-1);
				int top = image.getRGB(x, (y + 1) % image.getHeight());
				int left = image.getRGB((x == 0) ? image.getWidth()-1 : x-1, y);
				int right = image.getRGB((x + 1) % image.getWidth(), y);
				
				return getSquaredRGBGradient(bottom, top) + getSquaredRGBGradient(left, right);
			}
			
		}
		
	}
	
}
