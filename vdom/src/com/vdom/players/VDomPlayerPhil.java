package com.vdom.players;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.*;


public class VDomPlayerPhil extends BasePlayer  {

  // Customize Phil by selecting these:
  protected static final boolean actionPhaseHeuristicOnly = true;
  protected static final boolean buyPhaseHeuristicOnly = false;

  protected Evaluator gameEvaluator = null;

  public VDomPlayerPhil() {
    super();
    this.setName("Phil");
    gameEvaluator = new Evaluator(this);
  }

  @Override
  public String getPlayerName() {
    return getPlayerName(game.maskPlayerNames);
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
    midGame = 12;
    gameEvaluator = new Evaluator(this);
  }

  @Override
  public Card doAction(MoveContext context) {
    if (actionPhaseHeuristicOnly) {
      return doActionHeuristic(context);
    } else {
      return doActionEvalSearch(context);
    }
  }

  @Override
  public Card doBuy(MoveContext context) {
    if (buyPhaseHeuristicOnly) {
      return doBuyHeuristic(context);
    } else {
      return doBuyEvalSearch(context);
    }
  }


  // ----------------------------------------------
  // ACTUAL SMARTS BELOW...
  // ----------------------------------------------


  /*
  ** doActionHeuristic - Action Card Selection, based on Very Simple Heuristics
  */
  public Card doActionHeuristic(MoveContext context) {

    Card returnCard = null;

    // Get +Action Cards First
    for (int i = 0; i < hand.size(); i++) {
      Card cardInHand = hand.get(i);
      if (cardInHand.is(Type.Action) && cardInHand.getAddActions() > 0) {
        returnCard = cardInHand;
      }
    }

    // Get +Draw Cards Next
    if (returnCard == null && context.actions > 1) {
      for (int i = 0; i < hand.size(); i++) {
        Card cardInHand = hand.get(i);
        if (cardInHand.is(Type.Action) && cardInHand.getAddCards() > 0) {
          returnCard = cardInHand;
        }
      }
    }

    // Get +Gold Cards Next
    if (returnCard == null && context.actions > 1) {
      for (int i = 0; i < hand.size(); i++) {
        Card cardInHand = hand.get(i);
        if (cardInHand.is(Type.Action) && cardInHand.getAddGold() > 0) {
          returnCard = cardInHand;
        }
      }
    }

    // Pick a Terminal Card (optimally with the highest value)
    if (returnCard == null) {
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
      returnCard = highestValueCard;
    }

    return returnCard;

  }


  /*
  ** doActionEvalSearch - Evaluate Best Action to Play Using a Cloned Game
  ** State, and evaluating the effect of each Card in the Player's hand on it.
  */
  public Card doActionEvalSearch(MoveContext context) {

    double currentEvaluation = gameEvaluator.evaluateBuyPhase(context, this.getAllCards());
    int    indexActionToPlay = -1;

    for (int i = 0; i < hand.size(); i++) {
      if (hand.get(i).is(Type.Action)) {

        // Clone Game and Players
        Game clonedGame = context.game.cloneGame();
        VDomPlayerPhil clonedSelf = null;
        for (Player clonedPlayer : clonedGame.players) {
          if (clonedPlayer.getPlayerName() == "Phil") {
            clonedSelf = (VDomPlayerPhil) clonedPlayer;
          }
        }
        Evaluator clonedEvaluator = clonedSelf.gameEvaluator;
        MoveContext clonedContext = new MoveContext(clonedGame, clonedSelf, true);

        // Try the Indexed Action
        if (clonedGame.isValidAction(clonedContext, clonedSelf.hand.get(i))) {
          clonedGame.broadcastEvent(new GameEvent(GameEvent.EventType.Status, clonedContext));
          clonedSelf.hand.get(i).play(clonedGame, clonedContext, true);
          if (clonedEvaluator.evaluateBuyPhase(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
            currentEvaluation = clonedEvaluator.evaluateBuyPhase(clonedContext, clonedSelf.getAllCards());
            indexActionToPlay = i;
          }
        }
      }
    }

    // If Better State is Found, Return Action Card
    if (indexActionToPlay > -1) {
      return hand.get(indexActionToPlay);
    } else {
      return null;
    }

  }


  /*
  ** doBuyHeuristic - Simple Big Money Strategy, with the option to buy cards if
  ** the price is right.  This can be modified to use the Game State Evaluator.
  */
  public Card doBuyHeuristic(MoveContext context) {

    Card returnCard = null;

    // Buy Province or Gold, First
    if (context.getCoinAvailableForBuy() == 0) {
      returnCard = null;
    } else if (context.canBuy(Cards.province)) {
      returnCard = Cards.province;
    } else if (context.canBuy(Cards.gold)) {
      returnCard = Cards.gold;
    } else {

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
        returnCard = highestValueCard;
      } else if (context.canBuy(Cards.silver)) {
        returnCard = Cards.silver;
      } else {
        returnCard = null;
      }
    }

    // Update Game Evaluator
    return returnCard;

  }

