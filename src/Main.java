import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {

	public static void main(String []args) {
		new MainWindow();
		
	}

	public static void finishSeq(BufferedImage output, long timeElapsed) throws IOException {

		//Kamor se shrani slika
		String fileOutputPath = "src/Temp/temp";
        ImageIO.write(output, "jpg", new File(fileOutputPath + ".jpg"));
        MainWindow.processedImage = output;

        System.out.println("Time elapsed: " + timeElapsed + " ms");

    }

}
