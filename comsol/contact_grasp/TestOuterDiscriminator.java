import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestOuterDiscriminator {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            Model model = ModelUtil.load("Model", mphFile);

            double L_act = 220.0;
            double R0 = 11.5;
            double Rt = 3.2;
            double X_cyl = 48.0;
            double Y_cyl = -42.0;
            double rCyl = 8.0;
            double L_cyl = 50.0;
            double x_start = 5.0;
            double x_end = 215.0;
            double tw_0 = 2.2;
            double tw_t = 0.65;
            double depth_0 = 2.8;
            double depth_t = 0.85;

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();

            List<Integer> list_cyl = new ArrayList<>();
            List<Integer> list_base = new ArrayList<>();
            List<Integer> list_act_outer = new ArrayList<>();
            List<List<Integer>> list_drive = new ArrayList<>();
            List<List<Integer>> list_balloon = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                list_drive.add(new ArrayList<>());
                list_balloon.add(new ArrayList<>());
            }

            double[][] cavAxisYZ = {{1.0, 0.0}, {0.0, 1.0}, {-1.0, 0.0}, {0.0, -1.0}};
            double r_out0_val = R0 - tw_0;
            double r_outt_val = Rt - tw_t;
            double r_in0_val = r_out0_val - depth_0;
            double r_int_val = r_outt_val - depth_t;
            double L_cav_val = x_end - x_start;

            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{
                    "X", "Y", "Z", "1",
                    "(sqrt((Y*1000)^2+(Z*1000)^2) - (11.5 - (11.5-3.2)*((X*1000)/220.0)))^2"
                });
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0];
                double xm = (res[0][0] / area) * 1000.0;
                double ym = (res[1][0] / area) * 1000.0;
                double zm = (res[2][0] / area) * 1000.0;
                double rm = Math.hypot(ym, zm);
                double mean_sq_err = res[4][0] / area; // in mm^2
                model.result().numerical().remove("int_b");

                // Check Cylinder
                double distCylAxis = Math.hypot(xm - X_cyl, ym - Y_cyl);
                if (distCylAxis <= rCyl + 0.8 && Math.abs(zm) <= L_cyl/2.0 + 0.8) {
                    list_cyl.add(b);
                    continue;
                }

                // Check Base
                if (Math.abs(xm) < 0.2) {
                    list_base.add(b);
                    continue;
                }

                // Check Tip Cap
                if (Math.abs(xm - L_act) < 0.2) {
                    list_act_outer.add(b);
                    continue;
                }

                // Check True Outer Skin
                if (mean_sq_err < 0.05) {
                    list_act_outer.add(b);
                    continue;
                }

                // Internal Cavity
                int kCav = -1;
                if (ym > Math.abs(zm)) kCav = 0;
                else if (zm > Math.abs(ym)) kCav = 1;
                else if (ym < -Math.abs(zm)) kCav = 2;
                else kCav = 3;

                if (Math.abs(xm - x_start) < 0.8 || Math.abs(xm - x_end) < 0.8) {
                    list_drive.get(kCav).add(b);
                    continue;
                }

                double cosDeltaPhi = (ym * cavAxisYZ[kCav][0] + zm * cavAxisYZ[kCav][1]) / rm;
                double xi = (xm - x_start) / L_cav_val;
                double r_out_expected = r_out0_val - (r_out0_val - r_outt_val) * xi;
                double r_in_expected = r_in0_val - (r_in0_val - r_int_val) * xi;
                double r_mid_expected = 0.5 * (r_out_expected + r_in_expected);

                if (cosDeltaPhi >= 0.92 && rm < r_mid_expected) {
                    list_drive.get(kCav).add(b);
                } else {
                    list_balloon.get(kCav).add(b);
                }
            }

            System.out.println("Cylinder: " + list_cyl);
            System.out.println("Base: " + list_base);
            System.out.println("Actuator Outer (Contact Destination): " + list_act_outer.size() + " bnds -> " + list_act_outer);
            int totalCav = 0;
            for (int k = 0; k < 4; k++) {
                int dCount = list_drive.get(k).size();
                int bCount = list_balloon.get(k).size();
                totalCav += dCount + bCount;
                System.out.printf("Cavity %d: drive=%d %s, balloon=%d %s%n",
                                  k+1, dCount, list_drive.get(k), bCount, list_balloon.get(k));
            }
            int total = list_cyl.size() + list_base.size() + list_act_outer.size() + totalCav;
            System.out.println("Sum of classified boundaries: " + total + " / " + nBnd);

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
