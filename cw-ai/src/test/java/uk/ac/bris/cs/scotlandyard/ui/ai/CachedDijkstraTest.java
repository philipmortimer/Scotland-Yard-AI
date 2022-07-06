package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Tests that Dijkstra's algorithm has been implemented correctly
 */
public class CachedDijkstraTest extends ParameterisedModelTestBase{
    @Test
    /**
     * Tests that pre-computed distance calculations work
     */
    public void DistancePreComputed(){
        var graph = standardGraph();
        CachedDijkstra c = CachedDijkstra.preComputeAllDistances(graph, x -> 1);
        //Tests that all values are precomputed
        testDistance(c);
    }

    @Test
    /**
     * Tests that distance calculations work for non-precomputed objects
     */
    public void DistanceNotPreComputed(){
        CachedDijkstra c = CachedDijkstra.preComputeAllDistances(standardGraph(), x -> 1);
        var graph = standardGraph();
        testDistance(c);
    }

    /**
     * Tests that distance between two nodes is calculated correctly using the standard graph
     * @param c The CacheDijkstra object to test
     */
    private void testDistance(CachedDijkstra c){
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = standardGraph();
        for(Integer start : graph.nodes()){
            for(Integer dest : graph.nodes()){
                //Asserts that all distances are positive (or zero if source and destination are same)
                if(dest == start) assert(c.getDistance(start, dest) == 0);
                else assert (c.getDistance(start, dest) >= 1);
            }
        }
        //Uses some hand calculated values for the standard graph provided
        //Note score returned should be the minimum moves needed to reach a square by somebody who has infinite cards
        //(bar double move cards)
        assert (c.getDistance(140, 140) == 0);
        assert (c.getDistance(140, 132) == 1);
        assert (c.getDistance(140, 133) == 1);
        assert (c.getDistance(140, 154) == 1);
        assert (c.getDistance(140, 156) == 1);
        assert (c.getDistance(140, 153) == 1);
        assert (c.getDistance(140, 184) == 2);
        assert (c.getDistance(140, 183) == 3);
        assert (c.getDistance(140, 108) == 3);
        assert (c.getDistance(115, 108) == 1);
        assert (c.getDistance(1, 128) == 4);
    }
}
