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
    private static int id_parallel = 0;
    private static int id_single = 0;
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
    // シングルスレッドとマルチスレッドの割合を決める(調整難)
    private static int PARAM = 5000;// 8:300 9:5000
    private static byte SIGMA = 5;
    private static byte ELABELNUM = 1;
    private static int FINISH = 5;
    private static final boolean PARALLEL = false;
    private static final boolean SINGLE_And_PARALLEL = false;
    private static final boolean USING_STACK = false;
    private static final boolean LABEL_COPY = true;

    public static void main(String[] args) {
        System.out.println("|V|<=" + FINISH + " |Σ|=" + SIGMA + " ELABELNUM=" + ELABELNUM + " LABELCOPY " + LABEL_COPY);
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

        double maxMemoryUsedInMB = maxMemoryUsed / (1024.0 * 1024.0);
        System.out.println(String.format("最大メモリ使用量: %.1f MB", maxMemoryUsedInMB));
        double isCanonicalTime = (double) AcgmCode.isCanonicalTime / (1000 * 1000 * 1000);
        System.out.println(String.format("正準判定時間: %.1f s", isCanonicalTime));
        System.out.println("正準判定回数 " + AcgmCode.canonical_times);

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

            System.out.println("ans num: " + id_single);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startEnumarateParallel(boolean SINGLE_And_PARALLEL, boolean USING_STACK) throws IOException {
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
                    enumerateWithAcGMSingleAndParallelUsingStack(executorService, codeList, new ArrayList<>()).join();
                } else {
                    System.out.println("再帰");
                    enumarateWithAcGMSingleAndParallel(executorService, codeList, new ArrayList<>()).join();
                }
                CompletableFuture.allOf(singleTasks.toArray(new CompletableFuture[0])).join();
                bw.flush();
                bw2.flush();
                id_parallel = 0;
                id_single = 0;
                mergeFiles(new String[] { "outputSingle.gfu", "outputParallel.gfu" }, "output.gfu");
            } else {
                System.out.println("マルチスレッド");
                if (USING_STACK) {
                    System.out.println("スタック");
                    // enumerateWithAcGMParallelUsingStack(executorService, codeList, new
                    // ArrayList<>()).join();
                    enumerateWithAcGMUsingForkJoin(codeList, new ArrayList<>());
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
            System.out.println("実行時間：" + (start) / 1000 / 1000 + "ms");
            System.out.println("実行時間：" + String.format("%.1f", (double) (start) / 1000 /
                    1000 / 1000) + "s");
            System.out.println("ans num: " + (id_parallel + id_single));
            if (SINGLE_And_PARALLEL) {
                System.out.println("ans num(single thread): " + (id_single));
                System.out.println("ans num(multi thread): " + (id_parallel));
                System.out.println("シングルスレッドの発見解割合: "
                        + String.format("%.2f%%", (double) id_single / (id_parallel + id_single) * 100));
                System.out.println("マルチスレッドスレッドの発見解割合: "
                        + String.format("%.2f%%", (double) id_parallel / (id_parallel + id_single) * 100));
            }
        }
    }

    private static void enumarateWithAcGM(ArrayList<ObjectFragment> codeList, ArrayList<ObjectFragment> nowFragments)
            throws IOException {
        UpdatamaxMemoryUsed(codeList);
        for (int index = 0, len = codeList.size(); index < len; index++) {
            final ObjectFragment c = codeList.get(index);
            if (c.getIsConnected()) {
                nowFragments.add(c);
                if (isCanonical(c, nowFragments)) {
                    if (LABEL_COPY) {
                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                            // generateAllDiffVlabelGraph(g, nowFragments);
                            generateAllDiffVlabelGraph2(nowFragments);
                        } else {
                            return;
                        }
                    }

                    writeCodetoFileSingle(nowFragments);

                    if (nowFragments.size() == FINISH) {
                        nowFragments.remove(nowFragments.size() - 1);
                        // codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());
                    // codeList.set(index, null);
                    enumarateWithAcGM(childrenOfM1, nowFragments);
                }

                nowFragments.remove(nowFragments.size() - 1);
            }
        }
    }

    private static boolean isCanonical(ObjectFragment c, ArrayList<ObjectFragment> nowFragments) {
        // // 1
        // if (c.getIsAllSameVlabel() && c.getIsMaxLabel()) {
        // return true;
        // }

        // 2
        if (c.getIsMaxLabel() && isAllCanonical(nowFragments)) {
            return true;
        }

        int depth = nowFragments.size();
        boolean isAllSameVlabelExceptLast = nowFragments.get(depth - 2).getIsAllSameVlabel() && !c.getIsAllSameVlabel();

        // 3
        if (isAllSameVlabelExceptLast) {
            if (c.getIsMaxLabel()) {
                return true;
            }
        }

        // 4
        if (c.getIsAllSameElabel() > 0 && isAllSameElabelExceptLast(nowFragments)) {
            if (c.getIsAllSameVlabel() && !c.getIsMaxLabel()) {
                return false;
            } else if (isAllSameVlabelExceptLast && !c.getIsMaxLabel()) {
                return false;
            }
        }
        // print(nowFragments);

        return objectType.isCanonical(nowFragments);

    }

    private static boolean isAllCanonical(ArrayList<ObjectFragment> nowFragments) {
        for (ObjectFragment c : nowFragments) {
            if (!c.getIsMaxLabel()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllSameElabelExceptLast(ArrayList<ObjectFragment> nowFragments) {
        byte eLabel = nowFragments.get(1).getIsAllSameElabel();
        if (eLabel == 0) {
            return false;
        }
        for (int index = 2; index < nowFragments.size() - 1; index++) {
            if (eLabel != nowFragments.get(index).getIsAllSameElabel())
                return false;
        }
        return true;
    }

    private static ArrayList<ObjectFragment> getChildrenOfM1(ArrayList<ObjectFragment> codeList, int index,
            byte vlabel) {

        ArrayList<ObjectFragment> childrenOfM1 = new ArrayList<>();

        ArrayList<ObjectFragment> anotherList = getAnotherList(codeList, index, vlabel);
        for (ObjectFragment M2 : anotherList) {
            for (byte i = ELABELNUM; i >= 0; i--) {
                childrenOfM1.add(getChildrenOfM1Fragment(M2, i, vlabel));
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
                if (c.getelabel().length > 0 && c.getIsConnected())
                    continue;
                anotherList.add(c);
            }
        }

        // 自身を含めて弟のフラグメントを追加
        anotherList.addAll(codeList.subList(index, len));

        return anotherList;
    }

    private static ObjectFragment getChildrenOfM1Fragment(final ObjectFragment M2, final byte eLabel, byte vlabel) {
        final int depth = M2.getelabel().length + 1;
        byte[] eLabels = new byte[M2.getelabel().length + 1];

        boolean isConnected;
        boolean isAllSameVlabel = M2.getIsAllSameVlabel() && M2.getVlabel() == vlabel;

        if (depth > 1) {
            isConnected = eLabel > 0 || M2.getIsConnected() ? true : false;
        } else {
            isConnected = eLabel > 0 ? true : false;
        }

        System.arraycopy(M2.getelabel(), 0, eLabels, 0, depth - 1);

        eLabels[depth - 1] = eLabel;

        boolean isMaxLabel = isConnected && M2.getIsMaxLabel()
                && (depth - 1 == 0 || getIsCanonical(eLabels));

        byte isAllSameElabel = 0;
        if (depth - 1 == 0) {
            isAllSameElabel = eLabel;
        } else if (M2.getIsAllSameElabel() != 0 && isConnected
                && (depth - 1 == 0 || M2.getelabel()[0] == eLabel)) {
            isAllSameElabel = eLabel;
        }

        return objectType.generateCodeFragment(M2.getVlabel(), eLabels, isConnected, isMaxLabel,
                isAllSameVlabel, isAllSameElabel);
    }

    // 正準系の接頭辞は正準系＝追加されるコードフラグメントの辺ラベルが正準系であれば、正準系である（複数の辺ラベルある場合でも）
    private static boolean getIsCanonical(byte[] eLabels) {
        byte[] label = new byte[ELABELNUM + 1];
        for (byte e : eLabels) {
            label[e]++;
        }

        int index = 0;
        for (int l = ELABELNUM; l >= 0; l--) {
            for (int i = 0; i < label[l]; i++) {
                if (eLabels[index++] != l) {
                    return false;
                }
            }
        }
        return true;
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
            ArrayList<ObjectFragment> nowFragments = pastFragmentsStack.pop();
            UpdatamaxMemoryUsed(codeList);
            for (int index = 0, len = currentList.size(); index < len; index++) {
                final ObjectFragment c = currentList.get(index);
                if (c.getIsConnected()) {
                    nowFragments.add(c);

                    Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                    if (isCanonical(c, nowFragments)) {
                        if (LABEL_COPY) {
                            if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                                // generateAllDiffVlabelGraph(g, nowFragments);
                                generateAllDiffVlabelGraph2(nowFragments);
                            } else {
                                continue;
                            }
                        }
                        // writeGraphtoFileSingle(g);
                        writeCodetoFileSingle(nowFragments);

                        if (nowFragments.size() == FINISH) {
                            nowFragments.remove(nowFragments.size() - 1);
                            // currentList.set(index, null);
                            continue;
                        }
                        ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(currentList, index, c.getVlabel());
                        stack.push(childrenOfM1);
                        pastFragmentsStack.push(new ArrayList<>(nowFragments));
                    }
                    nowFragments.remove(nowFragments.size() - 1);
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

                // Graph g = objectType.generateGraphAddElabel(nowFragments, 0);
                if (isCanonical(c, nowFragments)) {
                    if (LABEL_COPY) {
                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                            // generateAllDiffVlabelGraph(g, nowFragments);
                            try {
                                generateAllDiffVlabelGraph2(nowFragments);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            break;
                        }
                    }

                    writeCodetoFile(nowFragments);

                    // writeGraphtoFile(g);

                    if (nowFragments.size() == FINISH) {
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());

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

                                // Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                                if (isCanonical(c, nowFragments)) {

                                    if (LABEL_COPY) {
                                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                                            // generateAllDiffVlabelGraph(g, nowFragments);
                                            generateAllDiffVlabelGraph2(nowFragments);

                                        } else {
                                            continue;
                                        }
                                    }

                                    // writeGraphtoFile(g);
                                    writeCodetoFile(nowFragments);

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

                ArrayList<ObjectFragment> nowFragments = new ArrayList<>(currentPastFragments);
                nowFragments.add(c);

                // Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                if (isCanonical(c, nowFragments)) {
                    if (LABEL_COPY) {
                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                            // generateAllDiffVlabelGraph(g, nowFragments);
                            try {
                                generateAllDiffVlabelGraph2(nowFragments);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            break;
                        }
                    }

                    // writeGraphtoFile(g);
                    writeCodetoFile(nowFragments);

                    if (nowFragments.size() == FINISH) {
                        continue;
                    }

                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(currentList, index, c.getVlabel());
                    EnumerationTask subTask = new EnumerationTask(childrenOfM1, nowFragments);
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

                                // Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                                if (isCanonical(c, nowFragments)) {
                                    if (LABEL_COPY) {
                                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                                            // generateAllDiffVlabelGraph(g, nowFragments);
                                            generateAllDiffVlabelGraph2(nowFragments);

                                        } else {
                                            break;
                                        }
                                    }

                                    // writeGraphtoFile(g);
                                    writeCodetoFile(nowFragments);

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

                // Graph g = objectType.generateGraphAddElabel(nowFragments, 0);

                if (isCanonical(c, nowFragments)) {
                    if (LABEL_COPY) {
                        if (SIGMA - 1 == nowFragments.get(0).getVlabel()) {
                            // generateAllDiffVlabelGraph(g, nowFragments);
                            try {
                                generateAllDiffVlabelGraph2(nowFragments);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            return CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]));
                        }
                    }
                    // writeGraphtoFile(g);
                    writeCodetoFile(nowFragments);

                    if (nowFragments.size() == FINISH) {
                        codeList.set(index, null);
                        continue;
                    }
                    ArrayList<ObjectFragment> childrenOfM1 = getChildrenOfM1(codeList, index, c.getVlabel());

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

    private static void generateAllDiffVlabelGraph(Graph g,
            ArrayList<ObjectFragment> pastFragments) {
        int index = 1;
        ArrayList<String> edgeLine = getEdgeLine(g.order(), g.size(), g.edges);
        List<byte[]> veList = new ArrayList<>();
        boolean loop = true;
        while (true) {
            byte[] vertices = new byte[g.order()];
            for (int i = 0; i < g.order(); i++) {
                ObjectFragment c = pastFragments.get(i);
                byte cvlabel = (byte) (c.getVlabel() - index);
                if (cvlabel < 0) {
                    // System.out.println();
                    loop = false;
                    break;
                }
                vertices[i] = cvlabel;
            }
            if (!loop)
                break;
            veList.add(vertices);
            index++;
        }
        if (PARALLEL) {
            synchronized (bw2) {
                writeGraph2GfuAddeLabel(veList, edgeLine, bw2);
                // new Graph(vertices, g.edges).writeGraph2GfuAddeLabel(bw2);
                id_parallel += veList.size();
            }
        } else {
            writeGraph2GfuAddeLabel(veList, edgeLine, bw);
            id_single += veList.size();
        }
    }

    private static void generateAllDiffVlabelGraph2(ArrayList<ObjectFragment> nowFragments) throws IOException {
        if (PARALLEL) {
            writeVlabelChangeCodetoFile_parallel(nowFragments, bw2);
        } else {
            writeVlabelChangeCodetoFile_single(nowFragments, bw);
        }
    }

    private static ArrayList<String> getEdgeLine(int order, int size, byte[][] edges) {
        ArrayList<String> edgeLine = new ArrayList<>();
        edgeLine.add(size + "\n");
        for (int i = 0; i < order; i++) {
            for (int j = i; j < order; j++) {
                if (edges[i][j] > 0) {
                    edgeLine.add(i + " " + j + " " + edges[i][j] + "\n");
                }
            }
        }
        return edgeLine;
    }

    private static void writeVlabelChangeCodetoFile_single(List<ObjectFragment> code, BufferedWriter out)
            throws IOException {
        int index = 1;
        while (true) {
            String line = "";
            for (ObjectFragment c : code) {
                byte v = (byte) (c.getVlabel() - index);
                if (v < 0) {
                    return;
                }
                line += (c.getVlabel() - index) + ":" + Arrays.toString(c.getelabel()).toString() + " ";
            }
            id_single++;
            out.write(line + "\n");
            index++;
        }
    }

    private static void writeVlabelChangeCodetoFile_parallel(List<ObjectFragment> code, BufferedWriter out)
            throws IOException {
        int index = 1;
        while (true) {
            String line = "";
            for (ObjectFragment c : code) {
                byte v = (byte) (c.getVlabel() - index);
                if (v < 0) {
                    return;
                }
                line += (c.getVlabel() - index) + ":" + Arrays.toString(c.getelabel()).toString() + " ";
            }
            synchronized (bw2) {
                out.write(line + "\n");
                id_parallel++;
            }
            index++;
        }
    }

    private static void writeGraph2GfuAddeLabel(List<byte[]> veList, ArrayList<String> edgeLine,
            BufferedWriter writer) {
        try {
            for (byte[] vertices : veList) {
                writer.write("#" + 0 + "\n");
                writer.write(vertices.length + "\n");
                for (byte v : vertices) {
                    writer.write(v + "\n");
                }
                for (String s : edgeLine) {
                    writer.write(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // boolean isMaxLabel = isConnected && M2.getIsMaxLabel() && (eLabel == 0 ||
    // getIsCanonical(eLabels));//左の数字が小さい時点でOUTか？

    // boolean isAllSameElabel = M2.getIsAllSameElabel() && isConnected
    // & (depth - 1 == 0 || M2.getelabel()[0] == eLabel);
    // boolean isAllSameElabel = M2.getIsAllSameElabel() && (isConnected ||
    // (eLabels.length == 1 && eLabel == 0))
    // & (depth - 1 == 0 || isAllSameElabel(eLabels));
    // boolean isMaxLabel = isConnected && M2.getIsMaxLabel()
    // && (depth - 1 == 0 || M2.getelabel()[M2.getelabel().length - 1] >= eLabel);//
    // 左の数字が小さい時点でOUTか？
    // boolean isMaxLabel = isConnected && M2.getIsMaxLabel() &&
    // getIsCanonical(eLabels);

    // boolean isAllSameElabel = M2.getIsAllSameElabel() && isConnected &&
    // getIsAllSameElabel(eLabels);

    // System.out.println(Arrays.toString(eLabels));
    // byte[] label = new byte[ELABELNUM + 1];
    // Set<Byte> eSet = new HashSet<>();
    // eSet.add((byte) 0);
    // int target = 0;
    // for (byte e : eLabels) {
    // label[e]++;
    // eSet.add(e);

    // if (e != 0) {
    // target = e;
    // }
    // if (eSet.size() > 2) {
    // return false;
    // }
    // }

    // int index = 0;
    // for (int l : new int[] { target, 0 }) {
    // for (int i = 0; i < label[l]; i++) {
    // if (eLabels[index++] != l) {
    // return false;
    // }
    // }
    // }

    private static boolean shouldRunInParallel(ThreadPoolExecutor executor) {
        int queuedTasks = executor.getQueue().size();
        return queuedTasks < PARAM;
    }

    private static void writeGraphtoFileSingle(Graph g) {
        g.writeGraph2GfuAddeLabel(bw);
        id_single++;
    }

    private static void writeGraphtoFile(Graph g) {
        synchronized (bw2) {
            g.writeGraph2GfuAddeLabel(bw2);
            id_parallel++;
        }
    }

    private static void writeCodetoFileSingle(ArrayList<ObjectFragment> nowFragments) {
        try {
            writeCodetoFile(nowFragments, bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        id_single++;
    }

    private static void writeCodetoFile(ArrayList<ObjectFragment> nowFragments) {
        synchronized (bw2) {
            try {
                writeCodetoFile(nowFragments, bw2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            id_parallel++;
        }
    }

    private static void writeCodetoFile(List<ObjectFragment> code, BufferedWriter out) throws IOException {// AcGMコード可視化
        for (ObjectFragment c : code) {
            out.write(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
        }
        out.write("\n");
        out.flush();
    }

    @SuppressWarnings("unused")
    private static void print(List<ObjectFragment> code) {// AcGMコード可視化
        for (ObjectFragment c : code) {
            System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
        }

        System.out.println();
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
                                id_single++;
                            } else {
                                id_parallel++;
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

// private static boolean getIsAllSameElabel(byte[] eLabels) {

// Set<Byte> eSet = new HashSet<>();
// for (byte e : eLabels) {
// eSet.add(e);
// if (eSet.size() == 2) {
// return false;
// }
// }
// return true;
// }

// private static boolean[] getIsCanonical(byte[] eLabels) {
// // System.out.println(Arrays.toString(eLabels));
// boolean[] result = new boolean[2];
// result[0] = true;
// result[1] = true;
// byte[] label = new byte[ELABELNUM + 1];
// byte first = eLabels[0];
// for (byte e : eLabels) {
// label[e]++;
// if (first != e) {
// result[0] = false;
// }
// }
// int index = 0;
// for (int l = ELABELNUM; l >= 0; l--) {
// for (int i = 0; i < label[l]; i++) {
// // System.out.println(eLabels[index]);
// // System.out.println(l);
// if (eLabels[index++] != l) {
// result[1] = false;
// return result;
// }
// }
// }
// return result;
// }

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
// id_parallel++;
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

// allSameVLabelAndGenerate(pastFragments);
// if ( SIGMA - 1 == c.getVlabel() &&
// pastFragments.get(pastFragments.size() -
// 1).getAllSameVlabel()) {
// generateDiffVlabelGraph(g, pastFragments);
// } else if (SIGMA - 1 != c.getVlabel()
// && pastFragments.get(pastFragments.size() - 1).getAllSameVlabel()) {
// return;
// }
// private static void generateDiffVlabelGraph(Graph g,
// ArrayList<ObjectFragment> pastFragments) {
// byte vlabel = (byte) (g.vertices[0] - 1);
// ArrayList<ObjectFragment> fragments = new ArrayList<>(pastFragments);
// while (vlabel > -1) {
// byte[] vertices = new byte[g.order()];
// for (int i = 0; i < g.order(); i++) {
// vertices[i] = vlabel;
// }
// for (ObjectFragment c : fragments) {
// System.out.print(vlabel + ":" + Arrays.toString(c.getelabel()).toString() +
// "");
// }
// System.out.println();
// new Graph(0, vertices, g.edges).writeGraph2GfuAddeLabel(bw2);
// id_single++;
// vlabel--;
// }
// }

// private static void allSameVLabelAndGenerate(ArrayList<ObjectFragment>
// pastFragments) {
// int depth = pastFragments.size();

// if (depth == 1 || (pastFragments.get(depth - 2).getAllSameVlabel()
// && pastFragments.get(depth - 2).getVlabel() == pastFragments
// .get(depth - 1).getVlabel())) {
// // try {
// // System.out.println("same");
// // print(pastFragments, true);
// // } catch (IOException e) {
// // // TODO Auto-generated catch block
// // e.printStackTrace();
// // }
// } else {
// pastFragments.get(depth - 1).setAllSameVlabel(false);
// // try {
// // System.out.println("non-same");
// // print(pastFragments, true);
// // } catch (IOException e) {
// // // TODO Auto-generated catch block
// // e.printStackTrace();
// // }
// }
// }

// private static void generateAllDiffVlabelGraph(Graph g,
// ArrayList<ObjectFragment> pastFragments) {
// int index = 1;
// byte[] vertices = new byte[g.order()];
// ArrayList<String> edgeLine = getEdgeLine(g.order(), g.size(), g.edges);
// while (true) {
// for (int i = 0; i < g.order(); i++) {
// ObjectFragment c = pastFragments.get(i);
// byte cvlabel = (byte) (c.getVlabel() - index);
// if (cvlabel < 0) {
// // System.out.println();
// return;
// }
// vertices[i] = cvlabel;
// // System.out.print(cvlabel + ":" +
// // Arrays.toString(c.getelabel()).toString()+
// // " ");
// }
// // System.out.println();
// index++;
// if (PARALLEL) {
// synchronized (bw2) {
// writeGraph2GfuAddeLabel(vertices, edgeLine, bw2);
// // new Graph(vertices, g.edges).writeGraph2GfuAddeLabel(bw2);
// id_parallel++;
// }
// } else {
// writeGraph2GfuAddeLabel(vertices, edgeLine, bw);
// id_single++;
// }
// }
// }

// private static void writeGraph2GfuAddeLabel(byte[] vertices,
// ArrayList<String> edgeLine, BufferedWriter writer) {
// try {
// writer.write("#" + 0 + "\n");
// writer.write(vertices.length + "\n");
// for (byte v : vertices) {
// writer.write(v + "\n");
// }
// for (String s : edgeLine) {
// writer.write(s);
// }
// } catch (Exception e) {
// e.printStackTrace();
// }
// }

// A[1] A[11]:A[111]:A[1111]→A[00001]～[011111]は非正準 (全て同じ辺ラベルのみ)
// boolean isNotCanonical = nowFragments.size() > 1
// && c.getIsMaxLabel() == false
// && c.getIsAllSameElabel() == true;
// boolean isNotCanonical = c.getIsAllSameVlabel() == true &&
// nowFragments.size() > 1
// && c.getIsMaxLabel() == false && c.getIsAllSameElabel() == true
// && nowFragments.get(nowFragments.size() - 2).getIsAllSameElabel() == true;
// if (isNotCanonical && objectType.isCanonical(g, nowFragments)) {
// print(nowFragments, true);
// nowFragments.remove(nowFragments.size() - 1);
// continue;
// }
// if (isNotCanonical && objectType.isCanonical(g, nowFragments)) {
// print(nowFragments, true);
// }

// if (c.getIsAllSameVlabel() && c.getIsMaxLabel()) {
// print(nowFragments, true);
// }

// if (nowFragments.size() > 1 && nowFragments.get(nowFragments.size() -
// 2).getIsMaxLabel() == true
// && c.getIsMaxLabel() == false
// && c.getVlabel() == nowFragments.get(nowFragments.size() - 2).getVlabel()) {
// print(nowFragments, true);
// if (objectType.isCanonical(g, nowFragments)) {
// print(nowFragments, true);
// }
// nowFragments.remove(nowFragments.size() - 1);
// // codeList.set(index, null);
// continue;
// }

// if ((c.getIsMaxLabel()) &&
// !objectType.isCanonical(g, nowFragments)) {
// System.out.println("canonical code: ");
// print(objectType.computeCanonicalCode(g), true);
// System.out.println("this code: ");
// print(nowFragments, true);
// System.out.println("miss");
// }

// System.out.println("target");
// print(nowFragments, true);

// private static boolean isAllSameElabel(byte[] eLabels) {
// Set<Byte> eSet = new HashSet<>();
// // System.out.println(Arrays.toString(eLabels));
// eSet.add((byte) 0);
// for (byte e : eLabels) {
// eSet.add(e);
// if (eSet.size() > 2) {
// return false;
// }
// }
// return true;
// }
// private static void runJsp() {
// try {
// // jps コマンドを実行
// ProcessBuilder processBuilder = new ProcessBuilder("jps");
// Process process = processBuilder.start();

// // コマンドの出力を読み取る
// BufferedReader reader = new BufferedReader(new
// InputStreamReader(process.getInputStream()));
// String line;

// String pid = "";
// // 出力を1行ずつ読み込む
// while ((line = reader.readLine()) != null) {
// System.out.println(line);
// // "Main" が含まれている行を探す
// if (line.contains("Main")) {
// // "Main" の前の数字を抽出
// pid = line.split(" ")[0]; // PIDは最初の項目にあります
// System.out.println("Main の PID: " + pid);
// break; // 最初に見つけた PID を表示して終了
// }
// }

// String command = String.format(" jstat -gcutil -t %s 1000 > jstat.tsv", pid);
// System.out.println(command);
// // processBuilder = new ProcessBuilder(command);
// // process = processBuilder.start();
// } catch (IOException e) {
// e.printStackTrace();
// }

// }

// if (nowFragments.get(depth - 2).getIsAllSameElabel() > 0 &&
// c.getIsAllSameVlabel() && !c.getIsMaxLabel()) {
// // if (objectType.isCanonical(objectType.generateGraphAddElabel(nowFragments,
// // 0), nowFragments)) {
// // print(nowFragments);
// // }

// // print(nowFragments);
// // System.out.println("事前判定");
// return false;
// } else if (nowFragments.get(depth - 2).getIsAllSameElabel() > 0 &&
// c.getIsAllSameVlabel() && c.getIsMaxLabel()) {
// // if
// (!objectType.isCanonical(objectType.generateGraphAddElabel(nowFragments,
// // 0), nowFragments)) {
// // print(nowFragments);
// // }
// return true;
// if (nowFragments.get(depth - 2).getIsAllSameElabel() > 0 &&
// c.getIsAllSameVlabel() && !c.getIsMaxLabel()) {
// if (objectType.isCanonical(objectType.generateGraphAddElabel(nowFragments,
// 0), nowFragments)) {
// print(nowFragments);
// }

// // print(nowFragments);
// // System.out.println("事前判定");
// return false;
// }

// if (nowFragments.get(depth - 2).getIsAllSameVlabel() &&
// nowFragments.get(depth - 2).getIsAllSameElabel() > 0
// && !c.getIsMaxLabel()) {
// if (objectType.isCanonical(objectType.generateGraphAddElabel(nowFragments,
// 0), nowFragments)) {
// print(nowFragments);
// }
// return false;
// }