package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree that searches through GameState using minimax and alpha-beta pruning
 */
public class Minimax{
    private final Heuristic eval; //Heuristic used to evaluate board states
    private final MyGameState rootState;
    private boolean killSearch = false; //Stores whether search should terminate
    private final CachedDijkstra distance;
    private PVLine previousDepthPv;//Stores the PV from the previous depth
    private int maxSearchDepth;
    private KillerHeuristic currentKiller; //Stores killer heuristic for current search depth
    //Stores killer heuristic from previous search depth
    private KillerHeuristic previousKiller = new KillerHeuristic(0);
    private final HistoryHeuristic history;//Stores history heuristic table
    //Stores whether history heuristic should be used instead for move ordering
    private final boolean useHistoryHeuristic;

    /**
     * Gets root state
     * @return The root state
     */
    public MyGameState getRootState(){
        return this.rootState;
    }

    /**
     * Creates a new minimax object that can be used to search through concrete game state objects
     * @param eval The evaluation function
     * @param rootState The concrete root state (the state at which to start the search from). This
     *                  must be non-terminal.
     * @param distance The object to use when calculating distance between two nodes on graph
     * @param h An initialized history heuristic. This is passed to save time creating a new large table.
     * @param useHistoryHeuristic Whether the history heuristic should be used for move ordering.
     */
    public Minimax(Heuristic eval, MyGameState rootState, CachedDijkstra distance, HistoryHeuristic h,
                   boolean useHistoryHeuristic){
        if(rootState.getAvailableMoves().size() == 0 ||
                rootState.getAvailableMoves().asList().get(0).commencedBy().isDetective())
            throw new IllegalArgumentException("Root state can't be terminal and must be a MrX move");
        this.eval = eval;
        this.rootState = rootState;
        this.distance = distance;
        this.previousDepthPv = new PVLine();
        this.previousDepthPv.setNoMoves();
        this.history = h;
        this.useHistoryHeuristic = useHistoryHeuristic;
    }

    /**
     * Orders moves using the heuristic.
     * If MrX is making the moves, then moves that give the highest values are at the lowest indices.
     * If a detective is making the moves, then moves that give the lowest values are at the lowest indices.
     * @param moveAndFutureState The list of all moves and their corresponding future game states
     * @param isPvNode Whether the root node is part of the principal variation
     * @param ply The ply of the current search
     */
    private void orderMoves(List<Pair<MyGameState, Move>> moveAndFutureState, final boolean isPvNode, final int ply){
        //Creates data structure that stores evaluation of each state
        List<Pair<Pair<MyGameState, Move>, Pair<Float, Float>>> scoreAndIndex =
                new ArrayList<>(moveAndFutureState.size());
        //Evaluates state score
        for(int i = 0; i < moveAndFutureState.size(); i++){
            Pair<MyGameState, Move> stateMovePair = moveAndFutureState.get(i);
            float evalScore = eval.gameScore(stateMovePair.getLeft());//Gets state evaluation
            if(stateMovePair.getRight().commencedBy().isMrX()) evalScore *= -1;
            float priorityScore; //Stores priority based on whether PV, killer, history etc.
            if(isPvNode && previousDepthPv.getPvLine()[ply].equals(stateMovePair.getRight())){//Checks for PV move
                priorityScore = Float.NEGATIVE_INFINITY;
            }else if(currentKiller.isKillerMove(stateMovePair.getRight(), ply)){//Checks for killer move
                priorityScore = (Float.MAX_VALUE * -1);
            }else if(previousKiller.isKillerMove(stateMovePair.getRight(), ply)){
                //Checks for killer move from previous search - this will be given less priority than current killer
                priorityScore = (Float.MAX_VALUE * -1) + 1E32f;
            }else if(useHistoryHeuristic){//Uses history table score (if enabled)
                priorityScore = history.historyScore(stateMovePair.getRight()) * -1;
            }else{
                priorityScore = Float.POSITIVE_INFINITY;
            }
            scoreAndIndex.add(new Pair<>(stateMovePair, new Pair<>(priorityScore, evalScore)));
        }
        //Implements ordering - the best moves are at start of list
        //Priority is roughly of form PV - killer (current) - killer (prev) - history (if enabled)
        //Within these sub categories, sorting is done based on evaluation function
        scoreAndIndex.sort((x, y) -> {
            //Checks to see if priority scores are different (if they are, use this)
            int prio = Float.compare(x.getRight().getLeft(), y.getRight().getLeft());
            if(prio != 0) return prio;
            //Sorts based on evaluation score if priority is the same
            return Float.compare(x.getRight().getRight(), y.getRight().getRight());
        });
        //Sorts actual move data structure
        for(int i = 0; i < moveAndFutureState.size(); i++) {
            moveAndFutureState.set(i, scoreAndIndex.get(i).getLeft());
        }
    }

