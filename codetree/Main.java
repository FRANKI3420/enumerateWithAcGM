package codetree;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import codetree.core.*;
import codetree.vertexBased.AcgmCode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedDeque;

class Main {
    private static ObjectType objectType = new AcgmCode();
    private static final AtomicInteger activeTasks = new AtomicInteger(0); // タスクのカウント
    private static int id = 0;
    private static int id2 = 0;
    private static byte maxVlabel;
    private static long maxMemoryUsed = 0;
    private static List<CompletableFuture<Void>> singleTasks = new ArrayList<>();
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
                    Paths.get("memory.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final boolean RUN_PYTHON = false;
    private static byte SIGMA = 2;
    private static byte ELABELNUM = 1;
    private static int FINISH = 3;
    // シングルスレッドとマルチスレッドの割合を決める(調整難)
    private static double PARAM = 5000;// 8:300 9:5000

    public static void main(String[] args) {
        final boolean PARALLEL = false;
        final boolean SINGLE_And_PARALLEL = false;
        final boolean USING_STACK = true;
        System.out.println("|V|<=" + FINISH + " |Σ|=" + SIGMA + " ELABELNUM=" + ELABELNUM);
        try {
            if (!PARALLEL) {
                startEnumarate(USING_STACK);
            } else {
                startEnumarateParallel(SINGLE_And_PARALLEL, USING_STACK);
            }
            fileClose();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double maxMemoryUsedInMB = maxMemoryUsed
                / (1024.0 * 1024.0);
        System.out.println(String.format("最大メモリ使用量: %.1f MB", maxMemoryUsedInMB));
        double isCanonicalTime = (double) AcgmCode.isCanonicalTime / (1000 * 1000 * 1000);
        System.out.println(String.format("正準判定時間: %.1f s", isCanonicalTime));

        if (RUN_PYTHON) {
            RunPythonFromJava();
        }
    }

    private static void startEnumarate(boolean USING_STACK) throws IOException {
        try {
            long start = System.nanoTime();
            ArrayList<ObjectFragment> codeList = objectType.computeCanonicalCode(SIGMA);
            maxVlabel = (byte) (SIGMA - 1);
            System.out.println("シングルスレッド");
            if (USING_STACK) {
                System.out.println("スタック");
                enumerateWithAcGMUsingStack(codeList, new ArrayList<>());
            } else {
                System.out.println("再帰");
                enumarateWithAcGM(codeList, new ArrayList<>());
            }
            System.out.println("実行時間：" + (System.nanoTime() - start) / 1000 / 1000 +
                    "ms");
            System.out.println(
                    "実行時間: " + String.format("%.2f", (double) (System.nanoTime() - start) / 1000 / 1000 / 1000) + "s");

            System.out.println("ans num: " + id2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startEnumarateParallel(boolean SINGLE_And_PARALLEL, boolean USING_STACK) throws IOException {
        // 固定サイズのスレッドプール作成
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long start = System.nanoTime();
        try {
            ArrayList<ObjectFragment> codeList = objectType.computeCanonicalCode(SIGMA);
            maxVlabel = (byte) (SIGMA - 1);
            System.out.println("コア数 " + AVAILABLE_PROCESSORS);
            if (SINGLE_And_PARALLEL) {
                System.out.println("マルチスレッド&シングルスレッド\n閾値=" + PARAM);
                bw = Files.newBufferedWriter(Paths.get("outputSingle.gfu"));

                if (USING_STACK) {
                    System.out.println("スタック");
                    // enumerateWithAcGMSingleAndParallelUsingStack(executorService, codeList, new
                    // ArrayList<>()).join();
                    enumerateWithAcGMUsingForkJoin(codeList, new ArrayList<>());
                } else {
                    System.out.println("再帰");
                    enumarateWithAcGMSingleAndParallel(executorService, codeList, new ArrayList<>()).join();
                }
                CompletableFuture.allOf(singleTasks.toArray(new CompletableFuture[0])).join();
                bw.flush();
                bw2.flush();
                id = 0;
                id2 = 0;
                mergeFiles(new String[] { "outputSingle.gfu", "outputParallel.gfu" }, "output.gfu");
            } else {
                System.out.println("マルチスレッド");
                if (USING_STACK) {
                    System.out.println("スタック");
                    enumerateWithAcGMParallelUsingStack(executorService, codeList, new ArrayList<>()).join();
                    // enumerateWithAcGMUsingForkJoin(codeList, new ArrayList<>());
                } else {
                    System.out.println("再帰");
                    enumarateWithAcGMParallel(executorService, codeList, new ArrayList<>()).join();
                }
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
            if (SINGLE_And_PARALLEL) {
                System.out.println("ans num(single thread): " + (id2));
                System.out.println("ans num(multi thread): " + (id));
                System.out.println("シングルスレッドの発見解割合: " + String.format("%.2f%%", (double) id2 / (id + id2) * 100));
                System.out.println("マルチスレッドスレッドの発見解割合: " + String.format("%.2f%%", (double) id / (id + id2) * 100));
            }
        }
    }

    private static void enumarateWithAcGM(ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments)
            throws IOException {
        UpdatamaxMemoryUsed(codeList);
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
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());
                    codeList.set(index, null);
                    enumarateWithAcGM(childrenOfM1, pastFragments);
                }
                pastFragments.remove(pastFragments.size() - 1);
            }
        }
    }

    private static void enumerateWithAcGMUsingStack(ArrayList<ObjectFragment> codeList,
            ArrayList<ObjectFragment> pastFragments)
            throws IOException {
        Stack<ArrayList<ObjectFragment>> stack = new Stack<>();
        Stack<ArrayList<ObjectFragment>> pastFragmentsStack = new Stack<>();
        stack.push(codeList);
        pastFragmentsStack.push(new ArrayList<>(pastFragments));

        while (!stack.isEmpty()) {
            ArrayList<ObjectFragment> currentList = stack.pop();
            ArrayList<ObjectFragment> currentPastFragments = pastFragmentsStack.pop();
            UpdatamaxMemoryUsed(codeList);
            for (int index = 0, len = currentList.size(); index < len; index++) {
                final ObjectFragment c = currentList.get(index);
                if (c.getIsConnected()) {
                    currentPastFragments.add(c);
                    Graph g = objectType.generateGraphAddElabel(currentPastFragments, 0);
                    if (objectType.isCanonical(g, currentPastFragments)) {
                        print(currentPastFragments, true);
                        g.writeGraph2GfuAddeLabel(bw);
                        id2++;
                        if (currentPastFragments.size() == FINISH) {
                            currentPastFragments.remove(currentPastFragments.size() - 1);
                            currentList.set(index, null);
                            continue;
                        }
                        ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(currentList, index, c.getVlabel());
                        stack.push(childrenOfM1);
                        currentList.set(index, null);
                        pastFragmentsStack.push(new ArrayList<>(currentPastFragments));
                    }
                    currentPastFragments.remove(currentPastFragments.size() - 1);
                }
            }
        }
    }

    private static CompletableFuture<Void> enumarateWithAcGMParallel(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();// 非同期タスクの管理
        UpdatamaxMemoryUsed(codeList);
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
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());
                    codeList.set(index, null);
                    tasks.add(CompletableFuture.supplyAsync(() -> {
                        return enumarateWithAcGMParallel(executorService, childrenOfM1, nowFragments);
                    }, executorService).thenComposeAsync(future -> future));

                }
            }
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static class Frame {
        ArrayList<ObjectFragment> codeList;
        ArrayList<ObjectFragment> pastFragments;

        Frame(ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments) {
            this.codeList = new ArrayList<>(codeList);
            this.pastFragments = new ArrayList<>(pastFragments);
        }
    }

