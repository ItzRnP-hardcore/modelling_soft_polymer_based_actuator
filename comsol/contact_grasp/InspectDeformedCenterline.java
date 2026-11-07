import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class InspectDeformedCenterline {
    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String outDir = baseDir + "\\loop_caging_optimization";

        String[] cases = {
            "actuator_LoopCaging_R8mm_Pos1_Xc36_Yc-46_solved.mph",
            "actuator_LoopCaging_R8mm_Pos2_Xc34_Yc-48_solved.mph",
            "actuator_LoopCaging_R8mm_Pos3_Xc32_Yc-52_solved.mph"
        };

        double[] xCyl = {36.0, 34.0, 32.0};
        double[] yCyl = {-46.0, -48.0, -52.0};

        int nPts = 45;
        double L_act = 220.0;
        double[] sVals = new double[nPts];
        for (int i = 0; i < nPts; i++) {
            sVals[i] = (i / (double)(nPts - 1)) * L_act;
        }

        for (int c = 0; c < cases.length; c++) {
            String mphPath = outDir + "\\" + cases[c];
            System.out.println(">>> Loading model: " + cases[c]);
            try {
                Model model = ModelUtil.load("Model_" + c, mphPath);

                double[] solvedP = model.sol("sol1").getPVals();
                int nP = (solvedP != null) ? solvedP.length : 0;
                System.out.println("Solved steps: " + nP);
                if (nP == 0) continue;

                int lastIdx = nP - 1;
                double pLast = solvedP[lastIdx] / 1000.0;
                System.out.printf(Locale.US, "Evaluating at P=%.1f kPa (index %d)%n", pLast, lastIdx);

                // Create cut point dataset for centerline
                String cptTag = "cpt_spine_" + c;
                model.result().dataset().create(cptTag, "CutPoint3D");
                model.result().dataset(cptTag).set("data", "dset1");
                model.result().dataset(cptTag).set("pointx", sVals);
                model.result().dataset(cptTag).set("pointy", new double[nPts]);
                model.result().dataset(cptTag).set("pointz", new double[nPts]);

                model.result().numerical().create("pev_x_" + c, "EvalPoint");
                model.result().numerical("pev_x_" + c).set("data", cptTag);
                model.result().numerical("pev_x_" + c).set("expr", new String[]{"X+u"});
                model.result().numerical("pev_x_" + c).set("unit", new String[]{"mm"});

                model.result().numerical().create("pev_y_" + c, "EvalPoint");
                model.result().numerical("pev_y_" + c).set("data", cptTag);
                model.result().numerical("pev_y_" + c).set("expr", new String[]{"Y+v"});
                model.result().numerical("pev_y_" + c).set("unit", new String[]{"mm"});

                double[][] vx = model.result().numerical("pev_x_" + c).getReal();
                double[][] vy = model.result().numerical("pev_y_" + c).getReal();

                System.out.printf(Locale.US, "=== CENTERLINE COORDS FOR %s (P=%.1f kPa) ===%n", cases[c], pLast);
                System.out.printf(Locale.US, "Cylinder center: (Xc=%.1f, Yc=%.1f) mm, Rcyl=8.0 mm%n", xCyl[c], yCyl[c]);

                for (int i = 0; i < nPts; i++) {
                    double xDef = vx[i][lastIdx];
                    double yDef = vy[i][lastIdx];
                    double distToCylCenter = Math.hypot(xDef - xCyl[c], yDef - yCyl[c]);
                    if (i % 3 == 0 || i == nPts - 1 || distToCylCenter < 18.0) {
                        System.out.printf(Locale.US, "s=%5.1f mm -> (X=%6.2f, Y=%6.2f) mm | dist_to_cyl=%5.2f mm%n",
                                          sVals[i], xDef, yDef, distToCylCenter);
                    }
                }

                ModelUtil.remove("Model_" + c);
            } catch (Exception e) {
                System.err.println("Error processing " + cases[c] + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

