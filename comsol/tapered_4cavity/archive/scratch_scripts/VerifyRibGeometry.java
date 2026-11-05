import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class VerifyRibGeometry {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
            model.modelPath(baseDir);

            model.param().set("L_act", "100[mm]");
            model.param().set("R0", "10[mm]");
            model.param().set("Rt", "7[mm]");
            model.param().set("x_start", "5[mm]");
            model.param().set("x_end", "92[mm]");
            model.param().set("L_cav", "x_end - x_start");

            model.param().set("tw_0", "2.0[mm]");
            model.param().set("tw_t", "1.5[mm]");
            model.param().set("depth_cav", "3.0[mm]");

            model.param().set("r_out0", "R0 - tw_0");
            model.param().set("r_outt", "Rt - tw_t");
            model.param().set("r_in0", "r_out0 - depth_cav");
            model.param().set("r_int", "r_outt - depth_cav");

            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").lengthUnit("mm");

            model.component("comp1").geom("geom1").create("cone_body", "Cone");
            model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_body").set("pos", new double[]{0.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
            model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
            model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");

            // CRITICAL FIX: To get an 80-deg arc wedge from the intersection of two Y>=0 half-spaces,
            // each half-space must be rotated by +/- (90 - 80/2) = +/- 50 degrees!
            // Because [-90 + 50, 90 - 50] = [-40, +40] -> 80 degrees arc!
            double rotOffset = 50.0; // 90 - (80 / 2) = 50 deg -> 80 deg cavity arc, 10 deg solid rib!
            double[] rotAngles = {0.0, 90.0, 180.0, -90.0}; // 0 (+Y), +90 (+Z), 180 (-Y), -90 (-Z)
            String[] cavTags = new String[4];

            for (int k = 1; k <= 4; k++) {
                double baseRot = rotAngles[k-1];
                String b1Tag = "blk1_" + k;
                String b2Tag = "blk2_" + k;
                String r1Tag = "rot1_" + k;
                String r2Tag = "rot2_" + k;
                String wTag = "wedge_" + k;
                String sTag = "shell_" + k;
                String cTag = "cav_" + k;
                cavTags[k-1] = cTag;

                model.component("comp1").geom("geom1").create("cone_out_" + k, "Cone");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("axistype", "x");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{5.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", "r_out0");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", "r_outt");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", "L_cav");

                model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{5.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", "r_in0");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", "r_int");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", "L_cav");

                model.component("comp1").geom("geom1").create(sTag, "Difference");
                model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
                model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

                model.component("comp1").geom("geom1").create(b1Tag, "Block");
                model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{4.5, 0.0, -50.0});
                model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{90.0, 50.0, 100.0});

                model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
                model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
                model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

                model.component("comp1").geom("geom1").create(b2Tag, "Block");
                model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{4.5, 0.0, -50.0});
                model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{90.0, 50.0, 100.0});

                model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - rotOffset);
                model.component("comp1").geom("geom1").feature(r2Tag).set("axistype", "x");
                model.component("comp1").geom("geom1").feature(r2Tag).selection("input").set(b2Tag);

                model.component("comp1").geom("geom1").create(wTag, "Intersection");
                model.component("comp1").geom("geom1").feature(wTag).selection("input").set(r1Tag, r2Tag);

                model.component("comp1").geom("geom1").create(cTag, "Intersection");
                model.component("comp1").geom("geom1").feature(cTag).selection("input").set(sTag, wTag);
            }

            model.component("comp1").geom("geom1").create("dif_actuator", "Difference");
            model.component("comp1").geom("geom1").feature("dif_actuator").selection("input").set("cone_body");
            model.component("comp1").geom("geom1").feature("dif_actuator").selection("input2").set(cavTags);

            model.component("comp1").geom("geom1").run();

            int nDom = model.component("comp1").geom("geom1").getNDomains();
            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            System.out.println("FIXED GEOMETRY: nDomains = " + nDom + ", nBoundaries = " + nBnd);

            model.component("comp1").mesh().create("mesh1");
            model.component("comp1").mesh("mesh1").autoMeshSize(4);
            model.component("comp1").mesh("mesh1").run();

            model.result().dataset().create("dset_mesh", "Mesh");
            model.result().dataset("dset_mesh").set("mesh", "mesh1");

            System.out.println("b,area_mm2,xm,ym,zm,rm,ang_deg");
            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0] * 1e6;
                double xm = res[0][0] / res[3][0];
                double ym = res[1][0] / res[3][0];
                double zm = res[2][0] / res[3][0];
                double rm = Math.sqrt(ym*ym + zm*zm);
                double ang = Math.atan2(zm, ym) * 180.0 / Math.PI;
                model.result().numerical().remove("int_b");
                System.out.printf(Locale.US, "%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f\n", b, area, xm, ym, zm, rm, ang);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

