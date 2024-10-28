package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import codetree.common.*;
import codetree.core.*;
import codetree.vertexBased.AcgmCode;
import java.nio.file.attribute.BasicFileAttributes;


import java.util.concurrent.*;

class Main2 {
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
    static final boolean filter = false;
    static ArrayList<ObjectFragment> anotherList = new ArrayList<>();


    public static void main(String[] args) {
        sigma = 3;
        eLabelNum = 1;
        finish = 3;

        // test();
        removeResultDirectory();

        startEnumarate();

        if (runPython) {
            RunPythonFromJava();
        }
    }

    private static void startEnumarate() {
        try {
            bw = Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            System.out.println("|V|<=" + finish + " |Σ|=" + sigma + " eLabelNum=" +
            eLabelNum);
            long start = System.nanoTime();
            List<ArrayList<ObjectFragment>> codeList = new CopyOnWriteArrayList<>();
            codeList = objectType.computeCanonicalCode(sigma);
            System.out.println("逐次処理");
            enumarateWithAcGM(codeList);
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (System.nanoTime() - start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);

            // id =0;
            // start = System.nanoTime();
            // System.out.println("並列処理:コア数 "+Runtime.getRuntime().availableProcessors());
            // enumarateWithAcGMParallel(codeList);
            // System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
            //         "ms");
            // System.out.println(
            //         "実行時間：" + String.format("%3f", (double) (System.nanoTime() - start) / 1000 /
            //                 1000 / 1000) + "s");
            // System.out.println("ans num: " + id);
        } catch (IOException e) {
            System.err.println(e);
        }finally{
            if (bw != null) {
                try {
                    bw.close(); // 最後に閉じる
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static int num = 0;
    private static void enumarateWithAcGMParallel(List<ArrayList<ObjectFragment>> codeList) throws IOException {
    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    try {
        List<Future<?>> futures = new ArrayList<>();

        for (int index = 0; index < codeList.size(); index++) {
            ArrayList<ObjectFragment> c = codeList.get(index); 

            if (c.get(c.size() - 1).getIsConnected()) {
                final int currentIdx = index;
                num++;
                futures.add(executorService.submit(() -> {
                    // String fileName = "result\\output_thread_" + num + ".gfu";
                    // try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(fileName), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    //     processFragment(c, currentIdx, codeList, bw);
                    // } catch (IOException e) {
                    //     e.printStackTrace();
                    // }
                    processFragment(c, currentIdx, codeList, bw);
                    
                }));
            }
        }
        // すべてのタスクの完了を待機
        for (Future<?> future : futures) {
            future.get();
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        executorService.shutdown();
    }
}

private static void processFragment(ArrayList<ObjectFragment> c, int index, List<ArrayList<ObjectFragment>> codeList, BufferedWriter bw) {
    try {

            Graph g = objectType.generateGraphAddElabel(c, id);
                if (objectType.isCanonical(g, c)) {
                    // print(c,true);//output g
                     synchronized (Main.class) { // または適切なオブジェクトを使用
                        // 共有リソースの更新
                        g.writeGraph2GfuAddeLabel(bw);
                        id++;
                    }
                    if (c.size() == finish) {
                        return;
                    }
                    final int depth = c.size() - 1;
                    final byte vLabel = c.get(depth).getVlabel();
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, depth,vLabel);
                    List<ArrayList<ObjectFragment>> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(c, M2, i));
                        }
                    }
                    
                    anotherList = null;
                    g = null;
                    enumarateWithAcGM(childrenOfM1);
                }
        
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    private static void enumarateWithAcGM(List<ArrayList<ObjectFragment>> codeLsit) throws IOException {
        int index = -1;
        for (ArrayList<ObjectFragment> c : codeLsit) {
            index++;
            if (c.get(c.size() - 1).getIsConnected()) {
                Graph g = objectType.generateGraphAddElabel(c, id);
                if (objectType.isCanonical(g, c)) {
                    // print(c,true);//output g
                    g.writeGraph2GfuAddeLabel(bw);
                    id++;
                    if (c.size() == finish) {
                        continue;
                    }
                    final int depth = c.size() - 1;
                    final byte vLabel = c.get(depth).getVlabel();
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeLsit, index, depth,vLabel);
                    final List<ArrayList<ObjectFragment>> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(c, M2, i));
                        }
                    }
                    
                    anotherList = null;
                    g = null;
                    enumarateWithAcGM(childrenOfM1);
                }
            }
        }
    }

    private static ArrayList<ObjectFragment> getChildrenOfM1(ArrayList<ObjectFragment> c, ObjectFragment M2,
            byte eLabel) throws IOException {
        byte[] eLabels = new byte[c.size()];

        final int edges =  eLabel != 0 ? M2.getEdges()+1 : M2.getEdges();
        final boolean isConnected = edges != 0 ? true : false;

        System.arraycopy(M2.getelabel(), 0, eLabels, 0, c.size() - 1);

        eLabels[c.size() - 1] = eLabel;

        final ObjectFragment leg = objectType.generateCodeFragment(M2.getVlabel(), eLabels, isConnected, edges);
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


    private static ArrayList<ObjectFragment> getAnotherList(
            List<ArrayList<ObjectFragment>> codeLsit,int index, int depth,byte vLabel) {

        anotherList.clear();

        ObjectFragment c;
        //兄の中で非連結なフラグメントのみ追加
        for (int i = 0; i < index; i++) {
            c =codeLsit.get(i).get(depth); 
            if(c.getIsConnected())
                continue;
            anotherList.add(c);
        }
        //自身を含めて弟のフラグメントを追加
        for (int i = index, len = codeLsit.size(); i < len; i++) {
            anotherList.add(codeLsit.get(i).get(depth));
        }
        return anotherList;
    }

    // private static void enumarateWithAcGM(List<ArrayList<ObjectFragment>> codeLsit) throws IOException {
    //     int index = -1;
    //     for (ArrayList<ObjectFragment> c : codeLsit) {
    //         index++;
    //         if (c.get(c.size() - 1).getIsConnected()) {
    //             Graph g = objectType.generateGraphAddElabel(c, id);
    //             if (objectType.isCanonical(g, c)) {
    //                 // print(c,true);//output g
    //                 g.writeGraph2GfuAddeLabel(bw);
    //                 id++;
    //                 if (c.size() == finish) {
    //                     continue;
    //                 }
    //                 final byte lastVlabel = c.get(c.size() - 1).getVlabel();
    //                 final int depth = c.size() - 1;
    //                 ArrayList<ObjectFragment> anotherList = getAnotherList(codeLsit, lastVlabel, index, depth);
    //                 List<ArrayList<ObjectFragment>> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * lastVlabel);
    //                 for (ObjectFragment M2 : anotherList) {
    //                     for (byte i = eLabelNum; i >= 0; i--) {
    //                         for (byte j = 0; j <= lastVlabel; j++) {
    //                             childrenOfM1.add(getChildrenOfM1(c, M2, j, i));
    //                         }
    //                     }
    //                 }
    //                 anotherList = null;
    //                 g = null;
    //                 enumarateWithAcGM(childrenOfM1);
    //             }
    //         }
    //     }
    // }
    // private static ArrayList<ObjectFragment> getAnotherList(
    //         List<ArrayList<ObjectFragment>> codeLsit, byte vLabel, int index, int depth) {
    //     anotherList.clear();
    //     ArrayList<ObjectFragment> code;

    //     for (int i = index, len = codeLsit.size(); i < len; i++) {
    //         code = codeLsit.get(i);
    //         if (vLabel != code.get(depth).getVlabel())
    //             continue;
    //         anotherList.add(code.get(depth));
    //     }

    //     return anotherList;
    // }

    // private static ArrayList<ObjectFragment> getChildrenOfM1(ArrayList<ObjectFragment> c, ObjectFragment M2,
    //         byte vLabel, byte eLabel) throws IOException {
    //     byte[] eLabels = new byte[c.size()];
    //     final boolean isConnected = (eLabel != 0 || M2.getIsConnected()) ? true : false;

    //     System.arraycopy(M2.getelabel(), 0, eLabels, 0, c.size() - 1);

    //     eLabels[c.size() - 1] = eLabel;

    //     ObjectFragment leg = objectType.generateCodeFragment(vLabel, eLabels, isConnected, 0);
    //     ArrayList<ObjectFragment> child = new ArrayList<>(c);
    //     child.add(leg);

    //     if (filter) {// 未完成
    //         if (isChildCodeExist(child)) {
    //             return null;
    //         } else {
    //             return child;
    //         }
    //     } else {
    //         return child;
    //     }
    // }



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

    static void removeResultDirectory(){
        // 削除したいディレクトリのパス
        Path dir = Paths.get("result");

        // ディレクトリ内のすべてのファイルを削除
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file); // ファイルを削除
                    return FileVisitResult.CONTINUE; // 次のファイルへ
                }
            });
            System.out.println("Files deleted successfully.");
        } catch (IOException e) {
            System.err.println("Error deleting files: " + e.getMessage());
        }
    
    }
}
