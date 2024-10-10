package codetree.core;

import java.util.*;

import codetree.common.Pair;

public class IndexNode {
    public IndexNode parent;
    public ArrayList<IndexNode> children;

    public CodeFragment frag;
    protected ArrayList<Integer> matchGraphIndices;
    public HashSet<Byte> adjLabels;
    protected int depth;
    protected int count;
    protected BitSet childEdgeFragAnd;
    protected BitSet childEdgeFragOr;

    // IndexNode(IndexNode parent, CodeFragment frag) {
    // this.parent = parent;
    // this.frag = frag;

    // children = new ArrayList<>();
    // matchGraphIndices = new ArrayList<>();

    // count = 0;
    // }

    IndexNode(IndexNode parent, CodeFragment frag, int dep) {
        this.parent = parent;
        this.frag = frag;
        children = new ArrayList<>();
        depth = dep;
        adjLabels = new HashSet<>();
        count = 0;
        matchGraphIndices = new ArrayList<>();

        childEdgeFragAnd = new BitSet(dep);
        for (int i = 0; i < dep; i++) {
            childEdgeFragAnd.flip(i);
        }
        childEdgeFragOr = new BitSet();
    }

    List<Integer> sizeOnDepth() {
        ArrayList<Integer> sizeList = new ArrayList<>();
        sizeList.add(children.size());

        ArrayList<IndexNode> nodeList1;
        ArrayList<IndexNode> nodeList2 = new ArrayList<>();
        nodeList2.addAll(children);

        while (!nodeList2.isEmpty()) {
            nodeList1 = nodeList2;
            nodeList2 = new ArrayList<>();
            int size = 0;
            for (IndexNode m : nodeList1) {
                size += m.children.size();
                nodeList2.addAll(m.children);
            }
            sizeList.add(size);
        }

        return sizeList;
    }

    int size() {
        int s = 1;
        for (IndexNode m : children) {
            s += m.size();
        }
        return s;
    }

    // void addPath(List<CodeFragment> code, int graphIndex) {
    // ++count;

    // final int height = code.size();
    // if (height <= 0) {
    // matchGraphIndices.add(graphIndex);
    // return;
    // }

    // CodeFragment car = code.get(0);
    // List<CodeFragment> cdr = code.subList(1, height);

    // for (IndexNode m : children) {
    // if (m.frag.equals(car)) {
    // m.addPath(cdr, graphIndex);
    // return;
    // }
    // }

    // IndexNode m = new IndexNode(this, car);
    // children.add(m);

    // m.addPath(cdr, graphIndex);
    // }

    void addPath(List<CodeFragment> code, int graphIndex, int dep) {
        final int height = code.size();
        dep++;

        ++count;
        if (height <= 0 && graphIndex != -1) {
            matchGraphIndices.add(graphIndex);
            return;
        }

        CodeFragment car = code.get(0);
        List<CodeFragment> cdr = code.subList(1, height);

        for (IndexNode m : children) {
            if (m.frag.equals(car)) {
                m.addPath(cdr, graphIndex, dep);
                return;
            }
        }

        IndexNode m = new IndexNode(this, car, dep);
        children.add(m);
        m.addPath(cdr, graphIndex, dep);
    }

    void addInfo() {
        if (depth > 0) {
            for (IndexNode m : children) {
                adjLabels.add(m.frag.getVlabel());
                BitSet eFragBitSet = new BitSet();
                for (int v = 0; v < depth; v++) {
                    if (m.frag.getelabel()[v] > 0) {
                        childEdgeFragOr.set(v);
                        eFragBitSet.set(v);
                    }
                }
                childEdgeFragAnd.and(eFragBitSet);
            }
        }
        for (IndexNode m : children) {
            m.addInfo();
        }
    }

    List<Integer> search(Graph q, GraphCode impl) {
        HashSet<IndexNode> result0 = new HashSet<>();

        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(q, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            info.left.search(result0, q, info.right, impl);
        }

        ArrayList<Integer> result = new ArrayList<>();
        for (IndexNode p : result0) {
            result.addAll(p.matchGraphIndices);

            final int c = p.matchGraphIndices.size();
            for (; p != null; p = p.parent) {
                p.count += c;
            }
        }

        Collections.sort(result);
        return result;
    }

    static int traverse_num = 0;

    private void search(Set<IndexNode> result, Graph q, SearchInfo info, GraphCode impl) {
        final int c = matchGraphIndices.size();
        traverse_num++;
        if (c > 0 && !result.contains(this)) {
            result.add(this);

            for (IndexNode p = this; p != null; p = p.parent) {
                p.count -= c;
            }
        }

        // List<Pair<CodeFragment, SearchInfo>> nextFrags =
        // impl.enumerateFollowableFragments(q, info);
        List<Pair<CodeFragment, SearchInfo>> nextFrags;
        if (childEdgeFragAnd.cardinality() > 0) {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragAnd);
        } else {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragOr);
        }
        for (IndexNode m : children) {
            if (m.count > 0) {
                for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                    if (frag.left.contains(m.frag)) {
                        m.search(result, q, frag.right, impl);
                    }
                }
            }
        }
    }
}
