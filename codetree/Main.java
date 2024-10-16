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
    static int id = 0;
    static BufferedWriter bw;
    static BufferedWriter bw2;
    public static void main(String[] args) {

        byte sigma = 1;
        byte edgeLabelNum = 1;

        List<ArrayList<CodeFragment>> codeList = graphCode.computeCanonicalCode(sigma);
        try{
            bw =  Files.newBufferedWriter(Paths.get("output.gfu"));
            bw2 = Files.newBufferedWriter(Paths.get("outputAcGMcode.txt"));
            long start = System.nanoTime();
            enumarateWithAcGM(codeList,sigma,edgeLabelNum);
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000+"ms");
            System.out.println("実行時間："+(System.nanoTime()-start)/1000/1000/1000+"s");
            System.out.println("ans num: "+id);
            bw.close();
            bw2.close();
        } catch (IOException e) {
            System.exit(1);
        }

        System.exit(0);

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

    private static void enumarateWithAcGM(List<ArrayList<CodeFragment>> codeLsit, byte sigma,byte eLabelNum) throws IOException {
        for(ArrayList<CodeFragment> c:codeLsit){
            Graph g = generateGraph(c, id);//AcGMcode2graph
            // print(c,true);
            // if(g.isConnected() && graphCode.isCanonical(g,c)){//g is a connected graph and g is canonical then
            if(c.get(c.size()-1).getIsConnected() && graphCode.isCanonical(g,c)){//g is a connected graph and g is canonical then
                print(c,false);//output g
                g.writeGraph2Gfu(bw);//output g
                id++;
                if(c.size()==7){
                    continue;
                }
                List<ArrayList<CodeFragment>> childrenOfM1 = new ArrayList<>();
                ArrayList<CodeFragment> anotherList=getAnotherList(c,codeLsit);//anotherList ← a sublist of list whose head is M1;
                byte lastVlabel = c.get(c.size()-1).getVlabel();
                for(CodeFragment M2:anotherList){
                    for(byte i=eLabelNum;i>=0;i--){
                        for(byte j=0;j<=lastVlabel;j++){
                            childrenOfM1.add(getChildrenOfM1(c,M2,j,i));
                        }
                    }
                }
                enumarateWithAcGM(childrenOfM1,sigma,eLabelNum);
            }
        }
    }

    private static ArrayList<CodeFragment> getChildrenOfM1(ArrayList<CodeFragment> c,CodeFragment M2, byte vLabel, byte eLabel) throws IOException {
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
        // if(!c.get(c.size()-1).getIsConnected()){
        //     isConnected =false;
        // }
        CodeFragment leg = graphCode.generateCodeFragment(vLabel,eLabels,isConnected);
        // CodeFragment leg = graphCode.generateCodeFragment(vLabel,eLabels,c.get(c.size()-1).getIsConnected());
        
        ArrayList<CodeFragment> child = new ArrayList<>(c);
        child.add(leg);
        // print(child,true);
        return child;
    }

    private boolean judgeIsConnected(byte[] eLabels) {
        for(byte e : eLabels){
            if(e==1){ 
                return true;
            }
        }
        return false;
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
    private static GraphCode graphCode = new AcgmCode();
    // private static GraphCode graphCode = new XAcgmCode();
}
