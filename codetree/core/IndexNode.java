package codetree.core;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import codetree.common.Pair;

public class IndexNode implements Serializable {
    public IndexNode parent;
    public ArrayList<IndexNode> children;
    public CodeFragment frag;
    protected ArrayList<Integer> matchGraphIndices;
    protected int count;
    protected int depth;
    protected boolean supNode;
    protected BitSet matchGraphIndicesBitSet;
    public HashSet<Byte> adjLabels;
    protected int nodeID;
    protected boolean traverseNecessity;
    protected boolean backtrackNode;
    protected int q_traverse_num;
    protected LinkedHashMap<Integer, BitSet> labelFiltering;
    protected BitSet childEdgeFragAnd;
    protected BitSet childEdgeFragOr;

    static BitSet In = new BitSet();
    static BitSet Can = new BitSet();
    static BitSet result = new BitSet();
    static BitSet U = new BitSet();
    static IndexNode root;
    static ArrayList<Integer> can = new ArrayList<>();
    static final Runtime runtime = Runtime.getRuntime();
    static final Path out = Paths.get("datagraph.gfu");
    static final Path path = Paths.get("output2.txt");
    static int a_in_count = 0;
    static int nodeIDcount = 0;
    static long search_time = 0;
    static int totoal_kai = 0;
    static int doukeicount = 0;
    static long write_time = 0;
    static long process_time = 0;
    static long read_time = 0;
    static double verification_time = 0;
    static double total_time = 0;
    static double fileter_time = 0;
    static double verify_time = 0;
    static double query_per_veq = 0;
    static double query_per_veqF = 0;
    static double query_per_veqV = 0;
    static double query_per_sum = 0;
    static double query_per_time = 0;
    static double veq_per_Can = 0;
    static double CT_verify = 0;
    static int nonfail = 0;
    static double FPratio_q = 0;
    static double FPre = 0;
    static double filpertime = 0;
    static double SPper_q = 0;
    static String command;
    static Process p;
    static double FP = 0;
    static double SP = 0;
    static double FPratio = 0;
    static int fil_count = 0;
    static int in_count = 0;
    static int a_fil_count = 0;
    static long a_filterTime = 0;
    static int veq_answer_num = 0;
    static int backtrack_count = 0;
    static long nodeFiltering_time = 0;
    static long a_nodeFiltering_time = 0;
    static int query_per_nf_count = 0;
    static int removeTotalSize = 0;
    static int removeTotalGraphs = 0;
    static int deletedVsumPerq = 0;
    static double a_deletedVsumPerq = 0;
    static double deletedVsumPerq_set = 0;
    static int lab_fil_num = 0;
    static int verfyNum = 0;
    static int fail = 0;
    static double ms_per_fil = 0;
    static boolean traverse;
    static ArrayList<IndexNode> removeNode = new ArrayList<>();
    static int traverse_cou = 0;
    static Set<IndexNode> initTraverseNecessity = new HashSet<>();
    static int q_trav_num = 0;

    IndexNode(IndexNode parent, CodeFragment frag, int dep, boolean supergraphSearchNode) {
        this.parent = parent;
        this.frag = frag;
        children = new ArrayList<>();
        depth = dep;
        supNode = supergraphSearchNode;
        adjLabels = new HashSet<>();
        q_traverse_num = 0;
        labelFiltering = new LinkedHashMap<>();
        traverseNecessity = true;
        backtrackNode = false;

        if (supergraphSearchNode) {
            count = 0;
            matchGraphIndices = new ArrayList<>();
        } else {
            matchGraphIndicesBitSet = new BitSet();
            nodeID = nodeIDcount++;
        }

        childEdgeFragAnd = new BitSet(dep);
        for (int i = 0; i < dep; i++) {
            childEdgeFragAnd.flip(i);
        }
        childEdgeFragOr = new BitSet();

    }

    int size() {
        int s = 1;
        for (IndexNode m : children) {
            s += m.size();
        }
        return s;
    }

    void init_traverse() {
        if (q_traverse_num == 0 && depth > 0)
            return;

        q_traverse_num = 0;
        for (IndexNode m : children) {
            m.init_traverse();
        }
    }

    void init_removeNode() {
        removeNode = new ArrayList<>();
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

    void checkBacktrackNode() {
        for (IndexNode m : children) {
            BitSet descendantsBitSet = new BitSet();
            descendantsBitSet.or(m.matchGraphIndicesBitSet);
            m.checkBacktrackNode(descendantsBitSet);
            if (descendantsBitSet.cardinality() == m.matchGraphIndicesBitSet.cardinality()) {
                m.backtrackNode = true;
            }
            m.checkBacktrackNode();
        }
    }

    private void checkBacktrackNode(BitSet descendantsBitSet) {
        for (IndexNode m : children) {
            descendantsBitSet.and(m.matchGraphIndicesBitSet);
            m.checkBacktrackNode(descendantsBitSet);
        }
    }

    // 密なパターンを子の左側に
    void sortChildren() {
        @SuppressWarnings("unchecked")
        Pair<Integer, IndexNode>[] pairs = new Pair[children.size()];

        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new Pair<Integer, IndexNode>(children.get(i).matchGraphIndicesBitSet.cardinality(),
                    children.get(i));
        }
        Arrays.sort(pairs, Comparator.comparingInt(p -> p.left));
        children.clear();
        for (int i = 0; i < pairs.length; i++) {
            children.add(pairs[i].right);
        }
        for (IndexNode m : children) {
            m.sortChildren();
        }
    }

    // 頻出ラベルを子の左側に
    void sortVlabelChildren() {
        @SuppressWarnings("unchecked")
        Pair<Byte, IndexNode>[] pairs = new Pair[children.size()];

        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new Pair<Byte, IndexNode>(children.get(i).frag.getVlabel(),
                    children.get(i));
            // System.out.print(children.get(i).frag.getVlabel() + " ");
        }
        Arrays.sort(pairs, Comparator.comparingInt(p -> p.left));
        children.clear();

        for (int i = 0; i < pairs.length; i++) {
            children.add(pairs[i].right);
            // System.out.print(children.get(i).frag.getVlabel() + " ");
        }

