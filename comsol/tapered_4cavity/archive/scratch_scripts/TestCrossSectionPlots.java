import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestCrossSectionPlots {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\TestRibbedVisuals_Model.mph");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";

            // Solnum 5 is P = 40 kPa (cleanly converged)
            // 1. Create CutPlane dataset at X = 45 mm (mid-actuator cross-section)
            model.result().dataset().create("cpl_mid", "CutPlane");
            model.result().dataset("cpl_mid").set("data", "dset1");
            model.result().dataset("cpl_mid").set("quickplane", "yz");
            model.result().dataset("cpl_mid").set("quickx", "45[mm]");

            // 2. 2D Cross-Section Plot Group showing the 4 Arc Cavities & Stress on the Separations
            model.result().create("pg_sec2d", "PlotGroup2D");
            model.result("pg_sec2d").set("data", "cpl_mid");
            model.result("pg_sec2d").set("solnum", "5");
            model.result("pg_sec2d").label("Cross-Section Cavity & Rib Separation Stress (X = 45 mm)");
            model.result("pg_sec2d").create("surf_sec", "Surface");
            model.result("pg_sec2d").feature("surf_sec").set("expr", "solid.mises");
            model.result("pg_sec2d").feature("surf_sec").set("unit", "kPa");
            model.result("pg_sec2d").feature("surf_sec").set("colortable", "RainbowLight");
            model.result("pg_sec2d").feature("surf_sec").create("def", "Deform");
            model.result("pg_sec2d").feature("surf_sec").feature("def").set("expr", new String[]{"v", "w"});
            model.result("pg_sec2d").feature("surf_sec").feature("def").set("scaleactive", true);
            model.result("pg_sec2d").feature("surf_sec").feature("def").set("scale", 1);
            model.result("pg_sec2d").run();

            model.result().export().create("exp_sec2d", "Image2D");
            model.result().export("exp_sec2d").set("sourceobject", "pg_sec2d");
            model.result().export("exp_sec2d").set("filename", baseDir + "\\test_cross_section_2d_stress.png");
            model.result().export("exp_sec2d").set("size", "manualweb");
            model.result().export("exp_sec2d").set("unit", "px");
            model.result().export("exp_sec2d").set("width", 1200);
            model.result().export("exp_sec2d").set("height", 1000);
            model.result().export("exp_sec2d").run();

            // 3. 3D Multi-Slice Stress Plot showing 6 Cross-Sectional Slices along the 3D Deformed Actuator
            model.result().create("pg_slice3d", "PlotGroup3D");
            model.result("pg_slice3d").set("data", "dset1");
            model.result("pg_slice3d").set("solnum", "5");
            model.result("pg_slice3d").label("3D Cross-Sectional Slices - Rib & Cavity Stress (solid.mises)");
            model.result("pg_slice3d").create("slc1", "Slice");
            model.result("pg_slice3d").feature("slc1").set("expr", "solid.mises");
            model.result("pg_slice3d").feature("slc1").set("unit", "kPa");
            model.result("pg_slice3d").feature("slc1").set("quickplane", "yz");
            model.result("pg_slice3d").feature("slc1").set("quickxnumber", 7);
            model.result("pg_slice3d").feature("slc1").create("def", "Deform");
            model.result("pg_slice3d").feature("slc1").feature("def").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_slice3d").feature("slc1").feature("def").set("scaleactive", true);
            model.result("pg_slice3d").feature("slc1").feature("def").set("scale", 1);
            model.result("pg_slice3d").run();

            model.result().export().create("exp_slc3d", "Image3D");
            model.result().export("exp_slc3d").set("sourceobject", "pg_slice3d");
            model.result().export("exp_slc3d").set("filename", baseDir + "\\test_slice_3d_stress.png");
            model.result().export("exp_slc3d").set("size", "manualweb");
            model.result().export("exp_slc3d").set("unit", "px");
            model.result().export("exp_slc3d").set("width", 1280);
            model.result().export("exp_slc3d").set("height", 720);
            model.result().export("exp_slc3d").run();

            // 4. 3D Deformed Displacement Plot
            model.result().create("pg_disp3d", "PlotGroup3D");
            model.result("pg_disp3d").set("data", "dset1");
            model.result("pg_disp3d").set("solnum", "5");
            model.result("pg_disp3d").label("3D Total Displacement (solid.disp)");
            model.result("pg_disp3d").create("surf1", "Surface");
            model.result("pg_disp3d").feature("surf1").set("expr", "solid.disp");
            model.result("pg_disp3d").feature("surf1").set("unit", "mm");
            model.result("pg_disp3d").feature("surf1").create("def", "Deform");
            model.result("pg_disp3d").feature("surf1").feature("def").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_disp3d").feature("surf1").feature("def").set("scaleactive", true);
            model.result("pg_disp3d").feature("surf1").feature("def").set("scale", 1);
            model.result("pg_disp3d").run();

            model.result().export().create("exp_disp3d", "Image3D");
            model.result().export("exp_disp3d").set("sourceobject", "pg_disp3d");
            model.result().export("exp_disp3d").set("filename", baseDir + "\\test_disp_3d_clean.png");
            model.result().export("exp_disp3d").set("size", "manualweb");
            model.result().export("exp_disp3d").set("unit", "px");
            model.result().export("exp_disp3d").set("width", 1280);
            model.result().export("exp_disp3d").set("height", 720);
            model.result().export("exp_disp3d").run();

            System.out.println("Exported all 3 test images successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