    public static CompletableFuture<Void> enumerateWithAcGMParallelUsingStack(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList,
            ArrayList<ObjectFragment> pastFragments) {
        ConcurrentLinkedDeque<Frame> stack = new ConcurrentLinkedDeque<>();
        stack.push(new Frame(codeList, pastFragments));

        return processStack(executorService, stack);
    }

    private static CompletableFuture<Void> processStack(ExecutorService executorService,
            ConcurrentLinkedDeque<Frame> stack) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        // スタックが空でないか、または実行中のタスクが存在する限りループ
        while (!stack.isEmpty() || activeTasks.get() > 0) {
            Frame frame = stack.poll();
            if (frame != null) {
                activeTasks.incrementAndGet(); // 新しいタスクを開始

                // フレームの処理を非同期に実行
                tasks.add(CompletableFuture.runAsync(() -> {
                    try {
                        ArrayList<ObjectFragment> currentCodeList = frame.codeList;
                        ArrayList<ObjectFragment> currentPastFragments = frame.pastFragments;
                        UpdatamaxMemoryUsed(currentCodeList);

                        for (int index = 0, len = currentCodeList.size(); index < len; index++) {
                            final ObjectFragment c = currentCodeList.get(index);
                            if (c != null && c.getIsConnected()) {
                                ArrayList<ObjectFragment> nowFragments = new ArrayList<>(currentPastFragments);
                                nowFragments.add(c);
                                Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                                if (objectType.isCanonical(g, nowFragments)) {
                                    synchronized (bw2) {
                                        g.writeGraph2GfuAddeLabel(bw2);
                                        id++;
                                    }

                                    if (nowFragments.size() == FINISH) {
                                        currentCodeList.set(index, null);
                                        continue;
                                    }

                                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(
                                            currentCodeList, index,
                                            c.getVlabel());

                                    stack.push(new Frame(childrenOfM1, nowFragments));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        activeTasks.decrementAndGet(); // タスクが完了したらカウントを減らす
                    }
                }, executorService));
            }
        }

        // すべてのタスクが完了するまで待機
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    public static void enumerateWithAcGMUsingForkJoin(ArrayList<ObjectFragment> codeList,
            ArrayList<ObjectFragment> pastFragments) throws IOException {
        ForkJoinPool pool = new ForkJoinPool(); // ForkJoinプールを作成
        pool.invoke(new EnumerationTask(codeList, pastFragments));
    }

    static class EnumerationTask extends RecursiveTask<Void> {
        private final ArrayList<ObjectFragment> currentList;
        private final ArrayList<ObjectFragment> currentPastFragments;

        EnumerationTask(ArrayList<ObjectFragment> currentList, ArrayList<ObjectFragment> currentPastFragments) {
            this.currentList = currentList;
            this.currentPastFragments = currentPastFragments;
        }

        @Override
        protected Void compute() {
            ArrayList<EnumerationTask> subTasks = new ArrayList<>();
            UpdatamaxMemoryUsed(currentList);

            for (int index = 0, len = currentList.size(); index < len; index++) {
                final ObjectFragment c = currentList.get(index);
                if (c == null || !c.getIsConnected())
                    continue;

                ArrayList<ObjectFragment> newPastFragments = new ArrayList<>(currentPastFragments);
                newPastFragments.add(c);

                Graph g = objectType.generateGraphAddElabel(newPastFragments, 0);
                if (objectType.isCanonical(g, newPastFragments)) {
                    synchronized (bw2) {
                        g.writeGraph2GfuAddeLabel(bw2);
                        id++;
                        g = null;
                    }

                    if (newPastFragments.size() == FINISH) {
                        continue;
                    }

                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(currentList, index, c.getVlabel());
                    EnumerationTask subTask = new EnumerationTask(childrenOfM1, newPastFragments);
                    subTasks.add(subTask);
                }
            }
            // サブタスクを並列実行
            invokeAll(subTasks);
            return null;
        }
    }

    public static CompletableFuture<Void> enumerateWithAcGMSingleAndParallelUsingStack(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList,
            ArrayList<ObjectFragment> pastFragments) {
        ConcurrentLinkedDeque<Frame> stack = new ConcurrentLinkedDeque<>();
        stack.push(new Frame(codeList, pastFragments)); // 初期フレームをスタックにプッシュ

        return processStackSingleAndParallel(executorService, stack);
    }

    private static CompletableFuture<Void> processStackSingleAndParallel(ExecutorService executorService,
            ConcurrentLinkedDeque<Frame> stack) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        // スタックが空でないか、または実行中のタスクが存在する限りループ
        while (!stack.isEmpty() || activeTasks.get() > 0) {
            Frame frame = stack.poll(); // スタックからフレームを取得
            if (frame != null) {
                activeTasks.incrementAndGet(); // 新しいタスクを開始
                // フレームの処理を非同期に実行
                tasks.add(CompletableFuture.runAsync(() -> {
                    try {
                        ArrayList<ObjectFragment> currentCodeList = frame.codeList;
                        ArrayList<ObjectFragment> currentPastFragments = frame.pastFragments;
                        UpdatamaxMemoryUsed(currentCodeList);

                        for (int index = 0, len = currentCodeList.size(); index < len; index++) {
                            final ObjectFragment c = currentCodeList.get(index);
                            if (c != null && c.getIsConnected()) {
                                ArrayList<ObjectFragment> nowFragments = new ArrayList<>(currentPastFragments);
                                nowFragments.add(c);
                                Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                                if (objectType.isCanonical(g, nowFragments)) {
                                    synchronized (bw2) {
                                        g.writeGraph2GfuAddeLabel(bw2);
                                        id++;
                                    }

                                    if (nowFragments.size() == FINISH) {
                                        currentCodeList.set(index, null);
                                        continue;
                                    }

                                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(currentCodeList, index,
                                            c.getVlabel());

                                    ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
                                    if (shouldRunInParallel(threadPoolExecutor)) {
                                        stack.push(new Frame(childrenOfM1, nowFragments));
                                    } else {
                                        enumerateWithAcGMUsingStack(childrenOfM1, nowFragments);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        activeTasks.decrementAndGet(); // タスクが完了したらカウントを減らす
                    }
                }, executorService));
            }
        }

        // すべてのタスクが完了するまで待機
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static CompletableFuture<Void> enumarateWithAcGMSingleAndParallel(ExecutorService executorService,
            ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> pastFragments) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();// 非同期タスクの管理
        UpdatamaxMemoryUsed(codeList);
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
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());

                    codeList.set(index, null);
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
        }
        return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
    }

    private static ObjectFragment getChildrenOfM1Fragment(final ObjectFragment M2, final byte eLabel) {
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

    private static ArrayList<ObjectFragment> getChildrenOfM1(ArrayList<ObjectFragment> codeList, int index,
            byte vlabel) {

        ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>();

        ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, vlabel);
        for (ObjectFragment M2 : anotherList) {
            for (byte i = ELABELNUM; i >= 0; i--) {
                childrenOfM1.add(getChildrenOfM1Fragment(M2, i));
            }
        }

        return childrenOfM1;

    }

    private static ArrayList<ObjectFragment> getAnotherList(
            final List<ObjectFragment> codeList, final int index, final byte vLabel) {

        final int len = codeList.size();
        ArrayList<ObjectFragment> anotherList = new ArrayList<>();

        if (SIGMA > 1 && maxVlabel != vLabel) {
            // 兄の中で非連結なフラグメントのみ追加
            for (int i = 0; i < index; i++) {
                ObjectFragment c = codeList.get(i);
                if (c == null || c.getIsConnected())
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

    private static boolean shouldRunInParallel(ThreadPoolExecutor executor) {
        // 現在のアクティブタスク数を取得
        int activeTasks = executor.getActiveCount();
        // キューの待機タスク数を取得
        int queuedTasks = executor.getQueue().size();

        // System.out.println("現在のアクティブタスク数: " + activeTasks + ", キュー内のタスク数: " +
        // queuedTasks);
        // System.out.println("\n現在の実行中タスク数: " + executor.getActiveCount());
        // System.out.println("現在キューに待機しているタスク数: " +
        // executor.getQueue().size());
        // System.out.println("現在のスレッドプール内のすべてのスレッドの数: " +
        // executor.getPoolSize());

        // アクティブタスク数やキュー内タスク数が閾値を超える場合は並列実行を避ける
        // System.out.println(activeTasks + queuedTasks);
        // return (activeTasks + queuedTasks) < AVAILABLE_PROCESSORS * PARAM;

        return queuedTasks < PARAM;
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

    static int memory_count = 0;

    @SuppressWarnings("unused") // kokokoko
    private static void UpdatamaxMemoryUsed(ArrayList<ObjectFragment> codeList) {
        Runtime runtime = Runtime.getRuntime();

        long currentMemoryUsed = runtime.totalMemory() - runtime.freeMemory();

        // synchronized (bw3) {
        // memory_count++;

        // if (memory_count % 1 == 0) {
        // try {
        // bw3.write(String.format("%.2f\n", (double) currentMemoryUsed / 1024 / 1024));
        // } catch (Exception e) {
        // System.err.println(e);
        // }
        // }
        // }

        maxMemoryUsed = Math.max(maxMemoryUsed, currentMemoryUsed);
        // System.out.println("max:" + maxMemoryUsed / 1024 / 1024);
        // System.out.println("now:" + currentMemoryUsed / 1024 / 1024);
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

    private static void runJsp() {
        try {
            // jps コマンドを実行
            ProcessBuilder processBuilder = new ProcessBuilder("jps");
            Process process = processBuilder.start();

            // コマンドの出力を読み取る
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            String pid = "";
            // 出力を1行ずつ読み込む
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                // "Main" が含まれている行を探す
                if (line.contains("Main")) {
                    // "Main" の前の数字を抽出
                    pid = line.split(" ")[0]; // PIDは最初の項目にあります
                    System.out.println("Main の PID: " + pid);
                    break; // 最初に見つけた PID を表示して終了
                }
            }

            String command = String.format(" jstat -gcutil -t %s 1000 > jstat.tsv", pid);
            System.out.println(command);
            // processBuilder = new ProcessBuilder(command);
            // process = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static void RunPythonFromJava() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "draw_graph_edgeLabel.py", String.valueOf(SIGMA),
                    String.valueOf(ELABELNUM));
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                System.out.println("ERROR: " + line);
            }

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