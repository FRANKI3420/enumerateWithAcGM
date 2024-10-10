package codetree.edgeBased;

import java.util.*;

import codetree.common.Pair;
import codetree.core.*;

public class DfsCode
    implements GraphCode
{
    @Override
    public List<CodeFragment> computeCanonicalCode(Graph g, int b)
    {
        final int n = g.size();
        ArrayList<CodeFragment> code = new ArrayList<>(n+1);

        ArrayList<DfsSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<DfsSearchInfo> infoList2 = new ArrayList<>(b);

        final byte min = g.getMinVertexLabel();
        code.add(new DfsCodeFragment(min, (byte)-1, -1));

        List<Integer> minVertexList = g.getVertexList(min);
        for (int v0: minVertexList) {
            infoList1.add(new DfsSearchInfo(g, v0));
        }

        for (int i = 0; i < n; ++i) {
            DfsCodeFragment minFrag = new DfsCodeFragment();

            for (DfsSearchInfo info: infoList1) {
                while (!info.rightmostPath.isEmpty()) {
                    int v = info.rightmostPath.peek();

                    int[] adj = g.adjList[v];
                    for (int u: adj) {
                        if (info.closed[v][u]) {
                            continue;
                        }
    
                        DfsCodeFragment frag = null;
                        if (info.closed[u][u]) {
                            if (info.map[u] < info.map[v]) { // backward edge
                                frag = new DfsCodeFragment((byte)-1, g.edges[u][v], info.map[u]);
                            }
                        } else { // forward edge
                            frag = new DfsCodeFragment(g.vertices[u], g.edges[v][u], info.map[v]);
                        }
    
                        if (frag != null) {
                            final int cmpres = minFrag.isMoreCanonicalThan(frag);
                            if (cmpres < 0) {
                                minFrag = frag;
    
                                infoList2.clear();
                                infoList2.add(new DfsSearchInfo(info, v, u));
                            } else if (cmpres == 0 && infoList2.size() < b) {
                                infoList2.add(new DfsSearchInfo(info, v, u));
                            }
                        }
                    }
    
                    if (infoList2.size() > 0) {
                        break;
                    }
    
                    info.rightmostPath.pop();
                }
            }

            code.add(minFrag);

            infoList1 = infoList2;
            infoList2 = new ArrayList<>(b);
        }

        return code;
    }

    @Override
    public List<Pair<IndexNode, SearchInfo>> beginSearch(Graph graph, IndexNode root)
    {
        ArrayList<Pair<IndexNode, SearchInfo>> infoList = new ArrayList<>();

        for (IndexNode m: root.children) {
            DfsCodeFragment frag = (DfsCodeFragment)m.frag;
            for (int v = 0; v < graph.order(); ++v) {
                if (graph.vertices[v] == frag.vLabel) {
                    infoList.add(new Pair<IndexNode, SearchInfo>(m, new DfsSearchInfo(graph, v)));
                }
            }
        }

        return infoList;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph graph, SearchInfo info0)
    {
        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        DfsSearchInfo info = (DfsSearchInfo)info0;

        boolean backtrack = false;
        while (!info.rightmostPath.isEmpty()) {
            int v = info.rightmostPath.peek();

            int[] adj = graph.adjList[v];
            for (int u: adj) {
                if (info.closed[v][u]) {
                    continue;
                }

                if (info.closed[u][u]) { // backward edge
                    if (!backtrack) {
                        frags.add(new Pair<CodeFragment, SearchInfo>(
                            new DfsCodeFragment((byte)-1, graph.edges[u][v], info.map[u]),
                            new DfsSearchInfo(info, v, u)));
                    }
                } else { // forward edge
                    frags.add(new Pair<CodeFragment, SearchInfo>(
                        new DfsCodeFragment(graph.vertices[u], graph.edges[v][u], info.map[v]),
                        new DfsSearchInfo(info, v, u)));
                }
            }

            info.rightmostPath.pop();
            backtrack = true;
        }

        return frags;
    }
}
