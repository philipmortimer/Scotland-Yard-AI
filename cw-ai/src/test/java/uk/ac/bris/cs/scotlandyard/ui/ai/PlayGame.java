package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.IOException;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.standardGraph;

/**
 * Test class used to have two AI's play against each other a certain number of times to
 * estimate which AI is generally better.
 */
public final class PlayGame {
    private final Ai mrX;
    private final Ai det;
    private final int noGames;
    private final int xWins;
    private final int detWins;
    private final double testTime;
    private final double range;
    private static final double z95 = 1.960; //Z score for 95% confidence interval
    private final Pair<Long, TimeUnit> moveTime;

    /**
     * Creates a new object that stores the test results
     * @param mrX The MrX AI
     * @param detectives The detective AI
     * @param noGames The number of games used to test two players
     * @param xWins The number of games mrX AI has won
     * @param detWins The number of games the detective AI has won
     * @param range Used for confidence interval. Multiply this by z value for confidence interval
     * @param moveTime The time the AI was allowed to take per move
     * @param testTime The total time spent performing the test
     */
    private PlayGame(Ai mrX, Ai detectives, int noGames, int xWins, int detWins,
                     double testTime, double range, Pair<Long, TimeUnit> moveTime){
        this.mrX = mrX;
        this.det = detectives;
        this.noGames = noGames;
        this.xWins = xWins;
        this.detWins = detWins;
        this.testTime = testTime;
        this.range = range;
        this.moveTime = moveTime;
    }

    @Override
    public String toString(){
        String stars = "************************************************";
        String result = stars +"\nMrXAi: " + mrX.name() + "\nDetectivesAI: " + det.name();
        result += "\nTime for each move: " + moveTime.left() + " " + moveTime.right();
        result += "\nTotal test time: " + testTime +" seconds";
        result += "\nTotal games tested: " + noGames;
        result += "\nMrX Wins: " + xWins + "\nDetective Wins: " + detWins;
        result += "\nMrX win rate: " +
                Double.parseDouble(String.valueOf(xWins)) / Double.parseDouble(String.valueOf(noGames)) * 100.0
                + "%" +" ± " + String.format("%.2f",((range * z95) * 100.0)) + "% (95% CI)";
        result += "\nDetective win rate: " +
                Double.parseDouble(String.valueOf(detWins)) / Double.parseDouble(String.valueOf(noGames)) * 100.0
                + "%" +" ± " + String.format("%.2f",((range * z95) * 100.0)) + "% (95% CI)";
        result += "\n" + stars;
        return result;
    }

    /**
     * Plays two AI opponents against each other a specified number of times
     * @param mrX The mrX AI
     * @param detectives The detective AI
     * @param noTests The number of tests to perform
     * @param moveTime The time allowed to make a move
     * @throws IOException When attempting to load the standard graph, an error may occur.
     * @return Test results
     */
    public static PlayGame testAi(Ai mrX, Ai detectives, int noTests,
                                  final Pair<Long, TimeUnit> moveTime ) throws IOException{
        double start = System.currentTimeMillis();
        int detWins = 0;
        int xWins = 0;
        for(int test = 0; test < noTests; test++){
            Board.GameState state = getRandomStartState();
            while(state.getWinner().isEmpty()){
                if(state.getAvailableMoves().asList().get(0).commencedBy().isMrX()){
                    state = state.advance(mrX.pickMove(state, moveTime));
                }else{
                    state = state.advance(detectives.pickMove(state, moveTime));
                }
            }
            if(state.getWinner().asList().get(0).isMrX()) xWins++;
            else detWins++;
        }
        //Calculate confidence interval of win-rate
        int sumSquareOfX = xWins;// Σ(x^2)
        int sumOfXAllSquared = xWins * xWins;// (Σx)^2
        // Σ(x^2) - (((Σx)^2) / n)
        double numerator = (double) sumSquareOfX -  (((double)(sumOfXAllSquared)) / ((double)(noTests)));
        double sampleStanDev = Math.sqrt(numerator / ((double) (noTests - 1)));
        double rangeZ = sampleStanDev / Math.sqrt(noTests);
        double end = System.currentTimeMillis();
        return new PlayGame(mrX, detectives, noTests, xWins, detWins, (end - start) / 1000.0,
                rangeZ, moveTime);
    }

    /**
     * Gets a random starting board states using a default graph and setup
     * @throws IOException When attempting to load that standard graph, an error may occur.
     * @return The state
     */
    public static Board.GameState getRandomStartState() throws IOException {
        Random rand = new Random();
        GameSetup set = new GameSetup(standardGraph(),
                ScotlandYard.STANDARD24MOVES);
        Player mrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(),
                ScotlandYard.generateMrXLocation(rand.nextInt()));
        ImmutableList.Builder<Player> detectives = new ImmutableList.Builder<>();
        Stack<Integer> detectiveLocations = new Stack<>();
        detectiveLocations.addAll(
                ScotlandYard.generateDetectiveLocations(rand.nextInt(), Piece.Detective.values().length));

        for(Piece.Detective det : Piece.Detective.values()){
            detectives.add(new Player(det, ScotlandYard.defaultDetectiveTickets(), detectiveLocations.pop()));
        }
        return MyGameStateFactory.a(set, mrX, detectives.build());
    }
    /**
     * Run this to test difference between two chosen AI players
     * @param args
     */
    public static void main(String[] args) {
        final Pair<Long, TimeUnit> MOVE_TIME = new Pair<>(1000L, TimeUnit.MILLISECONDS);
        Ai mrX = new ScotlandAiEasy();
        Ai det = new ScotlandAiEasy();
        mrX.onStart();
        det.onStart();
        try{
            System.out.println(PlayGame.testAi(mrX, det, 1000, MOVE_TIME));
        }catch(IOException e){
            System.err.println("Error " + e);
        }
        mrX.onTerminate();
        det.onTerminate();
    }
}
