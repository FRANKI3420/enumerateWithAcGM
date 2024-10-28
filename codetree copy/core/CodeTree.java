package codetree.core;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class CodeTree {
    GraphCode impl;
    public IndexNode root;

    public CodeTree(GraphCode impl, List<Graph> G, int b) {
        this.impl = impl;
        this.root = new IndexNode(null, null, 0);

        System.out.print("Indexing");
        for (int i = 0; i < G.size(); ++i) {
            Graph g = G.get(i);

            List<CodeFragment> code = impl.computeCanonicalCode(g, b);
            root.addPath(code, i, 0);

            if (i % 100000 == 0) {
                System.out.println();
            } else if (i % 10000 == 0) {
                System.out.print("*");
            } else if (i % 1000 == 0) {
                System.out.print(".");
            }
        }
        root.addInfo();

        System.out.println();
        System.out.println("Tree size: " + root.size());
    }

    public List<Integer> supergraphSearch(Graph query) {
        return root.search(query, impl);
    }
}