    /**
     * Gets the best move by performing the minimax search
     * @param maxDepth The maximum search depth allowed
     * @return The best possible move
     * @throws MinimaxSearchInterrupted If the search is interrupted an exception is thrown. This is done
     * to effectively kill threads using minimax search
     */
    public Move getBestMove(int maxDepth) throws MinimaxSearchInterrupted{
        this.maxSearchDepth = maxDepth;
        history.clearHistory();//Resets table
        currentKiller = new KillerHeuristic(maxDepth); //Creates killer heuristic for current round
        PVLine result = new PVLine();
        boolean isPv = previousDepthPv.getPvLine().length >= 1;//If previous depth not cached, no PV used
        minimaxSearch(new Pair<>(null, rootState), maxDepth,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, result, isPv);
        previousDepthPv = result;
        previousKiller = currentKiller;
        return result.getPvLine()[0];
    }

    /**
     * Performs an alpha-beta pruned search from the current state to determine the best move
     * @param moveAndState The root state and the move taken to get there
     * @param depth The maximum search depth
     * @param alpha The value of alpha
     * @param beta The value of beta
     * @param pline The principal variation
     * @param isPv Determines whether this state is part of the principal variation
     * @return The calculated value of the state along
     * @throws MinimaxSearchInterrupted If the minimax search should be interrupted this is thrown
     */
    private float minimaxSearch(Pair<Move, MyGameState> moveAndState, int depth, float alpha, float beta,
                                            PVLine pline, boolean isPv)
            throws MinimaxSearchInterrupted{
        if(killSearch) throw new MinimaxSearchInterrupted("Search Interrupted");
        PVLine line = new PVLine();
        //Checks to see if a terminal state or max depth has been reached
        MyGameState state = moveAndState.getRight();
        if(Heuristic.isTerminalState(state) || depth == 0){//Evaluates leaf node
            pline.setNoMoves();
            return eval.gameScore(state);
        }
        final int ply = maxSearchDepth - depth;
        //Order moves to maximise pruning
        List<Move> moves = state.getAvailableMoves().asList();
        List<Pair<MyGameState, Move>> futureStateAndMove = new ArrayList<>(moves.size());
        for(Move m : moves) futureStateAndMove.add(new Pair<>(state.advance(m), m));
        filterMoves(state, futureStateAndMove); //Filters some moves to speed up computation
        orderMoves(futureStateAndMove, isPv, ply);//Moves ordered to maximise AB pruning
        //Determines whether player is maximising or minimising and moves accordingly
        final boolean isMaximiser = moves.get(0).commencedBy().isMrX();
        float bestValue = isMaximiser? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        for(int i = 0; i < futureStateAndMove.size(); i++){
            final boolean isPvNextMove = isPv && i == 0 && (ply + 1 < previousDepthPv.getPvLine().length);
            Move move = futureStateAndMove.get(i).getRight();
            MyGameState stateNew = futureStateAndMove.get(i).getLeft();
            if(isMaximiser){//MrX move, hence maximising player
                float moveValue = minimaxSearch(new Pair<>(move, stateNew), depth - 1, alpha, beta, line,
                        isPvNextMove);
                bestValue = Math.max(bestValue, moveValue);
                if(bestValue >= beta){//beta cutoff
                    currentKiller.addKillerMove(move, ply);//Updates killer heuristic
                    history.cutoffEncountered(move, depth);//Updates history table
                    return beta;
                }
                if(bestValue > alpha){//alpha update
                    alpha = bestValue;
                    pline.setLine(move, line);//Updates principal variation
                }
            }else{//Detective move, hence minimising
                float moveValue = minimaxSearch(new Pair<>(move, stateNew), depth - 1,
                        alpha, beta, line, isPvNextMove);
                bestValue = Math.min(bestValue, moveValue);
                if(bestValue <= alpha) {//alpha cutoff
                    currentKiller.addKillerMove(move, ply);//Updates killer heuristic
                    history.cutoffEncountered(move, depth);//Updates history table
                    return alpha;
                }
                if(bestValue < beta){//Beta update
                    beta = bestValue;
                    pline.setLine(move, line);//Updates principal variation
                }
            }
        }
        return bestValue;
    }