        for (IndexNode m : children) {
            m.sortVlabelChildren();
        }
    }

    void addPath(List<CodeFragment> code, int graphIndex, boolean supergraphSearch, int dep) {
        final int height = code.size();
        dep++;

        if (supergraphSearch) {
            ++count;
            if (height <= 0 && graphIndex != -1) {
                matchGraphIndices.add(graphIndex);
                return;
            }

        } else {
            if (graphIndex != -1)
                matchGraphIndicesBitSet.set(graphIndex, true);

            if (height <= 0)
                return;
        }

        CodeFragment car = code.get(0);
        List<CodeFragment> cdr = code.subList(1, height);

        for (IndexNode m : children) {
            if (m.frag.equals(car)) {
                m.addPath(cdr, graphIndex, supergraphSearch, dep);
                return;
            }
        }

        IndexNode m = new IndexNode(this, car, dep, supNode);
        children.add(m);
        m.addPath(cdr, graphIndex, supergraphSearch, dep);
    }

    void depth_size_histgram(int[] histgram) {
        histgram[depth]++;
        for (IndexNode m : children) {
            m.depth_size_histgram(histgram);
        }
    }

    BitSet subsearch(Graph q, GraphCode impl, BufferedWriter bw, String mode, String dataset,
            BufferedWriter bw_data, BufferedWriter allbw, List<Graph> G,
            String directory, HashMap<Integer, ArrayList<String>> gMaps, int delta, BufferedWriter br_whole)
            throws IOException, InterruptedException {

        Path fileToDelete = Paths.get("output2.txt");
        try (BufferedWriter b = Files.newBufferedWriter(fileToDelete)){
            b.write("");
        }

        if (fail == 50) {
            if (q.id == 99) {
                init_param();
            }
            return new BitSet();
        }
        long start = System.nanoTime();

        init_Bitset();
        init_traverse();// reset travers num
        root = this;
        Can.or(matchGraphIndicesBitSet);// Can ← G
        List<IndexNode> traversedNode = new ArrayList<>(this.children);

        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(q, this);
        if (delta >= q.order) {
            traverse = true;
            for (Pair<IndexNode, SearchInfo> info : infoList) {
                info.left.doublesearch(q, info.right, impl, false, traversedNode);
                if (!traverse)
                    break;
            }
            result.or(In);

            if (!traverse) {
                Can.clear();
            } else {
                Can.andNot(In);
            }

        } else {
            for (Pair<IndexNode, SearchInfo> info : infoList) {
                info.left.subsearch(q, info.right, impl, traversedNode);
            }
            a_in_count = 0;
            initTraverseNecessity();
        }

        infoList = null;

        if (Can.cardinality() != 0) {
            verfyNum++;
            a_filterTime = System.nanoTime() - start;

            start = System.nanoTime();
            find_labelFiltering(G, traversedNode);
            a_nodeFiltering_time = System.nanoTime() - start;

            write_file_for_Ver(G, q, gMaps);

            verification_VEQ(directory, dataset, mode, q, G.size());
            if (timelimit) {// 時間制限を超えなかった場合
                a_fil_count = G.size() - Can.cardinality();
                fil_count += a_fil_count;
                search_time += a_filterTime;// filtering time
                nodeFiltering_time += a_nodeFiltering_time;
                doukeicount += Can.cardinality();
                a_in_count = In.cardinality();
                in_count += a_in_count;
            }
            // nonfail++;// filtering time を計測時にon
        } else {
            a_in_count = In.cardinality();
            in_count += a_in_count;
            fil_count += a_fil_count;
            doukeicount += Can.cardinality();
            a_filterTime = System.nanoTime() - start;
            search_time += a_filterTime;// filtering time
            nonfail++;
            SPper_q = 1;
            FPre = 1;
            FPratio_q = 1;
            totoal_kai += result.cardinality();
            FP += FPre;
            FPratio += FPratio_q;
            SP += SPper_q;
            query_per_time = (double) a_filterTime / 1000 / 1000;
        }

        write_file_indiv(q, bw_data, G.size());

        if (q.id == 99) {
            System.out.println("(G-C/(G-A)):" + String.format("%.3f", FPratio / nonfail));
            System.out.println("A:" + totoal_kai);
            System.out.println("filtering time per q" + String.format("%.3f",
                    (double) (search_time + nodeFiltering_time) / 1000 / 1000 / nonfail) + ": ms");
            System.out
                    .println("verification time per q" + String.format("%.3f", (verification_time / nonfail)) + ": ms");
            System.out.println("query time:" + String.format("%.3f", ((double) search_time / 1000 / 1000 +
                    verification_time + nodeFiltering_time / 1000 / 1000)
                    / 100));
            write_file(allbw, bw, G.size(), br_whole);
            init_param();
        }
        return result;
    }

    void find_labelFiltering(List<Graph> G, List<IndexNode> trIndexNodes) {
        BitSet target = new BitSet();
        for (IndexNode m : trIndexNodes) {
            target.or(Can);
            target.and(m.matchGraphIndicesBitSet);
            for (int trueIndex = target.nextSetBit(0); trueIndex != -1; trueIndex = target
                    .nextSetBit(++trueIndex)) {
                BitSet labelFrag = m.labelFiltering.get(trueIndex);
                G.get(trueIndex).filterFlag.or(m.labelFiltering.get(trueIndex));
                deletedVsumPerq += labelFrag.cardinality();
                query_per_nf_count += labelFrag.cardinality();
            }
            target.clear();
        }
    }

    private void doublesearch(Graph q, SearchInfo info, GraphCode impl, boolean superFrag,
            List<IndexNode> traversedNode) {
        boolean nowFrag = superFrag;
        if (depth == 1) {
            if (traversedNode.contains(this))
                traversedNode.remove(this);
        }
        q_trav_num++;
        q_traverse_num++;
        if (!superFrag) {
            Can.and(matchGraphIndicesBitSet);
        }

        if (depth == q.order) {
            In.or(matchGraphIndicesBitSet);
            if (!superFrag)// 誘導部分グラフとなるパターンを辿っているため
                traverse = false;
            return;
        }

        if (children.size() == 0 || backtrackJudge(q.order)) {
            return;
        }

        if (!In.isEmpty() && superFrag) {
            U.xor(In);
            U.and(Can);
            U.and(matchGraphIndicesBitSet);

            if (U.isEmpty()) { // if U and ID(n) = null then backtrack
                U.or(root.matchGraphIndicesBitSet);
                return;
            }
            U.or(root.matchGraphIndicesBitSet);
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags;
        if (childEdgeFragAnd.cardinality() > 0) {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragAnd);
        } else {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragOr);
        }

        for (IndexNode m : children) {
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (m.frag.equals(frag.left)) {// super pattern
                    m.doublesearch(q, frag.right, impl, superFrag, traversedNode);
                    if (!traverse)
                        return;
                } else if (m.frag.bigger(frag.left)) {// super pattern p
                    superFrag = true;
                    m.doublesearch(q, frag.right, impl, superFrag, traversedNode);
                    if (!traverse)
                        return;
                }
                superFrag = nowFrag;
            }
        }
    }

    private void subsearch(Graph q, SearchInfo info, GraphCode impl,
            List<IndexNode> traversedNode) {
        q_trav_num++;
        q_traverse_num++;
        if (depth == 1) {
            if (traversedNode.contains(this))
                traversedNode.remove(this);
        }

        Can.and(matchGraphIndicesBitSet);

        if (backtrackNode ||
                backtrackJudge(q.order)) {
            traverseNecessity = false;
            initTraverseNecessity.add(this);
            return;
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags;
        if (childEdgeFragAnd.cardinality() > 0) {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragAnd);
        } else {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragOr);
        }

        for (IndexNode m : children) {
            if (!m.traverseNecessity)
                continue;
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (!m.traverseNecessity)
                    break;
                if (frag.left.equals(m.frag)) {
                    m.subsearch(q, frag.right, impl, traversedNode);
                }
            }
        }
    }

    private void node2pattern() throws IOException {
        List<CodeFragment> code = getCode();
        Graph g = generateGraph(code, nodeID);
        Path output = Paths.get("patten.gfu");
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
            g.writeGraph2Gfu(bw);
        }
    }

    private boolean backtrackJudge(int order) {
        for (IndexNode m : children) {
            if (m.q_traverse_num == 0) {
                return false;
            }
            if (m.depth <= order) {
                if (!m.backtrackJudge(order))
                    return false;
            }
        }
        return true;
    }

    void initTraverseNecessity() {
        for (IndexNode m : initTraverseNecessity) {
            m.traverseNecessity = true;
        }
        initTraverseNecessity.clear();
    }

    void addIDtoTree(Graph g, GraphCode impl) {
        matchGraphIndicesBitSet.set(g.id, true);
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(g, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            info.left.addIDtoTree(g, info.right, impl);
        }
    }

    private void addIDtoTree(Graph g, SearchInfo info, GraphCode impl) {
        // traverse_cou++;
        if (depth == 1) {
            if (labelFiltering.get(g.id) == null) {
                labelFiltering.put(g.id, new BitSet(g.order));
            }
            labelFiltering.get(g.id).set(info.getVertexIDs()[0]);
        }

        matchGraphIndicesBitSet.set(g.id, true);

        if (children.size() == 0 || backtrackJudge(g.order, g.id)) {
            traverseNecessity = false;
            initTraverseNecessity.add(this);
            return;
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags;
        if (childEdgeFragAnd.cardinality() > 0) {
            nextFrags = impl.enumerateFollowableFragments(g, info, adjLabels, childEdgeFragAnd);
        } else {
            nextFrags = impl.enumerateFollowableFragments(g, info, adjLabels, childEdgeFragOr);
        }

        for (IndexNode m : children) {
            if (!m.traverseNecessity)
                continue;
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (!m.traverseNecessity)
                    break;
                if (frag.left.contains(m.frag)) {
                    m.addIDtoTree(g, frag.right, impl);
                }
            }
        }
    }

    private boolean backtrackJudge(int order, int id) {
        for (IndexNode m : children) {
            if (!m.matchGraphIndicesBitSet.get(id)) {
                return false;
            }
            if (m.depth <= order) {
                if (!m.backtrackJudge(order, id))
                    return false;
            }
        }
        return true;
    }

    void getLeafGraph(List<Graph> leafGraphs) {
        for (IndexNode m : children) {
            if (m.children.size() == 0) {
                List<CodeFragment> code = m.getCode();
                Graph g = generateGraph(code, m.nodeID);
                leafGraphs.add(g);
            } else {
                m.getLeafGraph(leafGraphs);
            }
        }
    }

    List<CodeFragment> getCode() {
        List<CodeFragment> code = new ArrayList<>();
        for (IndexNode n = this; n != null; n = n.parent) {
            code.add(n.frag);
        }
        Collections.reverse(code);
        code.remove(0);
        return code;
    }

    Graph generateGraph(List<CodeFragment> code, int id) {
        byte[] vertices = new byte[code.size()];
        byte[][] edges = new byte[code.size()][code.size()];
        int index = 0;
        for (CodeFragment c : code) {
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

    void removeTree() {
        if (removeNode.size() == 0)
            return;
        ArrayList<IndexNode> target = new ArrayList<>();
        for (IndexNode m : children) {
            target.add(m);
        }
        for (IndexNode m : target) {
            if (removeNode.contains(m)) {
                m.parent.children.remove(m.parent.children.indexOf(m));
                for (IndexNode p = m.parent; p.children.size() < 1; p = p.parent) {
                    if (p.depth == 1)
                        continue;
                    p.parent.children.remove(p.parent.children.indexOf(p));
                }
                removeNode.remove(removeNode.indexOf(m));
            }
            m.removeTree();
        }
    }

    void pruningEquivalentNodes(Graph g, GraphCode impl, int leafID, ArrayList<Integer> idList,
            ArrayList<Integer> removeIDList) {
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(g, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            info.left.pruningEquivalentNodes(g, info.right, impl, leafID, idList, removeIDList);
        }
    }

    private void pruningEquivalentNodes(Graph g, SearchInfo info, GraphCode impl, int leafID, ArrayList<Integer> idList,
            ArrayList<Integer> removeIDList) {
        //// this node is leaf and & not needed by index because of same path
        if (children.size() == 0 && depth != 1 && leafID != this.nodeID && !removeNode.contains(this)
                && !idList.contains(this.nodeID)) {
            removeIDList.add(this.nodeID);
            removeNode.add(this);
            return;
        }

        List<Pair<CodeFragment, SearchInfo>> nextFrags = impl.enumerateFollowableFragments(g, info);

        for (IndexNode m : children) {
            for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                if (frag.left.equals(m.frag)) {
                    m.pruningEquivalentNodes(g, frag.right, impl, nodeID, idList, removeIDList);
                }
            }
        }
    }

    private void init_Bitset() {
        In.clear();
        Can.clear();
        result.clear();
        can.clear();
    }

    private void write_file_for_Ver(List<Graph> D, Graph q, HashMap<Integer, ArrayList<String>> gMaps) {
        long time = System.nanoTime();
        try (BufferedWriter bw2 = Files.newBufferedWriter(out)) {
            for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can
                    .nextSetBit(++trueIndex)) {
                Graph g = D.get(trueIndex);
                if (g.filterFlag.cardinality() > 0) {
                    a_deletedVsumPerq += (double) g.filterFlag.cardinality() / g.order * 100;
                    int newSize = 0;
                    int newOrder = 0;
                    int[] map = new int[g.order];
                    int[] pam = new int[g.order];
                    boolean allZeros;
                    BitSet adj = new BitSet();
                    for (int v = g.filterFlag.nextClearBit(0); v < g.order; v = g.filterFlag.nextClearBit(++v)) {
                        allZeros = true; // 一行が全て0かどうかを示すフラグ
                        adj = g.edgeBitset.get(v);
                        for (int u = adj.nextSetBit(0); u != -1; u = adj.nextSetBit(++u)) {
                            if (g.filterFlag.get(u))
                                continue;
                            ++newSize;
                            allZeros = false; // 一行に0以外の要素がある場合、フラグをfalseに設定
                        }
                        if (!allZeros) {
                            pam[v] = newOrder;
                            map[newOrder++] = v;
                        }
                    }

                    newSize /= 2;

                    removeTotalSize += g.size - newSize;
                    if (g.size - newSize != 0)
                        removeTotalGraphs++;

                    if (newOrder == 0 || newSize == 0) {
                        g.filterFlag.clear();
                        continue;
                    }

                    if (q.order > newOrder || q.size > newSize) {
                        g.filterFlag.clear();
                        continue;
                    }
                    can.add(trueIndex);

                    String line = "#" + g.id + "\n";
                    bw2.write(line);
                    line = newOrder + "\n";
                    bw2.write(line);
                    for (int i = 0; i < newOrder; i++) {
                        bw2.write(g.vertices[map[i]] + "\n");
                    }

                    bw2.write(newSize + "\n");

                    for (int i = 0; i < newOrder; i++) {
                        adj = g.edgeBitset.get(map[i]);
                        for (int j = adj.nextSetBit(0); j != -1; j = adj.nextSetBit(++j)) {
                            if (g.filterFlag.get(j) || i > pam[j])
                                continue;
                            bw2.write(i + " " + pam[j] + "\n");
                        }
                    }
                    g.filterFlag.clear();

                } else {
                    can.add(trueIndex);
                    for (String line : gMaps.get(trueIndex)) {
                        bw2.write(line + "\n");
                    }
                }

            }
            bw2.close();
        } catch (IOException e) {
            System.exit(1);
        }
        write_time += System.nanoTime() - time;
    }

    static boolean timelimit;

    @SuppressWarnings("deprecation")
    private void verification_VEQ(String directory, String dataset, String mode, Graph q, int Gsize)
            throws InterruptedException {
        long start = System.nanoTime();

        try {
            String command = String.format("./VEQ_S -dg datagraph.gfu -qg %s/%s/%s/%d/q%d.gfu -o output2.txt",
                    directory, dataset, mode, q.size, q.id);
            p = runtime.exec(command);
            timelimit = p.waitFor(10, TimeUnit.MINUTES);
            if (!timelimit) {
                verification_time += 600000;// 10m
                query_per_time = 600000;
                query_per_veq = 0;
                veq_per_Can = 0;
                fileter_time = 0;// veq
                verify_time = 0;// veq
                a_filterTime = 0;
                FPratio_q = 0;
                FPre = 0;
                filpertime = 0;
                SPper_q = 0;
                a_nodeFiltering_time = 0;
                fail++;
            } else {
                nonfail++;
                InputStream inputStream = p.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.charAt(0) == 'F') {
                        String time = line.substring(21);
                        fileter_time = Double.parseDouble(time);
                        query_per_veqF += Double.parseDouble(time);
                    } else if (line.charAt(0) == 'V') {
                        String time = line.substring(24);
                        verify_time = Double.parseDouble(time);
                        query_per_veqV += Double.parseDouble(time);
                    } else if (line.startsWith("Number of C")) {
                        String num = line.substring(28);
                        veq_per_Can = Double.parseDouble(num);
                    }
                    // else if (line.startsWith("Number of A")) {
                    // String answer = line.substring(28);
                    // veq_per_An = Double.parseDouble(answer);
                    // }
                }
                query_per_veq = fileter_time + verify_time;
                query_per_time = query_per_veq + (double) a_filterTime / 1000 / 1000;
                CT_verify += query_per_veq;
                verification_time += query_per_veq;
                query_per_sum += query_per_veq;

                long start_read = System.nanoTime();
                veq_answer_num = 0;
                readAnswer(path, can, result);
                read_time += System.nanoTime() - start_read;

                if (Gsize == result.cardinality()) {
                    FPre = 1;
                } else {
                    FPre = (double) (Gsize - Can.cardinality() - In.cardinality()) /
                            (Gsize - result.cardinality());
                }

                FPratio_q = (double) (veq_answer_num) / (Can.cardinality());

                SPper_q = (double) In.cardinality() / (result.cardinality());

                if (Gsize - veq_per_Can != 0) {
                    filpertime = (Gsize - veq_per_Can) / (((double) a_filterTime / 1000 / 1000) +
                            fileter_time);
                }

            }
            p.destroyForcibly();
        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(0);
        }
        process_time += System.nanoTime() - start;
        totoal_kai += result.cardinality();
        FP += FPre;
        FPratio += FPratio_q;
        SP += SPper_q;
    }

    private void readAnswer(Path path, ArrayList<Integer> can, BitSet result) {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            int id;
            String line;
            while ((line = br.readLine()) != null) {
                id = Integer.parseInt(line);
                result.set(can.get(id));
                veq_answer_num++;
            }
            br.close();
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    private void write_file_indiv(Graph q, BufferedWriter bw_data, int size) throws IOException {
        if(bw_data==null)return;
        if (q.id == 0) {
            bw_data.write(
                    "query_id,FP,(G-C)/(G-A),SP,search_time(ms),node filtering time(ms),filtering_time(ms)(VEQ),verification_time(ms)(VEQ),VEQs_time(ms),query_time(ms),|F|,|In|,|C|,VEQ_|C|,|A|,|Σ(q)|,1ms_per_|F|,deleted Vsum/|Can|, ave_% of vertices were removed\n");
        }

        double deletedVsumPerqPerg2 = (Can.cardinality() != 0) ? a_deletedVsumPerq / (Can.cardinality())
                : 0;
        double deletedVsumPerqPerg = (Can.cardinality() != 0) ? (double) deletedVsumPerq / (Can.cardinality()) : 0;
        double a_ms_per_fil = (a_filterTime != 0) ? (double) a_fil_count / (a_filterTime / 1000.0 / 1000.0) : 0;

        bw_data.write(q.id + "," + FPratio_q + "," + FPre + ","
                + SPper_q + ","
                + String.format("%.8f", (double) a_filterTime / 1000 / 1000) + ","
                + String.format("%.8f", (double) a_nodeFiltering_time / 1000 / 1000) + ","
                + String.format("%.8f", fileter_time) + "," +
                String.format("%.8f", verify_time) + "," + String.format("%.8f",
                        query_per_veq)
                +
                "," + String.format("%.8f", query_per_time + (double) a_nodeFiltering_time / 1000 / 1000) + ","
                + a_fil_count + "," + a_in_count + "," +
                Can.cardinality() + "," + veq_per_Can + ","
                + result.cardinality() + "," +
                q.labels_Set().cardinality() + ","
                + a_ms_per_fil + ","
                + String.format("%.2f", deletedVsumPerqPerg)
                + "," + deletedVsumPerqPerg2
                + "\n");
        ms_per_fil += a_ms_per_fil;
        deletedVsumPerq_set += deletedVsumPerqPerg2;
        deletedVsumPerq = 0;
        a_nodeFiltering_time = 0;
        query_per_veq = 0;
        fileter_time = 0;
        verify_time = 0;
        veq_per_Can = 0;
        a_deletedVsumPerq = 0;
        a_fil_count = 0;
        bw_data.flush();
    }

    private void write_file(BufferedWriter allbw, BufferedWriter bw, int size, BufferedWriter br_whole) {
        try {
            String line = String.format("%.5f", FPratio / nonfail) + ","
                    + String.format("%.5f", FP / nonfail) + ","
                    + String.format("%.5f", SP / nonfail) + ","
                    + String.format("%.6f", (double) (search_time + nodeFiltering_time) / 1000 / 1000 / nonfail) + ","// fil
                    + String.format("%.6f", CT_verify / nonfail) + ","// ver
                    + String.format("%.6f", ((double) search_time / 1000 / 1000 +
                            verification_time + (double) (nodeFiltering_time) / 1000 / 1000) / 100)
                    + ","
                    + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail) + ","// tree1
                    + String.format("%.6f", (double) nodeFiltering_time / 1000 / 1000 / nonfail) + ","// node fil
                    + String.format("%.0f", (double) in_count) + ","
                    + String.format("%.0f", (double) totoal_kai) + ","
                    + String.format("%.0f", (double) doukeicount) + ","
                    + String.format("%.0f", (double) fil_count) + ","
                    + String.format("%.1f", (double) query_per_nf_count) + ","// 削除できた頂点数
                    + String.format("%.1f", (double) removeTotalSize) + ","// 削除できた総辺数
                    + nonfail + "," + verfyNum
                    + "," + q_trav_num + "," + ms_per_fil / nonfail + "," + deletedVsumPerq_set / verfyNum
                    + "\n";
            allbw.write(line);
            br_whole.write(line);

            allbw.flush();
            br_whole.flush();

            bw.write("(C)書き込み関数時間(ms): " + String.format("%.2f", (double) write_time /
                    1000 / 1000) + "\n");
            bw.write("(D)プロセス関数時間(ms): " + String.format("%.2f", (double) process_time /
                    1000 / 1000) + "\n");
            bw.write("(E)読み込み関数時間(ms): " + String.format("%.2f", (double) read_time /
                    1000 / 1000) + "\n");

            bw.write("contain_search_time (ms): "
                    + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail)
                    + "\n");
            bw.write("Number of Filtering Graphs: " + fil_count + "\n");
            bw.write("Number of inclusion Graphs: " + in_count + "\n");
            bw.write("Number of Candidate Graphs: " + doukeicount + "\n");
            bw.write("Number of Answer Graphs: " + totoal_kai + "\n");
            bw.write("filtering Presison : " + String.format("%.5f", FP / nonfail) +
                    "\n");
            bw.write("FP (A/C) : " + String.format("%.5f", FPratio / nonfail) + "\n");
            bw.write("inclusion Presison : " + String.format("%.5f", SP) + "%" + "\n");
            bw.write("削除できた頂点数/|Can(Q)|:" + (double) query_per_nf_count / (doukeicount) + "\n");
            bw.write("(A)=(B)+(C) Filtering Time (ms): "
                    + String.format("%.6f", (double) (search_time + nodeFiltering_time) / 1000 / 1000 / nonfail)
                    + "\n");
            bw.write("(B) search Time (ms): "
                    + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail)
                    + "\n");
            bw.write("(C)Node_Filtering_Time (ms): "
                    + String.format("%.6f", (double) nodeFiltering_time / 1000 / 1000 / nonfail)
                    + "\n");
            bw.write("(a)a VEQs Filtering Time (ms): " + String.format("%.6f",
                    query_per_veqF / nonfail) + "\n");
            bw.write("(b)a VEQs Verification Time (ms): " + String.format("%.6f",
                    query_per_veqV / nonfail) + "\n");
            bw.write("(D=a+b)a Verification Time (ms): " + String.format("%.6f",
                    query_per_sum / nonfail)
                    + "\n");
            bw.write("(A)+(D) a Processing Time (ms): "
                    + String.format("%.6f", ((double) search_time / 1000 / 1000 +
                            verification_time + (double) (nodeFiltering_time) / 1000 / 1000) / 100)
                    + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init_param() {
        deletedVsumPerq_set = 0;
        ms_per_fil = 0;
        fail = 0;
        removeTotalSize = 0;
        removeTotalGraphs = 0;
        query_per_nf_count = 0;
        deletedVsumPerq = 0;
        nonfail = 0;
        CT_verify = 0;
        doukeicount = 0;
        verification_time = 0;
        process_time = 0;
        write_time = 0;
        read_time = 0;
        totoal_kai = 0;
        search_time = 0;
        query_per_veqF = 0;
        query_per_veqV = 0;
        query_per_sum = 0;
        FP = 0;
        FPratio = 0;
        SP = 0;
        fil_count = 0;
        in_count = 0;
        nodeFiltering_time = 0;
        veq_per_Can = 0;
        query_per_veq = 0;
        fileter_time = 0;
        verify_time = 0;
        removeTotalGraphs = 0;
        removeTotalSize = 0;
        lab_fil_num = 0;
        verfyNum = 0;
        q_trav_num = 0;
        traverse_cou = 0;
        deletedVsumPerq = 0;
        a_nodeFiltering_time = 0;
        query_per_veq = 0;
        fileter_time = 0;
        verify_time = 0;
        veq_per_Can = 0;
    }

    List<Integer> search(Graph q, GraphCode impl) {
        HashSet<IndexNode> result0 = new HashSet<>();
        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(q, this);
        for (Pair<IndexNode, SearchInfo> info : infoList) {
            if (info.left.supNode) {
                info.left.search(result0, q, info.right, impl);
            }
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

    private void search(Set<IndexNode> result, Graph q, SearchInfo info, GraphCode impl) {
        final int c = matchGraphIndices.size();
        if (c > 0 && !result.contains(this)) {
            result.add(this);

            for (IndexNode p = this; p != null; p = p.parent) {// 最適化アルゴリズム発動
                p.count -= c;
            }
        }

        // List<Pair<CodeFragment, SearchInfo>> nextFrags =
        // impl.enumerateFollowableFragments(q, info, thisAdjLavels);
        List<Pair<CodeFragment, SearchInfo>> nextFrags;
        if (childEdgeFragAnd.cardinality() > 0) {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragAnd);
        } else {
            nextFrags = impl.enumerateFollowableFragments(q, info, adjLabels, childEdgeFragOr);
        }

        for (IndexNode m : children) {
            if (m.count > 0 && m.supNode) {
                for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
                    if (frag.left.equals(m.frag)) {
                        m.search(result, q, frag.right, impl);
                    }
                }
            }
        }
    }

}

