import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

/*
 * Renders the COMSOL images used in the BTP report from the solved .mph files.
 * Nothing is re-solved: each model is loaded, plot groups are added, images are exported.
 * It also re-evaluates the tip angle of the single-cavity models from the loaded solution,
 * so the images can be checked against the CSV files used in the report.
 *
 * Compile and run (from this folder):
 *   comsolcompile RenderReport.java
 *   comsolbatch -inputfile RenderReport.class
 */
public class Render180 {

    static final String ROOT = "C:\\Users\\rudra\\Documents\\BTP\\";
    static final String OUT = ROOT + "report\\btp_report\\render\\raw\\";
    static int cnt = 0;

    // camera looking from +z (side view of bending in the x-y plane)
    static final double[] SIDE = {0, 0, 1, 0, 1, 0};
    // camera looking from +x towards the base (end view of a cross-section)
    static final double[] END = {1, 0, 0, 0, 1, 0};
    // oblique view
    static final double[] ISO = {0.55, 0.45, 0.70, 0, 1, 0};

    public static Model run() {
        String name = "fc_sleeved";
        Model m;
        try { m = ModelUtil.load("m180", ROOT + "comsol\\tapered_4cavity_sleeved\\sleeved_base_E0.07_solved.mph"); }
        catch (Exception e) { System.out.println("LOAD_FAILED: " + e.getMessage()); return null; }
        double[] pv = pvals(m);
        System.out.println("PVALS " + Arrays.toString(pv));
        for (double p : new double[]{}) {
            int s = idx(pv, p);
            if (s < 0) { System.out.println("NO_STEP " + p); continue; }
            String tag = String.valueOf(p / 1000).replace(".0", "").replace(".", "p");
            img(m, "dset1", s, "Surface", "solid.disp", "mm", true, SIDE, new double[]{60, -40, 0}, 200,
                null, null, String.format(Locale.US, "P = %.1f kPa   Surface: displacement magnitude (mm)", p / 1000),
                OUT + name + "_side_P" + tag + ".png");
            img(m, "dset1", s, "Surface", "solid.mises", "kPa", true, SIDE, new double[]{60, -40, 0}, 200,
                new double[]{0, 1400}, null,
                String.format(Locale.US, "P = %.1f kPa   Surface: von Mises stress (kPa), range 0-1400", p / 1000),
                OUT + name + "_mises_side_P" + tag + ".png");
        }
        // where the stress exceeds 1.4 MPa: transparent grey body + red volume elements above 1.4 MPa
        for (double p : new double[]{20000}) {
            int s = idx(pv, p);
            if (s < 0) continue;
            String tag = String.valueOf(p / 1000).replace(".0", "").replace(".", "p");
            for (int k = 2; k < 3; k++) {
                double[] dir = k == 0 ? SIDE : ISO;
                String file = OUT + name + "_over_" + (k == 0 ? "side" : k == 1 ? "iso" : "zoom") + "_P" + tag + ".png";
                String pg = "ov" + tag + k;
                try {
                    m.result().create(pg, "PlotGroup3D");
                    m.result(pg).set("data", "dset1");
                    m.result(pg).set("solnum", String.valueOf(s));
                    m.result(pg).set("titletype", "manual");
                    m.result(pg).set("title", String.format(Locale.US, "P = %.1f kPa   red: von Mises stress > 1.4 MPa", p / 1000));
                    m.result(pg).set("edges", "off");
                    m.result(pg).create("b", "Surface");
                    m.result(pg).feature("b").set("expr", "1");
                    m.result(pg).feature("b").set("coloring", "uniform");
                    m.result(pg).feature("b").set("color", "gray");
                    m.result(pg).feature("b").create("d", "Deform");
                    m.result(pg).feature("b").feature("d").set("expr", new String[]{"u", "v", "w"});
                    m.result(pg).feature("b").feature("d").set("scaleactive", true);
                    m.result(pg).feature("b").feature("d").set("scale", 1.0);
                    try { m.result(pg).feature("b").create("t", "Transparency"); m.result(pg).feature("b").feature("t").set("transparency", 0.7); } catch (Exception e) { System.out.println("TRANSP_FAILED: " + e.getMessage()); }
                    m.result(pg).create("v", "Volume");
                    m.result(pg).feature("v").set("expr", "solid.mises");
                    m.result(pg).feature("v").set("unit", "MPa");
                    m.result(pg).feature("v").set("coloring", "uniform");
                    m.result(pg).feature("v").set("color", "red");
                    m.result(pg).feature("v").create("d", "Deform");
                    m.result(pg).feature("v").feature("d").set("expr", new String[]{"u", "v", "w"});
                    m.result(pg).feature("v").feature("d").set("scaleactive", true);
                    m.result(pg).feature("v").feature("d").set("scale", 1.0);
                    m.result(pg).feature("v").create("f", "Filter");
                    m.result(pg).feature("v").feature("f").set("expr", "solid.mises>1.4[MPa]");
                    m.result(pg).run();
                    String vw = "ovw" + tag + k;
                    double[] tg = k == 2 ? new double[]{8, 4, 0} : new double[]{60, -40, 0};
                    double D = 5000, ext = k == 2 ? 40 : 200;
                    m.component("comp1").view().create(vw, "geom1");
                    m.component("comp1").view(vw).set("showgrid", false);
                    double n = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
                    m.component("comp1").view(vw).camera().set("projection", "perspective");
                    m.component("comp1").view(vw).camera().set("target", tg);
                    m.component("comp1").view(vw).camera().set("position",
                        new double[]{tg[0] + D * dir[0] / n, tg[1] + D * dir[1] / n, tg[2] + D * dir[2] / n});
                    m.component("comp1").view(vw).camera().set("up", new double[]{dir[3], dir[4], dir[5]});
                    m.component("comp1").view(vw).camera().set("rotationpoint", tg);
                    m.component("comp1").view(vw).camera().set("zoomanglefull", 2 * Math.toDegrees(Math.atan(0.5 * ext / D)));
                    m.result(pg).set("view", vw);
                    String ex = "oex" + tag + k;
                    m.result().export().create(ex, "Image3D");
                    m.result().export(ex).set("sourceobject", pg);
                    m.result().export(ex).set("size", "manualweb");
                    m.result().export(ex).set("unit", "px");
                    m.result().export(ex).set("width", 1800);
                    m.result().export(ex).set("height", 1200);
                    for (String[] kv : new String[][]{{"options", "on"}, {"title", "on"}, {"legend", "off"}, {"axisorientation", "on"}, {"grid", "off"}, {"logo", "off"}, {"fontsize", "20"}})
                        try { m.result().export(ex).set(kv[0], kv[1]); } catch (Exception e) { }
                    m.result().export(ex).set("filename", file);
                    m.result().export(ex).run();
                    System.out.println("IMAGE_OK " + file);
                } catch (Exception e) { System.out.println("IMAGE_FAILED " + file + ": " + e.getMessage().split("\n")[0]); }
            }
            // section at x = 20 and 110 mm, stress range 0-1400 kPa
            for (double xs : new double[]{}) {
                img(m, "dset1", s, "Slice", "solid.mises", "kPa", true, END, new double[]{xs, -10, 0}, 40,
                    new double[]{0, 1400}, new double[]{xs},
                    String.format(Locale.US, "P = %.1f kPa   Slice x = %.0f mm: von Mises stress (kPa)", p / 1000, xs),
                    OUT + name + "_section_P" + tag + "_x" + (int) xs + ".png");
            }
        }
        // x position of the highest stress at each step
        try {
            if (true) throw new RuntimeException("skip");
            m.result().numerical("mxc").set("data", "dset1");
            m.result().numerical("mxc").selection().all();
            m.result().numerical("mxc").set("expr", new String[]{"solid.mises"});
            m.result().numerical("mxc").set("unit", new String[]{"kPa"});
            m.result().numerical("mxc").set("includepos", true);
            m.result().numerical().create("tb", "Table");
            m.result().numerical("mxc").set("table", "tb");
            m.result().numerical("mxc").computeResult();
            m.result().numerical("mxc").setResult();
            double[][] t = m.result().table("tb").getReal();
            for (double[] r : t) System.out.println("MAXPOS " + Arrays.toString(r));
        } catch (Exception e) { System.out.println("MAXPOS_FAILED: " + e.getMessage().split("\n")[0]); }
        return null;
    }

