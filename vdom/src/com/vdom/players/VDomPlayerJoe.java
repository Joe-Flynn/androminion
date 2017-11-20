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


public class VDomPlayerJoe extends BasePlayer  {

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
      return null;
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

      // Buy Silver
      if (context.canBuy(Cards.silver)) {
        return Cards.silver;
      }
      return null;
    }

}
