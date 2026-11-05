import com.comsol.model.*;
import com.comsol.model.util.*;

public class ExportDeformComp {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.load("Model", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\actuator_bare_solved_Model.mph");
            // Set parameter to P = 40 kPa (solution 5) and export
            model.result("pg_stress").set("solnum", "5");
            model.result("pg_stress").run();
            model.result().export("img_pg_stress").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\comsol_bare_P40.png");
            model.result().export("img_pg_stress").run();
            System.out.println("Exported P40 image");

            // Set parameter to P = 90 kPa (solution 10) and export
            model.result("pg_stress").set("solnum", "10");
            model.result("pg_stress").run();
            model.result().export("img_pg_stress").set("filename", "c:\\Users\\rudra\\Documents\\BTP\\comsol\\comsol_bare_P90.png");
            model.result().export("img_pg_stress").run();
            System.out.println("Exported P90 image");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
