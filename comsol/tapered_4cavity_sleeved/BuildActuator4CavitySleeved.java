import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

/*
 * Tapered 4-cavity actuator, PHYSICAL model:
 *   - real gauge pressure P on every wetted face of the driven cavity (no tuned load factors);
 *   - real silicone: nearly incompressible neo-Hookean, nu = 0.4995, mixed u-p formulation
 *     (same material formulation as comsol/sleeved/BuildActuatorSleeve.java, which matches the
 *     verified Python FE: 43.0 vs 42.7 deg at 110 kPa);
 *   - optional inextensible hoop sleeve on the whole outer skin, same weak-form energy as that
 *     validated model: W = ks/2 * e_h^2, e_h = Green hoop strain along t = (0,-Z,Y)/r, which is
 *     tangent to the cone, so the taper does not change the formulation.
 *
 * Case selection: COMSOL's security preferences block System.getenv, so run_sleeved_study.py writes a
 * copy of this file (class SleevedCase) with the case values inserted at the CFG marker below.
 * Keys (defaults in env() calls are used when a key is absent):
 *   CASE_NAME   label used for output files
 *   SLEEVE      1 = hoop sleeve on, 0 = bare
 *   E_MPA       small-strain Young's modulus [MPa]
 *   DEPTH0, DEPTHT  cavity radial depth at base / tip [mm]
 *   CAV_DEG     cavity arc angle [deg] (rib angle = 90 - CAV_DEG)
 *   PLIST       pressure list [kPa], space separated
 * Prints DOE_ROW and CL_ROW lines; the Python runner turns them into CSV files.
 */
public class BuildActuator4CavitySleeved {

    static final String[][] CFG = {/*CFG*/};

    static String env(String k, String def) {
        for (String[] kv : CFG) if (kv[0].equals(k)) return kv[1].trim();
        return def;
    }

