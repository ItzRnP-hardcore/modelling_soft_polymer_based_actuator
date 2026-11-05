import com.comsol.model.*;
import com.comsol.model.util.*;

public class ExtractBareResults {
    public static Model run() {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_bare_solved_Model.mph");
            double[][] uv_top = model.result().numerical("pev_top").getReal();
            double[][] uv_bot = model.result().numerical("pev_bot").getReal();
            System.out.println("uv_top dims: " + uv_top.length + " x " + uv_top[0].length);
            double[] p_vals = new double[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
            System.out.println("=== COMSOL_CSV_START ===");
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
                System.out.printf("%.2f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f%n",
                                  p_vals[i], ut, vt, ub, vb, x_tip, delta, theta_deg);
            }
            System.out.println("=== COMSOL_CSV_END ===");
            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void main(String[] args) { run(); }
}
