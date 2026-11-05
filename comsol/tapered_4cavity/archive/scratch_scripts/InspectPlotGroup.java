import com.comsol.model.*;
import com.comsol.model.util.*;

public class InspectPlotGroup {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity\\DiagnoseBulge_Model.mph");
            
            System.out.println("Properties of pg_disp:");
            String[] props = model.result("pg_disp").properties();
            for (String p : props) {
                try {
                    System.out.println("  " + p + " = " + model.result("pg_disp").getString(p));
                } catch (Exception e) {}
            }

            System.out.println("Features in pg_disp:");
            for (String f : model.result("pg_disp").feature().tags()) {
                System.out.println("  Feature: " + f + " (type=" + model.result("pg_disp").feature(f).getType() + ")");
                for (String p : model.result("pg_disp").feature(f).properties()) {
                    try {
                        System.out.println("    " + p + " = " + model.result("pg_disp").feature(f).getString(p));
                    } catch (Exception e) {}
                }
                for (String sub : model.result("pg_disp").feature(f).feature().tags()) {
                    System.out.println("    Subfeature: " + sub + " (type=" + model.result("pg_disp").feature(f).feature(sub).getType() + ")");
                    for (String p : model.result("pg_disp").feature(f).feature(sub).properties()) {
                        try {
                            System.out.println("      " + p + " = " + model.result("pg_disp").feature(f).feature(sub).getString(p));
                        } catch (Exception e) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

