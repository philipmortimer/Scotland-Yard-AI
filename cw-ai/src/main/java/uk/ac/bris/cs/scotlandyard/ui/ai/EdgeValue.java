package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Interface for calculating the edge value of a graph
 */
public interface EdgeValue {
    /**
     * Gets the cost of distance between two adjacent nodes by looking at transport modes between them
     * @param transportModes The possible methods of transport
     * @return The edge cost
     */
    Integer getEdgeCost(ImmutableSet<ScotlandYard.Transport> transportModes);
}
