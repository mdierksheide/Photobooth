import java.awt.image.*;

class ImageFilters
{
	protected static int whitePixel = 0xFFFFFFFF;
	protected static int blackPixel = 0xFF000000;
	
	static class Blur
	{
		public static void Run(BufferedImage img)
		{
			double[][] kernel = CalculateKernel();
			
			for (int x = 0; x < img.getWidth(); x++)
			{
				for (int y = 0; y < img.getHeight(); y++)
				{
					// ARGB format: AAAAAAAA | RRRRRRRR | GGGGGGGG | BBBBBBBB
					int argb = FilterPixel(img, kernel, x, y);
					img.setRGB(x, y, argb);
				}
			}
		}
		
		private static int FilterPixel(
			BufferedImage img, 
			double[][] kernel,
			int x, 
			int y)
		{
			int radius = kernel[0].length / 2;
			int newPixel = 0xFF000000;
			int mask = 0x000000FF;
			int numColors = 3;
			int bitsPerColor = 8;
			
			for (int color = 0; color < numColors; color++)
			{
				double sum = 0.0;
				int shift = color * bitsPerColor;
				
				for (int i = -radius; i <= radius; i++)
				{
					for (int j = -radius; j <= radius; j++)
					{
						int xTemp = x + i;
						int yTemp = y + j;
						
						if (0 <= xTemp && xTemp < img.getWidth() && 
							0 <= yTemp && yTemp < img.getHeight())
						{
							// Get the color value as an int
							int colorVal = (img.getRGB(xTemp, yTemp) & 
								(mask << shift)) >> shift;
							
							sum += colorVal * kernel[i + radius][j + radius];
						}
					}
				}
				
				newPixel |= ((int)sum & mask) << shift;
			}
			
			return newPixel;
		}
		
		private static double[][] CalculateKernel()
		{
			int length = 5;
			int radius = length/2;
			double stdDev = 10;
			
			double[][] kernel = new double[length][length];
			double sum = 0;
			
			double constant = 1 / (2 * Math.PI * stdDev * stdDev);
			
			for (int x = -radius; x <= radius; x++)
			{
				for (int y = -radius; y <= radius; y++)
				{
					double distance = (x * x + y * y) / (2 * stdDev * stdDev);
					
					kernel[x + radius][y + radius] = 
						constant * Math.exp(-distance);
					
					sum += kernel[x + radius][y + radius];
				}
			}
			
			for (int x = -radius; x <= radius; x++)
			{
				for (int y = -radius; y <= radius; y++)
				{
					kernel[x + radius][y + radius] = 
						kernel[x + radius][y + radius] / sum;
				}
			}
			
			return kernel;
		}
	}
	
	static class Edges
	{
		private static int STRONG_EDGE = 2;
		private static int WEAK_EDGE = 1;
		private static int NOT_EDGE = 0;
		
		private static int edgePixel = whitePixel;
		private static int nonEdgePixel = blackPixel;
		
		public static void Run(BufferedImage img)
		{
			int threshLow = 0;
			int threshHigh = 255;
			
			Blur.Run(img);
			GrayScale.Run(img);
			
			int[][] magnitudes = new int[img.getWidth()][img.getHeight()];
			double[][] directions = new double[img.getWidth()][img.getHeight()];
			
			SobelOperator(img, magnitudes, directions);
			int[][] output = NonMaximumSuppression(img, magnitudes, directions);
			int[][] edges = DoubleThreshold(output, 100, 0);
			HysteresisTracking(img, edges);
		}
		
