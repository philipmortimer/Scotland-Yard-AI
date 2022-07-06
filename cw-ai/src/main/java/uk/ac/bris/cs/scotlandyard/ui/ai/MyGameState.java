package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * This class stores all the details needed to encode a given game state.
 * The idea of this class is to have a state where all parties know MrX's location.
 */
public final class MyGameState implements Board.GameState {
    private final GameSetup setup;
    //Remaining stores the set of all pieces that should currently move
    private final ImmutableSet<Piece> remaining;
    private final ImmutableList<LogEntry> log;
    private final Player mrX;
    private final List<Player> detectives;
    private final ImmutableSet<Move> moves;
    private final ImmutableSet<Piece> winner;
    //Stores the set of possible mrX locations. Note that this set should always be from the detectives point of view.
    private final Set<Integer> mrXPossibleLocations;

    /**
     * Creates a new GameState using a board object and a known MrX location.
     * @param state The board object
     * @param mrXLocation MrX's location
     */
    private MyGameState(Board state, Integer mrXLocation){
        setup = state.getSetup();
        log = state.getMrXTravelLog();
        moves = state.getAvailableMoves();
        winner = state.getWinner();
        mrX = createPlayer(Piece.MrX.MRX, mrXLocation, state);
        detectives = new ArrayList<>(state.getPlayers().size());
        for(Piece.Detective p : Piece.Detective.values()){//Creates list of detectives
            Optional<Integer> location = state.getDetectiveLocation(p);
            if(location.isPresent()) detectives.add(createPlayer(p, location.get(), state));
        }
        //Looks at all available moves to find which pieces make moves
        ImmutableSet.Builder<Piece> remainBuild = ImmutableSet.builder();
        for(Move m : state.getAvailableMoves()){
            remainBuild.add(m.commencedBy());
        }
        remaining = remainBuild.build();
        //Calculates all possible mrX locations
        mrXPossibleLocations = MrXLocations.getPossibleMrXLocations(state, false);
    }

    /**
     * Gets all possible GameStates given the current board (i.e. by looking at all possible locations
     * for MrX in the current position).
     * @param b The current board
     * @return All possible states
     */
    public static List<MyGameState> getPossibleStates(Board b){
        Set<Integer> locations = MrXLocations.getPossibleMrXLocations(b, true);
        List<MyGameState> states = new ArrayList<>(locations.size());
        for(Integer xPlace : locations){
            states.add(new MyGameState(b, xPlace));
        }
        return states;
    }

    /**
     * Creates a new player object as specified
     * @param p The piece
     * @param location The location of the piece
     * @param state The board state used to create the player from
     * @return The player object
     */
    private static Player createPlayer(Piece p, Integer location, Board state){
        //Handles creation of ticket map
        TicketBoard tickets = state.getPlayerTickets(p).get();
        ImmutableMap.Builder<ScotlandYard.Ticket, Integer> ticketCount = ImmutableMap.builder();
        for(ScotlandYard.Ticket t : ScotlandYard.Ticket.values()){
            ticketCount.put(t, tickets.getCount(t));
        }
        return new Player(p, ticketCount.build(), location);
    }

    /**
     * Creates a new GameState
     * @param setup The game setup
     * @param remaining The remaining pieces
     * @param log MrX's travel log
     * @param mrX MrX Player Object
     * @param detectives The list of detectives
     * @param mrXPossibleLocations The set of possible MrXLocations
     */
    private MyGameState(final GameSetup setup,
                        final ImmutableSet<Piece> remaining,
                        final ImmutableList<LogEntry> log,
                        final Player mrX,
                        final List<Player> detectives,
                        final Set<Integer> mrXPossibleLocations){
        //Sets object attributes
        this.mrXPossibleLocations = mrXPossibleLocations;
        this.setup = setup;
        this.remaining = remaining;
        this.log = log;
        this.mrX = mrX;
        this.detectives = detectives;

        //Performs validation of method parameters - see error messages for details
        if(setup.graph.nodes().size() == 0) throw new IllegalArgumentException("Graph is empty!");
        if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
        if(detectives == null) throw new NullPointerException("Detective is null!");
        Set<Integer> detectiveNodes = new HashSet<>(detectives.size());
        for(Player player: detectives){
            if(player == null) throw new NullPointerException("A player is null!");
            if(player.isMrX()) throw new IllegalArgumentException("The detective list contains MrX!");
            if(detectiveNodes.contains(player.location()))
                throw new IllegalArgumentException("Multiple detectives are on the same node!");
            detectiveNodes.add(player.location());
            if(player.tickets().get(ScotlandYard.Ticket.SECRET) != 0)
                throw new IllegalArgumentException("A detective has a secret ticket!");
            if(player.tickets().get(ScotlandYard.Ticket.DOUBLE) != 0)
                throw new IllegalArgumentException("A detective has a double ticket!");
        }
        //There must be exactly one MrX in game
        if(mrX == null) throw new NullPointerException("MrX is null!");
        if(!mrX.isMrX()) throw new IllegalArgumentException("The MrX provided is not MrX");
        //Checks that no duplicate detectives are present
        if(new HashSet<>(detectives).size() != detectives.size())
            throw new IllegalArgumentException("There are duplicate detectives!");

        //Calculates and sets the available moves and winner
        moves = ImmutableSet.copyOf(getAllAvailableMoves(setup, remaining, mrX, detectives, log));
        this.winner = calculateWinner(setup, moves, remaining, detectives, mrX);
    }

