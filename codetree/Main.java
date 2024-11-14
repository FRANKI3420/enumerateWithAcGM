package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import codetree.core.*;
import codetree.vertexBased.AcgmCode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class Main {
    private static ObjectType objectType = new AcgmCode();
    private static int id = 0;
    private static int id2 = 0;
    private static byte maxVlabel;
    private static long maxMemoryUsed = 0;
    private static List<CompletableFuture<Void>> singleTasks = new ArrayList<>();
    private static int singleThreadCount = 0;
    private static int multiThreadCount = 0;
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static BufferedWriter bw;
    private static BufferedWriter bw2;
    private static BufferedWriter bw3;
    static {
        try {
            bw = Files.newBufferedWriter(
                    Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(
                    Paths.get("outputParallel.gfu"));
            bw3 = Files.newBufferedWriter(
                    Paths.get("outputAcGMcode.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final boolean RUN_PYTHON = false;
    private static byte SIGMA = 1;
    private static byte ELABELNUM = 1;
    private static int FINISH = 8;
    private static double PARAM = 500;// シングルスレッドとマルチスレッドの割合を決める(調整難)

    public static void main(String[] args) {
        final boolean PARALLEL = false;
        final boolean SINGLE_And_PARALLEL = true;
        System.out.println("|V|<=" + FINISH + " |Σ|=" + SIGMA + " ELABELNUM=" + ELABELNUM);

        try {
            if (!PARALLEL) {
                startEnumarate();
            } else {
                startEnumarateParallel(SINGLE_And_PARALLEL);
            }
            fileClose();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double maxMemoryUsedInMB = maxMemoryUsed
                / (1024.0 * 1024.0);
        System.out.println(String.format("最大メモリ使用量: %.1f MB", maxMemoryUsedInMB));

        if (RUN_PYTHON) {
            RunPythonFromJava();
        }
    }

    private static void startEnumarate() throws IOException {
        try {
            long start = System.nanoTime();
            ArrayList<ObjectFragment> codeList = objectType.computeCanonicalCode(SIGMA);
            maxVlabel = (byte) (SIGMA - 1);
            System.out.println("シングルスレッド");
            enumarateWithAcGM(codeList, new ArrayList<>());
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間: " + String.format("%.2f", (double) (System.nanoTime() - start) / 1000 / 1000 / 1000) + "s");

            System.out.println("ans num: " + id2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startEnumarateParallel(boolean singleAndparallel) throws IOException {
        // 固定サイズのスレッドプール作成
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long start = System.nanoTime();
        try {
            ArrayList<ObjectFragment> codeList = objectType.computeCanonicalCode(SIGMA);
            maxVlabel = (byte) (SIGMA - 1);
            System.out.println("コア数 " + AVAILABLE_PROCESSORS);
            if (singleAndparallel) {
                System.out.println("マルチスレッド&シングルスレッド\n閾値=" + PARAM);
                bw = Files.newBufferedWriter(Paths.get("outputSingle.gfu"));
                enumarateWithAcGMSingleAndParallel(executorService, codeList, new ArrayList<>()).join();
                CompletableFuture.allOf(singleTasks.toArray(new CompletableFuture[0])).join();
                bw.flush();
                bw2.flush();
                id = 0;
                id2 = 0;
                mergeFiles(new String[] { "outputSingle.gfu", "outputParallel.gfu" }, "output.gfu");
                int totalRuns = singleThreadCount + multiThreadCount;
                double singleThreadPercentage = (double) singleThreadCount / totalRuns * 100;
                double multiThreadPercentage = (double) multiThreadCount / totalRuns * 100;
                System.out.println("シングルスレッドの実行回数: " + singleThreadCount);
                System.out.println("マルチスレッドの実行回数: " + multiThreadCount);
                System.out.println(String.format("シングルスレッドの割合: %.2f%%",
                        singleThreadPercentage));
                System.out.println(String.format("マルチスレッドの割合: %.2f%%",
                        multiThreadPercentage));
            } else {
                System.out.println("マルチスレッド");
                enumarateWithAcGMParallel(executorService, codeList, new ArrayList<>()).join();
            }
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
                    "実行時間：" + String.format("%.1f", (double) (start) / 1000 /
                            1000 / 1000) + "s");
            System.out.println("ans num: " + (id + id2));
            if (singleAndparallel) {
                System.out.println("ans num(single thread): " + (id2));
                System.out.println("ans num(multi thread): " + (id));
                System.out.println("シングルスレッドの発見解割合: " + String.format("%.2f%%", (double) id2 / (id + id2) * 100));
                System.out.println("マルチスレッドスレッドの発見解割合: " + String.format("%.2f%%", (double) id / (id + id2) * 100));
            }
        }
    }

    private static CompletableFuture<Void> enumarateWithAcGMSingleAndParallel(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();// 非同期タスクの管理
        UpdatamaxMemoryUsed();
        multiThreadCount++;
        for (int index = 0, len = codeList.size(); index < len; index++) {
            final ObjectFragment c = codeList.get(index);
            if (c.getIsConnected()) {
                ArrayList<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
                nowFragments.add(c);
                Graph g = objectType.generateGraphAddElabel(nowFragments, 0);
                if (objectType.isCanonical(g, nowFragments)) {
                    synchronized (bw2) {
                        g.writeGraph2GfuAddeLabel(bw2);
                        id++;
                        g = null;
                    }
                    if (nowFragments.size() == FINISH) {
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, c.getVlabel());
                    ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * ELABELNUM * SIGMA);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = ELABELNUM; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    anotherList = null;
                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;

                    if (shouldRunInParallel(threadPoolExecutor)) {
                        // System.out.println("parallel");
                        tasks.add(CompletableFuture.supplyAsync(() -> {
                            return enumarateWithAcGMSingleAndParallel(executorService, childrenOfM1, nowFragments);
                        }, executorService).thenComposeAsync(future -> future));
                    } else {
                        // System.out.println("single");
                        singleTasks.add(CompletableFuture.runAsync(() -> {
                            try {
                                // 非同期で再帰処理を開始
                                enumarateWithAcGM(childrenOfM1, nowFragments);
                            } catch (IOException e) {
                                throw new CompletionException(e);
                            }
                        }, executorService));
                    }

                }
            }
            codeList.set(index, null);
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static CompletableFuture<Void> enumarateWithAcGMParallel(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();// 非同期タスクの管理
        UpdatamaxMemoryUsed();
        for (int index = 0, len = codeList.size(); index < len; index++) {
            final ObjectFragment c = codeList.get(index);
            if (c.getIsConnected()) {
                ArrayList<ObjectFragment> nowFragments = new ArrayList<ObjectFragment>(pastFragments);
                nowFragments.add(c);
                Graph g = objectType.generateGraphAddElabel(nowFragments, 0);
                if (objectType.isCanonical(g, nowFragments)) {
                    synchronized (bw2) {
                        g.writeGraph2GfuAddeLabel(bw2);
                        id++;
                        g = null;
                    }
                    if (nowFragments.size() == FINISH) {
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, c.getVlabel());
                    ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * ELABELNUM * SIGMA);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = ELABELNUM; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    anotherList = null;

                    tasks.add(CompletableFuture.supplyAsync(() -> {
                        return enumarateWithAcGMParallel(executorService, childrenOfM1, nowFragments);
                    }, executorService).thenComposeAsync(future -> future));

                }
            }
            codeList.set(index, null);
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static void enumarateWithAcGM(ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments)
            throws IOException {
        UpdatamaxMemoryUsed();
        singleThreadCount++;
        for (int index = 0, len = codeList.size(); index < len; index++) {
            final ObjectFragment c = codeList.get(index);
            if (c.getIsConnected()) {
                pastFragments.add(c);
                // print(pastFragments, true);
                Graph g = objectType.generateGraphAddElabel(pastFragments, 0);
                if (objectType.isCanonical(g, pastFragments)) {
                    g.writeGraph2GfuAddeLabel(bw);
                    g = null;
                    id2++;
                    if (pastFragments.size() == FINISH) {
                        pastFragments.remove(pastFragments.size() - 1);
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, c.getVlabel());
                    ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() * ELABELNUM * SIGMA);
                    for (ObjectFragment M2 : anotherList) {
                        for (byte i = ELABELNUM; i >= 0; i--) {
                            childrenOfM1.add(getChildrenOfM1(M2, i));
                        }
                    }
                    anotherList = null;
                    enumarateWithAcGM(childrenOfM1, pastFragments);
                }
                pastFragments.remove(pastFragments.size() - 1);
            }
            codeList.set(index, null);
        }
    }

    private static boolean shouldRunInParallel(ThreadPoolExecutor executor) {
        // 現在のアクティブタスク数を取得
        int activeTasks = executor.getActiveCount();
        // キューの待機タスク数を取得
        int queuedTasks = executor.getQueue().size();

        // System.out.println("現在のアクティブタスク数: " + activeTasks + ", キュー内のタスク数: " +
        // queuedTasks);
        // System.out.println("\n現在の実行中タスク数: " + threadPoolExecutor.getActiveCount());
        // System.out.println("現在キューに待機しているタスク数: " +
        // threadPoolExecutor.getQueue().size());
        // System.out.println("現在のスレッドプール内のすべてのスレッドの数: " +
        // threadPoolExecutor.getPoolSize());

        // アクティブタスク数やキュー内タスク数が閾値を超える場合は並列実行を避ける
        // System.out.println(activeTasks + queuedTasks);
        return (activeTasks + queuedTasks) < AVAILABLE_PROCESSORS * PARAM;
    }

    private static ObjectFragment getChildrenOfM1(final ObjectFragment M2, final byte eLabel) {
        final int depth = M2.getelabel().length + 1;
        byte[] eLabels = new byte[M2.getelabel().length + 1];

        boolean isConnected;
        if (depth > 1) {
            isConnected = eLabel > 0 || M2.getIsConnected() ? true : false;
        } else {
            isConnected = eLabel > 0 ? true : false;
        }

        System.arraycopy(M2.getelabel(), 0, eLabels, 0, depth - 1);

        eLabels[depth - 1] = eLabel;

        return objectType.generateCodeFragment(M2.getVlabel(), eLabels, isConnected);
    }

    private static ArrayList<ObjectFragment> getAnotherList(
            final List<ObjectFragment> codeList, final int index, final byte vLabel) {

        final int len = codeList.size();
        ArrayList<ObjectFragment> anotherList = new ArrayList<>();

        if (SIGMA > 1 && maxVlabel != vLabel) {
            // 兄の中で非連結なフラグメントのみ追加
            for (ObjectFragment c : codeList) {
                if (c.getIsConnected())
                    continue;
                anotherList.add(c);
            }
        }

        // 自身を含めて弟のフラグメントを追加
        for (int i = index; i < len; i++) {
            anotherList.add(codeList.get(i));
        }
        return anotherList;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    private static void memoryCondition() {
        Runtime runtime = Runtime.getRuntime();

        // Javaプロセスに割り当てられている総メモリ量
        long totalMemory = runtime.totalMemory();

        // 利用可能な最大メモリ量
        long maxMemory = runtime.maxMemory();

        // 現在使用中のメモリ量
        long usedMemory = totalMemory - runtime.freeMemory();

        // 使用可能な残りメモリ量
        long availableMemory = maxMemory - usedMemory;

        System.out.println("Total Memory: " + (double) totalMemory / 1024 / 1024 / 1024 + " GB");
        System.out.println("Max Memory: " + (double) maxMemory / 1024 / 1024 / 1024 + " GB");
        System.out.println("Used Memory: " + (double) usedMemory / 1024 / 1024 / 1024 + " GB");
        System.out.println("Available Memory: " + (double) availableMemory / 1024 / 1024 / 1024 + " GB");
    }

    @SuppressWarnings("unused")
    private static void UpdatamaxMemoryUsed() {
        Runtime runtime = Runtime.getRuntime();

        long currentMemoryUsed = runtime.totalMemory() - runtime.freeMemory();

        maxMemoryUsed = Math.max(maxMemoryUsed, currentMemoryUsed);
    }

    private static void mergeFiles(String[] inputFiles, String outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
            int index = 0;
            for (String inputFile : inputFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("#")) {
                            if (index == 0) {
                                id2++;
                            } else {
                                id++;
                            }
                        }
                        writer.write(line);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + inputFile);
                    e.printStackTrace();
                }
                index++;
            }
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + outputFile);
            e.printStackTrace();
        }
    }

    private static void fileClose() throws IOException {
        bw.close();
        bw2.close();
        bw3.close();
    }

    static void RunPythonFromJava() {
        try {
            // Pythonスクリプトを実行する
            ProcessBuilder pb = new ProcessBuilder("python", "draw_graph_edgeLabel.py", String.valueOf(SIGMA),
                    String.valueOf(ELABELNUM));
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

// private static CompletableFuture<Void>
// enumarateWithAcGMParallel2(ExecutorService executorService,
// ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments)
// {
// List<CompletableFuture<Void>> tasks = new ArrayList<>();
// for (int index = 0, len = codeList.size(); index < len; index++) {
// final ObjectFragment c = codeList.get(index);
// if (c.getIsConnected()) {
// ArrayList<ObjectFragment> nowFragments = new
// ArrayList<ObjectFragment>(pastFragments);
// nowFragments.add(c);
// Graph g = objectType.generateGraphAddElabel(nowFragments, 0);
// if (objectType.isCanonical(g, nowFragments)) {
// try (BufferedWriter writer = new BufferedWriter(
// new FileWriter("result\\output_" + Thread.currentThread().getId() + ".gfu",
// true))) {
// g.writeGraph2GfuAddeLabel(writer);
// } catch (Exception e) {
// System.out.println(nowFragments.size());
// e.printStackTrace();
// }

// if (nowFragments.size() == FINISH) {
// codeList.set(index, null);
// continue;
// }
// ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index,
// c.getVlabel());
// ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>(anotherList.size() *
// ELABELNUM * SIGMA);
// for (ObjectFragment M2 : anotherList) {
// for (byte i = ELABELNUM; i >= 0; i--) {
// childrenOfM1.add(getChildrenOfM1(M2, i));
// }
// }
// anotherList = null;
// tasks.add(CompletableFuture.supplyAsync(() -> {
// // try (BufferedWriter writer = new BufferedWriter(new
// // FileWriter("result\\output_" + Thread.currentThread().getId() +
// // ".gfu",true))) {
// // g.writeGraph2GfuAddeLabel(writer);
// // } catch (Exception e) {
// // e.printStackTrace();
// // }
// return enumarateWithAcGMParallel2(executorService, childrenOfM1,
// nowFragments);
// }, executorService).thenComposeAsync(future -> future));
// }
// }
// codeList.set(index, null);
// }
// return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
// }

// private static void MergeTextFiles() {
// // resultフォルダのパス
// File folder = new File("result");

// // 結果をまとめるファイル
// File outputFile = new File("merged_output.txt");

// try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile)))
// {
// // resultフォルダ内のすべてのtxtファイルを取得
// File[] files = folder.listFiles((dir, name) -> name.endsWith(".gfu"));
// if (files != null) {
// // 各ファイルの内容を順番に読み込んで書き込む
// for (File file : files) {
// try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
// String line;
// while ((line = reader.readLine()) != null) {
// if (line.startsWith("#")) {
// id++;
// }
// writer.write(line);
// writer.newLine(); // 改行
// }
// } catch (IOException e) {
// System.err.println("Error reading file: " + file.getName());
// e.printStackTrace();
// }
// }
// }

// System.out.println("Files have been successfully merged into " +
// outputFile.getAbsolutePath());
// } catch (IOException e) {
// System.err.println("Error writing the merged file.");
// e.printStackTrace();
// }
// }

// private static void OperateResult() {
// File directory = new File("result");

// if (!directory.exists())
// directory.mkdir();

// if (directory.exists() && directory.isDirectory()) {
// File[] files = directory.listFiles();

// if (files != null) {
// for (File file : files) {
// if (file.isFile()) {
// file.delete();
// }
// }
// }
// }
// }

// static void removeResultDirectory() {
// Path dir = Paths.get("result");

// try {
// Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
// @Override
// public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws
// IOException {
// Files.delete(file); // ファイルを削除
// return FileVisitResult.CONTINUE; // 次のファイルへ
// }
// });
// System.out.println("Files deleted successfully.");
// } catch (IOException e) {
// System.err.println("Error deleting files: " + e.getMessage());
// }
// }