    static double[] pvals(Model m) {
        try { return m.sol("sol1").getPVals(); } catch (Exception e) { return new double[0]; }
    }

    static int idx(double[] pv, double p) {
        for (int i = 0; i < pv.length; i++) if (Math.abs(pv[i] - p) < 1e-6 * Math.max(1, Math.abs(p))) return i + 1;
        return -1;
    }

    /* single-cavity half model (z >= 0); P in kPa (parameter P_kPa) */
    static void single(String rel, String name) {
        Model m;
        try { m = ModelUtil.load("m_" + name, ROOT + "comsol\\" + rel); }
        catch (Exception e) { System.out.println("LOAD_FAILED " + rel + ": " + e.getMessage()); return; }
        double[] pv = pvals(m);
        System.out.println("MODEL " + name + " PVALS " + Arrays.toString(pv));

        // check: tip angle from the two tip points, as in the build scripts
        try {
            String[] pts = {"100", "10", "0", "100", "-10", "0"};
            for (int k = 0; k < 2; k++) {
                String t = "chk" + k;
                m.result().dataset().create(t, "CutPoint3D");
                m.result().dataset(t).set("data", "dset1");
                m.result().dataset(t).set("pointx", pts[3 * k]);
                m.result().dataset(t).set("pointy", pts[3 * k + 1]);
                m.result().dataset(t).set("pointz", pts[3 * k + 2]);
                m.result().numerical().create("ev" + k, "EvalPoint");
                m.result().numerical("ev" + k).set("data", t);
                m.result().numerical("ev" + k).set("expr", new String[]{"u", "v"});
                m.result().numerical("ev" + k).set("unit", new String[]{"mm", "mm"});
            }
            double[][] a = m.result().numerical("ev0").getReal();
            double[][] b = m.result().numerical("ev1").getReal();
            for (int i = 0; i < a[0].length; i++) {
                double th = Math.toDegrees(Math.atan2((100 + a[0][i]) - (100 + b[0][i]), (10 + a[1][i]) - (-10 + b[1][i])));
                double dl = -0.5 * (a[1][i] + b[1][i]);
                System.out.printf(Locale.US, "CHECK %s P=%.1f theta=%.3f delta=%.3f%n", name, pv[i], th, dl);
            }
        } catch (Exception e) { System.out.println("CHECK_FAILED " + name + ": " + e.getMessage()); }

        try {
            m.result().dataset().create("mir", "Mirror3D");
            m.result().dataset("mir").set("data", "dset1");
            m.result().dataset("mir").set("planetype", "quick");
            m.result().dataset("mir").set("quickplane", "xy");
        } catch (Exception e) { System.out.println("MIRROR_FAILED: " + e.getMessage()); }

        int last = pv.length;
        // mesh (half model as solved)
        img(m, "dset1", 1, "Mesh", null, null, false, new double[]{-0.6, 0.35, 0.72, 0, 1, 0}, new double[]{25, 0, 4}, 70, null, null,
            "Mesh: free tetrahedral (half model)", OUT + name + "_mesh.png");
        // deformed shapes, side view, displacement magnitude
        double[] ps = name.equals("sc_bare") ? new double[]{40, 90} : new double[]{40, 90, 110};
        for (double p : ps) {
            int s = idx(pv, p);
            if (s < 0) continue;
            img(m, "mir", s, "Surface", "solid.disp", "mm", true, SIDE, new double[]{52, -15, 0}, 140,
                new double[]{0, 45}, null,
                String.format(Locale.US, "P = %.0f kPa   Surface: displacement magnitude (mm)", p),
                OUT + name + "_side_P" + (int) p + ".png");
        }
        // von Mises at the last step, oblique view
        img(m, "mir", last, "Surface", "solid.mises", "kPa", true, ISO, new double[]{52, -15, 0}, 150,
            null, null,
            String.format(Locale.US, "P = %.0f kPa   Surface: von Mises stress (kPa)", pv[last - 1]),
            OUT + name + "_mises_last.png");
        // cross-section at mid-length, end view, at P = 0 and 90 kPa
        for (double p : new double[]{0, 90}) {
            int s = idx(pv, p);
            if (s < 0) continue;
            img(m, "mir", s, "Slice", "solid.mises", "kPa", true, END, new double[]{50, -10, 0}, 60,
                new double[]{0, 120}, new double[]{50},
                String.format(Locale.US, "P = %.0f kPa   Slice x = 50 mm: von Mises stress (kPa)", p),
                OUT + name + "_section_P" + (int) p + ".png");
        }
        ModelUtil.remove("m_" + name);
    }

