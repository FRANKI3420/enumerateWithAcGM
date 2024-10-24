package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import codetree.common.*;
import codetree.core.*;
import codetree.vertexBased.AcgmCode;

class Main {
    private static String sdfFilename = "aido99sd.sdf";
    private static GraphCode graphCode = new AcgmCode();
    private static ObjectType objectType = new AcgmCode();
    static int id = 0;
    static BufferedWriter bw;
    static BufferedWriter bw2;
    static CodeTree tree;
    static int sigma;
    static byte eLabelNum;
    static int finish;
    static boolean runPython = false;

    public static void main(String[] args) {
        sigma = 1;
        eLabelNum = 1;
        finish = 9;
        final boolean filter = false;

        startEnumarate(filter);

        if (runPython) {
            RunPythonFromJava();
        }
    }

    private static void startEnumarate(boolean filter) {

        List<ArrayList<ObjectFragment>> codeList = objectType.computeCanonicalCode(sigma);
        try {
            bw = Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            long start = System.nanoTime();
            System.out.println("|V|<=" + finish + " |Σ|=" + sigma + " eLabelNum=" +
                    eLabelNum);
            enumarateWithAcGM(codeList, filter);
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (System.nanoTime() - start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);
            bw.close();
            bw2.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    static List<ArrayList<ObjectFragment>> childrenOfM1 = new ArrayList<>();

    private static void enumarateWithAcGM(List<ArrayList<ObjectFragment>> codeLsit, boolean filter) throws IOException {
        int index = -1;
        for (ArrayList<ObjectFragment> c : codeLsit) {
            index++;
            
            if (c.get(c.size() - 1).getIsConnected()) {
                Graph g = objectType.generateGraphAddElabel(c, id);// AcGMcode2graph
                if (objectType.isCanonical(g, c)) {// g is a connected graph and g is canonical then
                    // print(c,true);//output g
                    g.writeGraph2GfuAddeLabel(bw);// output g
                    id++;
                    if (c.size() == finish) {
                        continue;
                    }
                    final byte lastVlabel = c.get(c.size() - 1).getVlabel();
                    final int depth = c.size() - 1;
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeLsit, lastVlabel, index, depth);// anotherList                                                                                     // ←
                    childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * lastVlabel);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            for (byte j = 0; j <= lastVlabel; j++) {
                                childrenOfM1.add(getChildrenOfM1(c, M2, j, i, filter));
                            }
                        }
                    }
                    anotherList = null;
                    enumarateWithAcGM(childrenOfM1, filter);
                }
            }
        }
    }

    private static ArrayList<ObjectFragment> getChildrenOfM1(ArrayList<ObjectFragment> c, ObjectFragment M2,
            byte vLabel, byte eLabel, boolean filter) throws IOException {
        byte[] eLabels = new byte[c.size()];
        final boolean isConnected = (eLabel != 0 || M2.getIsConnected()) ? true : false;

        System.arraycopy(M2.getelabel(), 0, eLabels, 0, c.size() - 1);

        eLabels[c.size() - 1] = eLabel;

        ObjectFragment leg = objectType.generateCodeFragment(vLabel, eLabels, isConnected, 0);
        ArrayList<ObjectFragment> child = new ArrayList<>(c);
        child.add(leg);

        if (filter) {// 未完成
            if (isChildCodeExist(child)) {
                return null;
            } else {
                return child;
            }
        } else {
            return child;
        }
    }

    static ArrayList<ObjectFragment> anotherList = new ArrayList<>();

    private static ArrayList<ObjectFragment> getAnotherList(
            List<ArrayList<ObjectFragment>> codeLsit, byte vLabel, int index, int depth) {
        anotherList.clear();
        ArrayList<ObjectFragment> code;

        for (int i = index, len = codeLsit.size(); i < len; i++) {
            code = codeLsit.get(i);
            if (vLabel != code.get(depth).getVlabel())
                continue;
            anotherList.add(code.get(depth));
        }

        return anotherList;
    }

    // 完全グラフの辺の数
    private static int calculateCombination(int n) {
        if (n < 2) {
            return 0;
        }
        return n * (n - 1) / 2;
    }

    private static boolean isChildCodeExist(ArrayList<ObjectFragment> child) throws IOException {
        // List<Integer> result = tree.supergraphSearch(generateGraph(child, 0));
        List<Integer> result = new ArrayList<>();
        if (result.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    private static void print(List<ObjectFragment> code, boolean output) throws IOException {// AcGMコード可視化
        for (ObjectFragment c : code) {
            if (output) {
                System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            } else {
                bw2.write(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            }
        }

        if (output) {
            System.out.println();
        } else {
            bw2.write("\n");
            bw2.flush();
        }
    }

    private static int dataBaseElabel(List<Graph> G) {
        HashSet<Byte> elabels = new HashSet<>();
        for (Graph g : G) {
            for (byte[] edges : g.edges) {
                for (byte e : edges) {
                    elabels.add(e);
                }
            }
        }
        return elabels.size() - 1;
    }

    private static int dataBaseSigma(List<Graph> G) {
        int i = VertexLabel.size();
        HashSet<Byte> labels = new HashSet<>();
        for (Graph g : G) {
            for (byte v : g.vertices) {
                labels.add(v);
            }
        }
        return labels.size();
    }

    static void RunPythonFromJava() {
        try {
            // Pythonスクリプトを実行する
            ProcessBuilder pb = new ProcessBuilder("python", "draw_graph_edgeLabel.py", String.valueOf(sigma),
                    String.valueOf(eLabelNum));
            Process process = pb.start();

            // Pythonスクリプトの出力を取得する
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // 標準エラー出力の取得
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.out.println("ERROR: " + line); // エラーメッセージを確認
            }

            // プロセスが終了するのを待つ
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Program exited successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
