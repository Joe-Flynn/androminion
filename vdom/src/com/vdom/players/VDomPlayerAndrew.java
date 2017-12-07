package com.vdom.players;

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


public class VDomPlayerAndrew extends BasePlayer  {

  // Max Numbers of Cards to Buy
  protected int maxSilvers    = 99;
  protected int maxGolds      = 99;
  protected int maxTerminals  = 2;

  // Current Numbers of Each Card Bought
  protected int  numSilvers   = 0;
  protected int  numGolds     = 0;
  protected int  numTerminals = 0;
  protected Card terminalCard = null;


  public VDomPlayerAndrew() {
    super();
    this.setName("Andrew");
  }

  @Override
  public String getPlayerName() {
    return getPlayerName(game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Andrew";
  }

  @Override
  public boolean isAi() {
    return true;
  }

  @Override
  public void newGame(MoveContext context) {
    super.newGame(context);
    numSilvers   = 0;
    numGolds     = 0;
    numTerminals = 0;
    terminalCard = null;
  }

  // Favorite 2-Card Terminals (in Order)
  protected Card[] favoriteTerminals = {
    Cards.goons,
    Cards.grandMarket,
    Cards.huntingGrounds,
    Cards.hoard,
    Cards.mountebank,
    Cards.wharf,
    Cards.cultist,
    Cards.witch,
    Cards.torturer,
    Cards.margrave,
    Cards.ghostShip,
    Cards.bridgeTroll,
    Cards.hireling,
    Cards.giant,
    Cards.gear,
    Cards.jackOfAllTrades,
    Cards.catacombs,
    Cards.rabble,
    Cards.journeyman,
    Cards.councilRoom,
    Cards.vault,
    Cards.magpie,
    Cards.militia,
    Cards.monument,
    Cards.hauntedWoods,
    Cards.youngWitch,
    Cards.soothsayer,
    Cards.wildHunt,
    Cards.swampHag,
    Cards.bank,
    Cards.jester,
    Cards.masquerade,
    Cards.smithy,
    Cards.bank,
    Cards.treasureTrove,
    Cards.envoy,
    Cards.ranger,
    Cards.library,
    Cards.merchantShip,
    Cards.mine,
    Cards.marauder,
    Cards.watchTower,
    Cards.cutpurse,
    Cards.taxman,
    Cards.nobleBrigand,
    Cards.oracle,
    Cards.courtyard,
    Cards.bureaucrat,
    Cards.moat,
    Cards.nobles,
    Cards.harem
  };

  /*
  ** doBuy - Simple Single Terminal Card Buyer, with limits on Number of
  ** Silvers, Goals, and Terminal (Cards) to Buy
  */
  @Override
  public Card doBuy(MoveContext context) {

    // Select the Terminal Card
    int terminalIndex = 0;
    while (terminalCard == null && terminalIndex < favoriteTerminals.length) {
      Card terminal = favoriteTerminals[terminalIndex++];
      for (String p : context.game.piles.keySet()) {
        Card supplyCard = context.game.piles.get(p).placeholderCard();
        if (supplyCard.getKind() == terminal.getKind()) {
          terminalCard = terminal;
        }
      }
    }
    if (terminalCard == null) {
      terminalCard = Cards.province; // Default Terminal Card
    }

    // Buy Terminal Card if Possible
    if (numTerminals < maxTerminals && context.canBuy(terminalCard)) {
      numTerminals = numTerminals + 1;
      return terminalCard;
    }

    // Buy Province if Possible
    if (context.canBuy(Cards.province)) {
      return Cards.province;
    }

    // Buy Duchy if Province Pile is Getting Low
    if (game.piles.get(Cards.province.getName()).getCount() <= 2 && context.canBuy(Cards.duchy)) {
      return Cards.duchy;
    }

    // Buy Gold if Possible
    if (numGolds < maxGolds && context.canBuy(Cards.gold)) {
      numGolds = numGolds + 1;
      return Cards.gold;
    }

    // Buy Duchy if Province Pile is Getting Low and Cannot Buy Gold
    if (game.piles.get(Cards.province.getName()).getCount() <= 4 && context.canBuy(Cards.duchy)) {
      return Cards.duchy;
    }

    // Buy Estate over Silver if Province Pile is Getting Extremely Low
    if (game.piles.get(Cards.province.getName()).getCount() <= 1 && context.canBuy(Cards.estate)) {
      return Cards.estate;
    }

    // Buy Silver if Possible
    if (numSilvers < maxSilvers && context.canBuy(Cards.silver)) {
      numSilvers = numSilvers + 1;
      return Cards.silver;
    }

    // Else, Don't Buy Anything
    return null;
  }


  /*
  ** doAction - Just returns the first Action
  */
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

}
