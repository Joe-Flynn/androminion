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
	private ArrayList<Card> kingdomCards;

	public DeckGenerator(ArrayList<Card> kingdomCards, int deckSize, int percentAction, int percentTresureVictory) throws DeckException {
		if ((percentAction + percentTresureVictory) != 100)
			throw new DeckException();

		this.kingdomCards = kingdomCards;
		this.deckSize = deckSize;
	}

	class DeckException extends Exception {
	}

	public ArrayList<Card[]> generateInitialDecks() {
		return getCombinations(2, kingdomCards);
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
