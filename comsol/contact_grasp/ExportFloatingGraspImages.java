import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class ExportFloatingGraspImages {
    public static void main(String[] args) {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String imgDir = baseDir + "\\images";

        double[] radii = {12.0, 15.0, 18.0, 22.0};
        int[] optPIndices = {2, 2, 2, 4}; // P=20 kPa for 12,15,18; P=40 kPa for 22

        for (int i = 0; i < radii.length; i++) {
            double r = radii[i];
            String mphFile = String.format("%s\\actuator_floating_R%.0fmm_distal_solved.mph", baseDir, r);
            System.out.println("Loading " + mphFile + " for image rendering...");

            Model model = null;
            try {
                model = ModelUtil.load("Model", mphFile);

                // Create 3D plot group for displacement
                String pgDispTag = "pg_disp";
                model.result().create(pgDispTag, "PlotGroup3D");
                model.result(pgDispTag).set("data", "dset1");
                model.result(pgDispTag).setIndex("looplevel", optPIndices[i], 0);
                model.result(pgDispTag).create("surf1", "Surface");
                model.result(pgDispTag).feature("surf1").set("expr", "solid.disp");
                model.result(pgDispTag).feature("surf1").create("def1", "Deform");
                model.result(pgDispTag).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
                model.result(pgDispTag).feature("surf1").feature("def1").set("scaleactive", true);
                model.result(pgDispTag).feature("surf1").feature("def1").set("scale", 1.0);

                // Export displacement image
                String expDispTag = "exp_disp";
                model.result().export().create(expDispTag, "Image3D");
                model.result().export(expDispTag).set("sourceobject", pgDispTag);
                model.result().export(expDispTag).set("size", "manualweb");
                model.result().export(expDispTag).set("unit", "px");
                model.result().export(expDispTag).set("width", 1400);
                model.result().export(expDispTag).set("height", 900);
                String dispImgPath = String.format("%s\\actuator_floating_R%.0fmm_distal_disp.png", imgDir, r);
                model.result().export(expDispTag).set("pngfilename", dispImgPath);
                model.result().export(expDispTag).run();
                System.out.println("Saved: " + dispImgPath);

                // Create 3D plot group for von Mises stress
                String pgStressTag = "pg_stress";
                model.result().create(pgStressTag, "PlotGroup3D");
                model.result(pgStressTag).set("data", "dset1");
                model.result(pgStressTag).setIndex("looplevel", optPIndices[i], 0);
                model.result(pgStressTag).create("surf2", "Surface");
                model.result(pgStressTag).feature("surf2").set("expr", "solid.mises");
                model.result(pgStressTag).feature("surf2").set("unit", "kPa");
                model.result(pgStressTag).feature("surf2").create("def2", "Deform");
                model.result(pgStressTag).feature("surf2").feature("def2").set("expr", new String[]{"u", "v", "w"});
                model.result(pgStressTag).feature("surf2").feature("def2").set("scaleactive", true);
                model.result(pgStressTag).feature("surf2").feature("def2").set("scale", 1.0);

                // Export stress image
                String expStressTag = "exp_stress";
                model.result().export().create(expStressTag, "Image3D");
                model.result().export(expStressTag).set("sourceobject", pgStressTag);
                model.result().export(expStressTag).set("size", "manualweb");
                model.result().export(expStressTag).set("unit", "px");
                model.result().export(expStressTag).set("width", 1400);
                model.result().export(expStressTag).set("height", 900);
                String stressImgPath = String.format("%s\\actuator_floating_R%.0fmm_distal_stress.png", imgDir, r);
                model.result().export(expStressTag).set("pngfilename", stressImgPath);
                model.result().export(expStressTag).run();
                System.out.println("Saved: " + stressImgPath);

            } catch (Exception e) {
                System.err.println("Error rendering R=" + r + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (model != null) {
                    try {
                        ModelUtil.remove("Model");
                    } catch (Exception ignored) {}
                }
            }
        }
        System.out.println("Finished rendering all optimal grasp images!");
    }
}
