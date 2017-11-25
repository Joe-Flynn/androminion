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

  Evaluator gameEvaluator = new Evaluator(this);

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
    gameEvaluator = new Evaluator(this);
  }


  /*
  ** doActionEvalSearch - Evaluate Best Action to Play
  */
  public Card doActionEvalSearch(MoveContext context) {

    double currentEvaluation = gameEvaluator.evaluate(context);
    int    indexActionToPlay = -1;

    System.out.println("STARTING EVAL SEARCH");
    System.out.println("--------------------");
    System.out.println("Initial Eval: " + currentEvaluation);

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
          if (clonedEvaluator.evaluate(clonedContext) > currentEvaluation) {
            currentEvaluation = clonedEvaluator.evaluate(clonedContext);
            indexActionToPlay = i;

            System.out.println("Updated Eval: " + currentEvaluation);

          }
        }
      }
    }

    System.out.println("--------------------");

    // If Better State is Found, Return Action Card
    if (indexActionToPlay > -1) {
      return hand.get(indexActionToPlay);
    } else {
      return null;
    }

  }


  /*
  ** doAction - Action Card Selection, based on Very Simple Heuristics
  */
  @Override
  public Card doAction(MoveContext context) {

    // Card returnCard = null;
    //
    // // Run Evaluator
    // System.out.println(">>>>>> PHIL'S GAME EVALUATION: " + gameEvaluator.evaluate(context));
    //
    // // Clone and Print Cloned Game's Evaulation
    // if (turnCount == 5 && !clonedAlready) {
    //   clonedGameForSearch = context.game.cloneGame();
    //   clonedAlready = true;
    // }
    // if (turnCount > 5) {
    //   VDomPlayerPhil clonedSelf = null;
    //   for (Player clonedPlayer : clonedGameForSearch.players) {
    //     if (clonedPlayer.getPlayerName() == "Phil") {
    //       clonedSelf = (VDomPlayerPhil) clonedPlayer;
    //     }
    //   }
    //   Evaluator clonedEvaluator = clonedSelf.gameEvaluator;
    //   MoveContext clonedContext = new MoveContext(clonedGameForSearch, clonedSelf, true);
    //   double clonedEvaluation   = clonedEvaluator.evaluate(clonedContext);
    //   System.out.println(">>>>>> PHIL'S CLONED GAME EVAL = " + clonedEvaluation);
    // }
    //
    // // Get +Action Cards First
    // for (int i = 0; i < hand.size(); i++) {
    //   Card cardInHand = hand.get(i);
    //   if (cardInHand.is(Type.Action) && cardInHand.getAddActions() > 0) {
    //     returnCard = cardInHand;
    //   }
    // }
    //
    // // Get +Draw Cards Next
    // if (returnCard == null && context.actions > 1) {
    //   for (int i = 0; i < hand.size(); i++) {
    //     Card cardInHand = hand.get(i);
    //     if (cardInHand.is(Type.Action) && cardInHand.getAddCards() > 0) {
    //       returnCard = cardInHand;
    //     }
    //   }
    // }
    //
    // // Get +Gold Cards Next
    // if (returnCard == null && context.actions > 1) {
    //   for (int i = 0; i < hand.size(); i++) {
    //     Card cardInHand = hand.get(i);
    //     if (cardInHand.is(Type.Action) && cardInHand.getAddGold() > 0) {
    //       returnCard = cardInHand;
    //     }
    //   }
    // }
    //
    // // Pick a Terminal Card (optimally with the highest value)
    // if (returnCard == null) {
    //   int  highestValue = 0;
    //   Card highestValueCard = null;
    //   for (int i = 0; i < hand.size(); i++) {
    //     Card cardInHand = hand.get(i);
    //     if (cardInHand.is(Type.Action)) {
    //       if (cardInHand.getCost(context) > highestValue) {
    //         highestValue = cardInHand.getCost(context);
    //         highestValueCard = cardInHand;
    //       }
    //     }
    //   }
    //   returnCard = highestValueCard;
    // }
    //
    // // Update Game Evaluator
    // //if (returnCard != null) { gameEvaluator.updateWithActionChoice(returnCard); }
    // return returnCard;

    return doActionEvalSearch(context);

  }

  /*
  ** doBuy - Simple Big Money Strategy, with the option to buy cards if
  ** the price is right.  This can be modified to use the Game State Evaluator.
  */
  @Override
  public Card doBuy(MoveContext context) {

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
    //if (returnCard != null) { gameEvaluator.updateWithBuyChoice(returnCard); }
    return returnCard;

  }


  // We man want to modify which cards get considered to be "garbage", differently from the Base Player
  // public Card[] getTrashCards() { return null; }

}
