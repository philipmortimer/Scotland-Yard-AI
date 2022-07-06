package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * An AI that selects a random move.
 */
public class RandomAI implements AiCustom {
    @Nonnull
    @Override
    public String name() {
        return "Random Move";
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        ImmutableList<Move> moves = board.getAvailableMoves().asList();
        Random rnd = new Random();
        return moves.get(rnd.nextInt(moves.size()));
    }
}