    /* tapered 4-chamber full model; P in Pa (parameter P) */
    static void four(String rel, String name, boolean full) {
        Model m;
        try { m = ModelUtil.load("m_" + name, ROOT + "comsol\\" + rel); }
        catch (Exception e) { System.out.println("LOAD_FAILED " + rel + ": " + e.getMessage()); return; }
        double[] pv = pvals(m);
        System.out.println("MODEL " + name + " PVALS " + Arrays.toString(pv));
        int last = pv.length;

        img(m, "dset1", 1, "Mesh", null, null, false, new double[]{-0.5, 0.45, 0.75, 0, 1, 0}, new double[]{22, 0, 0}, 55, null, null,
            "Mesh: free tetrahedral (elements with z > 0 hidden)", OUT + name + "_mesh.png");
        // cross-sections at x = 20 and 100 mm, undeformed mesh (shows the four chambers)
        for (double xs : new double[]{20, 100}) {
            img(m, "dset1", 1, "Slice", "solid.mises", "kPa", false, END, null, 30,
                null, new double[]{xs}, "",
                OUT + name + "_section_geom_x" + (int) xs + ".png");
        }
        if (!full) {
            // bare: last converged step, side view
            if (name.equals("fc_bare")) img(m, "dset1", last, "Surface", "solid.disp", "mm", true, SIDE, new double[]{60, -20, 0}, 180, null, null,
                String.format(Locale.US, "P = %.2f kPa   Surface: displacement magnitude (mm)", pv[last - 1] / 1000),
                OUT + name + "_side_last.png");
            int s10 = idx(pv, 10000);
            if (s10 > 0) img(m, "dset1", s10, "Slice", "solid.mises", "kPa", true, END, new double[]{40, -3, 0}, 36,
                new double[]{0, 30}, new double[]{40},
                "P = 10 kPa   Slice x = 40 mm: von Mises stress (kPa)", OUT + name + "_section_P10_x40.png");
            ModelUtil.remove("m_" + name);
            return;
        }
        for (double p : new double[]{10000, 15000}) {
            int s = idx(pv, p);
            if (s < 0) { System.out.println("NO_STEP " + p); continue; }
            img(m, "dset1", s, "Surface", "solid.disp", "mm", true, SIDE, new double[]{60, -30, 0}, 190,
                new double[]{0, 110}, null,
                String.format(Locale.US, "P = %.0f kPa   Surface: displacement magnitude (mm)", p / 1000),
                OUT + name + "_side_P" + (int) (p / 1000) + ".png");
        }
        int s15 = idx(pv, 15000), s10 = idx(pv, 10000);
        if (s15 > 0) img(m, "dset1", s15, "Surface", "solid.mises", "kPa", true, ISO, new double[]{60, -30, 0}, 190,
            null, null, "P = 15 kPa   Surface: von Mises stress (kPa)", OUT + name + "_mises_P15.png");
        for (int s : new int[]{s10}) {
            if (s < 0) continue;
            img(m, "dset1", s, "Slice", "solid.mises", "kPa", true, END, new double[]{40, -3, 0}, 36,
                new double[]{0, 30}, new double[]{40},
                String.format(Locale.US, "P = %.0f kPa   Slice x = 40 mm: von Mises stress (kPa)", pv[s - 1] / 1000),
                OUT + name + "_section_P" + (int) (pv[s - 1] / 1000) + "_x40.png");
        }
        ModelUtil.remove("m_" + name);
    }

