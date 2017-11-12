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
    public void newGame(MoveContext context) {}

    @Override
    public Card doAction(MoveContext context) {return null;}

    // ---> May also want to add actionCardsToPlayInOrder(MoveContext context)

    @Override
    public Card doBuy(MoveContext context) {return null;}

    // ---> May also want to add treasureCardsToPlayInOrder(MoveContext context, int maxCards, Card responsible)
    // ---> May also want to add numGuildsCoinTokensToSpend(MoveContext context, int coinTokenTotal, boolean butcher)

    @Override
    public Card[] getTrashCards() {return null;}

}
