package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		GameState newState = new MyGameStateFactory().build(setup, mrX, detectives);
		return new MyModel(newState);
	}

	/**
	 * Implementation of Model
	 */
	private final static class MyModel implements Model {
		private GameState state;
		private final Set<Observer> observerSet;

		/**
		 * Creates new Model
		 * @param state The Game State
		 */
		private MyModel(GameState state){
			this.state = state;
			observerSet = new HashSet<>();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return state;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException("Observer is null");
			boolean alreadyInSet = !observerSet.add(observer);
			if(alreadyInSet) throw new IllegalArgumentException("Observer is already in set");
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if(observer == null) throw new NullPointerException("Observer is null");
			boolean legalObserver = observerSet.remove(observer);
			if(!legalObserver) throw new IllegalArgumentException("Illegal observer (not in set)");
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observerSet);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			//Makes a move and updates game state. This is transmitted to all observers
			state = state.advance(move);
			Observer.Event ev = state.getWinner().isEmpty()? Observer.Event.MOVE_MADE : Observer.Event.GAME_OVER;
			for(Observer o : observerSet)
				o.onModelChanged(state, ev);
		}
	}
}
