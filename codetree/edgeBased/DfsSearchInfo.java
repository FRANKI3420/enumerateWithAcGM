package codetree.edgeBased;

import java.util.*;

import codetree.core.*;

class DfsSearchInfo
    implements SearchInfo
{
    Stack<Integer> rightmostPath;

    boolean[][] closed;

    int numVertices;
    int[] map;

    DfsSearchInfo(Graph graph, int v0)
    {
        rightmostPath = new Stack<>();
        rightmostPath.push(v0);

        final int n = graph.order();

        closed = new boolean[n][n];
        closed[v0][v0] = true;

        numVertices = 1;
        map = new int[n];
    }

    @SuppressWarnings("unchecked")
    DfsSearchInfo(DfsSearchInfo src, int v, int u)
    {
        if (src.rightmostPath.peek() != v) {
            throw new IllegalArgumentException("Illegal vertex.");
        }

        rightmostPath = (Stack<Integer>)src.rightmostPath.clone();

        closed = cloneMatrix(src.closed);
        closed[v][u] = true;
        closed[u][v] = true;
        closed[v][v] = true;
        closed[u][u] = true;

        map = src.map.clone();
        if (src.closed[u][u]) { // backward edge
            numVertices = src.numVertices;
        } else { // forward edge
            rightmostPath.push(u);
            map[u] = src.numVertices;
            numVertices = src.numVertices + 1;
        }

    }

    private static boolean[][] cloneMatrix(boolean[][] src)
    {
        boolean[][] dest = new boolean[src.length][src[0].length];

        for (int i = 0; i < src.length; ++i) {
            dest[i] = src[i].clone();
        }

        return dest;
    }
}
