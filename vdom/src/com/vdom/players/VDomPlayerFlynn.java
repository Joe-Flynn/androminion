package com.vdom.players;

// ??? - KEEP WHAT YOU NEED

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.*;


public class VDomPlayerFlynn extends BasePlayer  {

	private Deck idealDeck;

	protected Evaluator gameEvaluator = new Evaluator(this);

	public VDomPlayerFlynn() {
		super();
		this.setName("Flynn");
		this.isPlanningPlayer = true;
	}

	@Override
	public String getPlayerName() {
		return getPlayerName(game.maskPlayerNames);
	}

	@Override
	public String getPlayerName(boolean maskName) {
		return maskName ? "Player " + (playerNumber + 1) : "Flynn";
	}

	@Override
	public boolean isAi() {
		return true;
	}

	@Override
	public void newGame(MoveContext context) {
		super.newGame(context);
	}

	@Override
	public Card doAction(MoveContext context) {
		for (int i = 0; i < hand.size(); i++) {
			Card cardInHand = hand.get(i);
			if (cardInHand.is(Type.Action)) {
				return cardInHand;
			}
		}
		return null;
	}

	@Override
	public Card doBuy(MoveContext context) {
		HashMap<Card, Double> idealPercentages = idealDeck.getCardPercentages();

		ArrayList<Card> cards = this.getAllCards();

		// Card -> [currentPercent, percentAwayFromIdeal]
		HashMap<Card, Double> currentPercentages = new HashMap<>();
		for (Card card : cards) {
			if (currentPercentages.containsKey(card)) {
				currentPercentages.put(card, currentPercentages.get(card) + 1);
			}
			else {
				currentPercentages.put(card, 1.0);
			}
		}

		HashMap<Card, Double> currentPercentAwayFromIdeal = new HashMap<>();
		for (Card key : currentPercentages.keySet())  {
			double cardPercentage = currentPercentages.get(key) / (double) cards.size();
			currentPercentAwayFromIdeal.put(key, cardPercentage / idealPercentages.get(key));
		}

		for (Card card : idealPercentages.keySet()) {
			if (!currentPercentAwayFromIdeal.containsKey(card)) {
				currentPercentAwayFromIdeal.put(card, 0.0);
			}
		}

		ArrayList<Double> percentsAwayFromIdeal = new ArrayList<>();
		for (Double percentages : currentPercentAwayFromIdeal.values()) {
			percentsAwayFromIdeal.add(percentages);
		}
		Collections.sort(percentsAwayFromIdeal);

		for (double percentAwayFromIdeal : percentsAwayFromIdeal) {
			for (Card card : currentPercentAwayFromIdeal.keySet()) {
				if (percentAwayFromIdeal == currentPercentAwayFromIdeal.get(card)) {
					if (context.canBuy(card)) {
						return card;
					}
					else {
						currentPercentAwayFromIdeal.remove(card);
					}
					break;
				}
			}
		}

		return null;
	}

	public void setIdealDeck(Deck deck) {
		this.idealDeck = deck;
	}

}
