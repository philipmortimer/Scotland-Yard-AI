package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.standardGraph;

/**
 * Class that uses the mean distance between all detectives and MrX as a score mechanism.
 * Looks one move into the future for both MrX and the detectives and makes the move.
 * If MrX's location isn't known (e.g. by the detectives), then the score is aggregated across
 * all possible MrX locations.
 */
public class OneMoveAheadDijkstraAi implements AiCustom {
    private DijkstraHeuristic heuristicFunction = null;
    private final double probabilityOfChoosingBestMove; //Stores probability AI chooses best move
    private static final double bestAiProb = 1.0;
    private static final double mediumAiProb = 0.5;

    /**
     * Returns the probability of the AI selecting the best available move
     * @param prob The probability (0.0, 1.0]
     */
    private OneMoveAheadDijkstraAi(double prob){
        this.probabilityOfChoosingBestMove = prob;
    }

    /**
     * Returns the best possible AI (i.e. has a 100% probability of choosing the best calculated answer).
     * @return The AI
     */
    public static OneMoveAheadDijkstraAi bestPossibleOneMove(){
        return new OneMoveAheadDijkstraAi(bestAiProb);
    }

    /**
     * Returns a OneMoveAhead AI that is of medium skill level.
     * This has a chance less than 100% of choosing the best possible move (per the one move look ahead calculations)
     * @return The AI
     */
    public static OneMoveAheadDijkstraAi mediumDifficultyAi(){
        return new OneMoveAheadDijkstraAi(mediumAiProb);
    }

    @Override public void onStart(){
        //Creates a heuristic that uses Dijkstra's to measure distance between players and MrX
        try{
            //Loads the default graph and precomputes distances using Dijkstra's
            heuristicFunction = new DijkstraHeuristic(standardGraph());
        }catch(IOException e){
            System.err.println("Error when retrieving standard graph. " + e);
        }
    }

    @Override public void onTerminate(){}

    @Nonnull
    @Override public String name() { return "One Move Dijkstra with p " + probabilityOfChoosingBestMove; }

    @Nonnull @Override public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        //Checks to see if failure occurred when creating object or the graph is different
        if(heuristicFunction == null || !heuristicFunction.graphEqualsCachedGraph(board)){
            heuristicFunction = new DijkstraHeuristic(board.getSetup().graph);
            System.err.println("Pre computation was unsuccessful");
        }
        //Checks to see if heuristic is for correct board
        // Gets a move that minimises / maximises score function for next move
        //One move lookahead using Dijkstra's
        boolean mrXTurn = board.getAvailableMoves().asList().get(0).commencedBy().isMrX();
        float bestValue = mrXTurn? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        Move bestMove = null;
        //Stores the total score for each move across all XLocation permutations
        float[] moveValue = new float[board.getAvailableMoves().size()];
        ImmutableList<Move> moves = board.getAvailableMoves().asList();
        //Loops through all possible states (if it's Detectives turn, they may not know where MrX is exactly)
        for(MyGameState startState : MyGameState.getPossibleStates(board)){
            for(int i = 0; i < moveValue.length; i++){
                MyGameState newBoard = startState.advance(moves.get(i));
                float score = heuristicFunction.gameScore(newBoard);
                moveValue[i] += score;
            }
        }
        //Selects best move
        for(int i = 0; i < moveValue.length; i++){
            if((mrXTurn && moveValue[i] >= bestValue) || (!mrXTurn && moveValue[i] <= bestValue)){
                bestValue = moveValue[i];
                bestMove = moves.get(i);
            }
            //Checks that heuristic doesn't lead to overflow
            if(moveValue[i] == Float.POSITIVE_INFINITY || moveValue[i] == Float.NEGATIVE_INFINITY
            || moveValue[i] == Float.NaN) System.err.println("Bad heuristic leads to values being off");
        }
        //Selects best move based on chosen probability. Otherwise, a random move is chosen
        if(Math.random() > probabilityOfChoosingBestMove){
            bestMove = moves.get(new Random().nextInt(moves.size()));
        }
        //Fail safe
        if(bestMove == null || !board.getAvailableMoves().contains(bestMove)){
            System.err.println("No best move was calculated");
            bestMove = board.getAvailableMoves().asList().get(0);
        }
        return bestMove;
    }
}
