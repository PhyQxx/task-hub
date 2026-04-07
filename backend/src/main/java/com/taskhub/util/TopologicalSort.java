package com.taskhub.util;

import java.util.*;

public class TopologicalSort {

    /**
     * Kahn算法拓扑排序，返回排序后的任务列表。如果有环，返回null。
     */
    public static List<String> sort(Map<String, Set<String>> graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : graph.keySet()) {
            inDegree.putIfAbsent(node, 0);
            for (String neighbor : graph.get(node)) {
                inDegree.put(neighbor, inDegree.getOrDefault(neighbor, 0) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (String node : inDegree.keySet()) {
            if (inDegree.get(node) == 0) {
                queue.offer(node);
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String node = queue.poll();
            result.add(node);
            Set<String> neighbors = graph.getOrDefault(node, Collections.emptySet());
            for (String neighbor : neighbors) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (result.size() != inDegree.size()) {
            return null; // 存在环路
        }
        return result;
    }

    /**
     * 检测环路，返回环路中的节点列表（如果有）
     */
    public static List<String> detectCycle(Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();
        List<String> cycleNodes = new ArrayList<>();

        for (String node : graph.keySet()) {
            if (detectCycleDFS(node, graph, visited, recStack, cycleNodes)) {
                return cycleNodes;
            }
        }
        return null;
    }

    private static boolean detectCycleDFS(String node, Map<String, Set<String>> graph,
                                          Set<String> visited, Set<String> recStack,
                                          List<String> cycleNodes) {
        if (recStack.contains(node)) {
            cycleNodes.add(node);
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }
        visited.add(node);
        recStack.add(node);

        Set<String> neighbors = graph.getOrDefault(node, Collections.emptySet());
        for (String neighbor : neighbors) {
            if (detectCycleDFS(neighbor, graph, visited, recStack, cycleNodes)) {
                if (!cycleNodes.isEmpty() && !cycleNodes.get(cycleNodes.size() - 1).equals(node)) {
                    cycleNodes.add(node);
                }
                return true;
            }
        }
        recStack.remove(node);
        return false;
    }
}
