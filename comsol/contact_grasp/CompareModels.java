import com.comsol.model.*;
import com.comsol.model.util.*;

public class CompareModels {
    public static void main(String[] args) {
        try {
            String goodMph = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\loop_caging_optimization\\actuator_LoopCaging_R8mm_Pos3_Xc32_Yc-52_solved.mph";
            String badMph = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\BuildFullEnclosureOptimization_Model_1794037067361.mph";

            Model mGood = ModelUtil.load("mGood", goodMph);
            Model mBad = ModelUtil.load("mBad", badMph);

            System.out.println("=== COMPARING GOOD (Pos 3) VS BAD (Opt Case 0) ===");

            // Physics features
            String[] fGood = mGood.component("comp1").physics("solid").feature().tags();
            String[] fBad = mBad.component("comp1").physics("solid").feature().tags();

            System.out.println("Good features: " + String.join(", ", fGood));
            System.out.println("Bad features:  " + String.join(", ", fBad));

            // Compare selections of every feature
            for (String f : fBad) {
                if (mGood.component("comp1").physics("solid").feature().tags() != null) {
                    try {
                        int[] selGood = mGood.component("comp1").physics("solid").feature(f).selection().entities();
                        int[] selBad = mBad.component("comp1").physics("solid").feature(f).selection().entities();
                        System.out.printf("Feature %s: Good sel=%s vs Bad sel=%s%n",
                                          f, java.util.Arrays.toString(selGood), java.util.Arrays.toString(selBad));
                    } catch (Exception e) {
                        System.out.println("Feature " + f + " exception: " + e.getMessage());
                    }
                }
            }

            // Compare pairs
            int[] srcGood = mGood.pair("p1").source().entities();
            int[] dstGood = mGood.pair("p1").destination().entities();
            int[] srcBad = mBad.pair("p1").source().entities();
            int[] dstBad = mBad.pair("p1").destination().entities();

            System.out.printf("Pair p1: Good src=%s dst=%s%n",
                              java.util.Arrays.toString(srcGood), java.util.Arrays.toString(dstGood));
            System.out.printf("Pair p1: Bad  src=%s dst=%s%n",
                              java.util.Arrays.toString(srcBad), java.util.Arrays.toString(dstBad));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

