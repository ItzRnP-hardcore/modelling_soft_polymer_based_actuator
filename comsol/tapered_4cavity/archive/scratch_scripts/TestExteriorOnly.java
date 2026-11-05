import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestExteriorOnly {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\DiagnoseBulge_Model.mph");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";

            // Set surf_d to ONLY exterior boundaries: 1 (base), 2,3,4,5 (cone skin), 46 (tip)
            model.result("pg_disp").feature("surf_d").selection().set(new int[]{1, 2, 3, 4, 5, 46});
            model.result("pg_disp").set("solnum", "6");
            model.result("pg_disp").run();

            String expD = "exp_ext_only";
            model.result().export().create(expD, "Image3D");
            model.result().export(expD).set("sourceobject", "pg_disp");
            model.result().export(expD).set("filename", baseDir + "\\comsol_cavity_1_exterior_only.png");
            model.result().export(expD).set("size", "manualweb");
            model.result().export(expD).set("unit", "px");
            model.result().export(expD).set("width", 1280);
            model.result().export(expD).set("height", 720);
            model.result().export(expD).set("colortheme", "default");
            model.result().export(expD).run();

            System.out.println("Exported comsol_cavity_1_exterior_only.png successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