// // 頻出ラベルを子の左側に
// void sortVlabelRootChildren() {
// @SuppressWarnings("unchecked")
// Pair<Byte, IndexNode>[] pairs = new Pair[children.size()];

// for (int i = 0; i < pairs.length; i++) {
// pairs[i] = new Pair<Byte, IndexNode>(children.get(i).frag.getVlabel(),
// children.get(i));
// }
// Arrays.sort(pairs, Comparator.comparingInt(p -> p.left));
// children.clear();

// for (int i = 0; i < pairs.length; i++) {
// children.add(pairs[i].right);
// System.out.print(children.get(i).frag.getVlabel() + " ");
// }
// }

// // sort check
// void printCanSize() {
// System.out.println("this");
// for (IndexNode m : children) {
// System.out.print(m.matchGraphIndicesBitSet.cardinality() + ",");
// }
// System.out.println();

// for (IndexNode m : children) {
// m.printCanSize();
// }
// }

// private boolean getPossibleDegree(List<Pair<CodeFragment, SearchInfo>>
// nextFrags, Graph q) {
// boolean result = true;
// byte vlabel;

// for (int i = 0; i < nextFrags.size(); i++) {
// Pair<CodeFragment, SearchInfo> frag = nextFrags.get(i);
// if (i == 0) {

