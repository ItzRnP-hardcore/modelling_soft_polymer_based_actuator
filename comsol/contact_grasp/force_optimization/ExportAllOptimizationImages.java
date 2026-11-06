import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class ExportAllOptimizationImages {

    public static void exportImage(Model model, String pgTag, String expr, String unit, int loopIndex, String filename) {
        try {
            model.result().create(pgTag, "PlotGroup3D");
            model.result(pgTag).set("data", "dset1");
            if (loopIndex >= 0) {
                model.result(pgTag).setIndex("looplevel", loopIndex, 0);
            }
            model.result(pgTag).create("surf1", "Surface");
            model.result(pgTag).feature("surf1").set("expr", expr);
            if (unit != null && !unit.isEmpty()) {
                model.result(pgTag).feature("surf1").set("unit", unit);
            }
            model.result(pgTag).feature("surf1").create("def1", "Deform");
            model.result(pgTag).feature("surf1").feature("def1").set("expr", new String[]{"u", "v", "w"});
            model.result(pgTag).feature("surf1").feature("def1").set("scaleactive", true);
            model.result(pgTag).feature("surf1").feature("def1").set("scale", 1.0);

            String expTag = "exp_" + pgTag;
            model.result().export().create(expTag, "Image3D");
            model.result().export(expTag).set("sourceobject", pgTag);
            model.result().export(expTag).set("size", "manualweb");
            model.result().export(expTag).set("unit", "px");
            model.result().export(expTag).set("width", 1400);
            model.result().export(expTag).set("height", 900);
            model.result().export(expTag).set("pngfilename", filename);
            model.result().export(expTag).run();
            System.out.println("Exported: " + filename);
        } catch (Exception e) {
            System.err.println("Error exporting " + filename + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Starting Optimization 3D Image Exporter ===");

        // 1. Full-Wrap R=20mm Model
        try {
            System.out.println("Loading actuator_fullwrap_R20mm_solved.mph...");
            Model model = ModelUtil.load("Model", "actuator_fullwrap_R20mm_solved.mph");
            exportImage(model, "pg_fw_disp", "solid.disp", "mm", 3, "images/actuator_fullwrap_R20mm_disp.png");
            exportImage(model, "pg_fw_stress", "solid.mises", "kPa", 3, "images/actuator_fullwrap_R20mm_stress.png");
            exportImage(model, "pg_fw_loop", "solid.disp", "mm", 4, "images/actuator_fullwrap_R20mm_P80_loop.png");
            ModelUtil.remove("Model");
            System.out.println("Full-wrap renders finished.");
        } catch (Exception e) {
            System.err.println("Full-wrap render error: " + e.getMessage());
        }

        // 2. Scaled S=1.50x R=22mm Model
        try {
            System.out.println("Loading actuator_scaled_S1.50x_R22mm_solved.mph...");
            Model model = ModelUtil.load("Model", "actuator_scaled_S1.50x_R22mm_solved.mph");
            exportImage(model, "pg_s15_disp", "solid.disp", "mm", 5, "images/actuator_scaled_S1.50x_disp_P100.png");
            exportImage(model, "pg_s15_stress", "solid.mises", "kPa", 5, "images/actuator_scaled_S1.50x_stress_P100.png");
            ModelUtil.remove("Model");
            System.out.println("Scale 1.50x renders finished.");
        } catch (Exception e) {
            System.err.println("Scale 1.50x render error: " + e.getMessage());
        }

        // 3. Scaled S=1.00x R=22mm Model
        try {
            System.out.println("Loading actuator_scaled_S1.00x_R22mm_solved.mph...");
            Model model = ModelUtil.load("Model", "actuator_scaled_S1.00x_R22mm_solved.mph");
            exportImage(model, "pg_s10_disp", "solid.disp", "mm", 5, "images/actuator_scaled_S1.00x_disp_P100.png");
            exportImage(model, "pg_s10_stress", "solid.mises", "kPa", 5, "images/actuator_scaled_S1.00x_stress_P100.png");
            ModelUtil.remove("Model");
            System.out.println("Scale 1.00x renders finished.");
        } catch (Exception e) {
            System.err.println("Scale 1.00x render error: " + e.getMessage());
        }

        System.out.println("=== All Optimization Images Rendered Successfully ===");
    }
}

