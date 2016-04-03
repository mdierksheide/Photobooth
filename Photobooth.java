import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

class Photobooth
{	
	public static void main(String[] args)
	{
		System.out.printf("%n");
		
		if (args.length != 2)
		{
			System.out.printf("Incorrect usage. Correct usage: " +
				"java ImageFilter pictureFileName imageFilter%n");
			return;
		}
		
		// Break image file name into its name and extension for use on write
		String fullImgName = args[0];
		int periodIndex = fullImgName.lastIndexOf(".");
		String name = fullImgName.substring(0, periodIndex);
		String ext = fullImgName.substring(periodIndex + 1);
		
		// Read the inputted image
		BufferedImage img = ReadImage(fullImgName);
		if (img == null)
		{
			System.out.println("Failed to read image. Make sure it is " +
				"in the Resources folder and try again.");
			return;
		}
		
		// Run algorithm
		System.out.printf("Applying filter...................................");
		switch(args[1].toLowerCase())
		{
			case "blur":
				ImageFilters.Blur.Run(img);
				break;
			case "edges":
				ImageFilters.Edges.Run(img);
				break;
			case "grayscale":
				ImageFilters.GrayScale.Run(img);
				break;
			case "sketch":
				ImageFilters.Sketch.Run(img);
				break;
			default:
				System.out.printf("Specified image filter was not recognized.");
				System.out.printf("Use \"-help\" for a list of valid image");
				System.out.printf("filters. Terminating program.%n");
				return;
		}
		System.out.printf("Complete%n");
		
		// Write the processed image
		boolean success = 
			WriteImage(img, name + "_" + args[1].toLowerCase(), ext);
		if (success == false)
		{
			System.out.println("Failed to write image.");
			return;
		}
		
		return;
	}
	
	private static BufferedImage ReadImage(String imgName)
	{
		try
		{
			String imgPath = "\\Resources\\" + imgName;
			BufferedImage imageIn = 
				ImageIO.read(ImageFilter.class.getResource(imgPath));
			return imageIn;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	private static boolean WriteImage(
		BufferedImage img, 
		String name, 
		String ext)
	{
		try
		{
			String imgPath = ".\\Resources\\" + name + "." + ext;
			ImageIO.write(img, ext, new File(imgPath));
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}