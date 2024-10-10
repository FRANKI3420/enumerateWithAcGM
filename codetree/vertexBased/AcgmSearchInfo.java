package codetree.vertexBased;

import java.util.BitSet;

import codetree.core.*;

final class AcgmSearchInfo
        implements SearchInfo {
    BitSet open;
    int[] vertexIDs;

    AcgmSearchInfo(Graph g, int v0) {
        final int n = g.order();

        open = new BitSet(n);

        int[] adj = g.adjList[v0];
        for (int u : adj) {
            open.set(u, true);
        }

        vertexIDs = new int[1];
        vertexIDs[0] = v0;
    }

    AcgmSearchInfo(AcgmSearchInfo src, Graph g, Integer v) {

        open = (BitSet) src.open.clone();

        final int n = src.vertexIDs.length;
        vertexIDs = new int[n + 1];
        System.arraycopy(src.vertexIDs, 0, vertexIDs, 0, n);
        vertexIDs[n] = v;

        open.set(v, false);
        int[] adj = g.adjList[v];
        for (int u : adj) {
            if (contain(vertexIDs, u)) {
                open.set(u, true);
            }
        }
    }

    AcgmSearchInfo(AcgmSearchInfo src, Integer v) {
        final int n = src.vertexIDs.length;
        vertexIDs = new int[n + 1];
        System.arraycopy(src.vertexIDs, 0, vertexIDs, 0, n);
        vertexIDs[n] = v;
    }

    boolean contain(int[] vertexIDs, int num) {
        for (int i : vertexIDs) {
            if (i == num)
                return false;
        }
        return true;
    }
}
