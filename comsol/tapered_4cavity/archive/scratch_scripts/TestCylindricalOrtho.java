import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestCylindricalOrtho {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Slender Tapered Actuator (No Kevlar cylinder to eliminate sliver mesh)
        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("r", 6.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", 3.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("h", 160.0);

        // 4 Cavities
        double rotOffset = 50.0;
        double[] rotAngles = {0.0, 90.0, 180.0, -90.0};
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
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", 5.6);
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", 2.9);
            model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", 150.0);

            model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", 1.4);
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", 0.9);
            model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", 150.0);

            model.component("comp1").geom("geom1").create(sTag, "Difference");
            model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
            model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

            model.component("comp1").geom("geom1").create(b1Tag, "Block");
            model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{3.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{152.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + rotOffset);
            model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

            model.component("comp1").geom("geom1").create(b2Tag, "Block");
            model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -50.0});
            model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{152.0, 50.0, 100.0});

            model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
            model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - rotOffset);
            model.component("comp1").geom("geom1").feature(r2Tag).set("axistype", "x");
            model.component("comp1").geom("geom1").feature(r2Tag).selection("input").set(b2Tag);

            model.component("comp1").geom("geom1").create(wTag, "Intersection");
            model.component("comp1").geom("geom1").feature(wTag).selection("input").set(r1Tag, r2Tag);

            model.component("comp1").geom("geom1").create(cTag, "Intersection");
            model.component("comp1").geom("geom1").feature(cTag).selection("input").set(sTag, wTag);
        }

        model.component("comp1").geom("geom1").create("dif1", "Difference");
        model.component("comp1").geom("geom1").feature("dif1").selection("input").set("cone_body");
        model.component("comp1").geom("geom1").feature("dif1").selection("input2").set(cavTags);
        model.component("comp1").geom("geom1").run();

        // Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(4);
        model.component("comp1").mesh("mesh1").run();

        // Physics: Transversely Isotropic (symmetry axis along X)
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").feature("lemm1").set("SolidModel", "Orthotropic");
        model.component("comp1").physics("solid").feature("lemm1").set("Evector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("Evector", new String[]{"0.25[MPa]", "5.0[MPa]", "5.0[MPa]"});
        model.component("comp1").physics("solid").feature("lemm1").set("nuvector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("nuvector", new String[]{"0.25", "0.45", "0.25"});
        model.component("comp1").physics("solid").feature("lemm1").set("Gvector_mat", "userdef");
        model.component("comp1").physics("solid").feature("lemm1").set("Gvector", new String[]{"0.1[MPa]", "0.15[MPa]", "0.1[MPa]"});

        // Fixed Base
        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(new int[]{1});

        // Cavity 1 load
        // Automated boundary search for cavity 1
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        model.result().dataset().create("dset_m", "Mesh");
        model.result().dataset("dset_m").set("mesh", "mesh1");
        List<Integer> c1 = new ArrayList<>();
        for (int b = 1; b <= nBnd; b++) {
            if (b == 1 || b == nBnd || (b >= 2 && b <= 5)) continue;
            model.result().numerical().create("ib", "IntSurface");
            model.result().numerical("ib").set("data", "dset_m");
            model.result().numerical("ib").selection().set(new int[]{b});
            model.result().numerical("ib").set("expr", new String[]{"Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("ib").getReal();
            double ym = res[0][0] / res[2][0];
            double zm = res[1][0] / res[2][0];
            model.result().numerical().remove("ib");
            double ang = Math.atan2(zm, ym) * 180.0 / Math.PI;
            if (ang >= -45.0 && ang <= 45.0) c1.add(b);
        }

        int[] arr_c1 = c1.stream().mapToInt(i -> i).toArray();
        model.param().set("P_step", "0[kPa]");
        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().set(arr_c1);
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_step");

        // Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{"0 2 5 10 15 20 25 30 40 50"});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});

        System.out.println("Running Cylindrical Orthotropic solve without Kevlar cylinder...");
        long t0 = System.currentTimeMillis();
        try {
            model.study("std1").run();
            long t1 = System.currentTimeMillis();
            System.out.println("Solve completed in " + (t1 - t0) / 1000.0 + " s");
        } catch (Exception e) {
            System.out.println("Solve note: " + e.getMessage());
        }

        model.result().dataset().create("cpt", "CutPoint3D");
        model.result().dataset("cpt").set("data", "dset1");
        model.result().dataset("cpt").set("pointx", "160");
        model.result().dataset("cpt").set("pointy", "0");
        model.result().dataset("cpt").set("pointz", "0");

        model.result().numerical().create("pev", "EvalPoint");
        model.result().numerical("pev").set("data", "cpt");
        model.result().numerical("pev").set("expr", new String[]{"P_step", "u", "v", "w", "solid.disp"});
        model.result().numerical("pev").set("unit", new String[]{"kPa", "mm", "mm", "mm", "mm"});
        double[][] uvw = model.result().numerical("pev").getReal();

        System.out.println("P_kPa, u_tip, v_tip, w_tip, delta_3d, theta_deg");
        for (int s = 0; s < uvw[0].length; s++) {
            double p = uvw[0][s];
            double u = uvw[1][s];
            double v = uvw[2][s];
            double w = uvw[3][s];
            double d3 = uvw[4][s];
            double theta = Math.atan2(d3, 160.0 + u) * 180.0 / Math.PI;
            System.out.printf(Locale.US, "%.1f, %.3f, %.3f, %.3f, %.3f, %.2f deg\n", p, u, v, w, d3, theta);
        }

        return model;
    }
    public static void main(String[] args) { run(); }
}