    /**
     * Sets a flag that indicates ongoing search should be killed.
     */
    public void killSearch(){
        killSearch = true;
    }

    /**
     * A helper function for filtering MrX moves. Filters all double moves if MrX has any safe moves to make
     * (i.e. double moves should only be used if MrX has no safe single moves to make)
     * If MrX has no safe single moves to make, then only double moves should be considered.
     * Thus, this function will leave MrX with either only double moves or only single moves.
     * @param rootState The root state
     * @param futureStateAndMove The list of MrX moves
     */
    private void filterDoubleMoves(MyGameState rootState, List<Pair<MyGameState, Move>> futureStateAndMove){
        //Checks to see if MrX can be captured regardless of what single move he makes for the round
        //If he can be captured, double moves are permitted
        boolean isDoubleMovePresent = false;
        boolean allowDouble = true;
        for(Pair<MyGameState, Move> stateMovePair : futureStateAndMove){
            if(!allowDouble) break; //Early loop exit
            //Checks to see if move is a single move
            int noTickets = 0;
            for(ScotlandYard.Ticket t : stateMovePair.getRight().tickets()){
                noTickets ++;
            }
            boolean isSingleMove = noTickets == 1;
            if(isSingleMove){
                //If there are no available moves, state is terminal and MrX may be captured
                boolean canXBeCaptured = !stateMovePair.getLeft().getWinner().isEmpty() &&
                        !stateMovePair.getLeft().getWinner().contains(Piece.MrX.MRX);
                for(Move m : stateMovePair.getLeft().getAvailableMoves()){
                    Move.SingleMove detMove = (Move.SingleMove) m; //Cast is safe as detective moves are always single
                    if(detMove.destination == stateMovePair.getLeft().getMrX().location()){
                        canXBeCaptured = true;
                        break;
                    }
                }
                allowDouble = allowDouble && canXBeCaptured; //Updates double move allowance
            }else{
                isDoubleMovePresent = true;
            }
        }
        //Ensures that filtering won't mean that all moves are removed
        if((isDoubleMovePresent && allowDouble) || (!allowDouble)){
            //Filters all double moves or all single moves depending.
            final boolean onlyDouble = allowDouble;
            futureStateAndMove.removeIf(x -> {
                int noTickets = 0;
                for(ScotlandYard.Ticket t : x.getRight().tickets()){
                    noTickets ++;
                }
                return (noTickets > 1) != onlyDouble;
            });
        }
    }

