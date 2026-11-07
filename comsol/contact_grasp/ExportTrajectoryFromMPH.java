import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class ExportTrajectoryFromMPH {

    public static void main(String[] args) {
        try {
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
            String mphFile = baseDir + "\\actuator_free_curling_220mm_solved.mph";
            System.out.println("Loading solved MPH: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            double L_act = 220.0;
            int nPts = 45;
            double[] xCoords = new double[nPts];
            double[] yCoords = new double[nPts];
            double[] zCoords = new double[nPts];
            for (int i = 0; i < nPts; i++) {
                xCoords[i] = (i / (double)(nPts - 1)) * L_act;
                yCoords[i] = 0.0;
                zCoords[i] = 0.0;
            }

            model.result().dataset().create("cpt_eval", "CutPoint3D");
            model.result().dataset("cpt_eval").set("data", "dset1");
            model.result().dataset("cpt_eval").set("pointx", xCoords);
            model.result().dataset("cpt_eval").set("pointy", yCoords);
            model.result().dataset("cpt_eval").set("pointz", zCoords);

            model.result().numerical().create("pev_x", "EvalPoint");
            model.result().numerical("pev_x").set("data", "cpt_eval");
            model.result().numerical("pev_x").set("expr", new String[]{"x+u"});

            model.result().numerical().create("pev_y", "EvalPoint");
            model.result().numerical("pev_y").set("data", "cpt_eval");
            model.result().numerical("pev_y").set("expr", new String[]{"y+v"});

            double[][] valX = model.result().numerical("pev_x").getReal();
            double[][] valY = model.result().numerical("pev_y").getReal();

            int nCols = valX[0].length;
            System.out.println("Extracted " + nCols + " solution steps.");

            String[] pStepList = {"0", "20", "40", "60", "80", "100"};
            for (int col = 0; col < nCols; col++) {
                String pStr = (col < pStepList.length) ? pStepList[col] : String.valueOf(col*20);
                for (int i = 0; i < nPts; i++) {
                    double defX = valX[i][col];
                    double defY = valY[i][col];
                    System.out.printf(Locale.US, "TRAJ_DATA: P=%s, pt=%d, s=%.2f, X=%.4f, Y=%.4f%n",
                                      pStr, i, xCoords[i], defX, defY);
                }
            }

            // Export 3D displacement renders at P = 40, 60, 80 kPa
            int[] exportSteps = {2, 3, 4}; // P = 40, 60, 80 kPa
            int[] exportPressures = {40, 60, 80};
            for (int e = 0; e < exportSteps.length; e++) {
                int sIdx = exportSteps[e];
                int pVal = exportPressures[e];
                String pgTag = "pg_step_" + pVal;
                model.result().create(pgTag, "PlotGroup3D");
                model.result(pgTag).set("data", "dset1");
                model.result(pgTag).setIndex("looplevel", sIdx, 0);
                model.result(pgTag).create("surf1", "Surface");
                model.result(pgTag).feature("surf1").set("expr", "solid.disp");
                model.result(pgTag).feature("surf1").create("def1", "Deform");
                model.result(pgTag).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgTag).feature("surf1").feature("def1").set("scaleactive", true);
                model.result(pgTag).feature("surf1").feature("def1").set("scale", 1.0);

                String expTag = "exp_step_" + pVal;
                model.result().export().create(expTag, "Image3D");
                model.result().export(expTag).set("sourceobject", pgTag);
                model.result().export(expTag).set("size", "manualweb");
                model.result().export(expTag).set("unit", "px");
                model.result().export(expTag).set("width", 1400);
                model.result().export(expTag).set("height", 900);
                String pOut = baseDir + "\\images\\actuator_free_curling_220mm_disp_P" + pVal + ".png";
                model.result().export(expTag).set("pngfilename", pOut);
                model.result().export(expTag).run();
                System.out.println("Exported render: " + pOut);
            }

            ModelUtil.remove("Model");
            System.out.println("Trajectory extraction complete!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

