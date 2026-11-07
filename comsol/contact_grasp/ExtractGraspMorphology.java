import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class ExtractGraspMorphology {
    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization";
        String[] cases = {
            "actuator_LoopCaging_R8mm_Pos1_Xc36_Yc-46_solved.mph",
            "actuator_LoopCaging_R8mm_Pos2_Xc34_Yc-48_solved.mph",
            "actuator_LoopCaging_R8mm_Pos3_Xc32_Yc-52_solved.mph"
        };
        double[] xCylOrig = {36.0, 34.0, 32.0};
        double[] yCylOrig = {-46.0, -48.0, -52.0};

        int nSpine = 45;
        double L_act = 220.0;
        double[] xSpine = new double[nSpine];
        double[] ySpine = new double[nSpine];
        double[] zSpine = new double[nSpine];
        for (int i = 0; i < nSpine; i++) {
            xSpine[i] = (i / (double)(nSpine - 1)) * L_act;
            ySpine[i] = 0.0;
            zSpine[i] = 0.0;
        }

        for (int c = 0; c < cases.length; c++) {
            String f = baseDir + "\\" + cases[c];
            System.out.println("==================================================");
            System.out.println("Case: " + cases[c]);
            try {
                Model model = ModelUtil.load("Model", f);
                double[] pvals = model.sol("sol1").getPVals();
                int nSteps = pvals.length;
                System.out.println("Converged steps: " + nSteps + " | Max P = " + (pvals[nSteps-1]/1000.0) + " kPa");

                // Evaluate spine
                model.result().dataset().create("cpt_sp", "CutPoint3D");
                model.result().dataset("cpt_sp").set("data", "dset1");
                model.result().dataset("cpt_sp").set("pointx", xSpine);
                model.result().dataset("cpt_sp").set("pointy", ySpine);
                model.result().dataset("cpt_sp").set("pointz", zSpine);

                model.result().numerical().create("pev_sp_u", "EvalPoint");
                model.result().numerical("pev_sp_u").set("data", "cpt_sp");
                model.result().numerical("pev_sp_u").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_sp_u").set("unit", new String[]{"mm", "mm"});
                double[][] valSp = model.result().numerical("pev_sp_u").getReal();

                // Evaluate cylinder displacement
                model.result().dataset().create("cpt_c", "CutPoint3D");
                model.result().dataset("cpt_c").set("data", "dset1");
                model.result().dataset("cpt_c").set("pointx", String.valueOf(xCylOrig[c]));
                model.result().dataset("cpt_c").set("pointy", String.valueOf(yCylOrig[c]));
                model.result().dataset("cpt_c").set("pointz", "0");

                model.result().numerical().create("pev_c_u", "EvalPoint");
                model.result().numerical("pev_c_u").set("data", "cpt_c");
                model.result().numerical("pev_c_u").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_c_u").set("unit", new String[]{"mm", "mm"});
                double[][] valC = model.result().numerical("pev_c_u").getReal();

                // Print spine and cylinder at max pressure
                int lastStep = nSteps - 1;
                double pMax = pvals[lastStep] / 1000.0;
                double uCyl = valC[0][lastStep];
                double vCyl = valC[1][lastStep];
                double xCylDef = xCylOrig[c] + uCyl;
                double yCylDef = yCylOrig[c] + vCyl;
                System.out.printf(Locale.US, "At P = %.1f kPa: Cylinder moved to (X=%.2f, Y=%.2f) mm [disp=(%.3f, %.3f) mm]%n",
                                  pMax, xCylDef, yCylDef, uCyl, vCyl);

                // Print tip
                double xTipDef = L_act + valSp[nSpine - 1][lastStep];
                double yTipDef = valSp[nSpine + nSpine - 1][lastStep];
                System.out.printf(Locale.US, "At P = %.1f kPa: Actuator Tip at (X=%.2f, Y=%.2f) mm%n",
                                  pMax, xTipDef, yTipDef);

                // Output CSV of deformed spine
                String csvPath = baseDir + "\\deformed_spine_case" + (c+1) + ".csv";
                try (PrintWriter pw = new PrintWriter(new FileWriter(csvPath))) {
                    pw.println("pt,s_orig_mm,x_def_mm,y_def_mm");
                    for (int i = 0; i < nSpine; i++) {
                        double xDef = xSpine[i] + valSp[i][lastStep];
                        double yDef = valSp[nSpine + i][lastStep];
                        pw.printf(Locale.US, "%d,%.1f,%.3f,%.3f%n", i, xSpine[i], xDef, yDef);
                    }
                }
                System.out.println("Saved deformed spine: " + csvPath);

                ModelUtil.remove("Model");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