    /**
     * Helper function for filtering mrx moves. Checks to see if player has enough secret tickets
     * to use until the end of the game. If this is true, non-secret moves are filtered
     * @param rootState The root state
     * @param futureStateAndMove MrX future moves
     * @return True is returned if only secret moves remain in list
     */
    private boolean filterNonSecretMoves(MyGameState rootState, List<Pair<MyGameState, Move>> futureStateAndMove){
        if(futureStateAndMove.size() == 0) return false; //Safety mechanism
        //Checks to see if MrX has enough secret tickets to exclusively make secret moves
        //until the end of the game (bar reveal rounds)
        MyGameState randomMoveState = futureStateAndMove.get(0).getLeft();
        final boolean isRevealRound = randomMoveState.getMrXTravelLog().
                get(randomMoveState.getMrXTravelLog().size() - 1).location().isPresent();
        int noSecret = rootState.getMrX().tickets().get(ScotlandYard.Ticket.SECRET);
        int noNoRevealMovesLeft = 0;
        for(int round = rootState.getMrXTravelLog().size(); round < rootState.getSetup().moves.size(); round++){
            if(rootState.getSetup().moves.get(round) == false) noNoRevealMovesLeft ++;
        }
        final boolean onlyUseSecretMoves = !isRevealRound && (noSecret >= noNoRevealMovesLeft);
        //Filters all non-secret moves if needed
        if(onlyUseSecretMoves){
            futureStateAndMove.removeIf(x -> {
                boolean onlySecret = true;
                for(ScotlandYard.Ticket t : x.getRight().tickets()){
                    onlySecret = onlySecret && (t == ScotlandYard.Ticket.SECRET || t == ScotlandYard.Ticket.DOUBLE);
                }
                return !onlySecret;
            });
        }
        return onlyUseSecretMoves;
    }

    /**
     * Helper function for filtering MrX moves. Checks to see if all
     * of MrX's neighbour nodes can be accessed by taxi
     * @param rootState The root game state
     * @return True if all the nodes can be accessed by taxi
     */
    private boolean allNeighbourNodesTaxi(MyGameState rootState){
        //Checks to see if all neighbouring nodes are taxis
        boolean isMrXConnectedByAllTaxis = true;
        for(Integer neigh : rootState.getSetup().graph.adjacentNodes(rootState.getMrX().location())){
            if(!isMrXConnectedByAllTaxis) break; //Early loop exit
            for (ScotlandYard.Transport t : rootState.getSetup().graph
                    .edgeValueOrDefault(neigh, rootState.getMrX().location(), ImmutableSet.of())){
                if(t.requiredTicket() != ScotlandYard.Ticket.TAXI && rootState.getMrX().has(t.requiredTicket())){
                    isMrXConnectedByAllTaxis = false;
                    break;
                }
            }
        }
        return isMrXConnectedByAllTaxis;
    }

    /**
     * Helper function for the Mr X filtering function. This function checks to see if all moves in the provided
     * array use at least one secret ticket. If it does, true is returned. Otherwise, false is returned
     * @param futureStateAndMove The list of all moves and their corresponding states
     * @return True if all moves in the list use at least one secret ticket, false otherwise
     */
    private boolean allMovesUseSecretTicket(List<Pair<MyGameState, Move>> futureStateAndMove){
        for(Pair<MyGameState, Move> m : futureStateAndMove){
            boolean usesSecretTicket = false;
            for(ScotlandYard.Ticket t : m.getRight().tickets()){
                if(t == ScotlandYard.Ticket.SECRET) usesSecretTicket = true;
            }
            if(usesSecretTicket == false) return false;
        }
        return true;
    }

