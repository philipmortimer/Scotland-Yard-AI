package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.standardGraph;

/**
 * Class that implements a paranoid MrX search.
 * A paranoid search assumes that detective AI always know where MrX is and thus
 * plays the worst possible move for MrX. This is obviously a flawed assumption
 * as a massive element of the game is maximising the uncertainty of MrX's location.
 * However, this reduces the search cost considerably. We counteract this somewhat
 * by using an evaluation function that rewards MrX's location being less certain
 * from the detectives point of view.
 */
public class ParanoidSearchMrX implements AiCustom {
    private ClosestDetectiveHeuristic heuristicFunction = null;
    private HistoryHeuristic h = new HistoryHeuristic();
    private CachedDijkstra dist = null;
    private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> standardGrph = null;

    @Nonnull
    @Override
    public String name() { return "Paranoid Minimax MrX"; }

    @Override
    public void onStart() {
        //Creates a heuristic that uses Dijkstra's to measure minimum distance between players and MrX
        try{
            //Loads the default graph and precomputes distances using Dijkstra's
            standardGrph = standardGraph();
            dist = CachedDijkstra.preComputeAllDistances(standardGrph, new OneCostPerEdge());
            heuristicFunction = new ClosestDetectiveHeuristic(standardGrph);
        }catch(IOException e){
            System.err.println("Error when retrieving standard graph for closest heuristic. " + e);
        }
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        long startTime = System.currentTimeMillis();
        //This is an AI only for MrX
        if(board.getAvailableMoves().asList().get(0).commencedBy().isDetective())
            throw new IllegalArgumentException("Paranoid Search can only be applied for MrX moves");
        List<MyGameState> currentStates = MyGameState.getPossibleStates(board);
        if(currentStates.size() != 1){//Fail-safe, this should never occur
            System.err.println("Error when calculating possible states. MrX states should only be of size 1");
            return new RandomAI().pickMove(board, timeoutPair);
        }
        MyGameState rootState = currentStates.get(0);
        //Checks to see if failure occurred when creating object or the graph is different
        if(heuristicFunction == null || !heuristicFunction.graphEqualsCachedGraph(board)){
            heuristicFunction = new ClosestDetectiveHeuristic(board.getSetup().graph);
            System.err.println("Pre computation was unsuccessful");
        }
        if(dist == null) dist = CachedDijkstra.preComputeAllDistances(board.getSetup().graph, new OneCostPerEdge());
        //Determines whether history heuristic can be safely used
        boolean useHistory = standardGrph != null && standardGrph.equals(board.getSetup().graph);
        //Performs Minimax search
        h.clearHistory();//Ensures table is in correct start state
        Minimax minimax = new Minimax(heuristicFunction, rootState, dist, h, useHistory);
        //Calculates end time
        long timeBeforeSearch = System.currentTimeMillis() - startTime;
        //Allows program specified ms to return value after ID search finishes
        //This parameter can normally be safely set to 10 ms or less. However, I have chosen a very high value
        //to ensure that the AI just never times out.
        long timeNeededAfterThreadTermination = 200;
        long computeTime =
                TimeUnit.MILLISECONDS.convert(timeoutPair.left(), timeoutPair.right()) - timeBeforeSearch
                        - timeNeededAfterThreadTermination;
        if(computeTime < 0) computeTime = 0;
        //Starts thread and allows it to run for certain amount of time
        IterativeDeepening iterativeDeep = new IterativeDeepening(minimax);
        Thread t = new Thread(iterativeDeep);
        t.start();
        try {
            Thread.sleep(computeTime);
        } catch (InterruptedException e) {
            System.err.println("Sleeping thread failed " + e);
        }
        t.interrupt();
        iterativeDeep.killSearch();
        Move bestMove = iterativeDeep.getBestMove();
        if(bestMove == null || !board.getAvailableMoves().contains(bestMove)){//Failsafe
            bestMove = board.getAvailableMoves().asList().get(0);
            System.err.println("Error in minimax search");
        }
        return bestMove;
    }

    @Override
    public void onTerminate() {

    }
}
