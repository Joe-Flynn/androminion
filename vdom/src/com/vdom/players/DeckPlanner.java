package com.vdom.players;


import com.vdom.api.Card;
import com.vdom.core.*;
import sun.plugin.com.event.COMEventHandler;


import java.util.*;


/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class DeckPlanner {


	private final static int initPercentKingdoms = 80;
	private final static int initPercentFirstKingdom = 50;
	private final static int numTurns = 350;
	private int deckSize;
	private Game game;

	public DeckPlanner(Game game, int deckSize) {

		this.deckSize = deckSize;
		this.game = game;
	}

	public Deck findBestDeck() {
		ArrayList<Card> kingdomCards = new ArrayList<>();
		for (CardPile cardPile : game.piles.values()) {
			if (cardPile.topCard().is(Type.Action) && !cardPile.topCard().is(Type.Ruins) && !cardPile.topCard().is(Type.Treasure)
					&& !cardPile.topCard().is(Type.Victory) && !cardPile.topCard().equals(Cards.cultist)) {
				kingdomCards.add(cardPile.topCard());
			}
		}

		ArrayList<Deck> currentPool = generateDecks(kingdomCards, initPercentKingdoms, initPercentFirstKingdom);
		ArrayList<Double> averageTurnEconomies = new ArrayList<>();

		int survivorPoolSize = currentPool.size() / 5; //20%
		double smallestMax = 0;
		ArrayList<Double> maxes = new ArrayList<>();

		// Get set of decks with the highest 20% of averageTurnEconomies
		for (Deck deck : currentPool) {
			Game clone = game.cloneGame();
			double averageTurnEconomy = clone.playPlanningGame(numTurns, deck);
			averageTurnEconomies.add(averageTurnEconomy);

			// If maxes isn't at capacity, add values and update minValOfMaxes. Otherwise, if averageTurnEconomy is less
			// larger than smallestMax, remove smallestMax from maxes, add averageTurnEconomy to maxes, and update smallest
			// max in maxes
			if (maxes.size() < survivorPoolSize) {
				maxes.add(averageTurnEconomy);
				if (averageTurnEconomy < smallestMax) {
					smallestMax = averageTurnEconomy;
				}
			}
			else if (!maxes.contains(averageTurnEconomy) && smallestMax < averageTurnEconomy) {
					maxes.remove(smallestMax);
					maxes.add(averageTurnEconomy);
					smallestMax = maxes.get(0);
					for (double max : maxes) {
						if (max < smallestMax) {
							smallestMax = max;
						}
					}
			}
		}

		ArrayList<Deck> survivors = new ArrayList<>();
		for (int i = 0; i < averageTurnEconomies.size(); i++) {
			if (maxes.contains(averageTurnEconomies.get(i))) {
				survivors.add(currentPool.get(i));
			}
		}

		Deck maxDeck;
		double max = (double) Collections.max(averageTurnEconomies);
		for (int i = 0; i < averageTurnEconomies.size(); i++) {
			if (averageTurnEconomies.get(i) == max) {
				maxDeck = currentPool.get(i);
			}
		}

		currentPool.clear();
		currentPool.addAll(survivors);
		currentPool.addAll(createMutantChildren(survivors, initPercentFirstKingdom));

		averageTurnEconomies.clear();
		for (Deck deck : currentPool) {
			Game clone = game.cloneGame();
			double averageTurnEconomy = clone.playPlanningGame(numTurns, deck);
			averageTurnEconomies.add(averageTurnEconomy);
		}


		HashSet<Double> uniqScores =  new HashSet<>();
		for (double avg : averageTurnEconomies) {
			uniqScores.add(avg);
		}

		Deck maxDeck2 = null;
		max = (double) Collections.max(averageTurnEconomies);
		for (int i = 0; i < averageTurnEconomies.size(); i++) {
			if (averageTurnEconomies.get(i) == max) {
				maxDeck2 = currentPool.get(i);
			}
		}

		return maxDeck2;
	}

	// this one was fun to name lol
	// This could potentially exceed the deck size if the amount of cards in the percent that are not kingdom cards are
	// less than 10, but we shouldn't run into issues with the size deck we are using
	private ArrayList<Deck> createMutantChildren(ArrayList<Deck> decks, int percentKingdom) {
		ArrayList<Deck> mutantChildren = new ArrayList<>();

//		for (int i = 0; i <= 100; i += 25) {
//			if (i != initPercentKingdoms) {
//				for (int j = 0; j <= 100; j += 25) {
//					if (j != initPercentFirstKingdom) {
//						mutantChildren.addAll(generateDecks(deck.getKingdomCards(), i, j));
//					}
//				}
//			}
//		}

		ArrayList<Deck[]> combos = getCombinationsDecks(2, decks);

		for (Deck[] combo : combos) {
			ArrayList<Card> kingdomCards = new ArrayList<>();
			kingdomCards.addAll(combo[0].getKingdomCards());
			kingdomCards.addAll(combo[1].getKingdomCards());

			int numOther  = (int) (deckSize *  ((100 - percentKingdom) / 100.0));
			int numKingdoms = deckSize - numOther;
			int numEachKingom = numKingdoms / 4;

			// add init cards
			ArrayList<Card> deck = new ArrayList<>();
			for (int i = 0; i < 7; i++) {
				if (i < 3) {
					deck.add(game.getGamePile(Cards.estate).topCard()); // need to get card in this manner so it is non-null, valid card
				}
				deck.add(game.getGamePile(Cards.copper).topCard());
			}

			// add kingdom cards
			for (Card card : kingdomCards) {
				for (int i = 0; i < numEachKingom; i++) {
					deck.add(card);
				}
			}

			// add remaining other cards subtracting 10 for the initial cards
			// 2 silver is added for ever 1 gold
			int j = 0;
			for (int i = 0 ; i < numOther - 10; i++) {
				if (j < 2) {
					deck.add(game.getGamePile(Cards.silver).topCard());
					j++;
				}
				else {
					deck.add(game.getGamePile(Cards.gold).topCard());
					j = 0;
				}
			}
			mutantChildren.add(new Deck(deck, kingdomCards, percentKingdom));
		}

		return mutantChildren;
	}

	// This could potentially exceed the deck size if the amount of cards in the percent that are not kingdom cards are
	// less than 10, but we shouldn't run into issues with the size deck we are using
	private ArrayList<Deck> generateDecks(ArrayList<Card> kingdomCards, int percentKingdom, int percentFirstKingdomCard) {
		ArrayList<Card[]> combos;
		if (kingdomCards.size() == 2) {
			combos = new ArrayList<Card[]>();
			combos.add(new Card[]{ kingdomCards.get(0), kingdomCards.get(1) });
		}
		else {
			combos = getCombinationsCards(2, kingdomCards);
		}

		int numOther  = (int) (deckSize *  ((100 - percentKingdom) / 100.0));
		int numKingdoms = deckSize - numOther;

		int numFirstKingdom = (int) (numKingdoms * (percentFirstKingdomCard / 100.0));
		int numSecondKingdom =  - numFirstKingdom;

		ArrayList<Deck> decks = new ArrayList<>();

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
			for (int i = 0; i < numFirstKingdom; i++) {
				deck.add(combo[0]);
			}
			for (int i = 0; i < numSecondKingdom; i++) {
				deck.add(combo[1]);
			}

			// add remaining other cards subtracting 10 for the initial cards
			// 2 silver is added for ever 1 gold
			int j = 0;
			for (int i = 0 ; i < numOther - 10; i++) {
				if (j < 2) {
					deck.add(game.getGamePile(Cards.silver).topCard());
					j++;
				}
				else {
					deck.add(game.getGamePile(Cards.gold).topCard());
					j = 0;
				}
			}
			kingdomCards.clear();
			kingdomCards.add(combo[0]);
			kingdomCards.add(combo[1]);
			decks.add(new Deck(deck, kingdomCards, percentKingdom));
		}

		return decks;
	}

	private ArrayList<Card[]> getCombinationsCards(int n, ArrayList<Card> cards) {

		ArrayList<Card[]> subsets = new ArrayList<>();

		int[] s = new int[n];

		if (n <= cards.size()) {
			for (int i = 0; (s[i] = i) < n - 1; i++);
			subsets.add(getSubsetCards(cards, s));
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
				subsets.add(getSubsetCards(cards, s));
			}
		}
		return subsets;
	}

	private Card[] getSubsetCards(ArrayList<Card> input, int[] subset) {
		Card[] result = new Card[subset.length];
		for (int i = 0; i < subset.length; i++)
			result[i] = input.get(subset[i]);
		return result;
	}

	private ArrayList<Deck[]> getCombinationsDecks(int n, ArrayList<Deck> cards) {

		ArrayList<Deck[]> subsets = new ArrayList<>();

		int[] s = new int[n];

		if (n <= cards.size()) {
			for (int i = 0; (s[i] = i) < n - 1; i++);
			subsets.add(getSubsetDecks(cards, s));
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
				subsets.add(getSubsetDecks(cards, s));
			}
		}
		return subsets;
	}

	private Deck[] getSubsetDecks(ArrayList<Deck> input, int[] subset) {
		Deck[] result = new Deck[subset.length];
		for (int i = 0; i < subset.length; i++)
			result[i] = input.get(subset[i]);
		return result;
	}


}
