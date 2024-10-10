package codetree.vertexBased;

import java.util.*;

import codetree.common.Pair;
import codetree.core.*;

public class XAcgmCode
        implements GraphCode {
    @Override
    public List<CodeFragment> computeCanonicalCode(Graph g, int b) {
        final int n = g.order();
        ArrayList<CodeFragment> code = new ArrayList<>(n);

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>(b);

        final byte max = g.getMaxVertexLabel();
        code.add(new XAcgmCodeFragment(max, 0));

        List<Integer> maxVertexList = g.getVertexList(max);
        for (int v0 : maxVertexList) {
            infoList1.add(new AcgmSearchInfo(g, v0));
        }

        for (int depth = 1; depth < n; ++depth) {
            XAcgmCodeFragment maxFrag = new XAcgmCodeFragment((byte) -1, depth);

            byte[] eLabels = new byte[depth];
            for (AcgmSearchInfo info : infoList1) {
                for (int v = 0; v < n; ++v) {
                    if (!info.open.get(v)) {
                        continue;
                    }

                    for (int i = 0; i < depth; ++i) {
                        final int u = info.vertexIDs[i];
                        eLabels[i] = g.edges[u][v];
                    }

                    int[] adj = g.adjList[v];

                    int e = 0;
                    for (int u : adj) {
                        // if (info.closed[u]) {
                        if (contain(info.vertexIDs, u)) {
                            ++e;
                        }
                    }

                    XAcgmCodeFragment frag = new XAcgmCodeFragment(g.vertices[v], e, adj.length, eLabels);
                    final int cmpres = maxFrag.isMoreCanonicalThan(frag);
                    if (cmpres < 0) {
                        maxFrag = frag;

                        infoList2.clear();
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    } else if (cmpres == 0 && infoList2.size() < b) {
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    }
                }
            }

            code.add(maxFrag);

            infoList1 = infoList2;
            infoList2 = new ArrayList<>(b);
        }

        return code;
    }

    @Override
    public List<Pair<IndexNode, SearchInfo>> beginSearch(Graph graph, IndexNode root) {
        ArrayList<Pair<IndexNode, SearchInfo>> infoList = new ArrayList<>();

        for (IndexNode m : root.children) {
            for (int v = 0; v < graph.order(); ++v) {
                XAcgmCodeFragment frag = (XAcgmCodeFragment) m.frag;
                if (graph.vertices[v] == frag.vLabel) {
                    infoList.add(new Pair<IndexNode, SearchInfo>(m, new AcgmSearchInfo(graph, v)));
                }
            }
        }

        return infoList;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph graph, SearchInfo info0) {
        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        AcgmSearchInfo info = (AcgmSearchInfo) info0;

        final int n = graph.order();
        final int depth = info.vertexIDs.length;

        byte[] eLabels = new byte[depth];
        for (int v = 0; v < n; ++v) {
            if (!info.open.get(v)) {
                continue;
            }

            for (int i = 0; i < depth; ++i) {
                int u = info.vertexIDs[i];
                eLabels[i] = graph.edges[u][v];
            }

            int[] adj = graph.adjList[v];

            int e = 0;
            for (int u : adj) {
                // if (info.closed[u]) {
                if (contain(info.vertexIDs, u)) {
                    ++e;
                }
            }

            frags.add(new Pair<CodeFragment, SearchInfo>(
                    new XAcgmCodeFragment(graph.vertices[v], e, adj.length, eLabels),
                    new AcgmSearchInfo(info, graph, v)));
        }

        return frags;

    }

    boolean contain(int[] vertexIDs, int num) {
        for (int i : vertexIDs) {
            if (i == num)
                return false;
        }
        return true;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph g, SearchInfo info,
            HashSet<Byte> adjLabels, BitSet childEdgeFrag) {
        return null;
    }
}