    /**
     * Calculates the winner of the game at this current state (if there is a winner).
     * @param moves All legal moves
     * @param detectives The list of detectives
     * @param mrX The mrX player
     * @return A set containing only mrX if mrX wins. A set containing all the detectives if
     * the detectives win. If there is no winner yet, returns an empty set.
     */
    private static ImmutableSet<Piece> calculateWinner(GameSetup setup,
                                                       ImmutableSet<Move> moves,
                                                       ImmutableSet<Piece> remaining,
                                                       List<Player> detectives,
                                                       Player mrX){
        //Create sets that represent game states
        ImmutableSet<Piece> detWins = ImmutableSet.copyOf(detectives.stream().
                map(x -> x.piece()).iterator());
        ImmutableSet<Piece> mrXWins = ImmutableSet.of(mrX.piece());
        ImmutableSet<Piece> noWinnersYet = ImmutableSet.of();
        //if moves are available then the game can continue, no moves available means game is over.
        if (!moves.isEmpty()) return noWinnersYet;
        //detectives win if they share location with mr. x
        if(isDetectiveSquare(detectives, mrX.location())) return detWins;
        //detectives also win if mr.x is stranded, can't move to any unoccupied locations (on mrx turn)
        //Checks to see whether all of mrX's neighbours are occupied, if's his turn
        if(remaining.contains(mrX.piece())){
            if(isMrXStranded(setup, mrX, detectives)) return detWins;
        }
        //Checked all conditions for detective win or draw, hence MrX must have won
        return mrXWins;
    }

