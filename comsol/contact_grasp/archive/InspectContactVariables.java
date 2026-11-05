import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class InspectContactVariables {
    public static void main(String[] args) throws Exception {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        String mphFile = baseDir + "\\TestActuatorCylinderContactSingle_Model.mph";

        System.out.println("Loading model: " + mphFile);
        Model model = ModelUtil.load("Model", mphFile);

        int[] arr_cyl = {39, 40, 41, 42, 43, 44};
        int[] arr_act_outer = {2, 3, 4, 5};

        System.out.println("Evaluating contact variables across solved steps...");

        // Evaluate on actuator outer skin (destination of contact pair)
        model.result().numerical().create("int_Tn_dst", "IntSurface");
        model.result().numerical("int_Tn_dst").set("data", "dset1");
        model.result().numerical("int_Tn_dst").selection().set(arr_act_outer);
        model.result().numerical("int_Tn_dst").set("expr", new String[]{"solid.cnt1.Tn"});
        model.result().numerical("int_Tn_dst").set("unit", new String[]{"N"});

        model.result().numerical().create("max_Tn_dst", "MaxSurface");
        model.result().numerical("max_Tn_dst").set("data", "dset1");
        model.result().numerical("max_Tn_dst").selection().set(arr_act_outer);
        model.result().numerical("max_Tn_dst").set("expr", new String[]{"solid.cnt1.Tn"});
        model.result().numerical("max_Tn_dst").set("unit", new String[]{"kPa"});

        model.result().numerical().create("int_gap_min", "MinSurface");
        model.result().numerical("int_gap_min").set("data", "dset1");
        model.result().numerical("int_gap_min").selection().set(arr_act_outer);
        model.result().numerical("int_gap_min").set("expr", new String[]{"solid.cnt1.gap"});
        model.result().numerical("int_gap_min").set("unit", new String[]{"mm"});

        double[][] resTn = model.result().numerical("int_Tn_dst").getReal();
        double[][] resMaxTn = model.result().numerical("max_Tn_dst").getReal();
        double[][] resGap = model.result().numerical("int_gap_min").getReal();

        String[] pSteps = {"0", "5", "10", "15", "20", "25", "30", "35", "40"};
        for (int s = 0; s < pSteps.length; s++) {
            System.out.printf(Locale.US, "Step P=%s kPa: Total Contact Force Fn=%.4f N, Max Contact Pressure=%.2f kPa, Min Gap=%.3f mm%n",
                pSteps[s], resTn[0][s], resMaxTn[0][s], resGap[0][s]);
        }

        // Export images of contact grasp deformation and contact pressure
        try {
            model.result().create("pg_contact", "PlotGroup3D");
            model.result("pg_contact").set("data", "dset1");
            model.result("pg_contact").label("Contact Pressure & Deformation");
            model.result("pg_contact").create("surf_c", "Surface");
            model.result("pg_contact").feature("surf_c").set("expr", "solid.cnt1.Tn");
            model.result("pg_contact").feature("surf_c").set("unit", "kPa");
            model.result("pg_contact").feature("surf_c").create("def", "Deform");
            model.result("pg_contact").feature("surf_c").feature("def").set("expr", new String[]{"u", "v", "w"});
            model.result("pg_contact").feature("surf_c").feature("def").set("scaleactive", true);
            model.result("pg_contact").feature("surf_c").feature("def").set("scale", 1.0);

            model.result().export().create("img_cnt", "Image3D");
            model.result().export("img_cnt").set("sourceobject", "pg_contact");
            model.result().export("img_cnt").set("size", "manualweb");
            model.result().export("img_cnt").set("unit", "px");
            model.result().export("img_cnt").set("width", 1400);
            model.result().export("img_cnt").set("height", 900);
            model.result().export("img_cnt").set("filename", baseDir + "\\images\\test_contact_R15mm_P15kPa.png");

            // Evaluate at step 4 (P = 15 kPa)
            model.result("pg_contact").set("solnum", "4");
            model.result("pg_contact").run();
            model.result().export("img_cnt").run();
            System.out.println("Exported test render at P = 15 kPa!");

            // Evaluate at step 8 (P = 35 kPa)
            model.result("pg_contact").set("solnum", "8");
            model.result("pg_contact").run();
            model.result().export("img_cnt").set("filename", baseDir + "\\images\\test_contact_R15mm_P35kPa.png");
            model.result().export("img_cnt").run();
            System.out.println("Exported test render at P = 35 kPa!");
        } catch (Exception ex) {
            System.out.println("Export render note: " + ex.getMessage());
            ex.printStackTrace();
        }

        ModelUtil.remove("Model");
    }
}

