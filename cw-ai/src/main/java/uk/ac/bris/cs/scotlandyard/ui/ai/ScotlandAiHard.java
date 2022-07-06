package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * Hard AI difficult setting for Scotland yard.
 * Uses AiCustom implementations depending on MrX or detectives
 */
public class ScotlandAiHard implements Ai {
    AiCustom mrX = new ParanoidSearchMrX();
    AiCustom det = OneMoveAheadDijkstraAi.bestPossibleOneMove();
    @Nonnull
    @Override
    public String name() {
        return "Scotland Hard";
    }

    @Override
    public void onStart() {
        mrX.onStart();
        det.onStart();
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        Move best;
        if(board.getAvailableMoves().asList().get(0).commencedBy().isMrX()){
            best  = mrX.pickMove(board, timeoutPair);
        }else{
            best = det.pickMove(board, timeoutPair);
        }
        //Fail safe
        if(best == null || !board.getAvailableMoves().contains(best)){
            System.err.println("No valid move calculated");
            best = board.getAvailableMoves().asList().get(0);
        }
        return best;
    }

    @Override
    public void onTerminate() {
        mrX.onTerminate();
        det.onTerminate();
    }
}
