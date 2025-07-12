import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import mpi.*;

public class DistributedLauncher {
    public static void main(String[] args) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();

        /*
         * Debug: print args on each rank before broadcast
         * 
         * System.out.print("Rank " + rank + " initial args: ");
         * for (int i = 0; i < args.length; i++) {
         * System.out.print(args[i] + " ");
         * }
         * System.out.println();
         * 
         * // Print the full args array on every rank
         * System.out.print("Rank " + rank + " full args: ");
         * for (int i = 0; i < args.length; i++) {
         * System.out.print(args[i] + " ");
         * }
         * System.out.println();
         */
        // Robust argument extraction: scan for a valid pair (imageNumber, kernelType)
        String[] argArray = new String[2];
        if (rank == 0) {
            boolean found = false;
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].matches("^[0-9]$") && args[i + 1].matches("[1-4]")) {
                    argArray[0] = args[i];
                    argArray[1] = args[i + 1];
                    found = true;

                }
            }
            if (!found) {
                System.out.println("No valid args found, using defaults: image 1, kernel 1");
                argArray[0] = "1";
                argArray[1] = "1";
            }
        }
        MPI.COMM_WORLD.Bcast(argArray, 0, 2, MPI.OBJECT, 0);
        // System.out.println("Rank " + rank + " args after broadcast: " + argArray[0] +
        // " " + argArray[1]);

        // Image selection
        String imagePath;
        switch (argArray[0]) {
            case "0":
                imagePath = "src/Images/300x300.jpg";
                break;
            case "1":
                imagePath = "src/Images/612x408.jpg";
                break;
            case "2":
                imagePath = "src/Images/1000x770.jpg";
                break;
            case "3":
                imagePath = "src/Images/2280x1400.jpg";
                break;
            case "4":
                imagePath = "src/Images/2500x1666.jpg";
                break;
            case "5":
                imagePath = "src/Images/2670 x 4000.jpg";
                break;
            case "6":
                imagePath = "src/Images/2732x2732.jpg";
                break;
            case "7":
                imagePath = "src/Images/3302 x 2398.jpg";
                break;
            case "8":
                imagePath = "src/Images/5088x3253.jpg";
                break;
            case "9":
                imagePath = "src/Images/6360x4372.jpg";
                break;
            default:
                if (rank == 0)
                    System.out.println("Invalid image number. Valid options: 0-9");
                try {
                    MPI.Finalize();
                } catch (Exception e) {
                }
                System.exit(1);
                return;
        }
        // Kernel selection
        int kernelType = Integer.parseInt(argArray[1]);
        float[][] kernelMatrix = getKernel(kernelType);
        float multiplier = 1.0f;
        if (kernelType == 3) { // blur
            multiplier = 1.0f / 9.0f;
        }
        if (kernelMatrix == null) {
            if (rank == 0)
                System.out.println("Invalid kernel type. Valid options: 1=edge, 2=sharpen, 3=blur, 4=emboss");
            try {
                MPI.Finalize();
            } catch (Exception e) {
            }
            System.exit(1);
            return;
        }
        // Time the distributed convolution
        long start = 0, end = 0;
        if (rank == 0)
            start = System.currentTimeMillis();
        distributed.convolution(imagePath, kernelMatrix, multiplier);
        if (rank == 0) {
            end = System.currentTimeMillis();
            System.out.println("Distributed convolution time: " + (end - start) + " ms");
            System.out.println("Result saved to src/Temp/temp.jpg");
        }
        try {
            MPI.Finalize();
        } catch (Exception e) {
        }
    }

    private static float[][] getKernel(int kernelType) {
        switch (kernelType) {
            case 1:
                return Kernels.edge_detection;
            case 2:
                return Kernels.sharpen;
            case 3:
                return Kernels.blur;
            case 4:
                return Kernels.emboss;
            default:
                return Kernels.edge_detection;
        }
    }
}