// }
// }
// for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
// int v = frag.right.getVertexIDs()[frag.right.getVertexIDs().length - 1];

// }
// return result;
// }

// private void subsearch(Graph q, SearchInfo info, GraphCode impl,
// List<IndexNode> traversedNode) {
// q_trav_num++;
// // q_traverse_num++;
// if (depth == 1) {
// if (traversedNode.contains(this))
// traversedNode.remove(this);
// }

// Can.and(matchGraphIndicesBitSet);

// if (backtrackNode || backtrackJudge(q.order)) {
// traverseNecessity = false;
// initTraverseNecessity.add(this);
// return;
// }

// List<Pair<CodeFragment, SearchInfo>> nextFrags =
// impl.enumerateFollowableFragments(q, info, adjLabels);
// Set<Byte> targetVlabel = targetVlabel(nextFrags, q);

// for (IndexNode m : children) {
// if (!m.traverseNecessity)
// continue;
// for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
// if (!m.traverseNecessity)
// break;

// if (targetVlabel.contains(frag.left.getVlabel())) {
// boolean thisDegreeOne = false;
// if (q.adjList[frag.right.getVertexIDs()[depth]].length == 1) {
// thisDegreeOne = true;
// }
// if (thisDegreeOne) {
// if (m.frag.equals(frag.left)) {
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// } else {
// if (m.frag.equals(frag.left) && !m.matchDegreeOne) {
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// }
// } else {
// if (m.frag.equals(frag.left)) {
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// }
// }
// }
// }

