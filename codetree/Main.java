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

    public static void main(String[] args) {
        List<Graph> G = SdfFileReader.readFile(Paths.get(sdfFilename));
        System.out.println("G size: " + G.size());

        // int sigma = dataBaseSigma(G);
        // sigma =  VertexLabel.size();
        // eLabelNum  = dataBaseElabel(G);
        sigma =  3;
        eLabelNum  = 3;
        boolean filter = false;

        long start = System.nanoTime();
        tree  =new CodeTree(graphCode, G, 100);
        System.out.println("Build tree: " + (System.nanoTime() - start) / 1000 / 1000 + "msec");

        startEnumarate(filter);

        if(runPython){
            RunPythonFromJava();
        }
    }

    private static void startEnumarate(boolean filter) {

        List<ArrayList<CodeFragment>> codeList = graphCode.computeCanonicalCode(sigma);
        try{
            bw =  Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            long start = System.nanoTime();
            enumarateWithAcGM(codeList,filter);
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000+"ms");
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000/1000+"s");
            System.out.println("ans num: "+id);
            bw.close();
            bw2.close();
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private static void enumarateWithAcGM(List<ArrayList<CodeFragment>> codeLsit,boolean filter) throws IOException {
        for(ArrayList<CodeFragment> c:codeLsit){
            Graph g = generateGraphAddElabel(c, id);//AcGMcode2graph
            // print(c,true);
            if(c.get(c.size()-1).getIsConnected() && graphCode.isCanonical(g,c)){//g is a connected graph and g is canonical then
                print(c,false);//output g
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
                            childrenOfM1.add(getChildrenOfM1(c,M2,j,i,filter));
                        }
                    }
                }
                enumarateWithAcGM(childrenOfM1,filter);
            }
        }
    }

    private static ArrayList<CodeFragment> getChildrenOfM1(ArrayList<CodeFragment> c,CodeFragment M2, byte vLabel, byte eLabel,boolean filter) throws IOException {
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

        if(filter){//未完成
            if(isChildCodeExist(child)){
                return null;
            }else{
                return child;
            }
        }else{
            return child;
        }
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

    private static void print(List<CodeFragment> code,boolean output) throws IOException {// AcGMコード可視化
        for (CodeFragment c : code) {
            if(output){
                System.out.print(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            }else{
                bw2.write(c.getVlabel() + ":" + Arrays.toString(c.getelabel()).toString() + " ");
            }
        }
       
        if(output){
            System.out.println();
        }else{
            bw2.write("\n");
            bw2.flush();
        }
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
}
