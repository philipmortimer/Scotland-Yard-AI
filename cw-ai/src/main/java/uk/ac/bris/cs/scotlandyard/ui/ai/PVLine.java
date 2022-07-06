package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

/**
 * Class to store a principal variation line used in a minimax search
 */
public final class PVLine {
    private Move[] pvLine; //The PV sequence of moves

    /**
     * Sets it so that pvLine has no moves
     */
    public void setNoMoves(){
        pvLine = new Move[0];
    }

    /**
     * Creates a new pv line that uses the provided line plus one move prepended.
     * @param rootMove The move to add to the start of the line
     * @param line The line itself
     */
    public void setLine(Move rootMove, PVLine line){
        pvLine = new Move[line.pvLine.length + 1];
        pvLine[0] = rootMove;
        for(int i = 0; i < line.pvLine.length; i ++){
            pvLine[i + 1] = line.pvLine[i];
        }
    }

    /**
     * Gets principal variation
     * @return The principal variation
     */
    public Move[] getPvLine() {
        return pvLine;
    }
}
