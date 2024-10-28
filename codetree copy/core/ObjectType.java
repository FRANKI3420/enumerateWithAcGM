package codetree.core;

import java.util.*;

public interface ObjectType {
    abstract ObjectFragment generateCodeFragment(byte vLabel, byte[] eLabel,boolean isConnected,int edges);

    abstract boolean isCanonical(Graph g ,ObjectFragment[] c);

    abstract boolean computeCanonicalCode(Graph g,ObjectFragment[] c);

    abstract ObjectFragment[] computeCanonicalCode(int labels_length);

    abstract Graph generateGraphAddElabel(ObjectFragment[] code, int id);

    abstract Graph generateGraph(List<ObjectFragment> code, int id);

}
