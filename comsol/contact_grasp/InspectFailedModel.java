import com.comsol.model.*;
import com.comsol.model.util.*;

public class InspectFailedModel {
    public static void main(String[] args) {
        try {
            String mph = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\BuildFullEnclosureOptimization_Model_1794036318993.mph";
            Model model = ModelUtil.load("FailedModel", mph);
            System.out.println("Model loaded successfully!");

            // Check study and sol features
            String[] studies = model.study().tags();
            for (String s : studies) {
                System.out.println("Study: " + s);
            }
            String[] sols = model.sol().tags();
            for (String s : sols) {
                System.out.println("Sol: " + s);
            }

            // Check physics features
            String[] phys = model.component("comp1").physics("solid").feature().tags();
            for (String f : phys) {
                String type = model.component("comp1").physics("solid").feature(f).getType();
                int[] sel = model.component("comp1").physics("solid").feature(f).selection().entities();
                int selLen = (sel != null) ? sel.length : 0;
                System.out.printf("Physics feature: %s (Type: %s, Sel length: %d)%n", f, type, selLen);
            }

            // Check pairs
            String[] pairs = model.pair().tags();
            for (String p : pairs) {
                System.out.println("Pair: " + p);
                int[] src = model.pair(p).source().entities();
                int[] dst = model.pair(p).destination().entities();
                System.out.printf("  src: %d entities, dst: %d entities%n",
                                  (src != null ? src.length : 0), (dst != null ? dst.length : 0));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
