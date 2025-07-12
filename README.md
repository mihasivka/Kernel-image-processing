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

## Compile with MPI

```bash
# Compile with MPI support
javac -cp ".:mpi.jar" src/*.java

# Run with MPI (example with 4 processes)
mpirun -np 4 java -cp ".:mpi.jar:src" Main
```

## Running the Application

### Sequential Processing and Parallel Processing:

1. Run Main.java
2. Choose a file and kernel in menu
3. Pick sequential/parallel
4. Press apply to run the convolution
5. the elapsed time is printed in the terminal

### Distributed Processing:

1. Compile the distributed.java and DistributedLauncher.java
2. Run the command with desired arguments (img and kernel)
3. Check the result: Temp/temp.jpg
4. the elapsed time, status and worker information are printed in the terminal

```bash
# Compile first run from root of project

javac -cp ".:mpi.jar" src/*.java
#or
javac -cp "C:\<path_to_mpj>\lib\*;src" src\*.java

# Run Distrubuted
# p - number of processes to run with (min 2), img - (1-10) where 1 is the smalles, kernel (1-4) in order: edge detection, sharpen, blur, emboss
# if img and kernel are not defiend the defaults are 1 1
# run from root of project

mpirun -np <p> java -cp ".:mpi.jar:src" DistributedLauncher <img> <kernel>
```

## Dependencies

- Java 8 or higher
- OpenMPI 4.1 or higher
- MPI Java bindings (mpi.jar)