// private void subsearch(Graph q, SearchInfo info, GraphCode impl,
// List<IndexNode> traversedNode) throws IOException {
// q_trav_num++;
// // q_traverse_num++;
// if (depth == 1) {
// if (traversedNode.contains(this))
// traversedNode.remove(this);
// }
// // if (depth == 1)
// // Can.and(matchGraphMap.get(q_traverse_num));
// // else
// // Can.and(matchGraphIndicesBitSet);
// // Can.and(matchGraphIndicesBitSet);

// if (backtrackNode || backtrackJudge(q.order)) {
// traverseNecessity = false;
// initTraverseNecessity.add(this);
// return;
// }

// List<Pair<CodeFragment, SearchInfo>> nextFrags =
// impl.enumerateFollowableFragments(q, info, adjLabels);
// Set<Byte> targetVlabel = targetVlabel(nextFrags, q);

// for (IndexNode m : children) {
// if (!m.traverseNecessity)
// continue;

// if (m.matchGraphIndicesBitSet.size() == 0)
// continue;
// for (Pair<CodeFragment, SearchInfo> frag : nextFrags) {
// if (!m.traverseNecessity)
// break;

// if (targetVlabel.contains(frag.left.getVlabel()) && m.brother != null) {
// boolean thisDegreeOne = false;
// if (q.adjList[frag.right.getVertexIDs()[depth]].length == 1) {
// thisDegreeOne = true;
// }
// if (thisDegreeOne) {
// if (m.frag.equals(frag.left)) {
// BitSet brosID = (BitSet) m.matchGraphIndicesBitSet.clone();
// brosID.or(m.brother.matchGraphIndicesBitSet);
// Can.and(brosID);
// if (Can.cardinality() == 0) {
// int a = 0;
// }
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// } else {
// if (m.frag.equals(frag.left) && !m.matchDegreeOne) {
// if (m.matchGraphIndicesBitSet.size() == 0) {
// int a = 0;
// }
// Can.and(m.matchGraphIndicesBitSet);
// if (Can.cardinality() == 0) {
// int a = 0;
// }
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// }
// } else {
// if (m.frag.equals(frag.left) && !m.matchDegreeOne) {
// if (m.brother != null) {
// BitSet brosID = (BitSet) m.matchGraphIndicesBitSet.clone();
// brosID.or(m.brother.matchGraphIndicesBitSet);
// Can.and(brosID);
// } else {
// Can.and(m.matchGraphIndicesBitSet);
// }
// if (m.matchGraphIndicesBitSet.size() == 0) {
// m.node2pattern();
// int a = 0;
// }
// if (Can.cardinality() == 0) {
// int a = 0;
// }
// m.subsearch(q, frag.right, impl, traversedNode);
// }
// }
// }
// }
// }

