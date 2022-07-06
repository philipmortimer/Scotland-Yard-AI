package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;


/**
 * Class that uses Dijkstra's algorithm as a heuristic to score a board state.
 * This structure uses the sum of the distance between the detectives and MrX as a score.
 */
public class DijkstraHeuristic extends Heuristic{
    private final CachedDijkstra graphDist;
    private final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> cachedGraph;

    /**
     * Creates a new heuristic that uses the distance between players on graph to produce game score
     * @param graphForCache The graph to be used for calculating the scores (can't be null)
     */
    public DijkstraHeuristic(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphForCache){
        if(graphForCache == null) throw new IllegalArgumentException("Null graph not allowed");
        this.cachedGraph = graphForCache;
        graphDist = CachedDijkstra.preComputeAllDistances(graphForCache, new OneCostPerEdge());
    }

    @Override
    /**
     * Calculates the game score for a given state.
     * <br>
     * <b> The callee is responsible for ensuring
     * that {@link DijkstraHeuristic#graphEqualsCachedGraph(Board)} is true for the given state.
     * If called when this property does not hold, the behaviour is undefined and
     * will likely cause errors.
     */
    public float gameScore(MyGameState state){
        Heuristic.State currentState = Heuristic.getGameState(state);
        //Game is over
        if(currentState == State.detWins) return Heuristic.DETECTIVE_WINS;
        if(currentState == State.xWins) return Heuristic.MRX_WINS;
        int totalDistance = 0;
        //Loops through all detectives and find their distance from MrX
        for(Player det : state.getDetectives()){
            totalDistance += getDistance(state.getMrX().location(), det.location());
        }
        //Keeps total distance within a reasonable range
        return Heuristic.clipValue(totalDistance);
    }

    /**
     * Checks that the graph stored in the board has the same graph as the one used for the heuristic
     * @param state The board state
     * @return Whether the graph state matches
     */
    public boolean graphEqualsCachedGraph(Board state){
        return cachedGraph.equals(state.getSetup().graph);
    }

    /**
     * Gets the distance between two points on the graph.
     * @param start The start node
     * @param dest The end node
     * @return The distance between the two points
     */
    private int getDistance(Integer start, Integer dest){
        return graphDist.getDistance(start, dest);
    }

}
