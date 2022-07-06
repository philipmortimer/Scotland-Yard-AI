package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Scoring function to be used in paranoid minimax search.
 * Scoring function heavily inspired by:
 * Nijssen, J.A.M. & Winands, Mark.
 * (2012).
 * Monte Carlo Tree Search for the Hide-and-Seek Game Scotland Yard.
 * IEEE Transactions on Computational Intelligence and AI in Games.
 * 4. 282 - 294. 10.1109/TCIAIG.2012.2210424.
 * https://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.704.9552&rep=rep1&type=pdf
 * Weights altered slightly based on tests performed against detective AI
 */
public class ClosestDetectiveHeuristic extends Heuristic {
    private final CachedDijkstra d;
    private final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> cachedGraph;

    /**
     * Creates a scoring function that looks primarily at the closest detective
     * @param graphForCache The graph to be cached (i.e. used)
     */
    public ClosestDetectiveHeuristic(ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphForCache){
        this.cachedGraph = graphForCache;
        d = CachedDijkstra.preComputeAllDistances(graphForCache, new DistanceWeighting());
    }

    @Override
    public float gameScore(MyGameState state) {
        Heuristic.State currentState = Heuristic.getGameState(state);
        //Game is over
        if(currentState == State.detWins) return Heuristic.DETECTIVE_WINS;
        if(currentState == State.xWins) return Heuristic.MRX_WINS;
        //Computes closest distance
        float closestDistance = Float.POSITIVE_INFINITY;
        float meanDistance = 0;
        for(Player det : state.getDetectives()){
            float dist = d.getDistance(det.location(), state.getMrX().location());
            closestDistance = Math.min(closestDistance, dist);
            meanDistance += dist;
        }
        meanDistance = meanDistance / (float) state.getDetectives().size();
        //Gets the number of possible locations for MrX
        int noLocations = state.getMrXPossibleLocations().size();
        //Gets number of secret tickets MrX has
        int noSecret = state.getMrX().tickets().get(ScotlandYard.Ticket.SECRET);
        //Score computing formula
        float score = (90 * closestDistance) + (1 * noSecret) + (2 * noLocations)
                + (10 * meanDistance);
        //Clips score to sensible range
       return Heuristic.clipValue(score);
    }

    /**
     * Gets object used to calculate distance between two points on graph
     * @return The distance calculating object
     */
    public CachedDijkstra getDistanceCalculator() {
        return d;
    }

    /**
     * Checks that the graph stored in the board has the same graph as the one used for the heuristic.
     * The game score function can only be safely used if this attribute holds
     * @param state The board state
     * @return Whether the graph state matches
     */
    public boolean graphEqualsCachedGraph(Board state){
        return cachedGraph.equals(state.getSetup().graph);
    }
}
