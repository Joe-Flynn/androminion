package com.vdom.players;

// ??? - KEEP WHAT YOU NEED

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.*;


public class VDomPlayerJoe extends BasePlayer  {

    private Evaluator evaluator;

    public VDomPlayerJoe() {
        super();
        this.setName("Joe");
        evaluator = new Evaluator(this);
    }

    @Override
    public String getPlayerName() {
      return getPlayerName(game.maskPlayerNames);
    }

    @Override
    public String getPlayerName(boolean maskName) {
      return maskName ? "Player " + (playerNumber + 1) : "Joe";
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
    	if (context.player.getPlayerName().equals("Dummy")) {
    		return null;
		}

        for (int i = 0; i < hand.size(); i++) {
            Card cardInHand = hand.get(i);
            if (cardInHand.is(Type.Action)) {
                return cardInHand;
            }
        }
        return null;
    }

    @Override

    public Card doBuy(MoveContext context) { return null; }

    public Card doBuyBigMoney(MoveContext context) {
        System.out.printf("<EVALUATOR> Buy eval: %s\n", evaluator.evaluateBuyPhase(context, this.getAllCards()));

      int coins = context.getCoinAvailableForBuy();
      Card retCard;

      if (coins == 0) {
          retCard = null;
      }
      else if (context.canBuy(Cards.province)) {
          retCard = Cards.province;
      }
      else if (context.canBuy(Cards.gold)) {
          retCard = Cards.gold;
      }
      else if (context.canBuy(Cards.silver)) {
          retCard = Cards.silver;
      }
      else {
          retCard = null;
      }

      return retCard;

      // // Buy a Duchy
      // if (context.canBuy(Cards.duchy)) {
      //   int provincesLeft = 0;
      //   for (Card card : context.getBuyableCards()) {
      //     if (card.equals(Cards.province))
      //     provincesLeft++;
      //   }
      //
      //   if (provincesLeft <= 5) {
      //     return Cards.duchy;
      //   }
      // }
    }

    public void setDeck(Deck deck) {
        CardList playerDeck = new CardList(this, "Deck");
        playerDeck.addAll(deck.getCards());
        this.deck = playerDeck;
    }
}
