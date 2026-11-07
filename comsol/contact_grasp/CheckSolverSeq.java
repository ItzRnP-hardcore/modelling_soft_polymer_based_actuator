import com.comsol.model.*;
import com.comsol.model.util.*;

public class CheckSolverSeq {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos1_Xc48_Yc-42_solved.mph");
            for (String tag : model.sol().tags()) {
                System.out.println("Sol tag: " + tag);
                for (String ftag : model.sol(tag).feature().tags()) {
                    System.out.println("  Feature: " + ftag + " (type: " + model.sol(tag).feature(ftag).getType() + ")");
                    try {
                        for (String sub : model.sol(tag).feature(ftag).feature().tags()) {
                            System.out.println("    Subfeature: " + sub + " (type: " + model.sol(tag).feature(ftag).feature(sub).getType() + ")");
                        }
                    } catch (Exception ignored) {}
                }
            }
            ModelUtil.remove("Model");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

