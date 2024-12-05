package codetree.core;

import java.util.BitSet;

public interface SearchInfo {
    public BitSet getOpen();

    public int[] getVertexIDs();
}
