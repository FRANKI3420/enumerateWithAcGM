package codetree.common;

import java.util.*;

public class VertexLabel {
    static List<String> atoms = Arrays.asList(new String[] { "H", "C", "O", "N",
            "S", "Cl", "F", "P", "Br", "I", "Si",
            "Na", "B", "Cu", "Sn", "Co", "Fe", "Se", "Ni",
            "Pt", "Ru", "Mo", "Pd", "As", "Mn", "Rh", "Zn", "Ge", "K", "Cr", "Ir", "Ga",
            "Hg", "W", "Re", "Pb", "Bi",
            "Te", "Au",
            "Sb", "Li", "Tl", "Mg", "Ti", "Ag", "Al", "Zr", "U", "Ca", "V", "Gd", "Ac",
            "Nb", "Er", "Yb", "Sm", "Nd",
            "Pr", "Os",
            "Cd", "Cs", "Tb", "Ho" });

    // static List<String> atoms = Arrays.asList(new String[] { "Ho", "Tb", "Cs",
    // "Cd", "Os", "Pr", "Nd", "Sm", "Yb", "Er",
    // "Nb", "Ac", "Gd", "V", "Ca", "U", "Zr", "Al",
    // "Ag", "Ti", "Mg", "Tl", "Li", "Sb", "Au", "Te", "Bi", "Pb", "Re", "W", "Hg",
    // "Ga", "Ir", "Cr", "K", "Ge",
    // "Zn", "Rh",
    // "Mn", "As", "Pd", "Mo", "Ru", "Pt", "Ni", "Se", "Fe", "Co", "Sn", "Cu", "B",
    // "Na", "Si", "I", "Br", "P",
    // "F",
    // "Cl", "S", "N", "O", "C", "H" });

    public static int size(){
        return atoms.size();
    }

    public static String id2string(int id) {
        if (id >= atoms.size()) {
            throw new IllegalArgumentException("Undefined label id.");
        }

        return atoms.get(id);
    }

    public static byte string2id(String s) {
        return (byte) atoms.indexOf(s);
    }
}
