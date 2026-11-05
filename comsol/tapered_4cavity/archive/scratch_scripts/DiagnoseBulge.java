import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class DiagnoseBulge {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\actuator_tapered_4cavity_solved.mph");
            model.param().set("w1", "1");
            model.param().set("w2", "0");
            model.param().set("w3", "0");
            model.param().set("w4", "0");

            System.out.println("Running Case 1 (Cavity 1 pressurized)...");
            long t0 = System.currentTimeMillis();
            model.study("std1").run();
            long t1 = System.currentTimeMillis();
            System.out.println("Solved in " + (t1 - t0)/1000.0 + " s");

            // Evaluate min and max deformed Y coordinate on outer skin vs Cavity 3
            int[] outerSkin = {2, 3, 4, 5};
            int[] cav1 = {29, 30, 31, 32, 33, 35, 36, 37, 44, 45};
            int[] cav2 = {11, 17, 18, 20, 22, 25, 26, 40, 42};
            int[] cav3 = {6, 7, 8, 9, 10, 12, 13, 14, 38, 39};
            int[] cav4 = {15, 16, 19, 21, 23, 24, 27, 28, 34, 41, 43};

            Map<String, int[]> groups = new LinkedHashMap<>();
            groups.put("Outer Skin", outerSkin);
            groups.put("Cavity 1 (+Y Top, Pressurized)", cav1);
            groups.put("Cavity 2 (+Z Right)", cav2);
            groups.put("Cavity 3 (-Y Bottom)", cav3);
            groups.put("Cavity 4 (-Z Left)", cav4);

            for (Map.Entry<String, int[]> entry : groups.entrySet()) {
                String name = entry.getKey();
                int[] bnds = entry.getValue();

                model.result().numerical().create("max_y", "MaxSurface");
                model.result().numerical("max_y").set("data", "dset1");
                model.result().numerical("max_y").selection().set(bnds);
                model.result().numerical("max_y").set("expr", "(Y+v)*1000");
                model.result().numerical("max_y").set("solnum", "6");
                double maxY = model.result().numerical("max_y").getReal()[0][0];
                model.result().numerical().remove("max_y");

                model.result().numerical().create("min_y", "MinSurface");
                model.result().numerical("min_y").set("data", "dset1");
                model.result().numerical("min_y").selection().set(bnds);
                model.result().numerical("min_y").set("expr", "(Y+v)*1000");
                model.result().numerical("min_y").set("solnum", "6");
                double minY = model.result().numerical("min_y").getReal()[0][0];
                model.result().numerical().remove("min_y");

                model.result().numerical().create("avg_disp", "IntSurface");
                model.result().numerical("avg_disp").set("data", "dset1");
                model.result().numerical("avg_disp").selection().set(bnds);
                model.result().numerical("avg_disp").set("expr", new String[]{"solid.disp*1000", "1"});
                model.result().numerical("avg_disp").set("solnum", "6");
                double[][] res = model.result().numerical("avg_disp").getReal();
                double avgDisp = res[0][0] / res[1][0];
                model.result().numerical().remove("avg_disp");

                System.out.printf(Locale.US, "%-32s : Deformed Y in [%.2f, %.2f] mm | Avg solid.disp = %.2f mm\n",
                    name, minY, maxY, avgDisp);
            }

            // Check what plot features exist in pg_disp
            System.out.println("PlotGroup pg_disp features:");
            for (String feat : model.result("pg_disp").feature().tags()) {
                System.out.println("  " + feat + " (type=" + model.result("pg_disp").feature(feat).getType() + ")");
                try {
                    String[] expr = model.result("pg_disp").feature(feat).getStringArray("expr");
                    System.out.println("    expr: " + Arrays.toString(expr));
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

