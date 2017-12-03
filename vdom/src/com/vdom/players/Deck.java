package com.vdom.players;

import com.vdom.api.Card;

import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;

/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class Deck {

	private ArrayList<Card> kingdomCards; //kingdom cards in deck
	private ArrayList<Card> cards; //all cards
	private int percentKingdom;

	public Deck(ArrayList<Card> cards, ArrayList<Card> kingdomCards, int percentKingdom) {
		this.kingdomCards = kingdomCards;
		this.cards = cards;
		this.percentKingdom = percentKingdom;
	}

	public ArrayList<Card> getKingdomCards() { return kingdomCards; }

	public ArrayList<Card> getCards() { return cards; }

	public int getPercentKingdom() { return percentKingdom; }


}
