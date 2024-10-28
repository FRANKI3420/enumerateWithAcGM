package codetree.core;

import java.util.*;
import java.util.concurrent.*;

import codetree.common.Pair;

class ParallelIndexNode
    extends IndexNode
{
    ParallelIndexNode(IndexNode parent, CodeFragment frag)
    {
        super(parent, frag);
    }

    List<Integer> search(Graph q, GraphCode impl)
    {
        HashSet<IndexNode> result0 = new HashSet<>();

        ForkJoinPool pool = new ForkJoinPool();

        List<Pair<IndexNode, SearchInfo>> infoList = impl.beginSearch(q, this);
        ArrayList<ForkJoinTask<Void>> tasks = new ArrayList<>(infoList.size());

        for (Pair<IndexNode, SearchInfo> info: infoList) {
            tasks.add(pool.submit(new SupergraphSearch(info.left, result0, info.right, impl, q)));
        }

        for (ForkJoinTask<Void> task: tasks) {
            task.join();
        }

        ArrayList<Integer> result = new ArrayList<>();

        for (IndexNode p: result0) {
            result.addAll(p.matchGraphIndices);

            final int c = p.matchGraphIndices.size();
            for (; p != null; p = p.parent) {
                p.count += c;
            }
        }

        return result;
    }

    private static class SupergraphSearch
        extends RecursiveAction
    {
        private static final long serialVersionUID = 1L;

        IndexNode node;
        Set<IndexNode> result;
        SearchInfo info;
        GraphCode impl;
        Graph graph;

        SupergraphSearch(IndexNode node, Set<IndexNode> result, SearchInfo info, GraphCode impl, Graph graph)
        {
            this.node = node;
            this.result = result;
            this.info = info;
            this.impl = impl;
            this.graph = graph;
        }

        @Override
        public void compute()
        {
            final int c = node.matchGraphIndices.size();
            if (c > 0) {
                synchronized(result) {
                    if (!result.contains(node)) {
                        result.add(node);

                        for (IndexNode p = node; p != null; p = p.parent) {
                            p.count -= c;
                        }
                    }
                }
            }

            List<Pair<CodeFragment, SearchInfo>> nextFrags = impl.enumerateFollowableFragments(graph, info);
            ArrayList<SupergraphSearch> tasks = new ArrayList<>(nextFrags.size());

            for (IndexNode m: node.children) {
                if (m.count > 0) {
                    for (Pair<CodeFragment, SearchInfo> frag: nextFrags) {
                        if (frag.left.contains(m.frag)) {
                            tasks.add(new SupergraphSearch(m, result, frag.right, impl, graph));
                        }
                    }
                }
            }

            invokeAll(tasks);
        }
    }
}
