import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestGeom {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").lengthUnit("mm");

            model.component("comp1").geom("geom1").create("cone_body", "Cone");
            model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_body").set("r", 10.0);
            model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_body").set("rtop", 7.0);
            model.component("comp1").geom("geom1").feature("cone_body").set("h", 100.0);

            double[] rotAngles = {0.0, -90.0, 180.0, 90.0};
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
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("r", 8.7);
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("rtop", 5.7);
                model.component("comp1").geom("geom1").feature("cone_out_" + k).set("h", 92.0);

                model.component("comp1").geom("geom1").create("cone_in_" + k, "Cone");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("axistype", "x");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("pos", new double[]{4.0, 0.0, 0.0});
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("r", 5.7);
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("specifytop", "radius");
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("rtop", 2.7);
                model.component("comp1").geom("geom1").feature("cone_in_" + k).set("h", 92.0);

                model.component("comp1").geom("geom1").create(sTag, "Difference");
                model.component("comp1").geom("geom1").feature(sTag).selection("input").set("cone_out_" + k);
                model.component("comp1").geom("geom1").feature(sTag).selection("input2").set("cone_in_" + k);

                model.component("comp1").geom("geom1").create(b1Tag, "Block");
                model.component("comp1").geom("geom1").feature(b1Tag).set("pos", new double[]{3.5, 0.0, -15.0});
                model.component("comp1").geom("geom1").feature(b1Tag).set("size", new double[]{93.0, 15.0, 30.0});

                model.component("comp1").geom("geom1").create(r1Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r1Tag).set("rot", baseRot + 40.0);
                model.component("comp1").geom("geom1").feature(r1Tag).set("axistype", "x");
                model.component("comp1").geom("geom1").feature(r1Tag).selection("input").set(b1Tag);

                model.component("comp1").geom("geom1").create(b2Tag, "Block");
                model.component("comp1").geom("geom1").feature(b2Tag).set("pos", new double[]{3.5, 0.0, -15.0});
                model.component("comp1").geom("geom1").feature(b2Tag).set("size", new double[]{93.0, 15.0, 30.0});

                model.component("comp1").geom("geom1").create(r2Tag, "Rotate");
                model.component("comp1").geom("geom1").feature(r2Tag).set("rot", baseRot - 40.0);
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

            // Mesh
            model.component("comp1").mesh().create("mesh1");
            model.component("comp1").mesh("mesh1").autoMeshSize(6);
            model.component("comp1").mesh("mesh1").run();

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            model.result().dataset().create("dset_mesh", "Mesh");
            model.result().dataset("dset_mesh").set("mesh", "mesh1");

            int[] cav1_list = new int[nBnd];
            int[] cav2_list = new int[nBnd];
            int[] cav3_list = new int[nBnd];
            int[] cav4_list = new int[nBnd];
            int c1_cnt = 0, c2_cnt = 0, c3_cnt = 0, c4_cnt = 0;

            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset_mesh");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[3][0];
                double xm = res[0][0] / area; // in mm
                double ym = res[1][0] / area; // in mm
                double zm = res[2][0] / area; // in mm
                double rm = Math.sqrt(ym*ym + zm*zm);
                model.result().numerical().remove("int_b");

                double r_cone_outer = 10.0 - 3.0 * xm / 100.0;

                if (xm < 1.0) {
                    System.out.printf("Bnd %2d: BASE (x=%.2f mm)%n", b, xm);
                } else if (xm > 99.0) {
                    System.out.printf("Bnd %2d: TIP_CAP (x=%.2f mm, r=%.2f mm)%n", b, xm, rm);
                } else if (rm > (r_cone_outer - 0.4)) {
                    System.out.printf("Bnd %2d: OUTER_SURFACE (x=%.2f mm, r=%.2f mm)%n", b, xm, rm);
                } else {
                    int cav_id = 0;
                    if (ym > Math.abs(zm)) {
                        cav_id = 1;
                        cav1_list[c1_cnt++] = b;
                    } else if (zm > Math.abs(ym)) {
                        cav_id = 2;
                        cav2_list[c2_cnt++] = b;
                    } else if (ym < -Math.abs(zm)) {
                        cav_id = 3;
                        cav3_list[c3_cnt++] = b;
                    } else if (zm < -Math.abs(ym)) {
                        cav_id = 4;
                        cav4_list[c4_cnt++] = b;
                    }
                    System.out.printf("Bnd %2d: CAVITY %d (x=%.2f, y=%.2f, z=%.2f, r=%.2f, area=%.2f mm2)%n",
                                      b, cav_id, xm, ym, zm, rm, area * 1e6);
                }
            }

            System.out.print("Cavity 1 (+Y) entities: [");
            for (int i = 0; i < c1_cnt; i++) System.out.print((i > 0 ? ", " : "") + cav1_list[i]);
            System.out.println("]");

            System.out.print("Cavity 2 (+Z) entities: [");
            for (int i = 0; i < c2_cnt; i++) System.out.print((i > 0 ? ", " : "") + cav2_list[i]);
            System.out.println("]");

            System.out.print("Cavity 3 (-Y) entities: [");
            for (int i = 0; i < c3_cnt; i++) System.out.print((i > 0 ? ", " : "") + cav3_list[i]);
            System.out.println("]");

            System.out.print("Cavity 4 (-Z) entities: [");
            for (int i = 0; i < c4_cnt; i++) System.out.print((i > 0 ? ", " : "") + cav4_list[i]);
            System.out.println("]");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
