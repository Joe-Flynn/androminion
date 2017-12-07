package com.vdom.players;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.*;

// -----------------------------------------------------
// IMPLEMENTS A SIMPLE BIG-MONEY PLAYER, FOR EVALUATIONS
//  - Action Phase: No Action
//  - Buy Phase: "Big-Money"
// -----------------------------------------------------

public class VDomPlayerJoe extends BasePlayer  {

  public VDomPlayerJoe() {
    super();
    this.setName("Joe");
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
  public Card doBuy(MoveContext context) {

    int coins = context.getCoinAvailableForBuy();
    Card retCard;

    if (coins == 0) {
      retCard = null;
    } else if (context.canBuy(Cards.province)) {
      retCard = Cards.province;
    } else if (context.canBuy(Cards.gold)) {
      retCard = Cards.gold;
    } else if (context.canBuy(Cards.silver)) {
      retCard = Cards.silver;
    } else {
      retCard = null;
    }

    return retCard;

  }
}
