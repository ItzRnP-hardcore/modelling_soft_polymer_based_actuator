import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TraceL250Centerline {
    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String outDir = baseDir + "\\loop_caging_optimization";
        String mphPath = outDir + "\\actuator_Caging_L250_FullWrap_Xc32_Yc-52_solved.mph";

        try {
            Model model = ModelUtil.load("ModelL250", mphPath);
            double[] solvedP = model.sol("sol1").getPVals();
            int nP = (solvedP != null) ? solvedP.length : 0;
            System.out.println("Solved steps: " + nP);
            int lastIdx = nP - 1;
            double pLast = solvedP[lastIdx] / 1000.0;
            System.out.printf(Locale.US, "Evaluating at P=%.1f kPa%n", pLast);

            int nPts = 51;
            double L_act = 250.0;
            double[] sVals = new double[nPts];
            for (int i = 0; i < nPts; i++) {
                sVals[i] = (i / (double)(nPts - 1)) * L_act;
            }

            model.result().dataset().create("cpt_spine", "CutPoint3D");
            model.result().dataset("cpt_spine").set("data", "dset1");
            model.result().dataset("cpt_spine").set("pointx", sVals);
            model.result().dataset("cpt_spine").set("pointy", new double[nPts]);
            model.result().dataset("cpt_spine").set("pointz", new double[nPts]);

            model.result().numerical().create("pev_x", "EvalPoint");
            model.result().numerical("pev_x").set("data", "cpt_spine");
            model.result().numerical("pev_x").set("expr", new String[]{"X+u"});
            model.result().numerical("pev_x").set("unit", new String[]{"mm"});

            model.result().numerical().create("pev_y", "EvalPoint");
            model.result().numerical("pev_y").set("data", "cpt_spine");
            model.result().numerical("pev_y").set("expr", new String[]{"Y+v"});
            model.result().numerical("pev_y").set("unit", new String[]{"mm"});

            double[][] vx = model.result().numerical("pev_x").getReal();
            double[][] vy = model.result().numerical("pev_y").getReal();

            System.out.println("=== L=250mm DEFORMED CENTERLINE AT P=" + pLast + " kPa ===");
            for (int i = 0; i < nPts; i++) {
                double xDef = vx[i][lastIdx];
                double yDef = vy[i][lastIdx];
                double distToCyl = Math.hypot(xDef - 32.0, yDef - (-52.0));
                System.out.printf(Locale.US, ">> L250_PT: s=%.1f, X=%.2f, Y=%.2f, dist=%.2f%n",
                                  sVals[i], xDef, yDef, distToCyl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

