package codetree.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import codetree.common.*;

public class Graph implements Serializable
{
    public final int id;

    public final byte[] vertices;
    public final byte[][] edges;

    public int[][] adjList;

    public int size;
    public int order;
    public BitSet filterFlag;
    public HashMap<Integer, BitSet> edgeBitset;

    public Graph(int id, byte[] vertices, byte[][] edges) {
        this.id = id;
        this.vertices = vertices;
        this.edges = edges;
        this.order = this.order();
        this.size = this.size();
        filterFlag = new BitSet();
        edgeBitset = this.getEdgeBitset();
    }

    public Graph(int id, byte[] vertices, byte[][] edges, HashMap<Integer, BitSet> edgeBitset) {
        this.id = id;
        this.vertices = vertices;
        this.edges = edges;
        this.order = this.order();
        this.size = this.size();
        this.edgeBitset = edgeBitset;
        filterFlag = new BitSet();
    }

    private HashMap<Integer, BitSet> getEdgeBitset() {

        HashMap<Integer, BitSet> edgeBitset = new HashMap<>();

        int n = order;
        for (int i = 0; i < n; i++) {
            BitSet value = new BitSet();
            for (int j = 0; j < n; j++) {
                if (edges[i][j] > 0) {
                    value.set(j);
                }
            }
            edgeBitset.put(i, value);
        }
        return edgeBitset;
    }


    private int[][] makeAdjList()
    {
        final int n = order();
        int[][] adjList = new int[n][];

        ArrayList<Integer> adj = new ArrayList<>();
        for (int v = 0; v < n; ++v) {
            for (int u = 0; u < n; ++u) {
                if (edges[v][u] > 0) {
                    adj.add(u);
                }
            }

            final int s = adj.size();

            adjList[v] = new int[s];
            for (int i = 0; i < adj.size(); ++i) {
                adjList[v][i] = adj.get(i);
            }

            adj.clear();

            /*for (int i = 0; i + 1 < s; i++) {
				int min = i;
				for (int j = i + 1; j < s; j++) {
					if (edges[v][adjList[v][min]] > edges[v][adjList[v][j]]
							|| (edges[v][adjList[v][min]] == edges[v][adjList[v][j]] && vertices[adjList[v][min]] > vertices[adjList[v][j]]))
						min = j;
				}
				if (min != i) {
					int temp = adjList[v][i];
					adjList[v][i] = adjList[v][min];
					adjList[v][min] = temp;
				}
			}*/
        }

        return adjList;
    }

    public int order()
    {
        return vertices.length;
    }

    public int size()
    {
        int s = 0;

        final int n = order();
        for (int v = 0; v < n; ++v) {
            for (int u = 0; u < n; ++u) {
                if (edges[v][u] > 0) {
                    ++s;
                }
            }
        }

        return s / 2;
    }

    public Graph shrink()
    {
        final byte H = VertexLabel.string2id("H");

        int[] map = new int[order()];
        int order = 0;
        for (int v = 0; v < map.length; ++v) {
            if (vertices[v] != H) {
                map[order++] = v;
            }
        }

        byte[] vertices = new byte[order];
        byte[][] edges = new byte[order][order];

        for (int v = 0; v < order; ++v) {
            vertices[v] = this.vertices[map[v]];

            for (int u = 0; u < order; ++u) {
                edges[v][u] = this.edges[map[v]][map[u]];
            }
        }

        return new Graph(id, vertices, edges);
    }

    public boolean isConnected()
    {
        ArrayDeque<Integer> open = new ArrayDeque<>();
        ArrayList<Integer> closed = new ArrayList<>();

        open.add(0);
        closed.add(0);

        final int n = order();

        while (!open.isEmpty()) {
            int v = open.poll();

            for (int u = 0; u < n; ++u) {
                if (edges[v][u] > 0 && !closed.contains(u)) {
                    open.add(u);
                    closed.add(u);
                }
            }
        }

        return closed.size() == n;
    }

    public byte getMaxVertexLabel()
    {
        byte max = -1;

        for (int v = 0; v < order(); ++v) {
            if (max < vertices[v]) {
                max = vertices[v];
            }
        }

        return max;
    }

