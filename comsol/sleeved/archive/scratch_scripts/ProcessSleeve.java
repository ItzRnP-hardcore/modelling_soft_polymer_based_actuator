import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;

public class ProcessSleeve {
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
            model.result().export("exp_" + fname).run();
            System.out.println("Exported " + fname);
        } catch (Exception e) {
            System.out.println("Error exporting " + fname + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_sleeve_solved_Model.mph");
            
            // 1. Extract values
            double[][] uv_top = model.result().numerical("pev_top").getReal();
            double[][] uv_bot = model.result().numerical("pev_bot").getReal();
            double[] p_vals = new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110};

            System.out.println("=== COMSOL_SLEEVE_CSV_START ===");
            System.out.println("P_kPa,u_top_mm,v_top_mm,u_bot_mm,v_bot_mm,x_tip_mm,delta_mm,theta_deg");
            int nSteps = Math.min(p_vals.length, uv_top[0].length);

            for (int i = 0; i < nSteps; i++) {
                double ut = uv_top[0][i];
                double vt = uv_top[1][i];
                double ub = uv_bot[0][i];
                double vb = uv_bot[1][i];
                double x_top = 100.0 + ut;
                double y_top = 10.0 + vt;
                double x_bot = 100.0 + ub;
                double y_bot = -10.0 + vb;
                double x_tip = 0.5 * (x_top + x_bot);
                double delta = -0.5 * (y_top + y_bot);
                double theta_deg = Math.atan2(x_top - x_bot, y_top - y_bot) * 180.0 / Math.PI;

                String row = String.format("%.2f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                                           p_vals[i], ut, vt, ub, vb, x_tip, delta, theta_deg);
                System.out.println(row);
            }
            System.out.println("=== COMSOL_SLEEVE_CSV_END ===");

            // 2. Total Displacement Plot Group
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

            // 3. von Mises Stress Plot Group
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

            // Export Displacements and Stresses
            // Step 1: P=0 (solnum 1)
            model.result("pg_disp").set("solnum", "1");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_sleeve_disp_P0");

            // Step 6: P=50 (solnum 6)
            model.result("pg_disp").set("solnum", "6");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_sleeve_disp_P50");

            // Step 12: P=110 (solnum 12)
            model.result("pg_disp").set("solnum", "12");
            model.result("pg_disp").run();
            savePlot(model, "pg_disp", "comsol_sleeve_disp_P110");

            model.result("pg_stress").set("solnum", "12");
            model.result("pg_stress").run();
            savePlot(model, "pg_stress", "comsol_sleeve_stress_P110");

            // Save model with ready plot groups
            model.save("c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_sleeve_solved_Model.mph");
            System.out.println("Successfully saved actuator_sleeve_solved_Model.mph with displacement & stress plot groups!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
