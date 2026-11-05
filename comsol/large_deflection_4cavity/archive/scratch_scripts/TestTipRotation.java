import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class TestTipRotation {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            model.component().create("comp1", true);
            model.component("comp1").geom().create("geom1", 3);
            model.component("comp1").geom("geom1").lengthUnit("mm");

            double L_act = 160.0;
            double R0 = 8.0;
            double Rt = 4.0;

            model.param().set("L_act", L_act + "[mm]");
            model.param().set("R0", R0 + "[mm]");
            model.param().set("Rt", Rt + "[mm]");
            model.param().set("E_mod", "0.20[MPa]");
            model.param().set("nu_val", "0.45");
            model.param().set("theta_bend", "0[deg]");

            model.component("comp1").geom("geom1").create("cone_body", "Cone");
            model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
            model.component("comp1").geom("geom1").feature("cone_body").set("r", "R0");
            model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
            model.component("comp1").geom("geom1").feature("cone_body").set("rtop", "Rt");
            model.component("comp1").geom("geom1").feature("cone_body").set("h", "L_act");
            model.component("comp1").geom("geom1").run();

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            System.out.println("nBnd = " + nBnd);
            // Print boundary areas / centroids
            for (int b = 1; b <= nBnd; b++) {
                double[] box = model.component("comp1").geom("geom1").getDomainBox(2, b);
                System.out.printf("Bnd %d: X in [%.1f, %.1f], Y in [%.1f, %.1f], Z in [%.1f, %.1f]\n",
                    b, box[0], box[1], box[2], box[3], box[4], box[5]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

