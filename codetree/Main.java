package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import codetree.common.*;
import codetree.core.*;

import codetree.vertexBased.AcgmCode;
import codetree.vertexBased.XAcgmCode;
import codetree.edgeBased.DfsCode;

class Main {
    public static void main(String[] args) {
        parseArgs(args);

        List<Graph> G = SdfFileReader.readFile(Paths.get(sdfFilename));
        // G = G.subList(0, 200);

        ArrayList<Pair<Integer, Graph>> Q = new ArrayList<>();
        for (int i = 0; i < G.size(); ++i) {
            Graph g = G.get(i);

            final int size = g.size();
            if (34 <= size && size <= 36 && g.isConnected()) {
                Q.add(new Pair<Integer, Graph>(i, g));
            }
        }

        System.out.println("G size: " + G.size());
        System.out.println("Q size: " + Q.size());

        long start = System.nanoTime();
        CodeTree tree = new CodeTree(graphCode, G, 100);
        System.out.println("Build tree: " + (System.nanoTime() - start) / 1000 / 1000 + "msec");

        G = null;

        Path out = Paths.get("output.txt");
        try (BufferedWriter bw = Files.newBufferedWriter(out)) {
            start = System.nanoTime();

            int total_answer_num = 0;

            for (Pair<Integer, Graph> q : Q) {
                List<Integer> result = tree.supergraphSearch(q.right);
                bw.write(q.left.toString() + result.toString() + "\n");
                total_answer_num += result.size();
            }

            final long time = System.nanoTime() - start;
            System.out.println((time) + " nano sec");
            System.out.println((time / 1000 / 1000) + " msec");
            System.out.println("|A| " + total_answer_num);
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-code")) {
                if (args[++i].equals("acgm"))
                    graphCode = new AcgmCode();
                else if (args[i].equals("xacgm"))
                    graphCode = new XAcgmCode();
                else if (args[i].equals("dfs"))
                    graphCode = new DfsCode();
                else {
                    System.err.println("無効なグラフコード: " + args[i]);
                    System.exit(1);
                }
            } else if (sdfFilename == null) {
                sdfFilename = args[i];
            } else {
                System.err.println("無効な引数: " + args[i]);
                System.exit(1);
            }
        }

        if (sdfFilename == null) {
            System.err.print("入力ファイルを指定してください -> ");
            Scanner sc = new Scanner(System.in);
            sdfFilename = sc.nextLine();
        }

        if (graphCode == null) {
            graphCode = new AcgmCode();
            System.out.println("AcGMコードで動作します");
        }
    }

    private static String sdfFilename = "aido99sd.sdf";
    private static GraphCode graphCode = null;
    // private static GraphCode graphCode = new XAcgmCode();
}
