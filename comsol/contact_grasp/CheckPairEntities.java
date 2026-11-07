import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class CheckPairEntities {
    public static void main(String[] args) {
        try {
            String mphFile = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph";
            System.out.println("Loading: " + mphFile);
            Model model = ModelUtil.load("Model", mphFile);

            int[] src = model.pair("p1").source().entities();
            int[] dst = model.pair("p1").destination().entities();
            System.out.println("Pair p1 source: " + Arrays.toString(src));
            System.out.println("Pair p1 destination: " + Arrays.toString(dst));

            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

