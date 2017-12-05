package com.vdom.players;

import com.vdom.api.Card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;

/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class Deck {

	private ArrayList<Card> kingdomCards; //kingdom cards in deck
	private ArrayList<Card> cards; //all cards
	private int percentKingdom;
	private HashMap<Card, Double> cardPercentages;

	public Deck(ArrayList<Card> cards, ArrayList<Card> kingdomCards, int percentKingdom) {
		this.kingdomCards = kingdomCards;
		this.cards = cards;
		this.percentKingdom = percentKingdom;

		this.cardPercentages = new HashMap<>();
		for (Card card : cards) {
			if (cardPercentages.containsKey(card)) {
				cardPercentages.put(card, cardPercentages.get(card) + 1);
			}
			else {
				cardPercentages.put(card, 1.0);
			}
		}

		for (Card key : cardPercentages.keySet())  {
			cardPercentages.put(key, cardPercentages.get(key) / (double) cards.size());
		}
	}

	public ArrayList<Card> getKingdomCards() { return kingdomCards; }

	public ArrayList<Card> getCards() { return cards; }

	public int getPercentKingdom() { return percentKingdom; }

	public HashMap<Card, Double> getCardPercentages() { return cardPercentages; }

}
