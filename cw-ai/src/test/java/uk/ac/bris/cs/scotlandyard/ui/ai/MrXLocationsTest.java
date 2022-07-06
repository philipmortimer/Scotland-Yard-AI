package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.*;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.TAXI;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket.UNDERGROUND;

/**
 * Tests that function that calculates all of MrX's possible locations.
 * This class tests a complete play through of a game and ensures that the possible locations
 * for MrX are calculated correctly at each location. A number of private helper functions are used
 * to make the code more readable.
 */
public class MrXLocationsTest extends ParameterisedModelTestBase{
    private ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph;
    private Player mrX;
    private Player white;
    private Player red;
    private Player green;
    private Player blue;
    private Player yellow;
    private Board.GameState stateBrd;
    private MyGameState state;
    private Set<Integer> possibleMrXLocations;
    private Set<Integer> correctLocations;
    private Set<Integer> busLocs;

    @Test
    /**
     * Tests values by doing a play through of the game. All
     * MrX locations have been hand calculated
     */
    public void testPlayThrough(){
        //This function just simulates a game play through and tests the MrX locations at certain points
        //in the game.
        initVariables();
        testPossibleLocationsForStartState();
        testPossibleAfterFirstXMove();
        testFirstDetRound();
        testMrXSecondBus();
        testDetectiveSecondRound();
        testMrXThirdMoveReveal();
        testDetThirdRound();
        testMrXDoubleMoveFourthRound();
        makeDetectiveFourthMoves();
        testFifthMoveMrXSecretMove();
        testGameOver();
    }

    /**
     * Initialises variables used for test
     */
    private void initVariables(){
        graph = standardGraph();
        mrX = new Player(MRX, defaultMrXTickets(), 106);
        red = new Player(RED, defaultDetectiveTickets(), 91);
        green = new Player(GREEN, defaultDetectiveTickets(), 29);
        blue = new Player(BLUE, defaultDetectiveTickets(), 94);
        white = new Player(WHITE, defaultDetectiveTickets(), 50);
        yellow = new Player(YELLOW, defaultDetectiveTickets(), 138);
        stateBrd = gameStateFactory.build(standard24MoveSetup(),
                mrX, red, green, blue, white, yellow);
        state = MyGameState.getPossibleStates(stateBrd).get(0);
        possibleMrXLocations = state.getMrXPossibleLocations();
    }

    /**
     * Tests that the possible locations are correctly calculated for the first state of the playthrough.
     * From MrX's perspective, there should only be one possible GameState. From the detectives perspective, MrX
     * should be at any one of the possible start locations
     */
    private void testPossibleLocationsForStartState(){
        assert(MyGameState.getPossibleStates(stateBrd).size() == 1
                && state.getAvailableMoves().equals(stateBrd.getAvailableMoves()));
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(MRX_LOCATIONS.stream().
                filter(x -> !Set.of(91, 29, 94, 50, 138).contains(x)).toList().toArray(new Integer[] {}));
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
    }

    /**
     * MrX makes a taxi move. This function tests that all valid taxi squares are selected.
     */
    private void testPossibleAfterFirstXMove(){
        state = state.advance(taxi(MRX, 106, 105)); //Makes move
        //Checks for next move
        correctLocations = new HashSet<>();
        for(Integer n : MRX_LOCATIONS) correctLocations.addAll(state.getSetup().graph.adjacentNodes(n));
        correctLocations.removeAll(Set.of(102)); //This is a bus location and hence shouldn't be included
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(correctLocations
                .stream().toList().toArray(new Integer[] {}));
    }

    /**
     * Tests that board is correct after first round of detective moves
     */
    private void testFirstDetRound(){
        //Checks that moving detectives doesn't alter anything significant
        state = state.advance(taxi(YELLOW, 138, 152));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(correctLocations
                .stream().toList().toArray(new Integer[] {}));
        state = state.advance(taxi(WHITE, 50, 49))
                .advance(bus(BLUE, 94, 77))
                .advance(taxi(GREEN, 29, 41))
                .advance(taxi(RED, 91, 90));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
    }

    /**
     * Tests that MrX's possible locations are correctly calculated when he makes his second move.
     * This move is a bus move and thus should significantly reduce the possible locations.
     */
    private void testMrXSecondBus(){
        //MrX takes a bus which should narrow options down significantly
        state = state.advance(bus(MRX, 105, 87));
        busLocs = new HashSet<>();
        //Gets all valid bus locations
        for(Integer source : possibleMrXLocations){
            for(Integer dest : graph.adjacentNodes(source)){
                for (ScotlandYard.Transport t :
                        graph.edgeValueOrDefault(source, dest, ImmutableSet.of())) {
                    if(t.requiredTicket() == Ticket.BUS){
                        busLocs.add(dest);
                    }
                }
            }
        }
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        busLocs.removeAll(Set.of(90, 41, 77, 49, 152));//Removes detective locations
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(busLocs.toArray(new Integer[] {}));
    }

