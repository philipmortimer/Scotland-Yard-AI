package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Class that implements edge values.
 * Each possible connection has a cost of 1
 */
public final class OneCostPerEdge implements EdgeValue{
    @Override
    public Integer getEdgeCost(ImmutableSet<ScotlandYard.Transport> transportModes) {
        return 1;
    }
}
