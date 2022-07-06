package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

/**
 * Calculates cost of distance between two nodes by looking at transport methods that are possible.
 * The idea behind this specific weighting is that certain modes of transport are inherently more expensive
 * as they use tickets that are rarer for the detectives.
 */
public class DistanceWeighting implements EdgeValue{
    private static final int TAXI_COST = 1;
    private static final int BUS_COST = 2;
    private static final int TUBE_COST = 4;
    private static final int SECRET_COST = 4;
    private static final int DOUBLE_COST = 8;
    @Override
    public Integer getEdgeCost(ImmutableSet<ScotlandYard.Transport> transportModes) {
        //Gets the cheapest link between the two routes
        Integer cost = Integer.MAX_VALUE;
        for(ScotlandYard.Transport t : transportModes){
            ScotlandYard.Ticket ticket = t.requiredTicket();
            //Updates cost
            Integer routeCost =  switch(ticket){
                case BUS -> BUS_COST;
                case TAXI -> TAXI_COST;
                case UNDERGROUND -> TUBE_COST;
                case DOUBLE -> DOUBLE_COST;
                case SECRET -> SECRET_COST;
            };
            cost = Math.min(cost, routeCost);
        }
        return cost;
    }
}
