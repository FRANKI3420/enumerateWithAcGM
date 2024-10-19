package codetree.common;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;

import codetree.core.Graph;

public class SdfFileReader {
    static String graphID;
    static int count;
        
    public static List<Graph> readFile(Path sdfFile) {
        ArrayList<Graph> G = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(sdfFile)) {
            Graph g;
            while ((g = next(br)) != null) {
                g = g.shrink();
                if (g.isConnected()) {
                    G.add(g);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        return G;
    }

    private static Graph next(BufferedReader sdf) throws Exception {
        ArrayList<String> mol = new ArrayList<>();

        String line;
        while ((line = sdf.readLine()) != null) {
            if (line.startsWith("$$$$") && mol.size() > 0) {
                return readMol(mol);
            } else {
                mol.add(line);
            }
        }

        return null;
    }

    private static Graph readMol(List<String> mol) {
        String line = mol.get(0);
        final int id = Integer.parseInt(line.substring(0, 6).trim());

        line = mol.get(3);
        final int order = Integer.parseInt(line.substring(0, 3).trim());
        final int size = Integer.parseInt(line.substring(3, 6).trim());

        byte[] vertices = new byte[order];
        for (int i = 0; i < order; ++i) {
            line = mol.get(4 + i);
            final String label = line.substring(31, 33).trim();
            vertices[i] = VertexLabel.string2id(label);
        }

        byte[][] edges = new byte[order][order];
        for (int i = 0; i < size; ++i) {
            line = mol.get(4 + order + i);
            final int v = Integer.parseInt(line.substring(0, 3).trim()) - 1;
            final int u = Integer.parseInt(line.substring(3, 6).trim()) - 1;
            final int w = Integer.parseInt(line.substring(6, 9).trim());

            edges[v][u] = (byte) w;
            edges[u][v] = (byte) w;
        }

        return new Graph(id, vertices, edges);
    }

    public static List<Graph> readFile_gfu(Path sdfFile) {
        List<Graph> G = new ArrayList<>();
        count = 0;

        try (BufferedReader br = Files.newBufferedReader(sdfFile)) {
            Graph g;
            while ((g = next_gfu(br)) != null) {
                G.add(g);
            }
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }

        graphID = null;
        return G;
    }
    private static Graph next_gfu(BufferedReader sdf) throws Exception {
        ArrayList<String> mol = new ArrayList<>();

        String line;

        while ((line = sdf.readLine()) != null) {

            if (!line.startsWith("#0") && line.startsWith("#")) {
                graphID = line;
                return readMol_gfu(mol);
            } else {
                if (mol.size() == 0 && graphID != null)
                    mol.add(graphID);
                mol.add(line);
            }
        }
        if (count == 0) {
            count++;
            return readMol_gfu(mol);
        }

        return null;
    }
    private static Graph readMol_gfu(List<String> mol) {
        String line = mol.get(0);// グラフid
        final int id = Integer.parseInt(line.substring(1));

        line = mol.get(1);
        final int order = Integer.parseInt(line);

        byte[] vertices = new byte[order];
        for (int i = 0; i < order; ++i) {
            line = mol.get(2 + i);
            vertices[i] = (byte) Integer.parseInt(line);
        }

        final int size = Integer.parseInt(mol.get(2 + order));
        byte[][] edges = new byte[order][order];

        HashMap<Integer, BitSet> edgeBitset = new HashMap<>();
        for (int i = 0; i < order; i++) {
            edgeBitset.put(i, new BitSet());
        }

        for (int i = 0; i < size; ++i) {
            line = mol.get(3 + order + i);
            String[] nums = line.split(" ");
            final int v = Integer.parseInt(nums[0]);
            final int u = Integer.parseInt(nums[1]);
            final int w = 1;

            edges[v][u] = (byte) w;
            edges[u][v] = (byte) w;

            edgeBitset.get(v).set(u);
            edgeBitset.get(u).set(v);
        }
        return new Graph(id, vertices, edges, edgeBitset);
    }

    public static Graph readFileQuery_gfu(Path sdfFile) {

        try (BufferedReader br = Files.newBufferedReader(sdfFile)) {
            return query_gfu(br);
        } catch (Exception e) {
            System.err.println(e);
            System.exit(1);
        }
        return null;
    }
    
    private static Graph query_gfu(BufferedReader sdf) throws Exception {
        ArrayList<String> mol = new ArrayList<>();

        String line;

        while ((line = sdf.readLine()) != null) {
            mol.add(line);
        }
        return readMol_gfu(mol);
    }
}
