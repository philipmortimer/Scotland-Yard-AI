package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;
import java.util.Random;

/**
 * Performs iterative deepening. This allows AI to dynamically search by time instead of using
 * a fixed depth.
 */
public class IterativeDeepening implements Runnable{
    private Move bestMove; //Stores best move
    private final Minimax minimax;
    private int depth;

    /**
     * Creates object used to implement iterative deepening
     * @param minimax The minimax object
     */
    public IterativeDeepening(Minimax minimax){
        this.minimax = minimax;
        //Initialises a default root state to ensure that some value is always selected
        List<Move> moves = minimax.getRootState().getAvailableMoves().asList();
        bestMove = moves.get(new Random().nextInt(moves.size()));
    }

    @Override
    public void run() {
        //Performs iterative deepening (increasing search depth until system runs out of time)
        depth = 0;
        try {
            while (!Thread.interrupted()) {
                bestMove = minimax.getBestMove(depth + 1);
                depth++;
            }
        }
        catch(Minimax.MinimaxSearchInterrupted e){}
    }

    /**
     * Attempts to kill minimax search
     */
    public void killSearch(){
        minimax.killSearch();
    }

    /**
     * Retrieves the calculated best move
     * @return The best available move
     */
    public Move getBestMove() { return bestMove; }
}
