import com.comsol.model.*;
import com.comsol.model.util.*;

public class ExportImages {
    public static void exportPlot(Model model, String pgTag, String outPath) {
        try {
            model.result().export().create("img_" + pgTag, "Image3D");
            model.result().export("img_" + pgTag).set("sourceobject", pgTag);
            model.result().export("img_" + pgTag).set("filename", outPath);
            model.result().export("img_" + pgTag).set("size", "manualweb");
            model.result().export("img_" + pgTag).set("unit", "px");
            model.result().export("img_" + pgTag).set("height", 720);
            model.result().export("img_" + pgTag).set("width", 1280);
            model.result().export("img_" + pgTag).set("colortheme", "default");
            model.result().export("img_" + pgTag).run();
            System.out.println("Exported image: " + outPath);
        } catch (Exception e) {
            System.out.println("Error exporting " + pgTag + ": " + e.getMessage());
        }
    }

    public static Model run() {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_bare_solved_Model.mph");
            // Check default plot groups
            for (String pg : model.result().tags()) {
                System.out.println("Result plot group: " + pg);
            }

            // Create a 3D volume plot of von Mises stress / displacement at P = 90 kPa
            model.result().create("pg_stress", "PlotGroup3D");
            model.result("pg_stress").set("data", "dset1");
            model.result("pg_stress").create("surf1", "Surface");
            model.result("pg_stress").feature("surf1").set("expr", "solid.mises");
            model.result("pg_stress").feature("surf1").create("def", "Deform");
            model.result("pg_stress").feature("surf1").feature("def").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_stress").run();

            exportPlot(model, "pg_stress", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\comsol_bare_deformed_stress.png");

            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String[] args) { run(); }
}
