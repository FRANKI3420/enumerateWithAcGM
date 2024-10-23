package codetree.core;

import java.util.*;

public interface ObjectType {
    abstract ObjectFragment generateCodeFragment(byte vLabel, byte[] eLabel,boolean isConnected,int edges);

    abstract boolean isCanonical(Graph g ,ArrayList<ObjectFragment> c);

    abstract boolean computeCanonicalCode(Graph g,ArrayList<ObjectFragment> c);

    abstract List<ArrayList<ObjectFragment>> computeCanonicalCode(int labels_length);

    abstract Graph generateGraphAddElabel(List<ObjectFragment> code, int id);

    abstract Graph generateGraph(List<ObjectFragment> code, int id);

}