    public byte getMinVertexLabel()
    {
        byte min = Byte.MAX_VALUE;

        for (int v = 0; v < order(); ++v) {
            if (min > vertices[v]) {
                min = vertices[v];
            }
        }

        return min;
    }

    public List<Integer> getVertexList(byte label)
    {
        ArrayList<Integer> res = new ArrayList<>();

        for (int v = 0; v < order(); ++v) {
            if (vertices[v] == label) {
                res.add(v);
            }
        }

        return res;
    }

    public void writeGraph2Gfu(BufferedWriter bw2) throws IOException {
        bw2.write("#" + id + "\n");
        bw2.write(order() + "\n");
        for (int i = 0; i < order(); i++) {
            bw2.write(vertices[i] + "\n");
        }

        bw2.write(size() + "\n");
        for (int i = 0; i < order(); i++) {
            for (int j = i; j < order(); j++) {
                if (edges[i][j] > 0) {
                    bw2.write(i + " " + j + "\n");
                }
            }
        }
        bw2.flush();
    }
    public void writeGraph2GfuAddeLabel(BufferedWriter bw2) throws IOException {
        bw2.write("#" + id + "\n");
        bw2.write(order() + "\n");
        for (int i = 0; i < order(); i++) {
            bw2.write(vertices[i] + "\n");
        }

        bw2.write(size() + "\n");
        for (int i = 0; i < order(); i++) {
            for (int j = i; j < order(); j++) {
                if (edges[i][j] > 0) {
                    bw2.write(i + " " + j +" "+ edges[i][j]+"\n");
                }
            }
        }
        bw2.flush();
    }

    public static int numOflabels(List<Graph> G) {
        Set<Byte> label = new HashSet<>();

        for (int i = 0; i < G.size(); i++) {
            Graph g = G.get(i);
            for (int v = 0; v < g.order(); ++v) {
                label.add(g.vertices[v]);
            }
        }
        return label.size();
    }

    public BitSet labels_Set() {

        BitSet labels = new BitSet();
        for (int v = 0; v < order; ++v) {
            labels.set(vertices[v]);
        }

        return labels;
    }

    public HashSet<Integer> getTargetVertices(int limDepth, int start_vertice) {
        HashSet<Integer> target = new HashSet<>();
        target.add(start_vertice);
        Random rand = new Random(0);
        boolean[] visited = new boolean[order];
        visited[start_vertice] = true;
        BitSet open = new BitSet();
        for (int v = edgeBitset.get(start_vertice).nextSetBit(0); v != -1; v = edgeBitset.get(start_vertice)
                .nextSetBit(++v)) {
            open.set(v);
        }

        for (int i = 0; i < limDepth - 1; i++) {
            ArrayList<Integer> next = new ArrayList<>();

            for (int v = open.nextSetBit(0); v != -1; v = open
                    .nextSetBit(++v)) {
                if (!visited[v]) {
                    next.add(v);
                }
            }

            if (next.size() == 0) {
                return target;
            }

            int random = rand.nextInt(next.size());
            start_vertice = next.get(random);
            target.add(start_vertice);
            visited[start_vertice] = true;
            for (int v = edgeBitset.get(start_vertice).nextSetBit(0); v != -1; v = edgeBitset.get(start_vertice)
                    .nextSetBit(++v)) {
                open.set(v);
            }
        }
        return target;
    }

    public Graph generateInducedGraph(HashSet<Integer> targetVertices) {

        int n = targetVertices.size();
        byte[] newvertices = new byte[n];
        byte[][] newedges = new byte[n][n];
        int count = 0;
        for (int v : targetVertices) {
            newvertices[count++] = vertices[v];
        }
        count = 0;
        int count2 = 0;
        for (int v : targetVertices) {
            for (int u : targetVertices) {
                if (edgeBitset.get(v).get(u)) {
                    newedges[count][count2] = 1;
                    newedges[count2][count] = 1;
                }
                count2++;
            }
            count++;
            count2 = 0;
        }

        return new Graph(id, newvertices, newedges);
    }
}
