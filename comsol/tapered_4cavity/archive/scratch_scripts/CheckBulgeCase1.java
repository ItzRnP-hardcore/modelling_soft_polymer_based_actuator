import com.comsol.model.*;
import com.comsol.model.util.*;
import java.io.*;
import java.util.*;

public class CheckBulgeCase1 {
    public static void main(String[] args) {
        try {
            Model model = ModelUtil.create("Model");
            String baseDir = "c:\\Users\\rudra\\Documents\\BTP\\comsol\\tapered_4cavity";
            model.modelPath(baseDir);

            // Run BuildActuatorTapered4CavityFull up to case 1
            // Or load the model and run case 1
            Model fullModel = BuildActuatorTapered4CavityFull.run();
            // In fullModel, we can inspect sol1 for case 1 or check what is going on!
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

