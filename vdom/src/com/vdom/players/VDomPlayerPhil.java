package com.vdom.players;

// ??? - KEEP WHAT YOU NEED

import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.BasePlayer;
import com.vdom.core.CardPile;
import com.vdom.core.Cards;
import com.vdom.core.Game;
import com.vdom.core.Expansion;
import com.vdom.core.GetCardsInGameOptions;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;
import com.vdom.core.Util;


public class VDomPlayerPhil extends BasePlayer  {

    @Override
    public String getPlayerName() {
      return getPlayerName(Game.maskPlayerNames);
    }

    @Override
    public String getPlayerName(boolean maskName) {
      return maskName ? "Player " + (playerNumber + 1) : "Phil";
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

      System.out.println("<PHIL> HAND: " + hand);

      // Get +Action Cards First
      for (int i = 0; i < hand.size(); i++) {
        Card cardInHand = hand.get(i);
        if (cardInHand.is(Type.Action) && cardInHand.getAddActions() > 0) {
          return cardInHand;
        }
      }

      // Get +Draw Cards Next
      if (context.actions > 1) {
        for (int i = 0; i < hand.size(); i++) {
          Card cardInHand = hand.get(i);
          if (cardInHand.is(Type.Action) && cardInHand.getAddCards() > 0) {
            return cardInHand;
          }
        }
      }

      // Get +Gold Cards Next
      if (context.actions > 1) {
        for (int i = 0; i < hand.size(); i++) {
          Card cardInHand = hand.get(i);
          if (cardInHand.is(Type.Action) && cardInHand.getAddGold() > 0) {
            return cardInHand;
          }
        }
      }

      // Pick a Terminal Card (optimally with the highest value)
      int  highestValue = 0;
      Card highestValueCard = null;
      for (int i = 0; i < hand.size(); i++) {
        Card cardInHand = hand.get(i);
        if (cardInHand.is(Type.Action)) {
          if (cardInHand.getCost(context) > highestValue) {
            highestValue = cardInHand.getCost(context);
            highestValueCard = cardInHand;
          }
        }
      }
      return highestValueCard;
    }

    /*
    ** doBuy - Simple Big Money Strategy
    */
    @Override
    public Card doBuy(MoveContext context) {

      int coins = context.getCoinAvailableForBuy();
      if (coins == 0) {
        return null;
      }

      // Buy Province or Gold
      if (context.canBuy(Cards.province)) {
        return Cards.province;
      }
      if (context.canBuy(Cards.gold)) {
        return Cards.gold;
      }

      // Get Highest Value Card Player can Buy
      int  highestValue = 0;
      Card highestValueCard = null;
      for (String p : context.game.placeholderPiles.keySet()) {
        CardPile pile = context.game.placeholderPiles.get(p);
        Card supplyCard = pile.placeholderCard();
        if (pile.topCard() != null) {
          if (context.canBuy(supplyCard) && supplyCard.getCost(context) > highestValue) {
            highestValue = supplyCard.getCost(context);
            highestValueCard = supplyCard;
          }
        }
      }
      if (highestValueCard != null) {
        return highestValueCard;
      }

      // Buy Silver
      if (context.canBuy(Cards.silver)) {
        return Cards.silver;
      }
      return null;
    }

    // ---> May also want to add treasureCardsToPlayInOrder(MoveContext context, int maxCards, Card responsible)
    // ---> May also want to add numGuildsCoinTokensToSpend(MoveContext context, int coinTokenTotal, boolean butcher)

    // We man want to modify which cards get considered to be "garbage", differently from the Base Player
    // public Card[] getTrashCards() { return null; }

}
