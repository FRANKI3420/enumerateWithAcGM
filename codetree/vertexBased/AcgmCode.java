package codetree.vertexBased;

import java.util.*;

import codetree.common.Pair;
import codetree.core.*;

public class AcgmCode
        implements GraphCode, ObjectType {

    @Override
    public ObjectFragment generateCodeFragment(byte vLabel, byte[] eLabel, boolean isConnected, boolean isMaxLabel,
            boolean isAllSameVlabel, byte isAllSameElabel) {
        return (new AcgmCodeFragment(vLabel, eLabel, isConnected, isMaxLabel, isAllSameVlabel, isAllSameElabel));
    }

    @Override
    public ArrayList<ObjectFragment> computeCanonicalCode(int labels_length) {
        ArrayList<ObjectFragment> codeList = new ArrayList<>(labels_length);
        for (int i = 0; i < labels_length; i++) {
            codeList.add(new AcgmCodeFragment((byte) (labels_length - 1 - i), 0));
        }
        // for (int i = labels_length - 1; i >= 0; i--) {
        // codeList.add(new AcgmCodeFragment((byte) i, 0));
        // }

        return codeList;
    }

    // for debug
    @Override
    public ArrayList<ObjectFragment> computeCanonicalCode(Graph g) {
        final int n = g.order();
        ArrayList<ObjectFragment> code = new ArrayList<>(n);

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>();

        final byte max = g.getMaxVertexLabel();
        code.add(new AcgmCodeFragment(max, 0));

        List<Integer> maxVertexList = g.getVertexList(max);
        for (int v0 : maxVertexList) {
            infoList1.add(new AcgmSearchInfo(g, v0));
        }

        for (int depth = 1; depth < n; ++depth) {
            AcgmCodeFragment maxFrag = new AcgmCodeFragment((byte) -1, depth);

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

                    AcgmCodeFragment frag = new AcgmCodeFragment(g.vertices[v], eLabels);
                    final int cmpres = maxFrag.isMoreCanonicalThan(frag);
                    if (cmpres < 0) {
                        maxFrag = frag;
                        infoList2.clear();
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    } else if (cmpres == 0) {
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    }
                }
            }

            code.add(maxFrag);

            infoList1 = infoList2;
            infoList2 = new ArrayList<>();
        }

        return code;
    }

    // BFS
    @Override
    public List<CodeFragment> computeCanonicalCode(Graph g, int c) {
        final int n = g.order();
        ArrayList<CodeFragment> code = new ArrayList<>(n);

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>(c);

        final byte max = g.getMaxVertexLabel();
        code.add(new AcgmCodeFragment(max, 0));

        List<Integer> maxVertexList = g.getVertexList(max);
        for (int v0 : maxVertexList) {
            infoList1.add(new AcgmSearchInfo(g, v0));
        }

        for (int depth = 1; depth < n; ++depth) {
            AcgmCodeFragment maxFrag = new AcgmCodeFragment((byte) -1, depth);

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

                    AcgmCodeFragment frag = new AcgmCodeFragment(g.vertices[v], eLabels);
                    final int cmpres = maxFrag.isMoreCanonicalThan(frag);
                    if (cmpres < 0) {
                        maxFrag = frag;
                        infoList2.clear();
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    } else if (cmpres == 0 && infoList2.size() < c) {
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    }
                }
            }

            code.add(maxFrag);

            infoList1 = infoList2;
            infoList2 = new ArrayList<>(c);
        }

        return code;
    }

    public static long isCanonicalTime = 0;
    public static int canonical_times = 0;

    @Override
    public boolean isCanonical(Graph g, List<ObjectFragment> c) {
        long start = System.nanoTime();
        // boolean isCanonical = computeCanonicalCode(g, c);
        // System.out.println("BFS" + isCanonical);
        boolean isCanonical = computeCanonicalCode_DFS(g, c);
        // System.out.println("\nDFS" + isCanonical2);
        // if (isCanonical != isCanonical2) {
        // System.out.println("different");
        // }
        canonical_times++;
        isCanonicalTime += System.nanoTime() - start;
        return isCanonical;
    }

    // BFS
    @Override
    public boolean computeCanonicalCode(Graph g, List<ObjectFragment> target) {
        final int n = g.order();

        ArrayList<AcgmSearchInfo> infoList1 = new ArrayList<>();
        ArrayList<AcgmSearchInfo> infoList2 = new ArrayList<>();

        final byte max = g.getMaxVertexLabel();
        if (max != target.get(0).getVlabel()) {
            return false;
        }

        final List<Integer> maxVertexList = g.getVertexList(max);
        for (int v0 : maxVertexList) {
            infoList1.add(new AcgmSearchInfo(g, v0));
        }

        for (int depth = 1; depth < n; ++depth) {
            AcgmCodeFragment maxFrag = new AcgmCodeFragment((byte) -1, depth);

            byte[] eLabels = new byte[depth];
            for (AcgmSearchInfo info : infoList1) {
                for (int v = info.open.nextSetBit(0); v >= 0 && v < n; v = info.open.nextSetBit(v + 1)) {

                    for (int i = 0; i < depth; ++i) {
                        final int u = info.vertexIDs[i];
                        eLabels[i] = g.edges[u][v];
                    }

                    AcgmCodeFragment frag = new AcgmCodeFragment(g.vertices[v], eLabels);
                    final int cmpres = maxFrag.isMoreCanonicalThan(frag);
                    if (cmpres < 0) {
                        maxFrag = frag;
                        infoList2.clear();
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    } else if (cmpres == 0) {
                        infoList2.add(new AcgmSearchInfo(info, g, v));
                    }
                }
            }

            if (!maxFrag.equals(target.get(depth))) {
                return false;
            }

            infoList1 = infoList2;
            infoList2 = new ArrayList<>();
        }

        return true;
    }

    public boolean computeCanonicalCode_DFS(Graph g, List<ObjectFragment> target) {

        // print(target);

        final int n = g.order();

        final byte max = g.getMaxVertexLabel();
        if (max != target.get(0).getVlabel()) {
            return false;
        }

        boolean[] open = new boolean[n];
        Arrays.fill(open, true);
        boolean[] close = new boolean[n];
        ArrayList<Integer> vertexIDs = new ArrayList<>(n);

        int ans = perm(g, target, open, close, vertexIDs, 0, n);

        if (ans < 0) {
            return false;
        }
        return true;
    }

    private int perm(Graph g, List<ObjectFragment> target, boolean[] open, boolean[] close,
            ArrayList<Integer> vertexIDs, int depth, int n) {

        byte[] eLabels = new byte[depth];
        for (int v = 0; v < n; v++) {
            if (!open[v])
                continue;

            boolean conected = false;

            for (int i = 0; i < depth; ++i) {
                final int u = vertexIDs.get(i);
                byte e = g.edges[u][v];
                eLabels[i] = e;
                if (e != 0) {
                    conected = true;
                }
            }
            if (conected || depth == 0) {

                ObjectFragment frag = new AcgmCodeFragment(g.vertices[v], eLabels);

                int ans = target.get(depth).isMoreCanonicalThan(frag);

                if (ans < 0) {
                    return ans;
                } else if ((ans == 0 && depth + 1 != n)) {
                    vertexIDs.add(v);
                    open[v] = false;
                    close[v] = true;
                    int[] adj = g.adjList[v];
                    for (int u : adj) {
                        if (!close[u]) {
                            open[u] = true;
                        }
                    }
                    ans = perm(g, target, open, close, vertexIDs, depth + 1, n);
                    open[v] = true;
                    close[v] = false;
                    vertexIDs.remove(vertexIDs.size() - 1);
                    for (int u : adj) {
                        if (close[u]) {
                            open[u] = false;
                        }
                    }
                    if (ans < 0) {
                        return ans;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public List<Pair<IndexNode, SearchInfo>> beginSearch(Graph g, IndexNode root) {
        ArrayList<Pair<IndexNode, SearchInfo>> infoList = new ArrayList<>();

        for (IndexNode m : root.children) {
            for (int v = 0; v < g.order(); ++v) {
                AcgmCodeFragment frag = (AcgmCodeFragment) m.frag;
                if (g.vertices[v] == frag.vLabel) {
                    infoList.add(new Pair<IndexNode, SearchInfo>(m, new AcgmSearchInfo(g, v)));
                }
            }
        }

        return infoList;
    }

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph g, SearchInfo info0) {
        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        AcgmSearchInfo info = (AcgmSearchInfo) info0;

        final int n = g.order();
        final int depth = info.vertexIDs.length;

        byte[] eLabels = new byte[depth];
        for (int v = 0; v < n; ++v) {
            if (!info.open.get(v)) {
                continue;
            }

            for (int i = 0; i < depth; ++i) {
                final int u = info.vertexIDs[i];
                eLabels[i] = g.edges[u][v];
            }

            frags.add(new Pair<CodeFragment, SearchInfo>(
                    new AcgmCodeFragment(g.vertices[v], eLabels), new AcgmSearchInfo(info, g, v)));
        }

        return frags;
    }

    BitSet openBitSet = new BitSet();

    @Override
    public List<Pair<CodeFragment, SearchInfo>> enumerateFollowableFragments(Graph g, SearchInfo info0,
            HashSet<Byte> childrenVlabel, BitSet childEdgeFrag) {
        ArrayList<Pair<CodeFragment, SearchInfo>> frags = new ArrayList<>();

        AcgmSearchInfo info = (AcgmSearchInfo) info0;

        final int depth = info.vertexIDs.length;

        byte[] eLabels = new byte[depth];

        openBitSet.clear();
        for (int v = childEdgeFrag.nextSetBit(0); v != -1; v = childEdgeFrag.nextSetBit(++v)) {
            int u = info.vertexIDs[v];
            int[] adj = g.adjList[u];
            for (int u2 : adj) {
                openBitSet.set(u2);
            }
        }

        for (int i : info.vertexIDs) {
            openBitSet.set(i, false);
        }

        for (int v = openBitSet.nextSetBit(0); v != -1; v = openBitSet.nextSetBit(++v)) {
            if (!childrenVlabel.contains(g.vertices[v])) {
                continue;
            }
            for (int i = 0; i < depth; ++i) {
                final int u = info.vertexIDs[i];
                eLabels[i] = g.edges[u][v];
            }
            frags.add(new Pair<CodeFragment, SearchInfo>(
                    new AcgmCodeFragment(g.vertices[v], eLabels), new AcgmSearchInfo(info, v)));
        }

        return frags;
    }

    @Override
    public Graph generateGraphAddElabel(List<ObjectFragment> code, int id) {
        byte[] vertices = new byte[code.size()];
        byte[][] edges = new byte[code.size()][code.size()];
        int index = 0;
        for (ObjectFragment c : code) {
            vertices[index] = c.getVlabel();
            byte eLabels[] = c.getelabel();
            if (eLabels == null) {
                if (index < code.size() - 1) {
                    edges[index][index + 1] = 1;
                    edges[index + 1][index] = 1;
                }
            } else {
                for (int i = 0; i < eLabels.length; i++) {
                    if (eLabels[i] > 0) {
                        edges[index][i] = eLabels[i];
                        edges[i][index] = eLabels[i];
                    }
                }
            }
            index++;
        }
        return new Graph(id, vertices, edges);
    }

    @Override
    public Graph generateGraph(List<ObjectFragment> code, int id) {
        byte[] vertices = new byte[code.size()];
        byte[][] edges = new byte[code.size()][code.size()];
        int index = 0;
        for (ObjectFragment c : code) {
            vertices[index] = c.getVlabel();
            byte eLabels[] = c.getelabel();
            if (eLabels == null) {
                if (index < code.size() - 1) {
                    edges[index][index + 1] = 1;
                    edges[index + 1][index] = 1;
                }
            } else {
                for (int i = 0; i < eLabels.length; i++) {
                    if (eLabels[i] == 1) {
                        edges[index][i] = 1;
                        edges[i][index] = 1;
                    }
                }
            }
            index++;
        }
        return new Graph(id, vertices, edges);
    }

}
