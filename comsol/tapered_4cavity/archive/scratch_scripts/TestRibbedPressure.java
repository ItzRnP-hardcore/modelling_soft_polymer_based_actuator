import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestRibbedPressure {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\actuator_tapered_4cavity_ribbed_solved.mph");
            
            double[] eVals = {1.65, 0.8, 0.4, 0.25, 0.15};
            for (double eMod : eVals) {
                System.out.println("\nTesting E_mod = " + eMod + " MPa...");
                model.param().set("E_mod", eMod + "[MPa]");
                model.param().set("w1", "1");
                model.param().set("w2", "0");
                model.param().set("w3", "0");
                model.param().set("w4", "0");
                
                try {
                    model.study("std1").run();
                    double[][] uvw = model.result().numerical("pev_tip").getReal();
                    int last = uvw[0].length - 1;
                    double u = uvw[0][last];
                    double v = uvw[1][last];
                    double w = uvw[2][last];
                    double d3d = Math.hypot(v, w);
                    double theta = Math.atan2(d3d, 100.0 + u) * 180.0 / Math.PI;
                    System.out.printf(Locale.US, "  SUCCESS! E=%.2f MPa at 100 kPa -> u=%.2f, v=%.2f, w=%.2f, tip_disp=%.2f mm, theta=%.1f deg\n",
                        eMod, u, v, w, d3d, theta);
                } catch (Exception ex) {
                    System.out.println("  FAILED for E=" + eMod + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
