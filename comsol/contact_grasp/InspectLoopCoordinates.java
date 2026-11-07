import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class InspectLoopCoordinates {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\proper_grasp_and_sleeve_optimization\\actuator_proper_ProperGrasp_R12mm_CagingLoop_solved.mph";
            System.out.println("Loading: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            System.out.println("Querying point coordinates at P = 0..100 kPa...");
            int nPts = 23;
            double[] xCoords = new double[nPts];
            double[] yCoords = new double[nPts];
            double[] zCoords = new double[nPts];
            for (int i = 0; i < nPts; i++) {
                xCoords[i] = i * 10.0;
                yCoords[i] = 0.0;
                zCoords[i] = 0.0;
            }

            model.result().dataset().create("cpt1", "CutPoint3D");
            model.result().dataset("cpt1").set("data", "dset1");
            model.result().dataset("cpt1").set("pointx", xCoords);
            model.result().dataset("cpt1").set("pointy", yCoords);
            model.result().dataset("cpt1").set("pointz", zCoords);

            model.result().numerical().create("pev_pts", "EvalPoint");
            model.result().numerical("pev_pts").set("data", "cpt1");
            model.result().numerical("pev_pts").set("expr", new String[]{"x+u", "y+v", "z+w"});

            double[][] vals = model.result().numerical("pev_pts").getReal();
            int nCols = vals[0].length;
            System.out.println("Number of pressure steps: " + nCols);

            for (int col = 0; col < nCols; col++) {
                System.out.println("\n--- PRESSURE STEP INDEX " + col + " ---");
                System.out.printf("%-6s %-12s %-12s %-12s %-12s%n", "Pt", "s_orig", "X_def", "Y_def", "Z_def");
                for (int i = 0; i < nPts; i++) {
                    double defX = vals[3*i + 0][col];
                    double defY = vals[3*i + 1][col];
                    double defZ = vals[3*i + 2][col];
                    System.out.printf("%-6d %-12.1f %-12.2f %-12.2f %-12.2f%n", i, xCoords[i], defX, defY, defZ);
                }
            }

            double[][] resCyl = model.result().numerical("pev_cyl").getReal();
            System.out.println("\nCylinder Center Displacements:");
            for (int col = 0; col < nCols; col++) {
                System.out.printf("Step %d: u=%.2f mm, v=%.2f mm, w=%.2f mm%n", col, resCyl[0][col], resCyl[1][col], resCyl[2][col]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

