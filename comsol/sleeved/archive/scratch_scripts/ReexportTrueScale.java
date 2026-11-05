import com.comsol.model.*;
import com.comsol.model.util.*;

public class ReexportTrueScale {
    static void exportTrue(String mphPath, String solnum, String outDisp, String outStress) throws Exception {
        Model m = ModelUtil.load("Model", mphPath);
        if (!java.util.Arrays.asList(m.result().tags()).contains("pg_disp")) {
            m.result().create("pg_disp", "PlotGroup3D");
        }
        m.result("pg_disp").set("data", "dset1");
        m.result("pg_disp").label("Total Displacement (solid.disp)");
        if (!java.util.Arrays.asList(m.result("pg_disp").feature().tags()).contains("surf_d")) {
            m.result("pg_disp").create("surf_d", "Surface");
        }
        m.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
        m.result("pg_disp").feature("surf_d").set("unit", "mm");
        if (!java.util.Arrays.asList(m.result("pg_disp").feature("surf_d").feature().tags()).contains("def")) {
            m.result("pg_disp").feature("surf_d").create("def", "Deform");
        }
        m.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});
        m.result("pg_disp").feature("surf_d").feature("def").set("scaleactive", true);
        m.result("pg_disp").feature("surf_d").feature("def").set("scale", 1);
        try {
            m.result("pg_disp").setIndex("looplevel", solnum, 0);
        } catch (Exception e) {
            m.result("pg_disp").set("solnum", solnum);
        }
        m.result("pg_disp").run();

        if (!java.util.Arrays.asList(m.result().export().tags()).contains("exp_true_d")) {
            m.result().export().create("exp_true_d", "Image3D");
        }
        m.result().export("exp_true_d").set("sourceobject", "pg_disp");
        m.result().export("exp_true_d").set("filename", outDisp);
        m.result().export("exp_true_d").set("size", "manualweb");
        m.result().export("exp_true_d").set("unit", "px");
        m.result().export("exp_true_d").set("width", 1280);
        m.result().export("exp_true_d").set("height", 720);
        m.result().export("exp_true_d").set("colortheme", "default");
        m.result().export("exp_true_d").run();

        if (outStress != null) {
            if (!java.util.Arrays.asList(m.result().tags()).contains("pg_stress")) {
                m.result().create("pg_stress", "PlotGroup3D");
            }
            m.result("pg_stress").set("data", "dset1");
            m.result("pg_stress").label("Stress (solid.mises)");
            if (!java.util.Arrays.asList(m.result("pg_stress").feature().tags()).contains("surf_s")) {
                m.result("pg_stress").create("surf_s", "Surface");
            }
            m.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
            m.result("pg_stress").feature("surf_s").set("unit", "MPa");
            if (!java.util.Arrays.asList(m.result("pg_stress").feature("surf_s").feature().tags()).contains("def")) {
                m.result("pg_stress").feature("surf_s").create("def", "Deform");
            }
            m.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});
            m.result("pg_stress").feature("surf_s").feature("def").set("scaleactive", true);
            m.result("pg_stress").feature("surf_s").feature("def").set("scale", 1);
            try {
                m.result("pg_stress").setIndex("looplevel", solnum, 0);
            } catch (Exception e) {
                m.result("pg_stress").set("solnum", solnum);
            }
            m.result("pg_stress").run();

            if (!java.util.Arrays.asList(m.result().export().tags()).contains("exp_true_s")) {
                m.result().export().create("exp_true_s", "Image3D");
            }
            m.result().export("exp_true_s").set("sourceobject", "pg_stress");
            m.result().export("exp_true_s").set("filename", outStress);
            m.result().export("exp_true_s").set("size", "manualweb");
            m.result().export("exp_true_s").set("unit", "px");
            m.result().export("exp_true_s").set("width", 1280);
            m.result().export("exp_true_s").set("height", 720);
            m.result().export("exp_true_s").set("colortheme", "default");
            m.result().export("exp_true_s").run();
        }

        m.save(mphPath);
        ModelUtil.remove("Model");
    }

    public static void main(String[] args) throws Exception {
        exportTrue(
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\actuator_sleeve_solved_Model.mph",
            "6",
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_disp_P50.png",
            null
        );
        exportTrue(
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\actuator_sleeve_solved_Model.mph",
            "12",
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_disp_P110.png",
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\sleeved\\comsol_sleeve_stress_P110.png"
        );
        exportTrue(
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\actuator_tapered_solved_Model.mph",
            "2",
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_disp.png",
            "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_stress.png"
        );
        System.out.println("DONE_REEXPORT");
    }
}