// private void deg_nodeFiltering_ver2(List<Graph> G) throws IOException {
// nodeFilteringByDeg.removeAll(non_nodeFilteringByDeg);
// int cansize = Can.cardinality();
// int targetDegNodeNum = nodeFilteringByDeg.size();
// if (cansize < targetDegNodeNum) {

// for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can
// .nextSetBit(++trueIndex)) {
// for (IndexNode n : nodeFilteringByDeg) {
// if (n.labelFiltering.get(trueIndex) == null)
// continue;
// nodeFilteringID.or(n.labelFiltering.get(trueIndex));
// }
// for (IndexNode n : non_nodeFilteringByDeg) {
// if (n.labelFiltering.get(trueIndex) == null)
// continue;
// nonnodeFilteringID.or(n.labelFiltering.get(trueIndex));
// }
// for (int trueIndex2 = nodeFilteringID.nextSetBit(0); trueIndex2 != -1;
// trueIndex2 = nodeFilteringID
// .nextSetBit(++trueIndex2)) {
// if (nonnodeFilteringID.get(trueIndex2))
// nodeFilteringID.set(trueIndex2, false);
// }
// G.get(trueIndex).filterFlag.or(nodeFilteringID);
// nodeFilteringID.clear();
// nonnodeFilteringID.clear();
// }
// } else {
// // if (q_count == 0) {
// // for (int i = 0; i < G.size(); i++) {
// // filetMap.put(i, new BitSet());
// // }
// // }
// // q_count++;
// for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can
// .nextSetBit(++trueIndex)) {
// filetMap.put(trueIndex, new BitSet());
// }

// for (IndexNode n : nodeFilteringByDeg) {
// for (int key : n.labelFiltering.keySet()) {
// if (Can.get(key)) {
// filetMap.get(key).or(n.labelFiltering.get(key));
// }
// }
// }
// for (IndexNode n : non_nodeFilteringByDeg) {
// for (int key : n.labelFiltering.keySet()) {
// if (Can.get(key)) {
// for (int trueIndex2 = n.labelFiltering.get(key).nextSetBit(
// 0); trueIndex2 != -1; trueIndex2 =
// n.labelFiltering.get(key).nextSetBit(++trueIndex2)) {
// filetMap.get(key).set(trueIndex2, false);
// }
// }
// }
// }
// for (int id : filetMap.keySet()) {
// G.get(id).filterFlag.or(filetMap.get(id));
// // filetMap.get(id).clear();
// }
// filetMap.clear();
// }

