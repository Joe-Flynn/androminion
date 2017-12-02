package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

import java.util.ArrayList;

/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class DeckGenerator {

	private int deckSize;
	private int percentAction;
	private Game game;
	private class DeckException extends Exception {}

	public DeckGenerator(Game game, int deckSize, int percentAction) throws DeckException {
		if (percentAction >= 100)
			throw new DeckException();

		this.deckSize = deckSize;
		this.percentAction = percentAction;
		this.game = game;
	}

	public ArrayList<Cards> findBestDeck() {
		ArrayList<ArrayList<Card>> decks = generateInitialDecks();
		ArrayList<Double> averageTurnEconomies = new ArrayList<>();

		for (ArrayList<Card> deck : decks) {
			Game clone = game.cloneGame();
			averageTurnEconomies.add(clone.playPlanningGame(5, deck));
		}


		// TODO Find best X% , mutate and play more planning games

		// TODO Choose highest score and return that deck

		return null;
	}


	// This could potentially exceed the deck size if the percentOther (non-action) is less than 10, but we shouldn't
	// run into issues with the size deck we are using
	private ArrayList<ArrayList<Card>> generateInitialDecks() {
		ArrayList<Card> kingdomCards = new ArrayList<>();
		for (CardPile cardPile : game.piles.values()) {
			if (cardPile.topCard().is(Type.Action) && !cardPile.topCard().is(Type.Ruins) && !cardPile.topCard().is(Type.Treasure)
					&& !cardPile.topCard().is(Type.Victory) && !cardPile.topCard().equals(Cards.cultist)) {
				kingdomCards.add(cardPile.topCard());
			}
		}

		ArrayList<Card[]> combos = getCombinations(2, kingdomCards);

		// for actions percentages that split deck into fractional amounts of cards, give action cards the extra card
		int numOther  = (int) (deckSize *  ((100 - percentAction) / 100.0));
		int numActions = deckSize - numOther;

		// attempt to evenly split numActions by 2 giving extra card to second card in combo if not possible
		int numFirstAction = numActions / 2;
		int numSecondAction = numActions - numFirstAction;

		ArrayList<ArrayList<Card>> decks = new ArrayList<>();

		// create decks
		for (Card[] combo : combos) {
			ArrayList<Card> deck =  new ArrayList<>();
			// add init cards
			for (int i = 0; i < 7; i++) {
				if (i < 3) {
					deck.add(game.getGamePile(Cards.estate).topCard()); // need to get card in this manner so it is non-null, valid card
				}
				deck.add(game.getGamePile(Cards.copper).topCard());
			}

			// add actions cards
			for (int i = 0; i < numFirstAction; i++) {
				deck.add(combo[0]);
			}

			for (int i = 0; i < numSecondAction; i++) {
				deck.add(combo[1]);
			}

			// add remaining other cards subtracting 10 for the initial cards
			// 2 silver is added for ever 1 gold
			int j = 0;
			for (int i = 0 ; i < numOther - 10; i++) {
				if (j < 2) {
					deck.add(game.getGamePile(Cards.silver).topCard()); // need to get card in this manner so it is a
					j++;                                                // non-null, valid card
				}
				else {
					deck.add(game.getGamePile(Cards.gold).topCard());
					j = 0;
				}
			}
			decks.add(deck);
		}

		return decks;
	}

	private ArrayList<Card[]> getCombinations(int n, ArrayList<Card> cards) {

		ArrayList<Card[]> subsets = new ArrayList<>();

		int[] s = new int[n];

		if (n <= cards.size()) {
			for (int i = 0; (s[i] = i) < n - 1; i++);
			subsets.add(getSubset(cards, s));
			for(;;) {
				int i;
				for (i = n - 1; i >= 0 && s[i] == cards.size() - n + i; i--);
				if (i < 0) {
					break;
				}
				s[i]++;
				for (++i; i < n; i++) {
					s[i] = s[i - 1] + 1;
				}
				subsets.add(getSubset(cards, s));
			}
		}
		return subsets;
	}

	private Card[] getSubset(ArrayList<Card> input, int[] subset) {
		Card[] result = new Card[subset.length];
		for (int i = 0; i < subset.length; i++)
			result[i] = input.get(subset[i]);
		return result;
	}


}