    /**
     * Checks to see if MrX is stranded (has no squares he can legally move to)
     * @param setup The game setup
     * @param mrX The mrX player
     * @param detectives The list of detectives
     * @return True if stranded, false otherwise
     */
    private static boolean isMrXStranded(final GameSetup setup,
                                         Player mrX,
                                         List<Player> detectives){
        //Loops through all neighbouring nodes of Mrx.
        //For each node, it checks whether MrX has the ticket(s) required to reach the node.
        //If he does, it checks to see whether a detective is on this neighbouring node.
        //If there is at least one neighbouring node that MrX can legally move to, he isn't stranded
        for(Integer neighbourNode : setup.graph.adjacentNodes(mrX.location())){
            boolean canDetectiveReachNode = mrX.has(ScotlandYard.Ticket.SECRET);
            if(!canDetectiveReachNode){
                //Checks to see if mrX can reach node
                for(ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(mrX.location(), neighbourNode,
                        ImmutableSet.of())){
                    if(mrX.has(t.requiredTicket())){
                        canDetectiveReachNode = true;
                        break;
                    }
                }
            }
            boolean detectiveAdjacent = isDetectiveSquare(detectives, neighbourNode);
            if(canDetectiveReachNode && !detectiveAdjacent){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks to see whether the given location is the same as one of the detectives
     * @param detectives The list of detectives
     * @param location The location to check
     * @return True if location is same as a detective.
     */
    private static boolean isDetectiveSquare(List<Player> detectives, Integer location){
        //Loops through all detectives and checks to see if any of them have the required
        //location.
        for(Player d : detectives){
            if(d.location() == location) return true;
        }
        return false;
    }


    @Nonnull
    @Override
    public GameSetup getSetup() {
        return setup;
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getPlayers() {
        //Adds all players to a set
        HashSet<Piece> players = new HashSet<>(detectives.size() + 1);
        for (Player d : detectives){
            players.add(d.piece());
        }
        players.add(mrX.piece());
        return ImmutableSet.copyOf(players);
    }

    public Player getMrX() {
        return mrX;
    }

    @Nonnull
    @Override
    public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
        //Loops through detectives and finds location
        for (Player d : detectives){
            if (d.piece() == detective) return Optional.of(d.location());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<TicketBoard> getPlayerTickets(Piece piece) {
        //Finds player object and returns the number of tickets they have
        for (Player d : detectives){
            if (d.piece() == piece) return Optional.of(new Tickets(d.tickets()));
        }
        if (piece == mrX.piece()){
            return Optional.of(new Tickets(mrX.tickets()));
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public ImmutableList<LogEntry> getMrXTravelLog() {
        return log;
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getWinner() {
        return winner;
    }

    /**
     * Calculates all legal moves that can be made by a given player
     * starting at a given location
     * @param setup The game setup
     * @param detectives The list of detectives
     * @param player The player to move
     * @param source The starting node to move from
     * @return All legal moves
     */
    private static Set<Move.SingleMove> getSingleMoves(GameSetup setup,
                                                       List<Player> detectives,
                                                       Player player,
                                                       int source){
        Set<Move.SingleMove> singleMoves = new HashSet<>(); //Stores legal moves
        //Loops through neighbouring nodes to check if they can be moved to
        for(int destination : setup.graph.adjacentNodes(source)) {
            //If there is no detective on square, checks to see if player has tickets needed to make move.
            if (!isDetectiveSquare(detectives, destination)) {
                //Loops through possible modes of transportation and see if player has needed ticket.
                for (ScotlandYard.Transport t :
                        setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    if (player.has(t.requiredTicket())) {
                        singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                    }
                }
                //Handles secret moves
                if (player.has(ScotlandYard.Ticket.SECRET)) {
                    singleMoves.add(new Move.SingleMove(player.piece(), source,
                            ScotlandYard.Ticket.SECRET, destination));
                }
            }
        }
        return singleMoves; //Returns moves
    }

    /**
     * Returns all legal moves for the current game state
     * @param setup The game setup
     * @param remaining The remaining pieces that are able to move
     * @param mrX The mrX player object
     * @param detectives The list of all detectives
     * @param log Mr's travel log
     * @return All available legal moves
     */
    private static Set<Move> getAllAvailableMoves(GameSetup setup,
                                                  ImmutableSet<Piece> remaining,
                                                  Player mrX,
                                                  List<Player> detectives,
                                                  ImmutableList<LogEntry> log){
        HashSet<Move> allMoves= new HashSet<>();
        //if detective is on same spot as mr x then there are no available moves
        if(isDetectiveSquare(detectives, mrX.location())) return Set.of();
        //check if all detectives are stranded
        //Even though it is MrX's turn, if detectives are all stranded, mrX wins
        if (remaining.contains(mrX.piece())) {
            if (getAllAvailableMoves(setup, ImmutableSet.copyOf(detectives.stream().
                    map(x -> x.piece()).iterator()), mrX, detectives, log).isEmpty()){
                return Set.of();
            }
        }
        //check if we have a double move available.
        for (Piece p : remaining){
            Player currentPlayer = getPlayerFromPiece(p, detectives, mrX); //Gets player that piece represents
            //Gets available moves if player is detective. Only gets MrX moves if they have space remaining
            //in the travel log.
            if (currentPlayer.isDetective() || (currentPlayer.isMrX() && log.size() < setup.moves.size())) {
                Set<Move.SingleMove> currentPlayerMoves =
                        getSingleMoves(setup, detectives, currentPlayer, currentPlayer.location());
                allMoves.addAll(currentPlayerMoves);
                //Checks to see if MrX can make a double move (by checking that log has two spaces and that
                // mrX has a double ticket). If he does, add double moves to all legal moves.
                if (currentPlayer.has(ScotlandYard.Ticket.DOUBLE) && log.size() +1 < setup.moves.size()){
                    //For each single move, check if MrX may make an additional move after it
                    for (Move.SingleMove sm : currentPlayerMoves){
                        //Makes single move with MrX using a ticket to reach square and
                        // calculates all legal moves after making the first move.
                        Set<Move.SingleMove> doubleMoves = getSingleMoves(setup, detectives,
                                currentPlayer.use(sm.ticket), sm.destination);
                        for (Move.SingleMove dm : doubleMoves){
                            //Adds new double moves to all moves
                            allMoves.add(new Move.DoubleMove(sm.commencedBy(), sm.source(), sm.ticket,
                                    sm.destination, dm.ticket, dm.destination));
                        }
                    }
                }
            }
        }
			/*
			It's possible that some, but not all, detectives will be able to move. If this is the case
			then remaining will eventually contain a set of detectives who can't move. However, if some
			detectives have already moved this round, then the game is not done. Hence, it just
			skips to MrX's turn.
			 */
        if(allMoves.isEmpty() && !remaining.contains(mrX.piece()) && remaining.size() < detectives.size()){
            return getAllAvailableMoves(setup, ImmutableSet.of(mrX.piece()), mrX, detectives, log);
        }
        return allMoves;
    }

    /**
     * Gets Player object that corresponds to piece
     * @param p The piece to find the corresponding player of
     * @param detectives The list of detectives
     * @param mrX The mrX player
     * @return The player object where player.piece() == p.
     *  IllegalArgumentException is thrown if no players have the piece
     */
    private static Player getPlayerFromPiece(Piece p,
                                             List<Player> detectives,
                                             Player mrX){
        if(p == mrX.piece()) return mrX;
        for(Player det : detectives)
            if(det.piece() == p) return det;
        throw new IllegalArgumentException("Piece not found");
    }


    @Nonnull
    @Override
    public ImmutableSet<Move> getAvailableMoves() {
        return moves;
    }

    /**
     * Makes a single MrX move and returns a new game state (which reflects this new move).
     * @param gameState The game state before the move
     * @param sm The move to make
     * @return The state after the move has been made in the game state passed to the method.
     */
    private MyGameState performMrXSingleMove(MyGameState gameState, Move.SingleMove sm){
        //Generates updated MrX log
        List<LogEntry> newLog = new ArrayList<>(gameState.getMrXTravelLog().size() + 1);
        newLog.addAll(gameState.getMrXTravelLog());
        //Adds log entry, checking whether MrX should reveal location or not
        LogEntry newEntry;
        if (gameState.setup.moves.get(gameState.log.size())) {
            newEntry = LogEntry.reveal(sm.ticket, sm.destination);
        }
        else newEntry = LogEntry.hidden(sm.ticket);
        newLog.add(newEntry);
        ImmutableList<LogEntry> newImmLog= ImmutableList.copyOf(newLog);
        //consumes ticket and updates location
        Player newMrX = gameState.getMrX().use(sm.ticket).at(sm.destination);
        //swap to detectives turn
        ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(gameState.detectives.stream()
                .map(x -> x.piece()).iterator());
        //Updates MrX possible locations
        Set<Integer> mrxLocationsNew = MrXLocations.getPossibleMrXLocations(gameState.mrXPossibleLocations, newEntry,
                gameState.detectives, gameState.setup.graph);
        //Returns new game state
        return new MyGameState(gameState.setup, newRemaining,
                newImmLog, newMrX, gameState.detectives, mrxLocationsNew);
    }

    /**
     * Makes a single detective move and returns a new game state (which reflects this new move).
     * @param gameState The game state before the move
     * @param sm The move to make
     * @return The state after the move has been made in the game state passed to the method.
     */
    private MyGameState performDetectiveSingleMove(MyGameState gameState, Move.SingleMove sm){
        //Updates set of possible MrX locations
        Set<Integer> mrXLocsNew = MrXLocations.getPossibleMrXLocations(gameState.mrXPossibleLocations, sm);
        //match piece to player, consume ticket and update location
        Player currentPlayer = getPlayerFromPiece(sm.commencedBy()
                , gameState.detectives, gameState.getMrX()).use(sm.ticket).at(sm.destination);
        Player newMrX = gameState.getMrX().give(sm.ticket);//Gives ticket to MrX
        //Updates remaining set: if all detectives have moved, sets it to MrX's turn.
        //Otherwise, just removes detective that has currently moved from the set
        ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(gameState.remaining.stream().
                filter(x-> x != currentPlayer.piece()).iterator());
        if (newRemaining.size() == 0){
            newRemaining = ImmutableSet.of(gameState.getMrX().piece());
        }
        //Updates list of detectives to reflect the fact the detective moving is different
        // (they've used a ticket and moved location)
        List<Player> newDetectives = new ArrayList<>();
        for (Player d : gameState.detectives){
            if (d.piece() == currentPlayer.piece()) newDetectives.add(currentPlayer);
            else newDetectives.add(d);
        }
        //Returns updated game state
        return new MyGameState(gameState.setup, newRemaining,
                gameState.log, newMrX, newDetectives, mrXLocsNew);
    }

    /**
     * Performs a single move
     * @param gameState The game state before the move
     * @param sm The single move to make.
     * @return The new game state after the move has been made
     */
    private MyGameState performSingleMove(MyGameState gameState, Move.SingleMove sm){
        //Checks to see if it's detective or MrX move and updates state accordingly
        if (sm.commencedBy().isMrX()){
            return performMrXSingleMove(gameState, sm);
        }else{
            return performDetectiveSingleMove(gameState, sm);
        }
    }

    /**
     * Performs a double move.
     * @param gameState The starting game state (before the move)
     * @param dm The double move to make
     * @return The state after the double move has been made.
     */
    private MyGameState performDoubleMove(MyGameState gameState, Move.DoubleMove dm){
        //Essentially makes two single moves, ensuring that the correct amount of tickets is used
        //(including double move ticket).
        Move.SingleMove move1 = new Move.SingleMove(dm.commencedBy(), dm.source(), dm.ticket1, dm.destination1);
        Move.SingleMove move2 = new Move.SingleMove(dm.commencedBy(), dm.destination1, dm.ticket2, dm.destination2);
        MyGameState gameState1 = performSingleMove(gameState, move1);
        MyGameState gameState2 = performSingleMove(gameState1, move2);
        return new MyGameState(gameState2.setup, gameState2.remaining, gameState2.log,
                gameState2.mrX.use(ScotlandYard.Ticket.DOUBLE), gameState2.detectives,
                gameState2.mrXPossibleLocations);
    }


    @Nonnull
    @Override
    public MyGameState advance(Move move) {
        //Checks to see if move is legal and makes it if it is.
        if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
        MyVisitor v = new MyVisitor(this);
        return move.accept(v);
    }

    /**
     * Visitor class that facilitates moves being made to advance GameState
     */
    private class MyVisitor implements Move.Visitor<MyGameState> {
        MyGameState g;

        /**
         * Creates new visitor that allows moves to be made on a GameState
         * @param g The game state
         */
        MyVisitor(MyGameState g) {
            this.g = g;
        }

        @Override
        public MyGameState visit(Move.SingleMove singleMove) {
            return performSingleMove(g, singleMove); //Performs the single move
        }

        @Override
        public MyGameState visit(Move.DoubleMove doubleMove) {
            return performDoubleMove(g, doubleMove); //Performs the double move
        }
    }

    /**
     * Class to calculate number of each ticket a player has
     */
    private static final class Tickets implements Board.TicketBoard {
        ImmutableMap<ScotlandYard.Ticket, Integer> noOfEachTicket;


        /**
         * Creates a new object that allows us to calculate the quantity of each ticket
         * a player has.
         * @param noOfEachTicket The maps between tickets and number of tickets.
         */
        Tickets(ImmutableMap<ScotlandYard.Ticket, Integer> noOfEachTicket){
            this.noOfEachTicket = noOfEachTicket;
        }

        @Override
        public int getCount(@Nonnull ScotlandYard.Ticket ticketType) {
            return this.noOfEachTicket.get(ticketType);
        }
    }

    /**
     * Gets the list of all detective players
     * @return The list of detectives
     */
    public List<Player> getDetectives() {
        return detectives;
    }

    /**
     * Returns set of all possible MrX locations
     * @return All possible mrx locations
     */
    public Set<Integer> getMrXPossibleLocations(){
        return mrXPossibleLocations;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true; //Reference equality check
        if(o == null) return false; //Null check
        if(o.getClass() != getClass()) return false;
        MyGameState st = (MyGameState) o;
        boolean sameWinner = st.winner.equals(winner);
        boolean sameRemaining = st.remaining.equals(remaining);
        boolean sameMrX = st.mrX.location() == mrX.location()
                && st.mrX.tickets().equals( mrX.tickets());
        boolean sameSetup = st.setup.equals(setup);
        boolean sameLog = st.log.equals(log);
        boolean sameMoves = st.moves.equals(moves);
        boolean sameDetectives = st.detectives.equals(detectives);
        return sameWinner && sameRemaining && sameMrX && sameSetup
                && sameLog && sameMoves && sameDetectives;
    }

    @Override
    public int hashCode(){
        return Objects.hash(winner, remaining, mrX, setup, log, moves, detectives);
    }

}
