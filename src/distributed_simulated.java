import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class distributed_simulated {

    private static final int MASTER_PORT = 8080;
    private static final String WORKER_HOST = "localhost";
    private static final int WORKER_PORT = 8081;

    public static void convolution(String fileLocation, float[][] kernelMatrix, float multiplier) throws IOException {
        System.out.println("Starting simulated distributed processing...");

        // Start worker processes in separate threads (simulating different machines)
        int numWorkers = 4;
        ExecutorService workerExecutor = Executors.newFixedThreadPool(numWorkers);

        // Start worker servers
        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i + 1;
            final int workerPort = WORKER_PORT + workerId;
            workerExecutor.submit(() -> {
                try {
                    startWorker(workerPort, workerId);
                } catch (IOException e) {
                    System.err.println("Worker " + workerId + " failed: " + e.getMessage());
                }
            });
        }

        // Give workers time to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Master process
        masterProcess(fileLocation, kernelMatrix, multiplier, numWorkers);

        // Shutdown workers
        workerExecutor.shutdown();
        try {
            workerExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            workerExecutor.shutdownNow();
        }
    }

    private static void masterProcess(String fileLocation, float[][] kernelMatrix, float multiplier, int numWorkers)
            throws IOException {
        System.out.println("Master: Starting distributed image processing with " + numWorkers + " workers");

        // Load the image
        BufferedImage originalImage = ImageIO.read(new File(fileLocation));
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        System.out.println("Master: Image loaded - " + width + "x" + height);

        // Calculate chunk sizes
        int chunkHeight = height / numWorkers;
        int remainder = height % numWorkers;

        // Prepare kernel data
        int kernelSize = kernelMatrix.length;
        float[] kernelFlat = new float[kernelSize * kernelSize];
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernelFlat[i * kernelSize + j] = kernelMatrix[i][j];
            }
        }

        // Distribute work to workers
        ExecutorService taskExecutor = Executors.newFixedThreadPool(numWorkers);
        Future<BufferedImage>[] futures = new Future[numWorkers];

        int currentHeight = 0;
        for (int worker = 1; worker <= numWorkers; worker++) {
            final int workerId = worker;
            final int workerPort = WORKER_PORT + workerId;
            final int workerChunkHeight = chunkHeight + (worker <= remainder ? 1 : 0);

            // Create chunk with overlap for convolution
            int overlap = kernelSize / 2;
            int startY = Math.max(0, currentHeight - overlap);
            int endY = Math.min(height, currentHeight + workerChunkHeight + overlap);
            int actualChunkHeight = endY - startY;

            // Create chunk
            BufferedImage chunk = originalImage.getSubimage(0, startY, width, actualChunkHeight);
            final int finalStartY = startY;
            final int finalEndY = endY;
            final int finalCurrentHeight = currentHeight;
            final int finalWorkerChunkHeight = workerChunkHeight;

            futures[worker - 1] = taskExecutor.submit(() -> {
                try {
                    return processChunkWithWorker(chunk, kernelFlat, kernelSize, multiplier,
                            width, finalStartY, finalEndY, finalCurrentHeight,
                            finalWorkerChunkHeight, workerPort);
                } catch (IOException e) {
                    System.err.println("Worker " + workerId + " processing failed: " + e.getMessage());
                    return null;
                }
            });

            currentHeight += workerChunkHeight;
        }

        // Collect results
        BufferedImage resultImage = new BufferedImage(width, height, originalImage.getType());
        long startTime = System.currentTimeMillis();

        for (int worker = 1; worker <= numWorkers; worker++) {
            try {
                BufferedImage processedChunk = futures[worker - 1].get(30, TimeUnit.SECONDS);
                if (processedChunk != null) {
                    // Place processed chunk in result image
                    int processedChunkHeight = processedChunk.getHeight();
                    for (int y = 0; y < processedChunkHeight; y++) {
                        for (int x = 0; x < width; x++) {
                            int rgb = processedChunk.getRGB(x, y);
                            resultImage.setRGB(x, (worker - 1) * (height / numWorkers) + y, rgb);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to get result from worker " + worker + ": " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;

        System.out.println("Master: Processing completed in " + processingTime + "ms");
        Main.finishSeq(resultImage, processingTime);

        taskExecutor.shutdown();
    }

    private static BufferedImage processChunkWithWorker(BufferedImage chunk, float[] kernelFlat,
            int kernelSize, float multiplier, int width,
            int startY, int endY, int currentHeight,
            int workerChunkHeight, int workerPort) throws IOException {

        try (Socket socket = new Socket(WORKER_HOST, workerPort)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Send work to worker
            out.writeObject(new WorkData(chunk, kernelFlat, kernelSize, multiplier, width,
                    startY, endY, currentHeight, workerChunkHeight));
            out.flush();

            // Receive processed chunk
            BufferedImage processedChunk = (BufferedImage) in.readObject();

            return processedChunk;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize result", e);
        }
    }

    private static void startWorker(int port, int workerId) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker " + workerId + ": Started on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                try (Socket clientSocket = serverSocket.accept()) {
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                    // Receive work data
                    WorkData workData = (WorkData) in.readObject();

                    // Process the chunk
                    BufferedImage processedChunk = processChunk(workData);

                    // Send result back
                    out.writeObject(processedChunk);
                    out.flush();

                    System.out.println("Worker " + workerId + ": Processed chunk");

                } catch (ClassNotFoundException e) {
                    System.err.println("Worker " + workerId + ": Failed to deserialize work data");
                }
            }
        }
    }

    private static BufferedImage processChunk(WorkData workData) {
        BufferedImage chunk = workData.chunk;
        float[] kernelFlat = workData.kernelFlat;
        int kernelSize = workData.kernelSize;
        float multiplier = workData.multiplier;
        int width = workData.width;
        int chunkHeight = chunk.getHeight();

        // Reconstruct kernel matrix
        float[][] kernel = new float[kernelSize][kernelSize];
        for (int i = 0; i < kernelSize; i++) {
            for (int j = 0; j < kernelSize; j++) {
                kernel[i][j] = kernelFlat[i * kernelSize + j];
            }
        }

        // Process the chunk
        BufferedImage processedChunk = new BufferedImage(width, chunkHeight, chunk.getType());

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < chunkHeight; j++) {
                float oldRed = 0f, oldGreen = 0f, oldBlue = 0f;

                for (int k = 0; k < kernelSize; k++) {
                    for (int l = 0; l < kernelSize; l++) {
                        int w = (i - kernelSize / 2 + k + width) % width;
                        int h = (j - kernelSize / 2 + l + chunkHeight) % chunkHeight;

                        int rgbTotal = chunk.getRGB(w, h);
                        int rgbRed = (rgbTotal >> 16) & 0xff;
                        int rgbGreen = (rgbTotal >> 8) & 0xff;
                        int rgbBlue = (rgbTotal) & 0xff;

                        oldRed += (rgbRed * kernel[k][l]);
                        oldGreen += (rgbGreen * kernel[k][l]);
                        oldBlue += (rgbBlue * kernel[k][l]);
                    }
                }

                int red = Math.min(Math.max((int) (oldRed * multiplier), 0), 255);
                int green = Math.min(Math.max((int) (oldGreen * multiplier), 0), 255);
                int blue = Math.min(Math.max((int) (oldBlue * multiplier), 0), 255);

                Color color = new Color(red, green, blue);
                processedChunk.setRGB(i, j, color.getRGB());
            }
        }

        return processedChunk;
    }

    // Data class for sending work to workers
    private static class WorkData implements Serializable {
        private static final long serialVersionUID = 1L;

        public final BufferedImage chunk;
        public final float[] kernelFlat;
        public final int kernelSize;
        public final float multiplier;
        public final int width;
        public final int startY, endY, currentHeight, workerChunkHeight;

        public WorkData(BufferedImage chunk, float[] kernelFlat, int kernelSize, float multiplier,
                int width, int startY, int endY, int currentHeight, int workerChunkHeight) {
            this.chunk = chunk;
            this.kernelFlat = kernelFlat;
            this.kernelSize = kernelSize;
            this.multiplier = multiplier;
            this.width = width;
            this.startY = startY;
            this.endY = endY;
            this.currentHeight = currentHeight;
            this.workerChunkHeight = workerChunkHeight;
        }
    }
}