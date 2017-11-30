package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.CardList;
import com.vdom.core.Cards;

import java.util.ArrayList;

/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class DeckGenerator {

	private int deckSize;
	private int percentAction;
	private int percentTreasureVictory;
	private ArrayList<Card> kingdomCards;

	public DeckGenerator(ArrayList<Card> kingdomCards, int deckSize, int percentAction) throws DeckException {
		if (percentAction >= 100)
			throw new DeckException();

		this.kingdomCards = kingdomCards;
		this.deckSize = deckSize;
		this.percentAction = percentAction;
	}

	private class DeckException extends Exception {}

	// This could potentially excede the decksize if the percentOther (non-action) is less than 10, but we shouldn't
	// run into issues with the size deck we are using
	public ArrayList<ArrayList<Card>> generateInitialDecks() {
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
					deck.add(Cards.estate);
				}
				deck.add(Cards.copper);
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
					deck.add(Cards.silver);
					j++;
				}
				else {
					deck.add(Cards.gold);
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
