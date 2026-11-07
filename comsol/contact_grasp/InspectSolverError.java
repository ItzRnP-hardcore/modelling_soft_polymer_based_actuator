import com.comsol.model.*;
import com.comsol.model.util.*;

public class InspectSolverError {
    public static void main(String[] args) {
        try {
            String mph = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\contact_grasp\\BuildFullEnclosureOptimization_Model_1794036536498.mph";
            System.out.println("Loading " + mph + "...");
            Model model = ModelUtil.load("ErrModel", mph);

            // Try running study and catch full exception
            try {
                model.study("std1").run();
            } catch (Exception e) {
                System.out.println("=== EXCEPTION CLASS ===");
                System.out.println(e.getClass().getName());
                System.out.println("=== EXCEPTION MESSAGE ===");
                System.out.println(e.getMessage());
                System.out.println("=== STACK TRACE ===");
                e.printStackTrace(System.out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

