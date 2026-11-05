import com.comsol.model.*;
import com.comsol.model.util.*;

public class TestStringGeom {
    public static Model run() {
        Model model = ModelUtil.create("Model");
        model.component().create("comp1", true);
        model.component("comp1").geom().create("geom1", 3);
        model.component("comp1").geom("geom1").lengthUnit("mm");

        // Outer Cone
        model.component("comp1").geom("geom1").create("cone_body", "Cone");
        model.component("comp1").geom("geom1").feature("cone_body").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cone_body").set("r", 6.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("specifytop", "radius");
        model.component("comp1").geom("geom1").feature("cone_body").set("rtop", 3.5);
        model.component("comp1").geom("geom1").feature("cone_body").set("h", 160.0);

        // Central Kevlar String
        model.component("comp1").geom("geom1").create("cyl_string", "Cylinder");
        model.component("comp1").geom("geom1").feature("cyl_string").set("axistype", "x");
        model.component("comp1").geom("geom1").feature("cyl_string").set("r", 0.6);
        model.component("comp1").geom("geom1").feature("cyl_string").set("h", 160.0);

        model.component("comp1").geom("geom1").run();

        int nDom = model.component("comp1").geom("geom1").getNDomains();
        int nBnd = model.component("comp1").geom("geom1").getNBoundaries();
        System.out.println("TestStringGeom: nDomains = " + nDom + ", nBoundaries = " + nBnd);
        return model;
    }
    public static void main(String[] args) { run(); }
}

