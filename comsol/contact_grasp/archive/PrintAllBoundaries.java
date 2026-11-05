import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class PrintAllBoundaries {
    public static void main(String[] args) throws Exception {
        String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp";
        double rCyl = 15.0;
        String[] pStepList = {"0"};
        Model model = TestActuatorCylinderContactSingle.buildContactModel(baseDir, rCyl, pStepList);

        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        System.out.println("Total boundaries: " + nBnd);

        for (int b = 1; b <= nBnd; b++) {
            model.result().numerical().create("int_b", "IntSurface");
            model.result().numerical("int_b").set("data", "dset_mesh");
            model.result().numerical("int_b").selection().set(new int[]{b});
            model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "1"});
            double[][] res = model.result().numerical("int_b").getReal();
            double area = res[3][0];
            double xm = res[0][0] / area;
            double ym = res[1][0] / area;
            double zm = res[2][0] / area;
            double rm = Math.sqrt(ym * ym + zm * zm);
            model.result().numerical().remove("int_b");

            System.out.printf(Locale.US, "Bnd %2d: xm=%7.2f, ym=%7.2f, zm=%7.2f, rm=%6.2f, area=%9.2e%n",
                b, xm, ym, zm, rm, area);
        }

        ModelUtil.remove("Model");
    }
}