// }
// IndexNode(IndexNode parent, CodeFragment frag, boolean matchDegreeOne_frag) {
// this.parent = parent;
// this.frag = frag;

// children = new ArrayList<>();
// matchGraphIndicesBitSet = new BitSet();
// matchGraphIndices = new ArrayList<>();
// matchGraphIndicesBitSetDegree = new BitSet();

// supNode = false;
// count = 0;
// depth = 0;
// nodeID = 0;
// adjLabels = new HashSet<>();
// q_traverse_num = 0;
// labelFiltering = new LinkedHashMap<>();
// childEdgeFrag = new BitSet();
// // matchGraphMap = new HashMap<>();
// // g_traverse_num = 0;
// traverseNecessity = true;
// backtrackNode = false;
// matchDegreeOne = matchDegreeOne_frag;
// }

// IndexNode(IndexNode m) {
// this.parent = m.parent;
// this.frag = m.frag;

// children = new ArrayList<>(m.children);
// matchGraphIndicesBitSet = new BitSet();
// supNode = false;
// depth = m.depth;
// nodeIDcount++;
// nodeID = nodeIDcount;
// adjLabels = new HashSet<>(m.adjLabels);
// q_traverse_num = m.q_traverse_num;
// labelFiltering = new LinkedHashMap<>(m.labelFiltering);
// childEdgeFrag = (BitSet) m.childEdgeFrag.clone();
// // matchGraphMap = new HashMap<>();
// // g_traverse_num = 0;
// traverseNecessity = true;
// backtrackNode = false;
// matchDegreeOne = true;
// } // public void addPath(List<CodeFragment> code, int graphIndex, boolean
// supergraphSearch, boolean[] degreeOne) {
// final int height = code.size();

// if (this.nodeID == 0) {
// nodeIDcount++;
// nodeID = nodeIDcount;
// }

// if (supergraphSearch) {
// ++count;
// supNode = true;
// if (height <= 0 && graphIndex != -1) {
// matchGraphIndices.add(graphIndex);
// return;
// }

// } else {
// if (graphIndex != -1)
// matchGraphIndicesBitSet.set(graphIndex, true);

// int dep = -1;
// for (IndexNode a = this; a != null; a = a.parent) {
// dep++;
// }
// depth = dep;
// if (height <= 0)
// return;
// }

// CodeFragment car = code.get(0);
// List<CodeFragment> cdr = code.subList(1, height);
// boolean[] degreeOneR = new boolean[degreeOne.length - 1];
// for (int i = 1; i < degreeOne.length; i++) {
// degreeOneR[i - 1] = degreeOne[i];
// }

// for (IndexNode m : children) {
// if (depth == 0) {
// if (m.frag.equals(car)) {
// m.addPath(cdr, graphIndex, supergraphSearch, degreeOneR);
// return;
// }
// } else {
// if (m.frag.equals(car) && m.matchDegreeOne == degreeOne[0]) {
// m.addPath(cdr, graphIndex, supergraphSearch, degreeOneR);
// return;
// }
// }
// }

// IndexNode m = new IndexNode(this, car, degreeOne[0]);
// if (supergraphSearch)
// m.supNode = true;
// children.add(m);

// m.addPath(cdr, graphIndex, supergraphSearch, degreeOneR);
// } // void divideNodeByDeg(int limit_depth) {
// if (depth > 0 && depth < limit_depth) {
// children.addAll(getDegTrueNode());
// }
// if (depth < limit_depth) {
// for (IndexNode m : children) {
// if (!m.matchDegreeOne) {
// m.divideNodeByDeg(limit_depth);
// }
// }
// }
// }

// private List<IndexNode> getDegTrueNode() {
// List<IndexNode> returnNode = new ArrayList<>();
// for (IndexNode m : children) {
// IndexNode newNode = new IndexNode(m);
// m.brother = newNode;
// newNode.brother = m;
// returnNode.add(newNode);
// }
// return returnNode;
// }// private void deg_nodeFiltering(List<Graph> G) throws IOException {
// nodeFilteringByDeg.removeAll(non_nodeFilteringByDeg);
// for (IndexNode n : nodeFilteringByDeg) {
// int mapsize = n.labelFiltering.size();
// int cansize = Can.cardinality();
// if (cansize <= mapsize) {
// for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can
// .nextSetBit(++trueIndex)) {
// BitSet labelFrag = n.labelFiltering.get(trueIndex);
// if (labelFrag != null) {
// G.get(trueIndex).filterFlag.or(labelFrag);
// }
// // degfiltering_sum += G.get(trueIndex).filterFlag.cardinality();
// }
// } else {
// for (int trueIndex : n.labelFiltering.keySet()) {
// if (Can.get(trueIndex)) {
// BitSet labelFrag = n.labelFiltering.get(trueIndex);
// G.get(trueIndex).filterFlag.or(labelFrag);
// }
// }
// }
// }
// }
// private void deg_nodeFiltering(List<Graph> G) throws IOException {
// BitSet nodeFilteringID = new BitSet();
// BitSet nonnodeFilteringID = new BitSet();
// nodeFilteringByDeg.removeAll(non_nodeFilteringByDeg);
// for (int trueIndex = Can.nextSetBit(0); trueIndex != -1; trueIndex = Can
// .nextSetBit(++trueIndex)) {
// for (IndexNode n : nodeFilteringByDeg) {
// if (n.labelFiltering.get(trueIndex) == null)
// continue;
// nodeFilteringID.or(n.labelFiltering.get(trueIndex));
// }
// for (IndexNode n : non_nodeFilteringByDeg) {
// if (n.labelFiltering.get(trueIndex) == null)
// continue;
// nonnodeFilteringID.or(n.labelFiltering.get(trueIndex));
// }
// for (int trueIndex2 = nodeFilteringID.nextSetBit(0); trueIndex2 != -1;
// trueIndex2 = nodeFilteringID
// .nextSetBit(++trueIndex2)) {
// if (nonnodeFilteringID.get(trueIndex2))
// nodeFilteringID.set(trueIndex2, false);
// }
// // for (IndexNode n : non_nodeFilteringByDeg) {
// // if (n.labelFiltering.get(trueIndex) == null)
// // continue;
// // for (int v = n.labelFiltering.get(trueIndex).nextSetBit(0); v != -1; v =
// // n.labelFiltering.get(trueIndex)
// // .nextSetBit(++v)) {
// // nodeFilteringID.set(v, false);
// // }
// // }
// G.get(trueIndex).filterFlag.or(nodeFilteringID);
// nodeFilteringID.clear();
// nonnodeFilteringID.clear();
// }
// }

