import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class ExtractTapered {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\actuator_tapered_solved_Model.mph");
            
            // 3D Plot Groups
            if (!java.util.Arrays.asList(model.result().tags()).contains("pg_disp")) {
                model.result().create("pg_disp", "PlotGroup3D");
            }
            model.result("pg_disp").set("data", "dset1");
            model.result("pg_disp").label("Total Displacement (solid.disp)");
            if (!java.util.Arrays.asList(model.result("pg_disp").feature().tags()).contains("surf_d")) {
                model.result("pg_disp").create("surf_d", "Surface");
            }
            model.result("pg_disp").feature("surf_d").set("expr", "solid.disp");
            model.result("pg_disp").feature("surf_d").set("unit", "mm");
            if (!java.util.Arrays.asList(model.result("pg_disp").feature("surf_d").feature().tags()).contains("def")) {
                model.result("pg_disp").feature("surf_d").create("def", "Deform");
            }
            model.result("pg_disp").feature("surf_d").feature("def").set("expr", new String[]{"u", "v", "w"});

            if (!java.util.Arrays.asList(model.result().tags()).contains("pg_stress")) {
                model.result().create("pg_stress", "PlotGroup3D");
            }
            model.result("pg_stress").set("data", "dset1");
            model.result("pg_stress").label("Stress (solid.mises)");
            if (!java.util.Arrays.asList(model.result("pg_stress").feature().tags()).contains("surf_s")) {
                model.result("pg_stress").create("surf_s", "Surface");
            }
            model.result("pg_stress").feature("surf_s").set("expr", "solid.mises");
            model.result("pg_stress").feature("surf_s").set("unit", "MPa");
            if (!java.util.Arrays.asList(model.result("pg_stress").feature("surf_s").feature().tags()).contains("def")) {
                model.result("pg_stress").feature("surf_s").create("def", "Deform");
            }
            model.result("pg_stress").feature("surf_s").feature("def").set("expr", new String[]{"u", "v", "w"});

            // Ensure datasets and eval points exist
            if (!java.util.Arrays.asList(model.result().dataset().tags()).contains("cpt_top")) {
                model.result().dataset().create("cpt_top", "CutPoint3D");
                model.result().dataset("cpt_top").set("data", "dset1");
                model.result().dataset("cpt_top").set("pointx", "100");
                model.result().dataset("cpt_top").set("pointy", "5");
                model.result().dataset("cpt_top").set("pointz", "0");
            }
            if (!java.util.Arrays.asList(model.result().dataset().tags()).contains("cpt_bot")) {
                model.result().dataset().create("cpt_bot", "CutPoint3D");
                model.result().dataset("cpt_bot").set("data", "dset1");
                model.result().dataset("cpt_bot").set("pointx", "100");
                model.result().dataset("cpt_bot").set("pointy", "-5");
                model.result().dataset("cpt_bot").set("pointz", "0");
            }

            if (!java.util.Arrays.asList(model.result().numerical().tags()).contains("pev_top")) {
                model.result().numerical().create("pev_top", "EvalPoint");
                model.result().numerical("pev_top").set("data", "cpt_top");
                model.result().numerical("pev_top").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_top").set("unit", new String[]{"mm", "mm"});
            }
            if (!java.util.Arrays.asList(model.result().numerical().tags()).contains("pev_bot")) {
                model.result().numerical().create("pev_bot", "EvalPoint");
                model.result().numerical("pev_bot").set("data", "cpt_bot");
                model.result().numerical("pev_bot").set("expr", new String[]{"u", "v"});
                model.result().numerical("pev_bot").set("unit", new String[]{"mm", "mm"});
            }

            // Evaluate displacements
            double[][] uv_top = model.result().numerical("pev_top").getReal();
            double[][] uv_bot = model.result().numerical("pev_bot").getReal();
            int n_sols = uv_top[0].length;
            System.out.println("Number of stored solution steps: " + n_sols);
            System.out.println("=== COMSOL_TAPERED_CSV_START ===");
            System.out.println("step,u_top_mm,v_top_mm,u_bot_mm,v_bot_mm,x_tip_mm,delta_mm,theta_deg");
            for (int i = 0; i < n_sols; i++) {
                double ut = uv_top[0][i];
                double vt = uv_top[1][i];
                double ub = uv_bot[0][i];
                double vb = uv_bot[1][i];
                double x_top = 100.0 + ut;
                double y_top = 5.0 + vt;
                double x_bot = 100.0 + ub;
                double y_bot = -5.0 + vb;
                double x_tip = 0.5 * (x_top + x_bot);
                double delta = -0.5 * (y_top + y_bot);
                double theta_deg = Math.atan2(x_top - x_bot, y_top - y_bot) * 180.0 / Math.PI;
                System.out.printf("%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                                  i, ut, vt, ub, vb, x_tip, delta, theta_deg);
            }
            System.out.println("=== COMSOL_TAPERED_CSV_END ===");

            String target_sol = String.valueOf(n_sols);

            // Export image of last solved step
            model.result("pg_disp").set("solnum", target_sol);
            model.result("pg_disp").run();
            model.result().export().create("exp_disp", "Image3D");
            model.result().export("exp_disp").set("sourceobject", "pg_disp");
            model.result().export("exp_disp").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_disp.png");
            model.result().export("exp_disp").set("size", "manualweb");
            model.result().export("exp_disp").set("unit", "px");
            model.result().export("exp_disp").set("width", 1280);
            model.result().export("exp_disp").set("height", 720);
            model.result().export("exp_disp").set("colortheme", "default");
            model.result().export("exp_disp").run();
            System.out.println("Exported comsol_tapered_disp.png");

            model.result("pg_stress").set("solnum", target_sol);
            model.result("pg_stress").run();
            model.result().export().create("exp_stress", "Image3D");
            model.result().export("exp_stress").set("sourceobject", "pg_stress");
            model.result().export("exp_stress").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\comsol_tapered_stress.png");
            model.result().export("exp_stress").set("size", "manualweb");
            model.result().export("exp_stress").set("unit", "px");
            model.result().export("exp_stress").set("width", 1280);
            model.result().export("exp_stress").set("height", 720);
            model.result().export("exp_stress").set("colortheme", "default");
            model.result().export("exp_stress").run();
            System.out.println("Exported comsol_tapered_stress.png");
            
            // Save model with ready plot groups
            model.save("c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered\\actuator_tapered_solved_Model.mph");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
