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
    public void newGame(MoveContext context) {}

    @Override
    public String getPlayerName() {}

    @Override
    public String getPlayerName(boolean maskName) {}

    @Override
    public boolean isAi() { return true; }

    @Override
    public Card doAction(MoveContext context) {}

    @Override
    public Card doBuy(MoveContext context) {}

    @Override
    public Card[] getTrashCards() {}

}
