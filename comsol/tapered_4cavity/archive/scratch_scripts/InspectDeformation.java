import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class InspectDeformation {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\actuator_tapered_4cavity_solved.mph");
            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            
            System.out.println("b,type,X,Y,Z,u,v,w,disp");
            for (int b = 1; b <= nBnd; b++) {
                model.result().numerical().create("int_b", "IntSurface");
                model.result().numerical("int_b").set("data", "dset1");
                model.result().numerical("int_b").selection().set(new int[]{b});
                model.result().numerical("int_b").set("expr", new String[]{"X*1000", "Y*1000", "Z*1000", "u*1000", "v*1000", "w*1000", "solid.disp*1000", "1"});
                
                // solnum 6 is max pressure
                model.result().numerical("int_b").set("solnum", "6");
                double[][] res = model.result().numerical("int_b").getReal();
                double area = res[7][0];
                double X = res[0][0] / area;
                double Y = res[1][0] / area;
                double Z = res[2][0] / area;
                double u = res[3][0] / area;
                double v = res[4][0] / area;
                double w = res[5][0] / area;
                double disp = res[6][0] / area;
                model.result().numerical().remove("int_b");

                String type = (b == 1) ? "base" : (b == 46 ? "tip" : ((b >= 2 && b <= 5) ? "outer_skin" : "cavity_wall"));
                System.out.printf(Locale.US, "%d,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    b, type, X, Y, Z, u, v, w, disp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

