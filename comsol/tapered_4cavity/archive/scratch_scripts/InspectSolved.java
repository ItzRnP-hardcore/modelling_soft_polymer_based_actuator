import com.comsol.model.*;
import com.comsol.model.util.*;
import java.util.*;

public class InspectSolved {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\actuator_tapered_4cavity_solved.mph");
            System.out.println("Model loaded successfully!");
            int nDom = model.component("comp1").geom("geom1").getNDomains();
            int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
            System.out.println("nDomains: " + nDom + ", nBoundaries: " + nBnd);

            for (int d = 1; d <= nDom; d++) {
                System.out.println("Domain " + d);
            }

            String[] features = model.component("comp1").physics("solid").feature().tags();
            for (String f : features) {
                System.out.println("Feature: " + f + ", type: " + model.component("comp1").physics("solid").feature(f).getType());
                try {
                    int[] sel = model.component("comp1").physics("solid").feature(f).selection().entities();
                    System.out.println("  Selection: " + Arrays.toString(sel));
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