    /**
     * Filters MrX moves that we as humans known are bad options and shouldn't be considered.
     * For example:
     * Secret tickets shouldn't be used in first three rounds.
     * Secret tickets shouldn't be used if it's a reveal round
     * Secret tickets shouldn't be used if node is only connected to other nodes by taxis
     * Double moves should only ever be used if detectives can capture MrX on next go for all possible single moves.
     * If MrX has enough secret tickets so that he can make every non-reveal move a secret move,
     * then he should only use secret tickets
     * @param rootState The root game state
     * @param futureStateAndMove The list of MrX moves
     */
    private void filterMrXMoves(MyGameState rootState, List<Pair<MyGameState, Move>> futureStateAndMove){
        filterDoubleMoves(rootState, futureStateAndMove); //Removes bad double moves
        final boolean onlySecretMovesInList = filterNonSecretMoves(rootState, futureStateAndMove);
        if(futureStateAndMove.size() == 0) return; //Safety mechanism
        MyGameState randomMoveState = futureStateAndMove.get(0).getLeft();
        final boolean isRevealRound = randomMoveState.getMrXTravelLog().
                get(randomMoveState.getMrXTravelLog().size() - 1).location().isPresent();
        final boolean isFirstThreeRounds = randomMoveState.getMrXTravelLog().size() <= 3;
        final boolean isMrXConnectedByAllTaxis = allNeighbourNodesTaxi(rootState);
        final boolean allRemainingMovesAreSecret = allMovesUseSecretTicket(futureStateAndMove);
        //Removes all moves that use secret ticket if needed
        final boolean removeSecretMoves = !onlySecretMovesInList && !allRemainingMovesAreSecret &&
                (isFirstThreeRounds || isRevealRound || isMrXConnectedByAllTaxis);
        if(removeSecretMoves){
            futureStateAndMove.removeIf(x -> {
                boolean usesSecret = false;
                for(ScotlandYard.Ticket t : x.getRight().tickets()){
                    usesSecret = usesSecret || t == ScotlandYard.Ticket.SECRET;
                }
                return usesSecret;
            });
        }
    }

    /**
     * Filters detectives moves that lead to unnecessary additional complexity.
     * This is done to reduce the size of the search space and allow deeper searches.
     * To achieve this, we assume that the closest detective to MrX should always move first.
     * Thus, all moves that aren't from this detective are filtered. This of course means
     * that detectives are far less coordinated, but the increased search depth this enables justifies this
     * @param rootState The game state before the moves made
     * @param futureStateAndMove The list of detectives moves
     */
    private void filterDetectiveMoves(MyGameState rootState, List<Pair<MyGameState, Move>> futureStateAndMove){
        int closestDistance = Integer.MAX_VALUE;
        Piece closestDet = null;
        int xLocation = rootState.getMrX().location();
        //Calculates closest detective
        for(Pair<MyGameState, Move> move : futureStateAndMove){
            Move.SingleMove sm = (Move.SingleMove) move.getRight(); //Cast is safe as det moves are single moves
            int distanceBetween = distance.getDistance(xLocation, sm.destination);
            if(distanceBetween <= closestDistance){
                closestDistance = distanceBetween;
                closestDet = move.getRight().commencedBy();
            }
        }
        if(closestDet != null){
            final Piece closestPiece = closestDet;
            //Removes all detectives that aren't closest from available moves
            futureStateAndMove.removeIf(x -> x.getRight().commencedBy() != closestPiece);
        }
    }

    /**
     * Filters certain moves from search space to speed up computation
     * @param rootState The root state
     * @param futureStateAndMove All moves resulting from the root state
     */
    private void filterMoves(MyGameState rootState, List<Pair<MyGameState, Move>> futureStateAndMove){
        final boolean mrXMove = futureStateAndMove.get(0).getRight().commencedBy() ==
                futureStateAndMove.get(0).getLeft().getMrX().piece();
        if(mrXMove){
            filterMrXMoves(rootState, futureStateAndMove);
        }else{
            filterDetectiveMoves(rootState, futureStateAndMove);
        }
        if(futureStateAndMove.size() == 0){//Fail-safe to ensure that function doesn't filter all possible moves
            System.err.println("Removed all available moves");
            //Adds all available moves back
            for(Move m : rootState.getAvailableMoves()){
                futureStateAndMove.add(new Pair<>(rootState.advance(m), m));
            }

        }
    }

    /**
     * Class used to indicate that the minimax search has been abandoned
     */
    public final class MinimaxSearchInterrupted extends Exception{
        //As exception implements/extends serializable, a serial ID must be produced
        private static final long serialVersionUID = 5772711486014408980L;
        /**
         * Creates new exception that indicates the minimax search has been ended
         * @param errorMessage The error message to display
         */
        public MinimaxSearchInterrupted(String errorMessage) { super(errorMessage); }
    }
}