    /*
     * One image. dir = {dx,dy,dz, ux,uy,uz}: camera direction (from target to camera) and up vector.
     * target = null: centred on the slice (x = slice, y = z = 0).
     * extent = width of the scene that must fit [mm]; the image is trimmed later.
     */
    static void img(Model m, String data, int solnum, String type, String expr, String unit, boolean deform,
                    double[] dir, double[] target, double extent, double[] range, double[] sliceX,
                    String title, String file) {
        String pg = "rpg" + (++cnt);
        String vw = "rvw" + cnt;
        try {
            m.result().create(pg, "PlotGroup3D");
            m.result(pg).set("data", data);
            if (!type.equals("Mesh")) m.result(pg).set("solnum", String.valueOf(solnum));
            m.result(pg).set("titletype", "manual");
            m.result(pg).set("title", title);
            m.result(pg).set("edges", type.equals("Slice") ? "off" : "on");
            m.result(pg).create("f", type);
            if (type.equals("Mesh")) {
                m.result(pg).feature("f").set("meshdomain", "volume");
                if (title.contains("hidden")) {
                    try {
                        m.result(pg).feature("f").create("filt", "MeshFilter");
                        m.result(pg).feature("f").feature("filt").set("expr", "z<0");
                    } catch (Exception e) {
                        System.out.println("FILTER_FAILED: " + e.getMessage().split("\n")[0]);
                        m.result(pg).set("title", "Mesh: free tetrahedral");
                    }
                }
            } else {
                m.result(pg).feature("f").set("expr", expr);
                m.result(pg).feature("f").set("unit", unit);
                m.result(pg).feature("f").set("colortable", "RainbowLight");
                if (range != null) {
                    m.result(pg).feature("f").set("rangecoloractive", true);
                    m.result(pg).feature("f").set("rangecolormin", range[0]);
                    m.result(pg).feature("f").set("rangecolormax", range[1]);
                }
                if (type.equals("Slice")) {
                    m.result(pg).feature("f").set("quickplane", "yz");
                    m.result(pg).feature("f").set("quickxmethod", "coord");
                    m.result(pg).feature("f").set("quickx", String.valueOf(sliceX[0]));
                    if (expr.equals("solid.mises") && title.isEmpty()) {
                        // geometry-only section: plain grey
                        m.result(pg).feature("f").set("coloring", "uniform");
                        m.result(pg).feature("f").set("color", "gray");
                    }
                }
                if (deform) {
                    m.result(pg).feature("f").create("d", "Deform");
                    m.result(pg).feature("f").feature("d").set("expr", new String[]{"u", "v", "w"});
                    m.result(pg).feature("f").feature("d").set("scaleactive", true);
                    m.result(pg).feature("f").feature("d").set("scale", 1.0);
                }
            }
            m.result(pg).run();

            double[] tg = target != null ? target : new double[]{sliceX[0], 0, 0};
            double D = 5000;
            m.component("comp1").view().create(vw, "geom1");
            m.component("comp1").view(vw).set("showgrid", false);
            try { m.component("comp1").view(vw).set("showaxisorientation", true); } catch (Exception e) { }
            double n = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
            m.component("comp1").view(vw).camera().set("projection", "perspective");
            m.component("comp1").view(vw).camera().set("target", new double[]{tg[0], tg[1], tg[2]});
            m.component("comp1").view(vw).camera().set("position",
                new double[]{tg[0] + D * dir[0] / n, tg[1] + D * dir[1] / n, tg[2] + D * dir[2] / n});
            m.component("comp1").view(vw).camera().set("up", new double[]{dir[3], dir[4], dir[5]});
            m.component("comp1").view(vw).camera().set("rotationpoint", new double[]{tg[0], tg[1], tg[2]});
            m.component("comp1").view(vw).camera().set("zoomanglefull", 2 * Math.toDegrees(Math.atan(0.5 * extent / D)));
            m.result(pg).set("view", vw);

            String ex = "rex" + cnt;
            m.result().export().create(ex, "Image3D");
            m.result().export(ex).set("sourceobject", pg);
            m.result().export(ex).set("size", "manualweb");
            m.result().export(ex).set("unit", "px");
            m.result().export(ex).set("width", 1800);
            m.result().export(ex).set("height", 1200);
            for (String[] kv : new String[][]{{"options", "on"}, {"title", "on"}, {"legend", "on"}, {"axisorientation", "on"}, {"grid", "off"}, {"logo", "off"}, {"fontsize", "20"}})
                try { m.result().export(ex).set(kv[0], kv[1]); } catch (Exception e) { System.out.println("EXPORT_OPT " + kv[0] + ": " + e.getMessage().split("\n")[0]); }
            m.result().export(ex).set("filename", file);
            m.result().export(ex).run();
            System.out.println("IMAGE_OK " + file);
        } catch (Exception e) {
            System.out.println("IMAGE_FAILED " + file + ": " + e.getMessage().split("\n")[0]);
        }
    }

    public static void main(String[] args) { run(); }
}
