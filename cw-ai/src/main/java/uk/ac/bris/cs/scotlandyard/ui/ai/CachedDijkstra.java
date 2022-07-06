package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An object that is used to calculate distance between two points.
 * This class caches searches already made, to speed up access.
 */
public final class CachedDijkstra{
    private final int[][] distances; //Stores the distance between two nodes
    //The graph used for the currently cached values
    private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphForCache;
    //Map that converts between node ID's and distances indices
    private final HashMap<Integer, Integer> nodeToDistanceIndex;
    private final EdgeValue val;
    /**
     * Creates dijkstra object that precomputes all values for the given graph
     * @param graphForCache The graph to cache
     * @param val The method used to calculate edge costs
     */
    private CachedDijkstra(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphForCache,
                           EdgeValue val){
        this.val = val;
        //Initialises values
        distances = new int[graphForCache.nodes().size()][graphForCache.nodes().size()];
        int i = 0;
        nodeToDistanceIndex = new HashMap<>(graphForCache.nodes().size());
        for(Integer node : graphForCache.nodes()){
            nodeToDistanceIndex.put(node, i);
            i++;
        }
        //Caches all nodes
        for(Integer startNode : graphForCache.nodes()){
            calculateAndCacheAllDistances(graphForCache, startNode);
        }
    }

    /**
     * Precomputes all distances for a given graph
     * @param graphForCache The graph to be cached
     * @param val The method used to calculate edge costs
     * @return The object
     */
    public static CachedDijkstra preComputeAllDistances(
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphForCache,
            EdgeValue val){
        return new CachedDijkstra(graphForCache, val);
    }

    /**
     * Gets the distance between two points on the cached graph
     * @param start The start node
     * @param end The end node
     * @return The distance
     */
    public int getDistance(Integer start, Integer end){
        return distances[nodeToDistanceIndex.get(start)][nodeToDistanceIndex.get(end)];
    }
    /**
     * Calculates the distances between all points from a given start node anc caches the values
     * @param graph The graph to use
     * @param startNode The node to start from
     */
    private void calculateAndCacheAllDistances(ImmutableValueGraph<Integer,
                                                        ImmutableSet<ScotlandYard.Transport>> graph,
                                               Integer startNode){
        //Creates and initialises needed datastructures
        HashMap<Integer, DijkstraNode> settled = new HashMap<>(graph.nodes().size());
        HashMap<Integer, DijkstraNode> allNodesNode = new HashMap<>(graph.nodes().size());
        PriorityQueue<DijkstraNode> unsettled = new PriorityQueue<>(graph.nodes().size());
        for(Integer keyVal : graph.nodes()){
            allNodesNode.put(keyVal,
                    new DijkstraNode(keyVal, keyVal == startNode?
                            DijkstraNode.DIST_SOURCE : DijkstraNode.DIST_INFINITY));
        }
        unsettled.add(allNodesNode.get(startNode));
        final int startNodeIndex = nodeToDistanceIndex.get(allNodesNode.get(startNode).getValue());
        //Main loop of Dijkstra's algorithm
        while(!unsettled.isEmpty()){
            DijkstraNode currNode = unsettled.remove();
            Set<Integer> neighbours = graph.adjacentNodes(currNode.getValue());
            //Loops through all adjacent nodes and updates cost if needed
            for(Integer keyVal : neighbours){
                DijkstraNode adjNode = allNodesNode.get(keyVal);
                //Items in the settled set have the min distance already calculated
                if(!settled.containsKey(adjNode.getValue())){
                    Integer newDist = currNode.getTentDistance() +
                            val.getEdgeCost(graph.edgeValue(currNode.getValue(), keyVal).get());
                    if(adjNode.getTentDistance() == DijkstraNode.DIST_INFINITY ||
                            adjNode.getTentDistance() > newDist){
                        adjNode.setTentDistance(newDist);
                    }
                    unsettled.remove(adjNode);
                    unsettled.add(adjNode);//Adds neighbour to unsettled category
                }
            }
            //Adds current node to settled set
            settled.put(currNode.getValue(), currNode);
            //Caches searched node
            distances[startNodeIndex][nodeToDistanceIndex.get(currNode.getValue())] =
                    currNode.getTentDistance();
        }
    }
}