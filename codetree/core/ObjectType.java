package codetree.core;

import java.util.*;

public interface ObjectType {
    abstract ObjectFragment generateCodeFragment(byte vLabel, byte[] eLabel, boolean isConnected, boolean isMaxLabel,
            boolean isAllSameVlabel, byte isAllSameElabel);

    abstract boolean isCanonical(Graph g, List<ObjectFragment> c);

    abstract ArrayList<ObjectFragment> computeCanonicalCode(Graph g);

    abstract boolean computeCanonicalCode(Graph g, List<ObjectFragment> c);

    abstract ArrayList<ObjectFragment> computeCanonicalCode(int labels_length);

    abstract Graph generateGraphAddElabel(List<ObjectFragment> code, int id);

    abstract Graph generateGraph(List<ObjectFragment> code, int id);
}
