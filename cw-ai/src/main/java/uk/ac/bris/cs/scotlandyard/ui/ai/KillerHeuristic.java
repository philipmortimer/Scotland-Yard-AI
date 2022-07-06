package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

/**
 * Class used to implement killer heuristic. This stores moves which lead to a cutoff at
 * the provided search depth
 */
public final class KillerHeuristic {
    private static final int NO_MOVES_PER_PLY = 3; //Stores number of killer moves per ply
    private final Move[][] killerTable;

    /**
     * Creates an object that stores the killer moves for the current search
     * @param maxSearchDepth The max search depth of the current search
     */
    public KillerHeuristic(int maxSearchDepth){
        killerTable = new Move[maxSearchDepth][NO_MOVES_PER_PLY];
    }

    /**
     * Adds a killer move to the table. Replacement scheme ensures that if this move is already
     * in table, no other move is overwritten
     * @param killer The move to add
     * @param ply The ply
     */
    public void addKillerMove(Move killer, int ply){
        if(!isKillerMove(killer, ply) && NO_MOVES_PER_PLY >= 0){//If it's already in table, nothing is done
            //Shifts entire array one to the left and adds killer move at 0 index
            Move prevMove = killerTable[ply][0];
            for(int i = 1; i < NO_MOVES_PER_PLY; i++){
                Move currMove = killerTable[ply][i];
                killerTable[ply][i] = prevMove;
                prevMove = currMove;
            }
            killerTable[ply][0] = killer;
        }
    }

    /**
     * Checks to see whether specified move is a killer move
     * @param m The move to check
     * @param ply The ply at which this move occurred
     * @return True if it is in the killer table, false otherwise
     */
    public boolean isKillerMove(Move m, int ply){
        if(ply >= killerTable.length) return false; //If ply is out of range, value isn't stored in table
        //Loops through entire table to check if move is present
        boolean isKiller = false;
        for(int i = 0; (i < NO_MOVES_PER_PLY) && (killerTable[ply][i] != null) && (!isKiller); i++){
            isKiller = isKiller || killerTable[ply][i].equals(m);
        }
        return isKiller;
    }
}