		private static void SobelOperator(
			BufferedImage img,
			int[][] magnitudes,
			double[][] directions)
		{
			int width = img.getWidth();
			int height = img.getHeight();
			int[][] gx = {{-1, 0, 1},
						  {-2, 0, 2},
						  {-1, 0, 1}};
			int[][] gy = {{ 1, 2, 1}, 
						  { 0, 0, 0},
						  {-1,-2,-1}};
			
			// Loop through image
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					int xSum = 0;
					int ySum = 0;
					int radius = gx[0].length / 2;
					
					// Loop through kernel
					for (int i = -radius; i <= radius; i++)
					{
						for (int j = -radius; j <= radius; j++)
						{
							// Calculate the Sobel vector values for each pixel
							int xTemp = (x + i + width) % width;
							int yTemp = (y + j + height) % height;
							
							int grayVal = img.getRGB(xTemp, yTemp) & 0xFF;
							
							xSum += grayVal * gx[i + radius][j + radius];
							ySum += grayVal * gy[i + radius][j + radius];
						}
					}
					
					// Calculate magnitude and direction
					magnitudes[x][y] = Math.abs(xSum) + Math.abs(ySum);
					
					if (xSum != 0)
					{
						double rawDir = Math.atan(ySum/xSum) * 180 / Math.PI;
						
						if (-22.5 < rawDir && rawDir < 22.5)
						{
							directions[x][y] = 0;
						}
						else if (22.5 <= rawDir && rawDir < 67.5)
						{
							directions[x][y] = 45;
						}
						else if (-90 < rawDir && rawDir <= -67.5 ||
								 67.5 <= rawDir && rawDir < 90)
						{
							directions[x][y] = 90;
						}
						else if (-67.5 < rawDir && rawDir <= -22.5)
						{
							directions[x][y] = 135;
						}
					}
					else
					{
						if (ySum == 0)
						{
							directions[x][y] = 0;
						}
						else
						{
							directions[x][y] = 90;
						}
					}
				}
			}
		}
		
		private static int[][] NonMaximumSuppression(
			BufferedImage img,
			int[][] magnitudes,
			double[][] directions)
		{
			int width = img.getWidth();
			int height = img.getHeight();
			int[][] output = new int[width][height];
		
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					int offsetX, offsetY;
					int currMag = magnitudes[x][y];
					double currDir = directions[x][y];
					
					// calculate x offset
					double sin = Math.sin(Math.toRadians(currDir));
					if (sin != 0)
					{
						// Positive or negative 1 depending on sign of sin
						offsetX = (int)(sin/Math.abs(sin));
					}
					else
					{
						offsetX = 0;
					}
					
					// Calculate y offset
					double cos = Math.cos(Math.toRadians(currDir));
					if (cos != 0)
					{
						// Positive or negative 1 depending on sign of cos
						offsetY = (int)(cos/Math.abs(cos));
					}
					else
					{
						offsetY = 0;
					}
					
					// Calculate x and y values of pixels being compared to,
					// be sure to account for edge pixels with modulus
					int compX1 = (x + offsetX + width) % width; 
					int compY1 = (y + offsetY + height) % height;
					int compX2 = (x - offsetX + width) % width; 
					int compY2 = (y - offsetY + height) % height;
					
					// Determine if current pixel is a local maxima
					if (currMag > magnitudes[compX1][compY1] &&
						currMag > magnitudes[compX2][compY2])
					{
						output[x][y] = magnitudes[x][y];
					}
					else
					{
						output[x][y] = -1;
					}
				}
			}
			
			return output;
		}
		
		private static int[][] DoubleThreshold(int[][] array, int th, int tl)
		{
			int width = array.length;
			int height = array[0].length;
			int[][] edges = new int[width][height];
			
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					if (array[x][y] > th)
					{
						edges[x][y] = STRONG_EDGE;
					}
					else if (array[x][y] > tl)
					{
						edges[x][y] = WEAK_EDGE;
					}
					else
					{
						edges[x][y] = NOT_EDGE;
					}
				}
			}
			
			return edges;
		}
		
		private static void HysteresisTracking(BufferedImage img, int[][] edges)
		{
			int width = edges.length;
			int height = edges[0].length;
			
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					if (edges[x][y] == STRONG_EDGE)
					{
						// Strong edges are immediately set to edges
						img.setRGB(x, y, edgePixel);
					}
					else if (edges[x][y] == WEAK_EDGE)
					{
						// Weak edges are an edge if connected to a strong edge
						boolean isEdge = false;
						
						for (int i = -1; i <= 1; i++)
						{
							for (int j = -1; j <= 1; j++)
							{
								int xTemp = (x + i + width) % width;
								int yTemp = (y + j + height) % height;
								
								if (edges[xTemp][yTemp] == 2)
								{
									isEdge = true;
								}
							}
						}
						
						if (isEdge)
						{
							img.setRGB(x, y, edgePixel);
						}
						else
						{
							img.setRGB(x, y, nonEdgePixel);
						}
					}
					else
					{
						// Not edges are immdiately set to not an edge... duh
						img.setRGB(x, y, nonEdgePixel);
					}
				}
			}
		}
	}
	
	static class Sketch
	{
		public static void Run(BufferedImage img)
		{
			GrayScale.Run(img);
			SobelOperator(img);
		}
		
		private static void SobelOperator(
			BufferedImage img)
		{
			BufferedImage imgCopy = new BufferedImage(
				img.getColorModel(),
				img.copyData(null),
				img.isAlphaPremultiplied(),
				null);
			int width = img.getWidth();
			int height = img.getHeight();
			int[][] gx = {{-1, 0, 1},
						  {-2, 0, 2},
						  {-1, 0, 1}};
			int[][] gy = {{ 1, 2, 1}, 
						  { 0, 0, 0},
						  {-1,-2,-1}};
			
			// Loop through image
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					int xSum = 0;
					int ySum = 0;
					int radius = gx[0].length / 2;
					
					// Loop through kernel
					for (int i = -radius; i <= radius; i++)
					{
						for (int j = -radius; j <= radius; j++)
						{
							// Calculate the Sobel vector values for each pixel
							int xTemp = (x + i + width) % width;
							int yTemp = (y + j + height) % height;
							
							int grayVal = imgCopy.getRGB(xTemp, yTemp) & 0xFF;
							
							xSum += grayVal * gx[i + radius][j + radius];
							ySum += grayVal * gy[i + radius][j + radius];
						}
					}
					
					// Find gradient magnitude in the range 0 <= magnitude < 256
					int magnitude = Math.abs(xSum) + Math.abs(ySum);
					magnitude = (magnitude + 4 * 256) % 256;

					// Flip black and white colors so it looks like a sketch
					magnitude = 255 - magnitude;
					
					int pix = (magnitude << 16) + (magnitude << 8) + magnitude;
					img.setRGB(x, y, pix);
				}
			}
		}
	}
	
	static class GrayScale
	{
		public static void Run(BufferedImage img)
		{
			double weightR = 0.2126;
			double weightG = 0.7152;
			double weightB = 0.0722;
			
			for (int x = 0; x < img.getWidth(); x++)
			{
				for (int y = 0; y < img.getHeight(); y++)
				{
					int rgb = img.getRGB(x, y);
					int r = (rgb >> 16) & 0x000000FF;
					int g = (rgb >> 8) & 0x000000FF;
					int b = rgb & 0x000000FF;
					
					int grayVal = 
						(int)(weightR * r + weightG * g + weightB * b);
					int grayPixel = (grayVal << 16) + (grayVal << 8) + grayVal;
					img.setRGB(x, y, grayPixel);
				}
			}
		}
	}
}