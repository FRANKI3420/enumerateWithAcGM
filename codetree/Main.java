package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import codetree.core.*;
import codetree.vertexBased.AcgmCode;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;


class Main {
    private static ObjectType objectType = new AcgmCode();
    private static int id = 0;
    private static BufferedWriter bw;
    private static BufferedWriter bw2;
    private static BufferedWriter bw3;
    private static byte sigma;
    private static byte eLabelNum;
    private static int finish;
    private static final boolean runPython = false;
    private static byte maxVlabel;
    static {
        try {
            bw = Files.newBufferedWriter(
                Paths.get("output.gfu")
            );
            bw2 = Files.newBufferedWriter(
                Paths.get("outputParallel.gfu")
            );
            bw3 = Files.newBufferedWriter(
                Paths.get("outputAcGMcode.txt")
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // OperateResult();
        sigma = 1;
        eLabelNum = 1;
        finish = 6;
        final boolean parallel = true;
        System.out.println("|V|<=" + finish + " |Σ|=" + sigma + " eLabelNum=" +eLabelNum);

        if(!parallel){
            try {
                startEnumarate();
                bw.close();
                bw2.close();
                bw3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                startEnumarateParallel();
                bw.close();
                bw2.close();
                bw3.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (runPython) {
            RunPythonFromJava();
        }
    }
    
   

        
    private static void startEnumarate() throws IOException {
        try {
            long start = System.nanoTime();
            ArrayList<ObjectFragment> codeList = objectType.computeCanonicalCode(sigma);
            maxVlabel = (byte) (sigma-1);
            System.out.println("シングルスレッド");
            enumarateWithAcGM(codeList,new ArrayList<>());
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (System.nanoTime() - start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startEnumarateParallel() throws IOException {
        long start = 0;
        //固定サイズのスレッドプール作成
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            start = System.nanoTime();
            id =0;
            System.out.println("並列処理:コア数 "+Runtime.getRuntime().availableProcessors());
            start = System.nanoTime();
            ArrayList<ObjectFragment> codeList =  objectType.computeCanonicalCode(sigma);
            maxVlabel = (byte) (sigma-1);
            enumarateWithAcGMParallel(executorService, codeList,new ArrayList<>()).join();
        } finally {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                e.printStackTrace();
            }
            // MergeTextFiles();
            start = System.nanoTime() - start;
            System.out.println("実行時間：" + (start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);   
        }
    }

    private static CompletableFuture<Void> enumarateWithAcGMParallel(ExecutorService executorService,ArrayList<ObjectFragment>codeList,ArrayList<ObjectFragment>pastFragments){
        List<CompletableFuture<Void>> tasks = new ArrayList<>();//非同期タスクの管理
        for(int index = 0,len=codeList.size();index<len;index++){
            final ObjectFragment c= codeList.get(index);
            if (c.getIsConnected()) {
                ArrayList<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
                nowFragments.add(c);
                Graph g = objectType.generateGraphAddElabel(nowFragments, id);
                if (objectType.isCanonical(g, nowFragments)) {
                    synchronized (bw2) {
                        g.writeGraph2GfuAddeLabel(bw2); 
                        id++;
                        g=null;
                    }
                    if (nowFragments.size() == finish) {
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index,c.getVlabel());
                    ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    anotherList = null;
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
                    System.out.println("\n現在の実行中タスク数: " + threadPoolExecutor.getActiveCount());
                    System.out.println("現在キューに待機しているタスク数: " + threadPoolExecutor.getQueue().size());
                    System.out.println("現在のスレッドプール内のすべてのスレッドの数: " + threadPoolExecutor.getPoolSize());
                    tasks.add(CompletableFuture.supplyAsync(() -> {
                        return enumarateWithAcGMParallel(executorService, childrenOfM1, nowFragments);
                    }, executorService).thenComposeAsync(future -> future));
                }
            }
            codeList.set(index, null);
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static void enumarateWithAcGM(ArrayList<ObjectFragment> codeList,ArrayList<ObjectFragment>pastFragments) throws IOException {
        for(int index = 0,len=codeList.size();index<len;index++){
            final ObjectFragment c= codeList.get(index);
            if (c.getIsConnected()) {
                pastFragments.add(c);
                // print(pastFragments, true);
                Graph g = objectType.generateGraphAddElabel(pastFragments, id);
                if(objectType.isCanonical(g, pastFragments)) {
                    g.writeGraph2GfuAddeLabel(bw);
                    g=null;
                    id++;
                    if (pastFragments.size() == finish) {
                        pastFragments.remove(pastFragments.size() - 1);
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, c.getVlabel());
                    ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    anotherList=null;
                    enumarateWithAcGM(childrenOfM1,pastFragments);
                }
                pastFragments.remove(pastFragments.size() - 1);
            }
            codeList.set(index, null);
        }
    }

    private static ObjectFragment getChildrenOfM1(final ObjectFragment M2,final byte eLabel){
        final int depth = M2.getelabel().length+1;
        byte[] eLabels = new byte[M2.getelabel().length+1];
        
        boolean isConnected;
        if(depth>1){
            isConnected = eLabel>0 || M2.getIsConnected() ? true : false;
        }else{
            isConnected = eLabel>0 ? true:false;
        }

        System.arraycopy(M2.getelabel(), 0, eLabels, 0, depth - 1);

        eLabels[depth-1] = eLabel;

        return objectType.generateCodeFragment(M2.getVlabel(), eLabels, isConnected);

    }


    private static ArrayList<ObjectFragment> getAnotherList(
            final List<ObjectFragment> codeList,final int index,final byte vLabel) {

        final int len = codeList.size();
        ArrayList<ObjectFragment>anotherList = new ArrayList<>();

        if(sigma>1 && maxVlabel!=vLabel){
            // 兄の中で非連結なフラグメントのみ追加
            for(ObjectFragment c : codeList){
                if(c.getIsConnected())
                    continue;
                anotherList.add(c);   
            }
        }
        
        //自身を含めて弟のフラグメントを追加
        for (int i = index; i < len; i++) {
            anotherList.add(codeList.get(i));
        }
        return anotherList;
    }

    private static void print(List<ObjectFragment> code, boolean output) throws IOException {// AcGMコード可視化
        for (ObjectFragment c : code) {
            if (output) {
                System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            } else {
                bw3.write(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            }
        }

        if (output) {
            System.out.println();
        } else {
            bw3.write("\n");
            bw3.flush();
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


    // private static CompletableFuture<Void> enumarateWithAcGMParallel(ExecutorService executorService,ArrayList<ObjectFragment>codeList,ArrayList<ObjectFragment>pastFragments){
    //     List<CompletableFuture<Void>> tasks = new ArrayList<>();
    //     for(int index = 0,len=codeList.size();index<len;index++){
    //         final ObjectFragment c= codeList.get(index);
    //         if (c.getIsConnected()) {
    //             ArrayList<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
    //             nowFragments.add(c);
    //             Graph g = objectType.generateGraphAddElabel(nowFragments, 0);
    //             if (objectType.isCanonical(g, nowFragments)) {
    //                 try (BufferedWriter writer = new BufferedWriter(new FileWriter("result\\output_" +  Thread.currentThread().getId() + ".gfu",true))) {
    //                     g.writeGraph2GfuAddeLabel(writer);
    //                     writer.close();
    //                 } catch (Exception e) {
    //                     e.printStackTrace();
    //                 }
    //                 if (nowFragments.size() == finish) {
    //                     codeList.set(index, null);
    //                     continue;
    //                 }
    //                 ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index,c.getVlabel());
    //                 ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
    //                 for (ObjectFragment M2 : anotherList) {
    //                     for (byte i = eLabelNum; i >= 0; i--) {
    //                         childrenOfM1.add(getChildrenOfM1(M2, i));
    //                     }
    //                 }
    //                 anotherList = null;
    //                 tasks.add(CompletableFuture.supplyAsync(() -> {
    //                     // try (BufferedWriter writer = new BufferedWriter(new FileWriter("result\\output_" +  Thread.currentThread().getId() + ".gfu",true))) {
    //                     //     g.writeGraph2GfuAddeLabel(writer);
    //                     // } catch (Exception e) {
    //                     //     e.printStackTrace();
    //                     // }
    //                     return enumarateWithAcGMParallel(executorService, childrenOfM1, nowFragments);
    //                 }, executorService).thenComposeAsync(future -> future));
    //             }
    //         }
    //         codeList.set(index, null);
    //     }
    //     return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    // }

    //  private static void OperateResult() {
    //     File directory = new File("result");

    //     if (!directory.exists())directory.mkdir();

    //     if (directory.exists() && directory.isDirectory()) {
    //         File[] files = directory.listFiles();

    //         if (files != null) {
    //             for (File file : files) {
    //                 if (file.isFile()) {
    //                     file.delete();
    //                 }
    //             }
    //         }
    //     }
    // }

    // private static void MergeTextFiles() {
    //     // resultフォルダのパス
    //     File folder = new File("result");

    //     // 結果をまとめるファイル
    //     File outputFile = new File("merged_output.txt");

    //     try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
    //         // resultフォルダ内のすべてのtxtファイルを取得
    //         File[] files = folder.listFiles((dir, name) -> name.endsWith(".gfu"));
    //         if (files != null) {
    //             // 各ファイルの内容を順番に読み込んで書き込む
    //             for (File file : files) {
    //                 try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
    //                     String line;
    //                     while ((line = reader.readLine()) != null) {
    //                         if(line.startsWith("#")){
    //                             id++;
    //                         }
    //                         writer.write(line);
    //                         writer.newLine(); // 改行
    //                     }
    //                 } catch (IOException e) {
    //                     System.err.println("Error reading file: " + file.getName());
    //                     e.printStackTrace();
    //                 }
    //             }
    //         }

    //         System.out.println("Files have been successfully merged into " + outputFile.getAbsolutePath());
    //     } catch (IOException e) {
    //         System.err.println("Error writing the merged file.");
    //         e.printStackTrace();
    //     }
    // }