package codetree.core;

import java.util.*;

public interface ObjectType {
    abstract ObjectFragment generateCodeFragment(byte vLabel, byte[] eLabel,boolean isConnected,int edges,boolean allElabelSame);

    abstract boolean isCanonical(Graph g ,List<ObjectFragment> c);

    abstract boolean computeCanonicalCode(Graph g,List<ObjectFragment> c);

    abstract List<ObjectFragment> computeCanonicalCode(int labels_length);

    abstract Graph generateGraphAddElabel(List<ObjectFragment> code, int id);

    abstract Graph generateGraph(List<ObjectFragment> code, int id);

}
