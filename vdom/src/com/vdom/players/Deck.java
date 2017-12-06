package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.GetCardsInGameOptions;
import com.vdom.core.MoveContext;
import com.vdom.core.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingDeque;

/**
 * @author Joseph Flynn jaf384@drexel.edu
 */
public class Deck {

	private ArrayList<Card> kingdomCards; //kingdom cards in deck
	private ArrayList<Card> cards; //all cards
	private double percentKingdom;
	private HashMap<Card, Double> cardPercentages;

	public Deck(ArrayList<Card> cards, ArrayList<Card> kingdomCards, double percentKingdom) {
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

	public double getPercentKingdom() { return percentKingdom; }

	public HashMap<Card, Double> getCardPercentages() { return cardPercentages; }

	public void generate(MoveContext context, HashMap<Card, Double> percentages, int deckSize)
	{
		cards = new ArrayList<>();
		kingdomCards = new ArrayList<>();
		percentKingdom = 0.0;
		Card[] context_kingdom_cards = context.game.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true, Type.Action);
		for(Card c : percentages.keySet())
		{
			for(Card kc : context_kingdom_cards)
			{
				if(c == kc) {
					kingdomCards.add(c);
					percentKingdom += percentages.get(c);
				}
			}
			for(int i = 0; i < deckSize * percentages.get(c) + 0.5; i++)
			{
				cards.add(c);
			}
		}
		return;
	}
}
