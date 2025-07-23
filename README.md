# Kernel-image-processing

Programijranje 3 projekt

This project implements image convolution processing using different approaches: sequential, parallel (multithreading), and distributed (MPI).

## Project Structure

- `src/Sequential.java` - Sequential image processing
- `src/parallel.java` - Parallel processing using threads
- `src/distributed.java` - Distributed processing using OpenMPI
- `src/Main.java` - Main application entry point
- `src/Kernels.java` - Predefined convolution kernels
- `src/MainWindow.java` - GUI interface

## Running the Application

### Sequential Processing and Parallel Processing:

1. Run Main.java
2. Choose a file and kernel in menu
3. Pick sequential/parallel
4. Press apply to run the convolution
5. the elapsed time is printed in the terminal

### Distributed Processing:

1. Compile the distributed.java and DistributedLauncher.java in cmd
2. Run the command with desired arguments (img and kernel) in cmd
3. Check the result: Temp/temp.jpg
4. the elapsed time, status and worker information are printed in the terminal

```bash
# Compile first run from root of project

javac -cp ".;C:\\Program Files\\mpj-v0_44\\lib\\*" -Xlint:unchecked src\\*.java
#or
javac -cp ".;C:\\<path_to_mpj>\\lib\\*" -Xlint:unchecked src\\*.java


# Run Distrubuted
# workers - number of processes to run with (min 2), img - (0-9) where 0 is the smallest, kernel (1-4) in order: edge detection, sharpen, blur, emboss
# if img and kernel are not defiend the defaults are 1 1
# run from root of project
# example on how to run with 4 workers on the 0th img and 1st kernel:
# "C:\Program Files\mpj-v0_44\bin\mpjrun.bat" -np 4 -cp src DistributedLauncher 0 1

"C:\<path-to-mpj-v0_44>\bin\mpjrun.bat" -np <workers> -cp src DistributedLauncher <img> <kernel>

# Check the result: Temp/temp.jpg
```

## Dependencies

- Java 8 or higher
- OpenMPI 4.1 or higher
- mpj-v0_44