// static Map<Integer, BitSet> filetMap = new HashMap<>();
// BitSet nodeFilteringID = new BitSet();
// BitSet nonnodeFilteringID = new BitSet();

// allbw.write(String.format("%.5f", FPratio / nonfail) + ","
// + String.format("%.5f", FP / nonfail) + ","
// + String.format("%.5f", SP / nonfail) + ","
// + String.format("%.6f", (double) (search_time + nodeFiltering_time) / 1000 /
// 1000 / nonfail) + ","// fil
// + String.format("%.6f", CT_verify / nonfail) + ","// ver
// // + String.format("%.6f", ((double) search_time / 1000 / 1000 + // query
// // verification_time) / 100)
// + String.format("%.6f", ((double) search_time / 1000 / 1000 +
// verification_time + (double) (nodeFiltering_time) / 1000 / 1000) / 100)
// + ","
// + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail) + ","//
// tree1
// // + String.format("%.6f", (double) equals_search / 1000 / 1000 / nonfail) +
// // ","// tree2
// // + String.format("%.6f", (double) edgeFiltering_time / 1000 / 1000 /
// nonfail)
// // + ","// edge fil
// + String.format("%.6f", (double) nodeFiltering_time / 1000 / 1000 / nonfail)
// + ","// node fil
// + String.format("%.0f", (double) in_count) + ","
// + String.format("%.0f", (double) totoal_kai) + ","
// + String.format("%.0f", (double) doukeicount) + ","
// + String.format("%.0f", (double) fil_count) + ","
// // + String.format("%.1f", (double) lab_fil_num) + ","
// // + String.format("%.1f", (double) labelNumFiltering) + ","
// // + String.format("%.1f", (double) removeTotalGraphs / nonfail) + ","//
// // 何かを削除できたグラフ数
// + String.format("%.1f", (double) query_per_nf_count) + ","// 削除できた頂点数
// // + String.format("%.1f", (double) filtering_edge_num) + ","// 削除できた辺数
// + String.format("%.1f", (double) removeTotalSize) + ","// 削除できた総辺数

// // + String.format("%.6f" , (((double) search_time / 1000 / 1000) / (size *
// // nonfail - doukeicount)))
// // + "," + (size * nonfail - doukeicount) + ","
// // + String.format("%.6f",
// // (size * nonfail - veq_Can_total)
// // / (((double) search_time / 1000 / 1000) + query_per_veqF))
// // + ","
// // + (size * nonfail - veq_Can_total) + ","
// + nonfail + "," + verfyNum
// + "," + q_trav_num + "," + ms_per_fil / nonfail
// + "\n");

// br_whole.write(String.format("%.5f", FPratio / nonfail) + ","
// + String.format("%.5f", FP / nonfail) + ","
// + String.format("%.5f", SP / nonfail) + ","
// + String.format("%.6f", (double) (search_time + nodeFiltering_time) / 1000 /
// 1000 / nonfail) + ","// fil
// + String.format("%.6f", CT_verify / nonfail) + ","// ver
// + String.format("%.6f", ((double) search_time / 1000 / 1000 +
// verification_time + (double) (nodeFiltering_time) / 1000 / 1000) / 100)
// + ","
// + String.format("%.6f", (double) search_time / 1000 / 1000 / nonfail) + ","//
// tree1
// + String.format("%.6f", (double) nodeFiltering_time / 1000 / 1000 / nonfail)
// + ","// node fil
// + String.format("%.0f", (double) in_count) + ","
// + String.format("%.0f", (double) totoal_kai) + ","
// + String.format("%.0f", (double) doukeicount) + ","
// + String.format("%.0f", (double) fil_count) + ","
// + String.format("%.1f", (double) query_per_nf_count) + ","// 削除できた頂点数
// + String.format("%.1f", (double) removeTotalSize) + ","// 削除できた総辺数
// + nonfail + "," + verfyNum
// + "," + q_trav_num + "," + ms_per_fil / nonfail
// + "\n");

// private void filtering_second_step(List<Graph> G, Graph q) {
// BitSet mgv = new BitSet();
// for (int id = Can.nextSetBit(0); id != -1; id = Can.nextSetBit(++id)) {
// for (int v = 0; v < q.order; v++) {
// mgv.clear();
// int loop_count = 0;
// if (q.FVQ.get(v).size() == 0)
// continue;
// for (IndexNode n : q.FVQ.get(v)) {
// if (loop_count == 0) {
// mgv.or(n.start_gv.get(id));
// } else {
// mgv.and(n.start_gv.get(id));
// }
// // System.out.println(mgv.cardinality());
// loop_count++;
// }
// if (mgv.isEmpty()) {
// // if (mgv.cardinality() == G.get(id).order) {
// Can.set(id, false);
// break;
// }
// }
// }
// }

// private void filtering_by_deg() {
// for (IndexNode n : children) {
// if (n.q_maxDeg == 0)
// continue;
// else {
// for (int id = Can.nextSetBit(0); id != -1; id = Can.nextSetBit(++id)) {
// if (n.matchGraphIndicesBitSet.get(id) && n.maxLabelMap.get(id) < n.q_maxDeg)
// {
// Can.set(id, false);
// }
// }
// n.q_maxDeg = 0;
// }
// }
// }

// private void filtering_by_count() {
// for (IndexNode m : this.children) {
// if (m.q_traverse_num < 1)
// continue;
// for (int id = Can.nextSetBit(0); id != -1; id = Can.nextSetBit(++id)) {
// if (!m.matchGraphIndicesBitSet.get(id))
// continue;
// if (m.path_count.get(id) < m.q_traverse_num) {
// Can.set(id, false);
// continue;
// }
// // if (m.matchGraphIndicesBitSet.get(id) && m.path_count.get(id) <
// // m.q_traverse_num) {
// // Can.set(id, false);
// // }

// // if (m.depth == 1 && m.maxLabelMap.get(id) < m.q_maxDeg) {
// // Can.set(id, false);
// // }

// // if (m.depth == 1 && m.maxLabelMap2[id] < m.q_maxDeg) {
// // Can.set(id, false);
// // }
// }
// // m.q_maxDeg = 0;

// if (m.depth == sikiiti)
// continue;
// m.filtering_by_count();
// }

// }
// void addDescendantsIDs() {
// for (IndexNode m : children) {
// m.descendantsMatchGraphIndicesBitSet.or(m.matchGraphIndicesBitSet);
// m.getDescendantsMatchGraphIndicesBitSet(m);
// m.addDescendantsIDs();
// }

// }

// private void getDescendantsMatchGraphIndicesBitSet(IndexNode target) {
// for (IndexNode m : children) {
// target.descendantsMatchGraphIndicesBitSet.and(m.matchGraphIndicesBitSet);
// // target.descendantsMatchGraphIndicesBitSet.or(m.matchGraphIndicesBitSet);
// //
// System.out.println(target.descendantsMatchGraphIndicesBitSet.cardinality());
// m.getDescendantsMatchGraphIndicesBitSet(target);
// }
// }