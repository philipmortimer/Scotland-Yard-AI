package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;

/**
 * A heuristic interface (/abstract class) that allows for static evaluation of a given game state
 */
public abstract class Heuristic {
    //Large values chosen to represent win / loss. Note they aren't
    // infinity because AI aggregates score over multiple possible game scores.
    // you can't achieve this with infinity. (infinitity + 1 = infinitity + 100 as far as computer is concerned)
    public static final float MRX_WINS = 10000;
    public static final float DETECTIVE_WINS = 0;
    enum State {xWins, detWins, gameStillGoing};
    /**
     * Estimates the score of a game state for a given game
     * @param state The current game state (note that all parties know where MrX is).
     * @return The value of the game state as a number. Positive means mrX
     * is winning. Negative means detectives are winning.
     */
    public abstract float gameScore(MyGameState state);

    /**
     * Returns the state of the game currently
     * @param state The current board state
     * @return The status of the game
     */
    public static final Heuristic.State getGameState(Board state){
        if(state.getWinner().isEmpty()) return State.gameStillGoing;
        return state.getWinner().contains(Piece.MrX.MRX)? State.xWins : State.detWins;
    }

    /**
     * Checks to see if game is in a terminal position
     * @param state The board state
     * @return True if a terminal position is reached, false otherwise
     */
    public static final boolean isTerminalState(Board state){
        return !getGameState(state).equals(State.gameStillGoing);
    }

    /**
     * Clips a value so that it is between the range of MRX_WINS and DETECTIVE_WINS
     * @param x The value to clip
     * @return The clipped value
     */
    public static final float clipValue(float x){
        float org = x;
        x = x > MRX_WINS? MRX_WINS : x;
        x = x < DETECTIVE_WINS? DETECTIVE_WINS : x;
        if(org != x) System.err.println("Bad heuristic leads to clipping of range");
        return x;
    }
}
