package codetree.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class OrderedNode {
    public OrderedNode parent;
    public ArrayList<OrderedNode> children;
    public ArrayList<CodeFragment> frag;
    public int depth = 0;

        static int id=0;


    OrderedNode(OrderedNode parent, ArrayList<CodeFragment> frag,int depth) {

        this.parent = parent;
        this.frag = frag;
        this.depth = depth;
        children = new ArrayList<>();
    }

    int size() {
        int s = 1;
        for (OrderedNode m : children) {
            s += m.size();
        }
        return s;
    }

    void addPath(ArrayList<CodeFragment> c,int dep) {
        OrderedNode o = new OrderedNode(this, c,dep);
        children.add(o);
    }

    //a sublist of list whose head is M1 
    // M1の兄弟(自分を含む)
    static ArrayList<OrderedNode>anotherList = new ArrayList<>();
    public void enumarateWithAcGM(BufferedWriter bw, int edgeLabelNum) throws IOException {
               Graph g = generateGraph(frag, id++);
               if(g.isConnected()){
                   g.writeGraph2Gfu(bw);
                //    anotherList=
               }
    }

    Graph generateGraph(List<CodeFragment> code, int id) {
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


}
