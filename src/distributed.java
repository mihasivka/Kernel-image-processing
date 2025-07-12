import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import mpi.*;

public class distributed {

    public static void convolution(String fileLocation, float[][] kernelMatrix, float multiplier) throws IOException {

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        if (size < 2) {
            if (rank == 0)
                System.out.println("Error: At least 2 processes are required (1 master, 1 worker).");
            try {
                MPI.Finalize();
            } catch (Exception e) {
                // Ignore shutdown errors from MPJ Express
            }
            System.exit(1);
        }

        if (rank == 0) {
            // Master process
            masterProcess(fileLocation, kernelMatrix, multiplier, size);
        } else {
            // Worker processes
            workerProcess(kernelMatrix, multiplier);
        }

        try {
            MPI.Finalize();
        } catch (Exception e) {
            // Ignore shutdown errors from MPJ Express
        }
    }

    private static void masterProcess(String fileLocation, float[][] kernelMatrix, float multiplier, int size)
            throws IOException {
        int numWorkers = size - 1;
        System.out.println("Master: Starting distributed image processing with " + numWorkers + " workers");

        // Load the image
        BufferedImage originalImage = ImageIO.read(new File(fileLocation));
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        System.out.println("Master: Image loaded - " + width + "x" + height);

        // Calculate chunk sizes
        int[] yStarts = new int[numWorkers];
        int[] yEnds = new int[numWorkers];
        int chunkHeight = height / numWorkers;
        int remainder = height % numWorkers;
        int y = 0;
        for (int i = 0; i < numWorkers; i++) {
            int extra = (i < remainder) ? 1 : 0;
            yStarts[i] = y;
            yEnds[i] = y + chunkHeight + extra;
            y = yEnds[i];
        }

        // Send chunks to workers
        for (int worker = 1; worker <= numWorkers; worker++) {
            int yStart = yStarts[worker - 1];
            int yEnd = yEnds[worker - 1];
            int actualChunkHeight = yEnd - yStart;
            BufferedImage chunk = originalImage.getSubimage(0, yStart, width, actualChunkHeight);
            byte[] chunkData = imageToBytes(chunk);
            // System.out.println("Master: Sending chunk to worker " + worker + " yStart=" +
            // yStart + " yEnd=" + yEnd
            // + " height=" + actualChunkHeight);
            // Send kernel and multiplier first (order must match worker)
            int kernelSize = kernelMatrix.length;
            float[] kernelFlat = flattenKernel(kernelMatrix);
            MPI.COMM_WORLD.Send(kernelFlat, 0, kernelFlat.length, MPI.FLOAT, worker, 0);
            MPI.COMM_WORLD.Send(new int[] { kernelSize }, 0, 1, MPI.INT, worker, 2);
            MPI.COMM_WORLD.Send(new int[] { width }, 0, 1, MPI.INT, worker, 3);
            MPI.COMM_WORLD.Send(new float[] { multiplier }, 0, 1, MPI.FLOAT, worker, 4);
            // Then send chunk position and data
            MPI.COMM_WORLD.Send(new int[] { yStart, yEnd, width, actualChunkHeight }, 0, 4, MPI.INT, worker, 6);
            // Send the chunkData length first!
            MPI.COMM_WORLD.Send(new int[] { chunkData.length }, 0, 1, MPI.INT, worker, 10);
            MPI.COMM_WORLD.Send(chunkData, 0, chunkData.length, MPI.BYTE, worker, 5);
        }

        // Receive processed chunks from workers
        BufferedImage resultImage = new BufferedImage(width, height, originalImage.getType());
        long startTime = System.currentTimeMillis();

        for (int worker = 1; worker <= numWorkers; worker++) {
            // Receive chunk size
            int[] chunkSize = new int[1];
            MPI.COMM_WORLD.Recv(chunkSize, 0, 1, MPI.INT, worker, 7);

            // Receive processed chunk
            byte[] processedChunkData = new byte[chunkSize[0]];
            MPI.COMM_WORLD.Recv(processedChunkData, 0, chunkSize[0], MPI.BYTE, worker, 8);

            // Receive chunk position
            int[] chunkPos = new int[4];
            MPI.COMM_WORLD.Recv(chunkPos, 0, 4, MPI.INT, worker, 9);

            BufferedImage processedChunk = bytesToImage(processedChunkData);
            int yStart = chunkPos[0];
            int actualChunkHeight = chunkPos[3];
            // System.out.println("Master: Receiving chunk from worker " + worker + "
            // yStart=" + yStart + " height="+ actualChunkHeight);
            // Convert back to image and place in result
            for (int yRow = 0; yRow < actualChunkHeight; yRow++) {
                for (int x = 0; x < width; x++) {
                    int rgb = processedChunk.getRGB(x, yRow);
                    resultImage.setRGB(x, yStart + yRow, rgb);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        System.out.println("Master: Processing completed in " + processingTime + "ms");
        Main.finishSeq(resultImage, processingTime);
    }

    private static void workerProcess(float[][] kernelMatrix, float multiplier) throws IOException {
        int rank = MPI.COMM_WORLD.Rank();
        // System.out.println("Worker " + rank + ": Starting");

        // Receive kernel and parameters
        float[] kernelFlat = new float[kernelMatrix.length * kernelMatrix.length];
        MPI.COMM_WORLD.Recv(kernelFlat, 0, kernelFlat.length, MPI.FLOAT, 0, 0);

        int[] kernelSizeArray = new int[1];
        MPI.COMM_WORLD.Recv(kernelSizeArray, 0, 1, MPI.INT, 0, 2);
        int actualKernelSize = kernelSizeArray[0];

        int[] widthArray = new int[1];
        MPI.COMM_WORLD.Recv(widthArray, 0, 1, MPI.INT, 0, 3);
        int width = widthArray[0];

        float[] multiplierArr = new float[1];
        MPI.COMM_WORLD.Recv(multiplierArr, 0, 1, MPI.FLOAT, 0, 4);
        float actualMultiplier = multiplierArr[0];

        // Reconstruct kernel matrix
        float[][] actualKernel = new float[actualKernelSize][actualKernelSize];
        for (int i = 0; i < actualKernelSize; i++) {
            for (int j = 0; j < actualKernelSize; j++) {
                actualKernel[i][j] = kernelFlat[i * actualKernelSize + j];
            }
        }

        // Receive chunk data
        int[] chunkPos = new int[4];
        MPI.COMM_WORLD.Recv(chunkPos, 0, 4, MPI.INT, 0, 6);

        // Receive the chunkData length first!
        int[] chunkDataLenArr = new int[1];
        MPI.COMM_WORLD.Recv(chunkDataLenArr, 0, 1, MPI.INT, 0, 10);
        int chunkDataLen = chunkDataLenArr[0];
        byte[] chunkData = new byte[chunkDataLen];
        MPI.COMM_WORLD.Recv(chunkData, 0, chunkDataLen, MPI.BYTE, 0, 5);

        // Convert chunk data to image
        BufferedImage chunk = bytesToImage(chunkData);
        int chunkHeight = chunk.getHeight();

        // Process the chunk
        BufferedImage processedChunk = new BufferedImage(width, chunkHeight, chunk.getType());

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < chunkHeight; j++) {
                float oldRed = 0f, oldGreen = 0f, oldBlue = 0f;

                for (int k = 0; k < actualKernelSize; k++) {
                    for (int l = 0; l < actualKernelSize; l++) {
                        int w = (i - actualKernelSize / 2 + k + width) % width;
                        int h = (j - actualKernelSize / 2 + l + chunkHeight) % chunkHeight;

                        int rgbTotal = chunk.getRGB(w, h);
                        int rgbRed = (rgbTotal >> 16) & 0xff;
                        int rgbGreen = (rgbTotal >> 8) & 0xff;
                        int rgbBlue = (rgbTotal) & 0xff;

                        oldRed += (rgbRed * actualKernel[k][l]);
                        oldGreen += (rgbGreen * actualKernel[k][l]);
                        oldBlue += (rgbBlue * actualKernel[k][l]);
                    }
                }

                int red = Math.min(Math.max((int) (oldRed * actualMultiplier), 0), 255);
                int green = Math.min(Math.max((int) (oldGreen * actualMultiplier), 0), 255);
                int blue = Math.min(Math.max((int) (oldBlue * actualMultiplier), 0), 255);

                Color color = new Color(red, green, blue);
                processedChunk.setRGB(i, j, color.getRGB());
            }
        }

        // Convert processed chunk back to bytes
        byte[] processedChunkData = imageToBytes(processedChunk);

        // Send processed chunk back to master
        MPI.COMM_WORLD.Send(new int[] { processedChunkData.length }, 0, 1, MPI.INT, 0, 7);
        MPI.COMM_WORLD.Send(processedChunkData, 0, processedChunkData.length, MPI.BYTE, 0, 8);
        MPI.COMM_WORLD.Send(chunkPos, 0, 4, MPI.INT, 0, 9);

        System.out.println("Worker " + rank + ": Processing completed");
    }

    private static byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static BufferedImage bytesToImage(byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        return ImageIO.read(bais);
    }

    private static float[] flattenKernel(float[][] kernelMatrix) {
        int kernelSize = kernelMatrix.length;
        float[] kernelFlat = new float[kernelSize * kernelSize];
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernelFlat[i * kernelSize + j] = kernelMatrix[i][j];
            }
        }
        return kernelFlat;
    }
}
