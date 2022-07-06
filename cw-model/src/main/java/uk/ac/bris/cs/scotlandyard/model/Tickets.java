package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

import javax.annotation.Nonnull;

/**
 * Class to calculate number of each ticket a player has
 */
public class Tickets implements Board.TicketBoard {
    ImmutableMap<Ticket, Integer> noOfEachTicket;


    /**
     * Creates a new object that allows us to calculate the quantity of each ticket
     * a player has.
     * @param noOfEachTicket The maps between tickets and number of tickets.
     */
    Tickets(ImmutableMap<Ticket, Integer> noOfEachTicket){
        this.noOfEachTicket = noOfEachTicket;
    }

    @Override
    public int getCount(@Nonnull ScotlandYard.Ticket ticketType) {
        return this.noOfEachTicket.get(ticketType);
    }
}
