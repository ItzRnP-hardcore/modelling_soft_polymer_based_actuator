import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class TestGetPVals {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph");
            double[] pvals = model.sol("sol1").getPVals();
            System.out.println("Solved PVals: " + Arrays.toString(pvals));
            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

