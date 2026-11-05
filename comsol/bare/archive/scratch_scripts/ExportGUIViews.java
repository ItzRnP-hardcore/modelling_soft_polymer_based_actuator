import com.comsol.model.*;
import com.comsol.model.util.*;

public class ExportGUIViews {
    public static void savePlot(Model model, String pgTag, String fname) {
        try {
            model.result().export().create("exp_" + fname, "Image3D");
            model.result().export("exp_" + fname).set("sourceobject", pgTag);
            model.result().export("exp_" + fname).set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\" + fname + ".png");
            model.result().export("exp_" + fname).set("size", "manualweb");
            model.result().export("exp_" + fname).set("unit", "px");
            model.result().export("exp_" + fname).set("width", 1280);
            model.result().export("exp_" + fname).set("height", 720);
            model.result().export("exp_" + fname).set("colortheme", "default");
            model.result().export("exp_" + fname).set("showaxis", true);
            model.result().export("exp_" + fname).run();
            System.out.println("Exported " + fname);
        } catch (Exception e) {
            System.out.println("Error on " + fname + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_bare_solved_Model.mph");
            
            // 1. Total Displacement field plot group
            model.result().create("pg_disp", "PlotGroup3D");
            model.result("pg_disp").set("data", "dset1");
            model.result("pg_disp").label("Total Displacement (solid.disp)");
            model.result("pg_disp").create("surf_d", "Surface");
            model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
            model.result("pg_disp").feature("surf_d").set("unit", "mm");
            model.result("pg_disp").feature("surf_d").create("def", "Deform");
            model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
            
            // Set solnum = 1 (P=0)
            model.result("pg_disp").set("solnum", "1");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_disp_P0");

            // Set solnum = 5 (P=40 kPa)
            model.result("pg_disp").set("solnum", "5");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_disp_P40");

            // Set solnum = 10 (P=90 kPa)
            model.result("pg_disp").set("solnum", "10");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_disp_P90");

            // Save back into model so user sees these plot groups ready in GUI Model Builder
            model.save("c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_bare_solved_Model.mph");
            System.out.println("Saved updated model with ready plot groups.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
