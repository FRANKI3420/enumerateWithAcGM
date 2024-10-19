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
    static int id = 0;
    static BufferedWriter bw;
    static BufferedWriter bw2;
    static CodeTree tree;
    static int sigma;
    static int eLabelNum;
    static int finish = 3;
    static boolean runPython = true;
    static boolean allEnumarateFromDatabase =true;
    static String gfuFilename;
    static String dataset;
    static String q_gfuFilename;
    static boolean filter = false;
    static CodeTree subtree;
    static HashMap<Integer, ArrayList<String>> gMaps;
    static List<Graph> G;


    public static void main(String[] args) throws InterruptedException{
        if(allEnumarateFromDatabase){
            try {
                tree = buildTreeForSubgraphSearch(0);
                gMaps = makeGmaps(gfuFilename);
                finish = Integer.MAX_VALUE;
                startEnumarate();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }else if(filter){
            G = SdfFileReader.readFile(Paths.get(sdfFilename));
            System.out.println("G size: " + G.size());

            sigma =  VertexLabel.size();
            eLabelNum  = dataBaseElabel(G);

            long start = System.nanoTime();
            tree  =new CodeTree(graphCode, G, 100);
            System.out.println("Build tree: " + (System.nanoTime() - start) / 1000 / 1000 + "msec");

            startEnumarate();
    }else{
        sigma =  3;
        eLabelNum  = 3;    
        startEnumarate();
    }


    
        // int sigma = dataBaseSigma(G);
        // 
       
        

        // ArrayList<Pair<Integer, Graph>> Q = new ArrayList<>();
        //     for (int i = 0; i < G.size(); ++i) {
        //         Graph g = G.get(i);

        //         final int size = g.size();

        //         if (34 <= size && size <= 36 && g.isConnected()) {
        //             Q.add(new Pair<Integer, Graph>(i, g));
        //         }
        //     }
        // int answer_num = 0;
            // Path out = Paths.get("output_supergraph.txt");
            // try (BufferedWriter bw = Files.newBufferedWriter(out)) {
            //     start = System.nanoTime();

            //     for (Pair<Integer, Graph> q : Q) {
            //         List<Integer> result = tree.supergraphSearch(q.right);
            //         answer_num += result.size();
            //         bw.write(q.left.toString() + result.toString() + "\n");
            //     }

            //     final long time = System.nanoTime() - start;
            //     System.out.println((time) + " nano sec");
            //     System.out.println((time / 1000 / 1000) + " msec");
            //     System.out.println("answer : " + answer_num);
            // } catch (IOException e) {
            //     System.exit(1);
            // }


        if(runPython){
            RunPythonFromJava();
        }
    }

    private static CodeTree buildTreeForSubgraphSearch(int datasetID) throws IOException, InterruptedException {
        String directoryPath = "result";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }

            String allindex = "result/all_index.csv";
            Path writeindex = Paths.get(allindex);
            try (BufferedWriter allfind = Files.newBufferedWriter(writeindex)) {
                allfind.write(
                        "dataset,depth,addPathtoTree(s),Tree_size,Tree_size(new),removeTime(s),addIDtoTree(s),Build_tree(s),memory cost\n");

                    parseArgs(datasetID);

                    long start_read = System.nanoTime();
                    G = SdfFileReader.readFile_gfu(Paths.get(gfuFilename));
                    sigma = Graph.numOflabels(G);
                    eLabelNum = 1;
                    // dataset_search(G);
                    start_read = System.nanoTime() - start_read;
                    System.out.println(dataset + " dataset load time:" + start_read / 1000 / 1000 + "ms");

                    
                    System.out.println("G size: " + G.size());


                    String output = String.format("result/%s_output.txt", dataset);
                    Path out = Paths.get(output);

                    String allresult = String.format("result/%s_result.csv",
                            dataset);
                    Path all = Paths.get(allresult);

                    try (BufferedWriter bw2 = Files.newBufferedWriter(out);
                            BufferedWriter allbw = Files.newBufferedWriter(all);) {
                        allbw.write(
                                "dataset,query_set,A/C,(G-C)/(G-A),SP,filtering_time(ms),verification_time(ms),query_time(ms),search_time(ms),node_fil_time(ms),|In(Q)|,|A(Q)|,|Can(Q)|,|F(Q)|,Num deleted Vertices,total deleted edges Num,nonfail,verify num,q_trav_num,1ms per filtering graph,ave_% of vertices were removed\n");

                        System.out.println(" ");
                        String resultFilename = String.format("result/%s_result.txt",
                                dataset);

                        Path res = Paths.get(resultFilename);
                        try (BufferedWriter bw = Files.newBufferedWriter(res)) {


                            System.out.println("tree");
                            CodeTree tree = new CodeTree(graphCode, G, bw, dataset, allfind);
                            directoryPath = "data_structure";
                            directory = new File(directoryPath);
                            if (!directory.exists()) {
                                directory.mkdir();
                            }

                            String codetree = String.format("data_structure/%s.ser",
                                    dataset);
                            File file = new File(codetree);
                            long fileSize = file.length();
                            System.out.println(
                                    "File size: " + String.format("%.2f", (double) fileSize / 1024 / 1024) + " MB");
                            allfind.write(String.format("%.2f", (double) fileSize / 1024 / 1024) + "\n");
 
                            allfind.flush();
                            return tree;
                        }
                    }
                }
    }

    static HashMap<Integer, ArrayList<String>> makeGmaps(String filePath) {
        HashMap<Integer, ArrayList<String>> gMaps = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = "a";
            while (line != null) {
                if (line.startsWith("#")) {
                    int id = Integer.parseInt(line.substring(1));
                    ArrayList<String> lines = new ArrayList<>();
                    lines.add(line);
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("#"))
                            break;
                        lines.add(line);
                    }
                    gMaps.put(id, lines);
                } else {
                    line = br.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gMaps;
    }

    private static void startEnumarate() throws InterruptedException {

        List<ArrayList<CodeFragment>> codeList = graphCode.computeCanonicalCode(sigma);
        try{
            bw =  Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            long start = System.nanoTime();
            enumarateWithAcGM(codeList,allEnumarateFromDatabase);
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000+"ms");
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000/1000+"s");
            System.out.println("ans num: "+id);
            bw.close();
            bw2.close();
        } catch (IOException e) {
            System.err.println(e);
            System.exit(1);
        }
    }

    private static void enumarateWithAcGM(List<ArrayList<CodeFragment>> codeLsit,boolean allEnumarateFromDatabase) throws IOException, InterruptedException {
        for(ArrayList<CodeFragment> c:codeLsit){
            Graph g = generateGraphAddElabel(c, id);//AcGMcode2graph
            // print(c,true);
            if(c.get(c.size()-1).getIsConnected() && graphCode.isCanonical(g,c)&& filter(c)){//g is a connected graph and g is canonical then
                print(c,true);//output g
                g.writeGraph2GfuAddeLabel(bw);//output g
                id++;
                if(c.size()==finish){
                    continue;
                }
                List<ArrayList<CodeFragment>> childrenOfM1 = new ArrayList<>();
                ArrayList<CodeFragment> anotherList=getAnotherList(c,codeLsit);//anotherList ← a sublist of list whose head is M1;
                byte lastVlabel = c.get(c.size()-1).getVlabel();
                for(CodeFragment M2:anotherList){
                    for(byte i=(byte)eLabelNum;i>=0;i--){
                        for(byte j=0;j<=lastVlabel;j++){
                            childrenOfM1.add(getChildrenOfM1(c,M2,j,i,allEnumarateFromDatabase));
                        }
                    }
                }
                enumarateWithAcGM(childrenOfM1,allEnumarateFromDatabase);
            }
        }
    }

    private static boolean filter(ArrayList<CodeFragment> c) throws IOException, InterruptedException {
        if(filter && !isChildCodeExist(c)){//未完成
            return false;         
        }else if (allEnumarateFromDatabase){
            Graph q = generateGraph(c, -1);
            String qString =String.format("%s/%s/%s/%d",
                    "Query", dataset, "child", q.size);
            Path qPath = Paths.get(qString);
            File directory = new File(qString);
            directory.mkdirs();
            qString =String.format("%s/%s/%s/%d/q%d.gfu",
            "Query", dataset, "child", q.size, q.id);
            qPath = Paths.get(qString);
            try (BufferedWriter bw = Files.newBufferedWriter(qPath)){
                q.writeGraph2Gfu(bw);
            }
            BitSet result = tree.subgraphSearch(q, bw, "child",
                                                    dataset,
                                                    null, null, G, gMaps, null);
            if(result.cardinality()==0){
                return false;         
        } 
    }
    return true;
    }

    private static ArrayList<CodeFragment> getChildrenOfM1(ArrayList<CodeFragment> c,CodeFragment M2, byte vLabel, byte eLabel,boolean allEnumarateFromDatabase) throws IOException, InterruptedException {
        byte []eLabels = new byte[c.size()];
        boolean isConnected=false;

        if(eLabel!=0){
            isConnected = true;
        }
        if(M2.getelabel().length==0){
            eLabels[0]=eLabel;
        }else{
            for(int i=0;i<M2.getelabel().length;i++){
                eLabels[i]=M2.getelabel()[i];
                if(eLabels[i]!=0){
                    isConnected = true;
                }
            }
             eLabels[c.size()-1]=eLabel;
        }
        CodeFragment leg = graphCode.generateCodeFragment(vLabel,eLabels,isConnected);
        
        ArrayList<CodeFragment> child = new ArrayList<>(c);
        child.add(leg);

        return child;
        
    }

    private static boolean isChildCodeExist(ArrayList<CodeFragment> child) throws IOException {
         List<Integer> result = tree.supergraphSearch(generateGraph(child, 0));
         if(result.size()==0){
            return false;
         }else{
            return true;
         }
    }

    private static ArrayList<CodeFragment> getAnotherList(ArrayList<CodeFragment> c, List<ArrayList<CodeFragment>> codeLsit) {
        ArrayList<CodeFragment> anotherList = new ArrayList<>();
        int depth = c.size()-1;
        byte vLabel = c.get(depth).getVlabel();
        for(ArrayList<CodeFragment> code:codeLsit){
            if(vLabel!=code.get(depth).getVlabel())
                continue;
            anotherList.add(code.get(depth));
        }
        return anotherList;
    }

    //  if(filter){//未完成
    //         if(isChildCodeExist(child)){
    //             return null;
    //         }else{
    //             return child;
    //         }
    //     }else if (allEnumarateFromDatabase){
    //         Graph q = generateGraph(child, -1);
    //         String qString =String.format("%s/%s/%s/%d",
    //                 "Query", dataset, "child", q.size);
    //         Path qPath = Paths.get(qString);
    //         File directory = new File(qString);
    //         directory.mkdirs();
    //         qString =String.format("%s/%s/%s/%d/q%d.gfu",
    //         "Query", dataset, "child", q.size, q.id);
    //         qPath = Paths.get(qString);
    //         try (BufferedWriter bw = Files.newBufferedWriter(qPath)){
    //             q.writeGraph2Gfu(bw);
    //         }
    //         try{
    //         BitSet result = tree.subgraphSearch(q, bw, "child",
    //                                                 dataset,
    //                                                 null, null, G, gMaps, null);
    //         if(result.size()!=0){
    //             return child;
    //         }else{
    //             return null;
    //         }
    //     } catch (IndexOutOfBoundsException e) {
    //         e.printStackTrace();
    //     }
    //     }else{
    //         return child;   
    //     }

    private static void print(List<CodeFragment> code,boolean output) throws IOException {// AcGMコード可視化
        for (CodeFragment c : code) {
            if(output){
                System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            }
            bw2.write(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
        }
       
        if(output){
            System.out.println();
        }
        bw2.write("\n");
        bw2.flush();
    }

    static Graph generateGraph(List<CodeFragment> code, int id) {
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

    static Graph generateGraphAddElabel(List<CodeFragment> code, int id) {
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
                    if (eLabels[i] > 0) {
                        edges[index][i] = eLabels[i];
                        edges[i][index] = eLabels[i];
                    }
                }
            }
            index++;
        }
        return new Graph(id, vertices, edges);
    }

     private static int dataBaseElabel(List<Graph> G) {
        HashSet<Byte>elabels = new HashSet<>();
        for(Graph g :G){
            for(byte []edges:g.edges){
                for(byte e:edges){
                    elabels.add(e);
                }
            }
        }
        return elabels.size()-1;
    }


    private static int dataBaseSigma(List<Graph> G) {
        int i = VertexLabel.size();
        HashSet<Byte>labels = new HashSet<>();
        for(Graph g :G){
            for(byte v:g.vertices){
                labels.add(v);
            }
        }
        return labels.size();
    }

    private static void parseArgs(int datasetID) {

        if (datasetID == 1) {
            gfuFilename = "pdbs.gfu";
            dataset = "pdbs";
            System.out.println("PDBS");
        } else if (datasetID == 2) {
            gfuFilename = "pcms.gfu";
            dataset = "pcms";
            System.out.println("PCM");
        } else if (datasetID == 3) {
            gfuFilename = "ppigo.gfu";
            dataset = "ppigo";
            System.out.println("PPI");
        } else if (datasetID == 4) {
            gfuFilename = "IMDB-MULTI.gfu";
            dataset = "IMDB-MULTI";
            System.out.println("IMDB");
        } else if (datasetID == 5) {
            gfuFilename = "REDDIT-MULTI-5K.gfu";
            dataset = "REDDIT-MULTI-5K";
            System.out.println("REDDIT");
        } else if (datasetID == 6) {
            gfuFilename = "COLLAB.gfu";
            dataset = "COLLAB";
            System.out.println("COLLAB");
        } else if (datasetID == 0) {
            gfuFilename = "AIDS.gfu";
            dataset = "AIDS";
            System.out.println("AIDS");
        } else if (datasetID == -1) {
            gfuFilename = "NCI.gfu";
            dataset = "NCI";
            System.out.println("NCI");
        }
    }

    static void RunPythonFromJava(){
        try {
            // Pythonスクリプトを実行する
            ProcessBuilder pb = new ProcessBuilder("python", "draw_graph_edgeLabel.py", String.valueOf(sigma), String.valueOf(eLabelNum));
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
                System.out.println("ERROR: " + line);  // エラーメッセージを確認
            }

            // プロセスが終了するのを待つ
            int exitCode = process.waitFor();
            if(exitCode==0){
                System.out.println("Program exited successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void subgraphSearch(int datasetID) throws IOException, InterruptedException {
            String directoryPath = "result";
                File directory = new File(directoryPath);
                if (!directory.exists()) {
                    directory.mkdir();
                }
    
                String allindex = "result/all_index.csv";
                String wholeresult = "result/all_result.csv";
                Path writeindex = Paths.get(allindex);
                Path writewhole = Paths.get(wholeresult);
                try (BufferedWriter allfind = Files.newBufferedWriter(writeindex);
                        BufferedWriter br_whole = Files.newBufferedWriter(writewhole)) {
                    allfind.write(
                            "dataset,depth,addPathtoTree(s),Tree_size,Tree_size(new),removeTime(s),addIDtoTree(s),Build_tree(s),memory cost\n");
    
                        br_whole.write(
                                "dataset,query_set,A/C,(G-C)/(G-A),SP,filtering_time(ms),verification_time(ms),query_time(ms),search_time(ms),node_fil_time(ms),|In(Q)|,|A(Q)|,|Can(Q)|,|F(Q)|,Num deleted Vertices,total deleted edges Num,nonfail,verify num,q_trav_num,1ms per filtering graph,ave_% of vertices were removed\n");
    
                        parseArgs(datasetID);
    
                        List<ArrayList<Pair<Integer, Graph>>> Q = new ArrayList<>();
                        final int querysize = 100;
                        final int minedge = 4;
                        final int maxedge = 64;
    
                        long start_read = System.nanoTime();
                        List<Graph> G = SdfFileReader.readFile_gfu(Paths.get(gfuFilename));
                        // dataset_search(G);
                        start_read = System.nanoTime() - start_read;
                        System.out.println(dataset + " dataset load time:" + start_read / 1000 / 1000 + "ms");
    
                        for (int numOfEdge = minedge; numOfEdge <= maxedge; numOfEdge *= 2) {
                            ArrayList<Pair<Integer, Graph>> qset = new ArrayList<>();
                            for (int i = 0; i < querysize; i++) {
                                q_gfuFilename = String.format("Query/%s/randomwalk/%d/q%d.gfu", dataset,
                                        numOfEdge, i);
                                Graph q = SdfFileReader.readFileQuery_gfu(Paths.get(q_gfuFilename));
                                qset.add(new Pair<Integer, Graph>(i, q));
                            }
                            // query_search(qset, numOfEdge, "R");
                            Q.add(qset);
                        }
    
                        for (int numOfEdge = minedge; numOfEdge <= maxedge; numOfEdge *= 2) {
                            ArrayList<Pair<Integer, Graph>> qset = new ArrayList<>();
                            for (int i = 0; i < querysize; i++) {
                                q_gfuFilename = String.format("Query/%s/bfs/%d/q%d.gfu", dataset, numOfEdge,
                                        i);
                                Graph q = SdfFileReader.readFileQuery_gfu(Paths.get(q_gfuFilename));
                                qset.add(new Pair<Integer, Graph>(i, q));
                            }
                            // query_search(qset, numOfEdge, "B");
                            Q.add(qset);
                        }
    
                        System.out.println("G size: " + G.size());
    
                        System.out.println("Q size: " + Q.size() * querysize);
    
                        String output = String.format("result/%s_output.txt", dataset);
                        Path out = Paths.get(output);
    
                        String allresult = String.format("result/%s_result.csv",
                                dataset);
                        Path all = Paths.get(allresult);
    
                        try (BufferedWriter bw2 = Files.newBufferedWriter(out);
                                BufferedWriter allbw = Files.newBufferedWriter(all);) {
                            allbw.write(
                                    "dataset,query_set,A/C,(G-C)/(G-A),SP,filtering_time(ms),verification_time(ms),query_time(ms),search_time(ms),node_fil_time(ms),|In(Q)|,|A(Q)|,|Can(Q)|,|F(Q)|,Num deleted Vertices,total deleted edges Num,nonfail,verify num,q_trav_num,1ms per filtering graph,ave_% of vertices were removed\n");
    
                            System.out.println(" ");
                            String resultFilename = String.format("result/%s_result.txt",
                                    dataset);
    
                            Path res = Paths.get(resultFilename);
                            try (BufferedWriter bw = Files.newBufferedWriter(res)) {
    
                                long start = System.nanoTime();
    
                                System.out.println("tree");
                                CodeTree tree = new CodeTree(graphCode, G, bw, dataset, allfind);
    
                                directoryPath = "data_structure";
                                directory = new File(directoryPath);
                                if (!directory.exists()) {
                                    directory.mkdir();
                                }
    
                                String codetree = String.format("data_structure/%s.ser",
                                        dataset);
                                File file = new File(codetree);
                                long fileSize = file.length();
                                System.out.println(
                                        "File size: " + String.format("%.2f", (double) fileSize / 1024 / 1024) + " MB");
                                allfind.write(String.format("%.2f", (double) fileSize / 1024 / 1024) + "\n");
     
                                allfind.flush();
                                // if (true) 索引構築のみの実験時にコメントアウト外す
                                // continue;
    
                                HashMap<Integer, ArrayList<String>> gMaps = makeGmaps(gfuFilename);
    
                                int index = minedge;
                                String mode = null;
                                String data_out = null;
                                int[] adjust = new int[Q.size()];
                                int count = 0;
                                int count2 = 0;
    
                                for (ArrayList<Pair<Integer, Graph>> Q_set : Q) {
                                    adjust[count++] = index;
    
                                    if (index <= maxedge) {
                                        System.out.println("\nQ" + index + "R");
                                        bw.write("Q" + index + "R\n");
                                        bw2.write("Q" + index + "R\n");
                                        allbw.write(dataset + ",Q" + index + "R,");
                                        br_whole.write(dataset + ",Q" + index + "R,");
                                        data_out = String.format("result/%s_%dR_data.csv", dataset,
                                                index);
                                        mode = "randomwalk";
                                    } else {
                                        int size = adjust[count2++];
    
                                        System.out.println("\nQ" + size + "B");
                                        bw.write("Q" + size + "B\n");
                                        bw2.write("Q" + size + "B\n");
                                        allbw.write(dataset + ",Q" + size + "B,");
                                        br_whole.write(dataset + ",Q" + size + "B,");
                                        data_out = String.format("result/%s_%dB_data.csv", dataset,
                                                size);
                                        mode = "bfs";
                                    }
    
                                    try (BufferedWriter bwout = new BufferedWriter(
                                            new OutputStreamWriter(new FileOutputStream(data_out), "UTF-8"));) {
    
                                        start = System.nanoTime();
    
                                        for (Pair<Integer, Graph> q : Q_set) {
                                            if (q.left == 0) {
                                                System.out.print("");
                                            } else if (q.left % 50 == 0) {
                                                System.out.print("*");
                                            } else if (q.left % 10 == 0) {
                                                System.out.print(".");
                                            }
                                            BitSet result = tree.subgraphSearch(q.right, bw, mode,
                                                    dataset,
                                                    bwout, allbw, G, gMaps, br_whole);
    
                                            bw2.write(
                                                    q.left.toString() + " " + result.cardinality() + "個"
                                                            + result.toString()
                                                            + "\n");
                                        }
                                        final long time = System.nanoTime() - start;
                                        bw.write("(A)*100+(C)+(D)+(E)+(α) 合計処理時間(ms): " + (time / 1000 / 1000) +
                                                "\n");
                                        index *= 2;
                                        Q_set = null;
                                    }
                                    bw.write("*********************************\n");
                                }
                            }
    
                        } catch (IOException e) {
                            System.out.println(e);
                        }
                    }
        }
}
