package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;


/**
 * Class used to implement history heuristic for minimax search.
 * This is typically regarded as a depth independent implementation of the killer heuristic.
 */
public final class HistoryHeuristic {
    private static final int NO_PIECES = Piece.Detective.values().length + 1;
    private static final int NODES_ON_GRAPH = 199;//Number of nodes on graph
    private static final int TICKET_TYPES = ScotlandYard.Ticket.values().length - 1;
    /**
     * Table is indexed as follows:
     * [piece][ticket][startLocation][endLocation]
     * Note this doesn't exactly specify every possible move. Double moves are simply
     * too computationally expensive to specify (hence, they are treated as single moves)
     */
    private final int[][][][] historyTable = new int[NO_PIECES][TICKET_TYPES][NODES_ON_GRAPH][NODES_ON_GRAPH];

    /**
     * Updates history table with cutoff move
     * @param m The move
     * @param depth The depth the move was encountered at
     */
    public void cutoffEncountered(Move m, int depth){
        int piece = pieceIndex(m.commencedBy());
        int ticket = ticketIndex(m);
        int start = startLocationIndex(m);
        int dest = lastLocationIndex(m);
        historyTable[piece][ticket][start][dest] += depth * depth;
    }

    /**
     * Gets the associated history score for a piece
     * @param m The move made
     * @return The history heuristic score
     */
    public int historyScore(Move m){
        int piece = pieceIndex(m.commencedBy());
        int ticket = ticketIndex(m);
        int start = startLocationIndex(m);
        int dest = lastLocationIndex(m);
        return historyTable[piece][ticket][start][dest];
    }

    /**
     * Gets index used for last location of move
     * @param m The move made
     * @return The index
     */
    private static int lastLocationIndex(Move m){
        int node;
        if(isDoubleMove(m)){
            Move.DoubleMove dm = (Move.DoubleMove) m;
            node = dm.destination2;
        }else{
            Move.SingleMove sm = (Move.SingleMove) m;
            node = sm.destination;
        }
        return node - 1;
    }

    /**
     * Checks to see if move is a double move
     * @param m The move
     * @return True if it is a double move, false otherwise
     */
    private static boolean isDoubleMove(Move m){
        int noTickets = 0;
        for(ScotlandYard.Ticket t : m.tickets()) noTickets++;
        return noTickets > 1;
    }

    /**
     * Gets index for first ticket used
     * @param m The move made
     * @return The index
     */
    private static int ticketIndex(Move m){
        ScotlandYard.Ticket firstTicket = m.tickets().iterator().next();
        Integer index =  switch(firstTicket){
            case BUS -> 0;
            case TAXI -> 1;
            case UNDERGROUND -> 2;
            case DOUBLE -> -1;//This should never occur here
            case SECRET -> 3;
        };
        return index;
    }

    /**
     * Sets all values in array to 0. As array is so large, this may be needed to save allocating
     * new array on stack
     */
    public void clearHistory(){
        for(int i = 0; i < NO_PIECES; i++){
            for(int j = 0; j < TICKET_TYPES; j++){
                for(int k = 0; k < NODES_ON_GRAPH; k++){
                    for(int z = 0; z < NODES_ON_GRAPH; z++){
                        historyTable[i][j][k][z] = 0;
                    }
                }
            }
        }
    }

    /**
     * Gets index used for first location of move
     * @param m The move made
     * @return The index
     */
    private static int startLocationIndex(Move m){
        return m.source() - 1;
    }

    /**
     * Returns the index of the table that the piece maps into
     * @param p The piece
     * @return The index
     */
    private static int pieceIndex(Piece p){
        int index;
        if(p.isMrX()){
            index = 0;
        }else{
            //Gets ordinal value. Cast is safe as piece must be a detective
            index = ((Piece.Detective) p).ordinal() + 1;
        }
        return index;
    }
}
