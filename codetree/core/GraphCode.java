package codetree.core;

import java.util.*;

import codetree.common.Pair;

public interface GraphCode {
    abstract List<CodeFragment> computeCanonicalCode(Graph g, int b);

    abstract List<Pair<IndexNode, SearchInfo>> beginSearch(Graph g, IndexNode root);

    abstract List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph g, SearchInfo info);

    abstract List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph g, SearchInfo info,
            HashSet<Byte> adjLabels, BitSet childEdgeFrag);
}
