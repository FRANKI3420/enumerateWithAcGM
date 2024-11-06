package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import codetree.common.*;
import codetree.core.*;
import codetree.vertexBased.AcgmCode;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;



class Main {
    private static ObjectType objectType = new AcgmCode();
    private static int id = 0;
    private static BufferedWriter bw;
    private static BufferedWriter bw2;
    private static CodeTree tree;
    private static byte sigma;
    private static byte eLabelNum;
    private static int finish;
    private static final boolean runPython = false;
    private static final boolean filter = false;
    private static byte maxVlabel;


    public static void main(String[] args) {
        sigma = 1;
        eLabelNum = 1;
        finish = 9;
        final boolean parallel = true;

        if(!parallel){
            try {
                startEnumarate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                startEnumarateParallel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (runPython) {
            RunPythonFromJava();
        }
    }

    private static void startEnumarate() throws IOException {
        long start = 0;
        try {
            bw = Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            System.out.println("|V|<=" + finish + " |Σ|=" + sigma + " eLabelNum=" +
            eLabelNum);
            start = System.nanoTime();
            List<ObjectFragment> codeList = objectType.computeCanonicalCode(sigma);
            maxVlabel = (byte) (sigma-1);
            System.out.println("逐次処理");
            enumarateWithAcGM(codeList,new ArrayList<>());
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (System.nanoTime() - start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);
            bw.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void startEnumarateParallel() throws IOException {
        long start = 0;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            bw = Files.newBufferedWriter(Paths.get("outputParallel.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            System.out.println("|V|<=" + finish + " |Σ|=" + sigma + " eLabelNum=" +
            eLabelNum);
            start = System.nanoTime();
            id =0;
            System.out.println("並列処理:コア数 "+Runtime.getRuntime().availableProcessors());
            start = System.nanoTime();
            List<ObjectFragment> codeList =  objectType.computeCanonicalCode(sigma);
            maxVlabel = (byte) (sigma-1);
            enumarateWithAcGMParallel(executorService, codeList,new ArrayList<>()).join();
        } finally {
            start = System.nanoTime() - start;
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                e.printStackTrace();
            }
            System.out.println("実行時間：" + (start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間：" + String.format("%3f", (double) (start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + id);   
            if (bw != null) {
                try {
                    bw.close(); // 最後に閉じる
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static CompletableFuture<Void> enumarateWithAcGMParallel(ExecutorService executorService,List<ObjectFragment>codeList,List<ObjectFragment>pastFragments){
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        for(int index = 0,len=codeList.size();index<len;index++){
            final ObjectFragment c= codeList.get(index);
            if (c.getIsConnected()) {
                List<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
                nowFragments.add(c);
                final Graph g = objectType.generateGraphAddElabel(nowFragments, id);
                if (objectType.isCanonical(g, nowFragments)) {
                    synchronized (bw) {
                        g.writeGraph2GfuAddeLabel(bw); 
                        id++;
                    }
                    if (nowFragments.size() == finish) {
                        continue;
                    }
                    final byte vLabel = c.getVlabel();
                    final List<ObjectFragment> anotherList = getAnotherList(codeList, index,vLabel);
                    final List<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                
                    tasks.add(CompletableFuture.supplyAsync(() -> {
                        return enumarateWithAcGMParallel(executorService, childrenOfM1, nowFragments);
                    }, executorService).thenComposeAsync(future -> future));
                }
            }
        }
        
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static void enumarateWithAcGM(List<ObjectFragment> codeList,List<ObjectFragment>pastFragments) throws IOException {
        for(int index = 0,len=codeList.size();index<len;index++){
            final ObjectFragment c= codeList.get(index);
            if (c.getIsConnected()) {
                List<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
                nowFragments.add(c);
                Graph g = objectType.generateGraphAddElabel(nowFragments, id);
                if(objectType.isCanonical(g, nowFragments)) {
                    g.writeGraph2GfuAddeLabel(bw);
                    id++;
                    if (nowFragments.size() == finish) {
                        continue;
                    }
                    final byte vLabel = c.getVlabel();
                    final ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index,vLabel);
                    final List<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * eLabelNum * sigma);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = eLabelNum; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    enumarateWithAcGM(childrenOfM1,nowFragments);
                }
            }
        }
    }

    private static ObjectFragment getChildrenOfM1(ObjectFragment M2,byte eLabel){
        final int depth = M2.getelabel().length+1;
        byte[] eLabels = new byte[depth];
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
            List<ObjectFragment> codeList,int index,byte vLabel) {

        final int len = codeList.size();
        ArrayList<ObjectFragment>anotherList = new ArrayList<>(len);

        if(sigma>1 && maxVlabel!=vLabel){
            // 兄の中で非連結なフラグメントのみ追加
            ObjectFragment c;
            for (int i = 0; i < index; i++) {
                c =codeList.get(i); 
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