  /*
  ** doBuyEvalSearch - Evaluate Best Card to Buy Using a Cloned Game State,
  ** and evaluating the effect of each Buyable Card against the Current State.
  */
  public Card doBuyEvalSearch(MoveContext context) {

    Card returnCard = null;

    // Buy Treasures and Provinces
    if (turnCount < midGame) {
      if (context.canBuy(Cards.gold)) {
        returnCard = Cards.gold;
      } else if (turnCount < (midGame / 2) && context.canBuy(Cards.silver)) {
        returnCard = Cards.silver;
      }
    } else {
      if (context.canBuy(Cards.province)) {
        returnCard = Cards.province;
      }
    }

    // Buy Card that Improves Player's Evaluation Most
    double currentEvaluation = gameEvaluator.evaluateBuyPhase(context, this.getAllCards());
    String cardToBuy = "";

    for (String pileName : context.game.piles.keySet()) {

      // Clone Game and Players
      Game clonedGame = context.game.cloneGame();
      VDomPlayerPhil clonedSelf = null;
      for (Player clonedPlayer : clonedGame.players) {
        if (clonedPlayer.getPlayerName() == "Phil") {
          clonedSelf = (VDomPlayerPhil) clonedPlayer;
        }
      }
      Evaluator clonedEvaluator = clonedSelf.gameEvaluator;
      MoveContext clonedContext = new MoveContext(context, clonedGame, clonedSelf);

      // Try the Buy
      Card supplyCard = clonedContext.game.piles.get(pileName).placeholderCard();
      Card buyCard = clonedGame.getPile(supplyCard).topCard();

      if (buyCard != null && clonedGame.isValidBuy(clonedContext, buyCard)) {
        clonedGame.broadcastEvent(new GameEvent(GameEvent.EventType.Status, clonedContext));
        clonedGame.playBuy(clonedContext, buyCard);
        clonedGame.playerPayOffDebt(clonedSelf, clonedContext);
        if (clonedEvaluator.evaluateBuyPhase(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
          currentEvaluation = clonedEvaluator.evaluateBuyPhase(clonedContext, clonedSelf.getAllCards());
          cardToBuy = pileName;
        }
      }
    }

    // Update Card to Buy
    if (cardToBuy != "") {
      returnCard = context.game.piles.get(cardToBuy).placeholderCard();
    } else if (context.canBuy(Cards.silver)) {
      returnCard = Cards.silver;
    } else {
      returnCard = null;
    }

    // Update Game Evaluator
    return returnCard;

  }


  // ---------------------------------------------------------------------
  // Sets the Parameters for the Evaluator
  // ---------------------------------------------------------------------

  public void setEvaluator(double coinFactor, double potionFactor, double threeCostGainFactor,
                           double fourCostGainFactor, double fiveCostGainFactor, double coinTokenFactor,
                           double debtTokenFactor, double victoryTokenFactor, double enemyHandSizeFactor,
                           double treasureDeltaFactor, double actionDeltaFactor, double victoryPointFactor,
                           double planEvalActionMultiplier, double planEvalTreasureMultiplier,
                           double planEvalVictoryPointFactor) {

    this.gameEvaluator = new Evaluator(this, coinFactor, potionFactor, threeCostGainFactor, fourCostGainFactor,
                                       fiveCostGainFactor, coinTokenFactor, debtTokenFactor, victoryTokenFactor,
                                       enemyHandSizeFactor, treasureDeltaFactor, actionDeltaFactor, victoryPointFactor,
                                       planEvalActionMultiplier, planEvalTreasureMultiplier, planEvalVictoryPointFactor);
  }

  public Evaluator getEvaluator() {
    return this.gameEvaluator;
  }

}
