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

    public void setDeck(ArrayList<Card> deck) {
        CardList playerDeck = new CardList(this, "Deck");
        playerDeck.addAll(deck);
        this.deck = playerDeck;
    }
}