    /**
     * Tests that the locations are correctly calculated for the second round of detective moves
     */
    private void testDetectiveSecondRound(){
        //Detective moves
        state = state.advance(taxi(RED, 90, 105))
                .advance(taxi(WHITE, 49, 66))
                .advance(taxi(BLUE, 77, 96))
                .advance(taxi(YELLOW, 152, 138));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        busLocs.removeAll(Set.of(105, 66, 96, 138));
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(busLocs.toArray(new Integer[] {}));
        state = state.advance(taxi(GREEN, 41, 54));
        busLocs.remove(54);
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(busLocs.toArray(new Integer[] {}));
    }

    /**
     * Tests that the possible locations are calculated correctly for MrX's third move, where he reveals his
     * location.
     */
    private void testMrXThirdMoveReveal(){
        //On this turn, MrX will reveal his location
        state = state.advance(taxi(MRX, 87, 88));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assert(possibleMrXLocations.size() == 1 && possibleMrXLocations.contains(88));
    }

    /**
     * Tests that all of MrX's locations are calculated correctly for the third round of detective moves.
     * As MrX has just revealed his location, all detectives should know where he is.
     */
    private void testDetThirdRound(){
        //Detective moves
        state = state.advance(taxi(RED, 105, 90));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assert(possibleMrXLocations.size() == 1 && possibleMrXLocations.contains(88));
        state = state.advance(taxi(WHITE, 66, 49))
                .advance(taxi(BLUE, 96, 77))
                .advance(taxi(YELLOW, 138, 152))
                .advance(taxi(GREEN, 54, 41));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assert(possibleMrXLocations.size() == 1 && possibleMrXLocations.contains(88));
    }

    /**
     * Tests that MrX's possible locations are correctly calculated for his 4th move.
     * He makes a double move that makes him easy to identify, narrowing down the space of possible hiding locations.
     */
    private void testMrXDoubleMoveFourthRound(){
        //Moves MrX using a double move in such a way that he should be easy to identify
        state = state.advance(x2(MRX, 88, TAXI, 89, UNDERGROUND, 67));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(67, 13, 140, 128);
    }

    /**
     * Makes the detectives fourth moves. There is no need to test that the possible location
     * calculation is correct as this has already been shown in other functions (i.e. that detectives
     * have no impact on it if they are not on possible squares themselves).
     */
    private void makeDetectiveFourthMoves(){
        //Shuffles detectives
        state = state.advance(taxi(WHITE, 49, 66))
                .advance(taxi(BLUE, 77, 96))
                .advance(taxi(YELLOW, 152, 138))
                .advance(taxi(GREEN, 41, 54))
                .advance(taxi(RED, 90, 105));
    }

    /**
     * Makes MrX's fifth move and tests that the secret move logic works correctly.
     */
    private void testFifthMoveMrXSecretMove(){
        //Tests secret move
        state = state.advance(secret(MRX, 67, 84));
        possibleMrXLocations = state.getMrXPossibleLocations();
        assertMrXNotOnDetectiveSquare(state, possibleMrXLocations);
        Set<Integer> locs = new HashSet<>(graph.adjacentNodes(67));
        locs.addAll(graph.adjacentNodes(13));
        locs.addAll(graph.adjacentNodes(140));
        locs.addAll(graph.adjacentNodes(128));
        locs.removeAll(Set.of(66, 96, 138, 54, 105));
        assertThat(possibleMrXLocations).containsExactlyInAnyOrder(locs.toArray(new Integer[] {}));
    }

    /**
     * Tests that MrX possible location calculation functions correctly for terminal states.
     */
    private void testGameOver(){
        //Tests Game end
        state = state.advance(taxi(WHITE, 66, 67))
                .advance(taxi(BLUE, 96, 77))
                .advance(taxi(YELLOW, 138, 152))
                .advance(taxi(GREEN, 54, 41))
                .advance(taxi(RED, 105, 90));
        state = state.advance(x2(MRX, 84, TAXI, 85, TAXI, 84));
        state = state.advance(taxi(WHITE, 67, 84)); //White captures MrX
        possibleMrXLocations = state.getMrXPossibleLocations();
        //Tests that Possible MrX locations returns nothing.
        assert(possibleMrXLocations.isEmpty());
    }

    /**
     * Checks that none of MrX's possible locations overlap with those of detectives
     * @param b The board
     * @param loc All possible locations
     */
    private static void assertMrXNotOnDetectiveSquare(Board b, Set<Integer> loc){
        for(Piece.Detective det : Piece.Detective.values()){
            if(b.getDetectiveLocation(det).isPresent()){
                assert(!loc.contains(b.getDetectiveLocation(det).get()));
            }
        }
    }
}
