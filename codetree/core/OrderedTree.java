package codetree.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class OrderedTree {
    GraphCode impl;
    public OrderedNode root;


    public OrderedTree(GraphCode impl, int sigma) {
        this.impl = impl;
        this.root = new OrderedNode(null, null,0);

        // 深さ1にGのすべての頂点ラベルを登録
        List<ArrayList<CodeFragment>> codelist = impl.computeCanonicalCode(sigma);
        for (ArrayList<CodeFragment> c : codelist) {
            root.addPath(c,1);
        }

    }

    public void enumarateWithAcGM(BufferedWriter bw,int edgeLabelNum) throws IOException {
                for(OrderedNode m:root.children){

        m.enumarateWithAcGM(bw,edgeLabelNum);
                }
    }

    

    

}
