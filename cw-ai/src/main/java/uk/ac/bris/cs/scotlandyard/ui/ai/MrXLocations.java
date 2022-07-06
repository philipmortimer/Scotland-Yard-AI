package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Class used to find location of MrX
 */
public final class MrXLocations {
    /**
     * Gets the location of MrX from the current board
     * @param b The board state
     * @return The location of MrX. Empty is returned if there are no legal moves or if it's not
     * MrX's turn to move next
     */
    private static Optional<Integer> getMrXLocation(Board b){
        if(b.getAvailableMoves().isEmpty() ||
                b.getAvailableMoves().asList().get(0).commencedBy().isDetective()){
            return Optional.empty();
        }
        return Optional.of(b.getAvailableMoves().asList().get(0).source());
    }


    /**
     * Gets all possible MrX locations given a detective move.
     * This actual just boils down to removing the destination square from the set of possible
     * locations
     * @param currentPossibleLocations The current list of possible locations
     * @param m The move
     * @return The old list but simply now with the move destination square removed
     */
    public static Set<Integer> getPossibleMrXLocations(
            Set<Integer> currentPossibleLocations,
            Move.SingleMove m){
        Set<Integer> newLocs = new HashSet<>(currentPossibleLocations.size());
        newLocs.addAll(currentPossibleLocations);
        newLocs.remove(m.destination); //MrX can't be on same square as detectives
        return newLocs;
    }

    /**
     * Takes in a current set of possible MrX locations and a log entry to update list of possible locations
     * @param currentXLocations The current set of locations
     * @param entry The new log entry
     * @param detectives The list of detectives on the board. These locations are filtered from final output
     * @param graph The graph of the board
     * @return The set of possible locations
     */
    public static Set<Integer> getPossibleMrXLocations(
            Set<Integer> currentXLocations,
            LogEntry entry,
            List<Player> detectives,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
        final boolean isRevealMove = entry.location().isPresent();
        Set<Integer> xLocationsNext = new HashSet<>(isRevealMove? 1 : graph.nodes().size());
        if(!isRevealMove){
            for(Integer start : currentXLocations){
                //Loops through all neighbours for current node set
                for(Integer neighbour : graph.adjacentNodes(start)){
                    //Loops through modes of transport to see if it matches one used
                    for(ScotlandYard.Transport t :
                            graph.edgeValueOrDefault(start, neighbour, ImmutableSet.of())){
                        if((t.requiredTicket() == entry.ticket())){
                            xLocationsNext.add(neighbour);
                        }
                    }
                    //Handles secret move
                    if(entry.ticket() == ScotlandYard.Ticket.SECRET) xLocationsNext.add(neighbour);
                }
            }
        }else{
            xLocationsNext.add(entry.location().get());
        }
        //Removes detectives from set
        for(Player det : detectives){
            xLocationsNext.remove(det.location());
        }
        return xLocationsNext;
    }

    /**
     * Returns all possible MrX locations for MrX for a non-terminal board state.
     * For terminal board states it will return an empty set.
     * @param state The board state
     * @param findMrX True if function should attempt to locate MrX exactly (by looking at legal moves)
     * @return All possible locations for MrX
     */
    public static Set<Integer> getPossibleMrXLocations(Board state, boolean findMrX){
        //If it's MrX turn, we know his location if we are allowed to access it
        Optional<Integer> xLocation = getMrXLocation(state);
        if(findMrX && xLocation.isPresent()) return ImmutableSet.of(xLocation.get());
        //Stores all possible nodes for the current turn
        Set<Integer> xLocationsCurrent = new HashSet<>(state.getSetup().graph.nodes().size());
        //Loads all possible staring nodes. If mrX has revealed himself, this is the start.
        //Otherwise, it's all start locations
        int lastKnownLogEntryIndex = -1;
        List<LogEntry> entries = state.getMrXTravelLog().asList();
        for(int i = entries.size() - 1; i >= 0 && lastKnownLogEntryIndex == -1; i--){
            if(entries.get(i).location().isPresent()){
                xLocationsCurrent.add(entries.get(i).location().get());
                lastKnownLogEntryIndex = i;
            }
        }
        //Adds all valid start locations
        if(lastKnownLogEntryIndex == -1){
            xLocationsCurrent.addAll(ScotlandYard.MRX_LOCATIONS);
        }
        //Loops through all moves after known entry and updates values
        for(int i = lastKnownLogEntryIndex + 1; i < entries.size(); i++){
            Set<Integer> xLocationsNext = new HashSet<>(state.getSetup().graph.nodes().size());
            for(Integer start : xLocationsCurrent){
                //Loops through all neighbours for current node set
                for(Integer neighbour : state.getSetup().graph.adjacentNodes(start)){
                    //Loops through modes of transport to see if it matches one used
                    for(ScotlandYard.Transport t :
                            state.getSetup().graph.edgeValueOrDefault(start, neighbour, ImmutableSet.of())){
                        if((t.requiredTicket() == entries.get(i).ticket())){
                            xLocationsNext.add(neighbour);
                        }
                    }
                    //Handles secret move
                    if(entries.get(i).ticket() == ScotlandYard.Ticket.SECRET) xLocationsNext.add(neighbour);
                }
            }
            xLocationsCurrent = xLocationsNext;
        }
        //Removes all detective locations from map as MrX can't be on detective square
        for(Piece.Detective det : Piece.Detective.values()){
            if(state.getDetectiveLocation(det).isPresent()){
                xLocationsCurrent.remove(state.getDetectiveLocation(det).get());
            }
        }
        return xLocationsCurrent;
    }
}
