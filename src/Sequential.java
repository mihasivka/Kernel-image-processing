import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Sequential {

	public static void convolution(String fileLocation, float[][] kernelMatrix, float multiplier) throws IOException {

        int len = kernelMatrix.length;

        BufferedImage img1 = ImageIO.read(new File(fileLocation));
        int width = img1.getWidth();
        int height = img1.getHeight();

        BufferedImage img2 = new BufferedImage(width, height, img1.getType());

        long start = System.currentTimeMillis();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                float oldRed = 0f;
                float oldGreen = 0f;
                float oldBlue = 0f;

                for (int k = 0; k < len; k++) {
                    for (int l = 0; l < len; l++) {

                        int w = (i - len / 2 + k + width) % width;
                        int h = (j - len / 2 + l + height) % height;

                        int rgbTotal = img1.getRGB(w, h);

                        int rgbRed = (rgbTotal >> 16) & 0xff;
                        int rgbGreen = (rgbTotal >> 8) & 0xff;
                        int rgbBlue = (rgbTotal) & 0xff;

                        oldRed += (rgbRed * kernelMatrix[k][l]);
                        oldGreen += (rgbGreen * kernelMatrix[k][l]);
                        oldBlue += (rgbBlue * kernelMatrix[k][l]);
                        
                    }
                }

                int red = Math.min(Math.max((int) (oldRed * multiplier), 0), 255);
                int green = Math.min(Math.max((int) (oldGreen * multiplier), 0), 255);
                int blue = Math.min(Math.max((int) (oldBlue * multiplier), 0), 255);
                Color color = new Color(red, green, blue);
                img2.setRGB(i, j, color.getRGB());
            }
        }

        long finish = System.currentTimeMillis();
        long time = finish - start;

        Main.finishSeq(img2, time);


    }
}
