
public class Kernels {

	public static float[][] edge_detection ={
            {-1, -1, -1},
            {-1, 8, -1},
            {-1, -1, -1}
    };
	public static float[][] sharpen = {
            {0, -1, 0},
            {-1, 5, -1},
            {0, -1, 0}
    };
    
    public static float[][] blur ={
            {1, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
    };
    public static float[][] emboss ={
            {-2, -1, 0},
            {-1, 1, 1},
            {0, 1, 2}
    };
    
    public static float[][] getKernelMatrix(String s) {
        switch (s) {
            case "edge_detection":
                return edge_detection;                
            case "sharpen":
                return sharpen;            
            case "box_blur":
                return blur;
            case "emboss":
                return emboss;
            default:
                return edge_detection;
        }
    }
    
}