    public static Model run() {
        String caseName = env("CASE_NAME", "sleeved_base_E0.07");
        boolean sleeve = env("SLEEVE", "1").equals("1");
        double E = Double.parseDouble(env("E_MPA", "0.07"));
        double depth_0 = Double.parseDouble(env("DEPTH0", "2.5"));
        double depth_t = Double.parseDouble(env("DEPTHT", "1.0"));
        double cavDeg = Double.parseDouble(env("CAV_DEG", "56"));
        String plist = env("PLIST", "0 2 4 6 8 10 12.5 15 17.5 20 22.5 25 27.5 30 35 40 45 50 55 60 65 70 75 80 90 100");

        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity_sleeved";
        Model model = ModelUtil.create("Model");
        model.modelPath(baseDir);

        // ---------------- 1. Parameters (geometry = canonical tapered 4-cavity unless overridden)
        double L_act = 140.0, R0 = 9.5, Rt = 3.8, tw_0 = 2.0, tw_t = 0.8, x_start = 4.0, x_end = 136.0;
        double rotOffset = 90.0 - cavDeg / 2.0;   // half-space trick: sector half-angle = 90 - rotOffset

        model.param().set("L_act", L_act + "[mm]");
        model.param().set("R0", R0 + "[mm]");
        model.param().set("Rt", Rt + "[mm]");
        model.param().set("E_mod", E + "[MPa]", "Small-strain Young's modulus");
        model.param().set("nu_val", "0.4995", "Real silicone: nearly incompressible");
        model.param().set("mu_mod", "E_mod/(2*(1+nu_val))");
        model.param().set("kappa_mod", "2*mu_mod*(1+nu_val)/(3*(1-2*nu_val))");
        model.param().set("ks_sleeve", "100*E_mod*R0", "Hoop sleeve stiffness (inextensible limit), as validated model");
        model.param().set("P_step", "0[kPa]", "Gauge pressure in cavity 1");

        double r_out0 = R0 - tw_0, r_outt = Rt - tw_t;
        double r_in0 = r_out0 - depth_0, r_int = r_outt - depth_t;
        double L_cav = x_end - x_start;
        System.out.printf(Locale.US, "CASE %s: sleeve=%b E=%.3f MPa cav=%.1f deg depth %.2f->%.2f mm, core r %.2f->%.2f mm%n",
            caseName, sleeve, E, cavDeg, depth_0, depth_t, r_in0, r_int);

        // ---------------- 2. Geometry (same construction as BuildActuatorTapered4CavityFull)
        model.component().create("comp1", true);
        GeomSequence g = model.component("comp1").geom().create("geom1", 3);
        g.lengthUnit("mm");
        g.create("cone_body", "Cone");
        g.feature("cone_body").set("axistype", "x");
        g.feature("cone_body").set("pos", new double[]{0, 0, 0});
        g.feature("cone_body").set("r", "R0");
        g.feature("cone_body").set("specifytop", "radius");
        g.feature("cone_body").set("rtop", "Rt");
        g.feature("cone_body").set("h", "L_act");

        double rFil = Double.parseDouble(env("FILLET", "0"));   // corner fillet radius at the base [mm]
        double[] rotAngles = {0.0, -90.0, 180.0, 90.0};
        String[] cavTags = new String[4];
        if (rFil > 0) {
            // Filleted chambers: 2-D annular sector in a yz work plane at x_start, 4 corners filleted,
            // extruded to x_end with uniform scaling. Exact for a homothetic taper (all radii scale alike).
            double sc = r_outt / r_out0;
            if (Math.abs(r_int / r_in0 - sc) > 1e-9)
                throw new RuntimeException("FILLET needs a homothetic taper: r_int/r_in0 != r_outt/r_out0");
            System.out.printf(Locale.US, "FILLET r = %.3f mm at base -> %.3f mm at tip (scale %.3f)%n", rFil, rFil * sc, sc);
            g.create("wp1", "WorkPlane");
            g.feature("wp1").set("quickplane", "yz");
            g.feature("wp1").set("quickx", x_start);
            g.feature("wp1").set("unite", false);
            GeomSequence wg = g.feature("wp1").geom();
            for (int k = 1; k <= 4; k++) {
                double centre = 90.0 * (k - 1);   // local angle of the chamber centre line
                wg.create("so" + k, "Circle");
                wg.feature("so" + k).set("r", r_out0);
                wg.feature("so" + k).set("angle", cavDeg);
                wg.feature("so" + k).set("rot", centre - cavDeg / 2.0);
                wg.create("si" + k, "Circle");
                wg.feature("si" + k).set("r", r_in0);
                wg.feature("si" + k).set("angle", cavDeg);
                wg.feature("si" + k).set("rot", centre - cavDeg / 2.0);
                wg.create("sd" + k, "Difference");
                wg.feature("sd" + k).selection("input").set("so" + k);
                wg.feature("sd" + k).selection("input2").set("si" + k);
                wg.create("sf" + k, "Fillet");
                wg.feature("sf" + k).selection("point").set("sd" + k, new int[]{1, 2, 3, 4});
                wg.feature("sf" + k).set("radius", rFil);
            }
            g.create("ext", "Extrude");
            g.feature("ext").selection("input").set("wp1");
            g.feature("ext").setIndex("distance", String.valueOf(L_cav), 0);
            g.feature("ext").set("scale", new double[][]{{sc, sc}});
            g.create("dif", "Difference");
            g.feature("dif").selection("input").set("cone_body");
            g.feature("dif").selection("input2").set("ext");
        } else
        for (int k = 1; k <= 4; k++) {
            double baseRot = rotAngles[k - 1];
            g.create("co" + k, "Cone");
            g.feature("co" + k).set("axistype", "x");
            g.feature("co" + k).set("pos", new double[]{x_start, 0, 0});
            g.feature("co" + k).set("r", String.valueOf(r_out0));
            g.feature("co" + k).set("specifytop", "radius");
            g.feature("co" + k).set("rtop", String.valueOf(r_outt));
            g.feature("co" + k).set("h", String.valueOf(L_cav));
            g.create("ci" + k, "Cone");
            g.feature("ci" + k).set("axistype", "x");
            g.feature("ci" + k).set("pos", new double[]{x_start, 0, 0});
            g.feature("ci" + k).set("r", String.valueOf(r_in0));
            g.feature("ci" + k).set("specifytop", "radius");
            g.feature("ci" + k).set("rtop", String.valueOf(r_int));
            g.feature("ci" + k).set("h", String.valueOf(L_cav));
            g.create("sh" + k, "Difference");
            g.feature("sh" + k).selection("input").set("co" + k);
            g.feature("sh" + k).selection("input2").set("ci" + k);
            for (int s = 1; s <= 2; s++) {
                g.create("b" + s + "_" + k, "Block");
                g.feature("b" + s + "_" + k).set("pos", new double[]{x_start - 0.5, 0.0, -50.0});
                g.feature("b" + s + "_" + k).set("size", new double[]{L_cav + 2.0, 50.0, 100.0});
                g.create("r" + s + "_" + k, "Rotate");
                g.feature("r" + s + "_" + k).set("rot", baseRot + (s == 1 ? rotOffset : -rotOffset));
                g.feature("r" + s + "_" + k).set("axistype", "x");
                g.feature("r" + s + "_" + k).selection("input").set("b" + s + "_" + k);
            }
            g.create("w" + k, "Intersection");
            g.feature("w" + k).selection("input").set("r1_" + k, "r2_" + k);
            cavTags[k - 1] = "cav" + k;
            g.create("cav" + k, "Intersection");
            g.feature("cav" + k).selection("input").set("sh" + k, "w" + k);
        }
        if (rFil <= 0) {
            g.create("dif", "Difference");
            g.feature("dif").selection("input").set("cone_body");
            g.feature("dif").selection("input2").set(cavTags);
        }
        g.run();
        System.out.println("GEOM boundaries=" + g.getNBoundaries() + " domains=" + g.getNDomains());
        int nBnd = g.getNBoundaries();

        // ---------------- 3. Mesh
        model.component("comp1").mesh().create("mesh1");
        model.component("comp1").mesh("mesh1").autoMeshSize(Integer.parseInt(env("MESH", "4")));
        model.component("comp1").mesh("mesh1").run();
        model.result().dataset().create("dset_mesh", "Mesh");
        model.result().dataset("dset_mesh").set("mesh", "mesh1");

        // ---------------- 4. Purely geometric boundary classification (no index assumptions)
        // For each face: area, centroid x, mean direction (Y/r, Z/r), and RMS distance from the
        // outer cone r = R(X). Outer skin: RMS distance ~ 0.
        List<Integer> base = new ArrayList<>(), outer = new ArrayList<>(), tipFace = new ArrayList<>();
        List<List<Integer>> cav = new ArrayList<>();
        for (int k = 0; k < 4; k++) cav.add(new ArrayList<>());
        String Rx = "(" + R0 + "-(" + (R0 - Rt) + ")*X[1/mm]/" + L_act + ")"; // outer radius in mm
        for (int b = 1; b <= nBnd; b++) {
            model.result().numerical().create("ib", "IntSurface");
            model.result().numerical("ib").set("data", "dset_mesh");
            model.result().numerical("ib").selection().set(new int[]{b});
            model.result().numerical("ib").set("expr", new String[]{
                "1", "X[1/mm]", "Y/sqrt(Y^2+Z^2+1e-18[m^2])", "Z/sqrt(Y^2+Z^2+1e-18[m^2])",
                "(sqrt(Y^2+Z^2)[1/mm]-" + Rx + ")^2"});
            double[][] r = model.result().numerical("ib").getReal();
            model.result().numerical().remove("ib");
            double A = r[0][0], xm = r[1][0] / A, cy = r[2][0] / A, cz = r[3][0] / A;
            double rmsOuter = Math.sqrt(Math.max(r[4][0] / A, 0));
            System.out.printf(Locale.US, "FACE %d: A=%.4g xm=%.3f cy=%.3f cz=%.3f rmsOuter=%.4g%n", b, A, xm, cy, cz, rmsOuter);
            if (xm < 0.05) { base.add(b); continue; }
            if (xm > L_act - 0.05) { tipFace.add(b); continue; }
            // outer skin: ~0.06 mm (mesh faceting); cavity faces: >= 1.5 mm
            if (rmsOuter < 0.5) { outer.add(b); continue; }
            int k;
            if (cy > Math.abs(cz)) k = 0; else if (cz > Math.abs(cy)) k = 1; else if (cy < -Math.abs(cz)) k = 2; else k = 3;
            cav.get(k).add(b);
        }
        System.out.println("CLASSIFY base=" + base + " tip=" + tipFace + " outer=" + outer);
        for (int k = 0; k < 4; k++) System.out.println("CLASSIFY cavity" + (k + 1) + "=" + cav.get(k));
        int[] arrOuter = outer.stream().mapToInt(i -> i).toArray();
        if (env("CLASSIFY_ONLY", "0").equals("1")) return model;

        // ---------------- 5. Physics
        model.component("comp1").physics().create("solid", "SolidMechanics", "geom1");
        model.component("comp1").physics("solid").create("hmm1", "HyperelasticModel", 3);
        model.component("comp1").physics("solid").feature("hmm1").selection().all();
        model.component("comp1").physics("solid").feature("hmm1").set("MaterialModel", "NeoHookean");
        model.component("comp1").physics("solid").feature("hmm1").set("Compressibility_NeoHookean", "NearlyIncompressible");
        model.component("comp1").physics("solid").feature("hmm1").set("VolumetricEnergyUncoupled", "quadratic");
        model.component("comp1").physics("solid").feature("hmm1").set("MixedFormulation", "pFormulation");
        model.component("comp1").physics("solid").feature("hmm1").set("IsotropicOption", "KG");
        model.component("comp1").physics("solid").feature("hmm1").set("G_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("G", "mu_mod");
        model.component("comp1").physics("solid").feature("hmm1").set("K_mat", "userdef");
        model.component("comp1").physics("solid").feature("hmm1").set("K", "kappa_mod");

        model.component("comp1").physics("solid").create("fix1", "Fixed", 2);
        model.component("comp1").physics("solid").feature("fix1").selection().set(base.stream().mapToInt(i -> i).toArray());

        // Real pressure: same P on every wetted face of cavity 1 (pitch down). Cavities 2-4 vented.
        model.component("comp1").physics("solid").create("bndl1", "BoundaryLoad", 2);
        model.component("comp1").physics("solid").feature("bndl1").selection().set(cav.get(0).stream().mapToInt(i -> i).toArray());
        model.component("comp1").physics("solid").feature("bndl1").set("forceType", "FollowerPressure");
        model.component("comp1").physics("solid").feature("bndl1").set("pressure", "P_step");

        if (sleeve) {
            model.component("comp1").variable().create("var_sleeve");
            model.component("comp1").variable("var_sleeve").selection().geom("geom1", 2);
            model.component("comp1").variable("var_sleeve").selection().set(arrOuter);
            model.component("comp1").variable("var_sleeve").set("Rr", "sqrt(Y^2+Z^2)");
            model.component("comp1").variable("var_sleeve").set("dut", "(-Z*uTY + Y*uTZ)/Rr");
            model.component("comp1").variable("var_sleeve").set("dvt", "(-Z*vTY + Y*vTZ)/Rr");
            model.component("comp1").variable("var_sleeve").set("dwt", "(-Z*wTY + Y*wTZ)/Rr");
            model.component("comp1").variable("var_sleeve").set("eh", "(-Z*dvt + Y*dwt)/Rr + 0.5*(dut^2 + dvt^2 + dwt^2)");
            model.component("comp1").physics("solid").create("wf_sleeve", "WeakContribution", 2);
            model.component("comp1").physics("solid").feature("wf_sleeve").selection().set(arrOuter);
            model.component("comp1").physics("solid").feature("wf_sleeve").set("weakExpression", "-ks_sleeve*eh*test(eh)");
        }

        // ---------------- 6. Study
        model.study().create("std1");
        model.study("std1").create("stat", "Stationary");
        model.study("std1").feature("stat").set("geometricNonlinearity", true);
        model.study("std1").feature("stat").set("useparam", true);
        model.study("std1").feature("stat").set("pname", new String[]{"P_step"});
        model.study("std1").feature("stat").set("plistarr", new String[]{plist});
        model.study("std1").feature("stat").set("punit", new String[]{"kPa"});
        model.study("std1").showAutoSequences("all");

        // ---------------- 7. Probes: tip directors, centreline, section diameter, stress, J
        double rp = 1.5;
        String[][] tipPts = {{"py", String.valueOf(rp), "0"}, {"ny", String.valueOf(-rp), "0"}};
        for (String[] t : tipPts) {
            model.result().dataset().create("cp_" + t[0], "CutPoint3D");
            model.result().dataset("cp_" + t[0]).set("data", "dset1");
            model.result().dataset("cp_" + t[0]).set("pointx", String.valueOf(L_act - 0.2));
            model.result().dataset("cp_" + t[0]).set("pointy", t[1]);
            model.result().dataset("cp_" + t[0]).set("pointz", t[2]);
            model.result().numerical().create("pe_" + t[0], "EvalPoint");
            model.result().numerical("pe_" + t[0]).set("data", "cp_" + t[0]);
            model.result().numerical("pe_" + t[0]).set("expr", new String[]{"u", "v", "w"});
            model.result().numerical("pe_" + t[0]).set("unit", new String[]{"mm", "mm", "mm"});
        }
        int nCL = 29; // centreline x = 0, 5, ..., 140 (axis is inside the solid core)
        double[] xCL = new double[nCL];
        for (int i = 0; i < nCL; i++) {
            xCL[i] = Math.min(5.0 * i, L_act - 0.2);
            model.result().dataset().create("cp_cl" + i, "CutPoint3D");
            model.result().dataset("cp_cl" + i).set("data", "dset1");
            model.result().dataset("cp_cl" + i).set("pointx", String.valueOf(xCL[i]));
            model.result().dataset("cp_cl" + i).set("pointy", "0");
            model.result().dataset("cp_cl" + i).set("pointz", "0");
            model.result().numerical().create("pe_cl" + i, "EvalPoint");
            model.result().numerical("pe_cl" + i).set("data", "cp_cl" + i);
            model.result().numerical("pe_cl" + i).set("expr", new String[]{"u", "v", "w"});
            model.result().numerical("pe_cl" + i).set("unit", new String[]{"mm", "mm", "mm"});
        }

        double xb = 70.0, Rb = R0 - (R0 - Rt) * xb / L_act - 0.01;
        for (int j = 0; j < 2; j++) {
            String tg = j == 0 ? "bt" : "bb";
            model.result().dataset().create("cp_" + tg, "CutPoint3D");
            model.result().dataset("cp_" + tg).set("data", "dset1");
            model.result().dataset("cp_" + tg).set("pointx", String.valueOf(xb));
            model.result().dataset("cp_" + tg).set("pointy", String.valueOf((j == 0 ? 1 : -1) * Rb));
            model.result().dataset("cp_" + tg).set("pointz", "0");
            model.result().numerical().create("pe_" + tg, "EvalPoint");
            model.result().numerical("pe_" + tg).set("data", "cp_" + tg);
            model.result().numerical("pe_" + tg).set("expr", new String[]{"u", "v", "w"});
            model.result().numerical("pe_" + tg).set("unit", new String[]{"mm", "mm", "mm"});
        }
        model.result().numerical().create("mx_vm", "MaxVolume");
        model.result().numerical("mx_vm").set("data", "dset1");
        model.result().numerical("mx_vm").selection().all();
        model.result().numerical("mx_vm").set("expr", new String[]{"solid.mises"});
        model.result().numerical("mx_vm").set("unit", new String[]{"kPa"});
        model.result().numerical().create("mn_J", "MinVolume");
        model.result().numerical("mn_J").set("data", "dset1");
        model.result().numerical("mn_J").selection().all();
        model.result().numerical("mn_J").set("expr", new String[]{"solid.J"});

        // ---------------- 8. Solve (keep converged steps if the solver stops)
        long t0 = System.currentTimeMillis();
        try {
            model.study("std1").run();
        } catch (Exception e) {
            System.out.println("SOLVER_STOPPED: " + e.getMessage().split("\n")[0]);
        }
        double[] pv = null;
        try { pv = model.sol("sol1").getPVals(); } catch (Exception e) { }
        int n = pv == null ? 0 : pv.length;
        System.out.println("CONVERGED_STEPS: " + n + (n > 0 ? ", last P = " + pv[n - 1] / 1000.0 + " kPa" : "")
            + ", solve time " + (System.currentTimeMillis() - t0) / 1000 + " s");
        if (n == 0) return model;

        double[][] py = model.result().numerical("pe_py").getReal();
        double[][] ny = model.result().numerical("pe_ny").getReal();
        double[][][] cl = new double[nCL][][];
        for (int i = 0; i < nCL; i++) cl[i] = model.result().numerical("pe_cl" + i).getReal();
        double[][] bt = model.result().numerical("pe_bt").getReal();
        double[][] bb = model.result().numerical("pe_bb").getReal();
        double[][] vm = model.result().numerical("mx_vm").getReal();
        double[][] jm = model.result().numerical("mn_J").getReal();

        double thPrev = 0.0;
        for (int s = 0; s < n; s++) {
            double P = pv[s] / 1000.0;
            // material director across the tip section (originally +y); bending is about z
            double ex = (py[0][s] - ny[0][s]) / (2 * rp), ey = (2 * rp + py[1][s] - ny[1][s]) / (2 * rp);
            double th = Math.toDegrees(Math.atan2(ex, ey));
            while (th - thPrev > 180) th -= 360;            // unwrap continuously across steps
            while (th - thPrev < -180) th += 360;
            thPrev = th;
            int last = nCL - 1;
            double uT = cl[last][0][s], vT = cl[last][1][s], wT = cl[last][2][s];
            double dx = bt[0][s] - bb[0][s], dy = 2 * Rb + bt[1][s] - bb[1][s], dz = bt[2][s] - bb[2][s];
            double Dr = Math.sqrt(dx * dx + dy * dy + dz * dz) / (2 * Rb);
            System.out.printf(Locale.US, "DOE_ROW: %s,%d,%.4f,%.1f,%.2f,%.2f,%.3f,%.3f,%.3f,%.3f,%.2f,%.4f,%.4f%n",
                caseName, sleeve ? 1 : 0, E, cavDeg, depth_0, P, xCL[last] + uT, vT, wT, th, vm[0][s], jm[0][s], Dr);
            for (int i = 0; i < nCL; i++) {
                System.out.printf(Locale.US, "CL_ROW: %s,%.2f,%.1f,%.3f,%.3f,%.3f%n", caseName, P, xCL[i],
                    xCL[i] + cl[i][0][s], cl[i][1][s], cl[i][2][s]);
            }
        }

        // ---------------- 9. Images at last converged step (true scale)
        try {
            model.result().create("pg_d", "PlotGroup3D");
            model.result("pg_d").set("data", "dset1");
            model.result("pg_d").set("solnum", String.valueOf(n));
            model.result("pg_d").create("s", "Surface");
            model.result("pg_d").feature("s").set("expr", "solid.disp");
            model.result("pg_d").feature("s").set("unit", "mm");
            model.result("pg_d").feature("s").create("d", "Deform");
            model.result("pg_d").feature("s").feature("d").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_d").feature("s").feature("d").set("scaleactive", true);
            model.result("pg_d").feature("s").feature("d").set("scale", 1.0);
            model.result("pg_d").run();
            model.result().export().create("im_d", "Image3D");
            model.result().export("im_d").set("sourceobject", "pg_d");
            model.result().export("im_d").set("size", "manualweb");
            model.result().export("im_d").set("unit", "px");
            model.result().export("im_d").set("width", 1400);
            model.result().export("im_d").set("height", 900);
            model.result().export("im_d").set("filename", baseDir + "\\images\\" + caseName + "_disp_last.png");
            model.result().export("im_d").run();

            model.result().create("pg_s", "PlotGroup3D");
            model.result("pg_s").set("data", "dset1");
            model.result("pg_s").set("solnum", String.valueOf(n));
            model.result("pg_s").create("s", "Surface");
            model.result("pg_s").feature("s").set("expr", "solid.mises");
            model.result("pg_s").feature("s").set("unit", "kPa");
            model.result("pg_s").feature("s").create("d", "Deform");
            model.result("pg_s").feature("s").feature("d").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_s").feature("s").feature("d").set("scaleactive", true);
            model.result("pg_s").feature("s").feature("d").set("scale", 1.0);
            model.result("pg_s").run();
            model.result().export().create("im_s", "Image3D");
            model.result().export("im_s").set("sourceobject", "pg_s");
            model.result().export("im_s").set("size", "manualweb");
            model.result().export("im_s").set("unit", "px");
            model.result().export("im_s").set("width", 1400);
            model.result().export("im_s").set("height", 900);
            model.result().export("im_s").set("filename", baseDir + "\\images\\" + caseName + "_stress_last.png");
            model.result().export("im_s").run();
        } catch (Exception e) {
            System.out.println("IMAGE_EXPORT_FAILED: " + e.getMessage());
        }
        try {
            model.save(baseDir + "\\" + caseName + "_solved.mph");
        } catch (Exception e) {
            System.out.println("MPH_SAVE_FAILED: " + e.getMessage());
        }
        return model;
    }

    public static void main(String[] args) { run(); }
}
