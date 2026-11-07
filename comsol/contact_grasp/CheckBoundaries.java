import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class CheckBoundaries {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            Model model = ModelUtil.load("Model", mphFile);

            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            System.out.println("Total boundaries: " + nBnd);

            // Check contact pair definition
            int[] src = model.pair("p1").source().getInt("entity");
            int[] dst = model.pair("p1").destination().getInt("entity");
            System.out.println("Pair p1 source count: " + src.length + ", boundaries: " + Arrays.toString(src));
            System.out.println("Pair p1 dest count: " + dst.length + ", boundaries: " + Arrays.toString(dst));

            // Check contact feature cnt1
            int[] cnt1Pairs = model.component("comp1").physics("solid").feature("cnt1").selection().getEntities(2);
            System.out.println("Contact cnt1 selection: " + Arrays.toString(cnt1Pairs));

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

