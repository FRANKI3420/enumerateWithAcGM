package codetree.core;

import java.util.BitSet;

public interface SearchInfo
{
    abstract BitSet getOpen();
    abstract int[] getVertexIDs();
    
}
