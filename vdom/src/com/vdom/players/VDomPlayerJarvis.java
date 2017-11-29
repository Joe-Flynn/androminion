package com.vdom.players;

import java.util.ArrayList;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;

import com.vdom.core.BasePlayer;
import com.vdom.core.CardPile;
import com.vdom.core.Cards;
import com.vdom.core.Game;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;


public class VDomPlayerJarvis extends BasePlayer  {

  Evaluator gameEvaluator = new Evaluator(this);

  int actionPhasePlayCount       = 0;
  SearchTree actionSearchTree    = null;
  ArrayList<SearchTree.TreeNode> actionPath = null;


  @Override
  public String getPlayerName() {
    return getPlayerName(game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Jarvis";
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

  @Override
  public Card doAction(MoveContext context) {

    // Build Tree and Find Path to Best Action Phase Evaluation
    if (actionPhasePlayCount == 0) {
      actionSearchTree = new SearchTree(context);
      actionPath = actionSearchTree.getPathToMaxEval();
    }

    // Get Node Along Best Action Phase Evaluation's Path
    SearchTree.TreeNode actionNode = actionPath.get(actionPhasePlayCount);
    Card actionCard = actionNode.getActionCard();
    SearchTree.PlayerDecision decision = actionNode.getPlayerDecision();

    // TODO: IMPLEMENT DECISIONS BASED ON THIS!!
    // NOTE: I PLAN TO DO THIS BY OVERRIDING THE PLAYEROPTIONS-RELATED
    // FUNCTIONS FROM BASEPLAYER HERE IN JARVIS TO CHECK PLAYER DECISIONS
    // FIRST, AND THEN IF NOT SET, JUST .SUPER() IT.

    /* NOTE: Alternatively, we could use Game.actionChains (however, then we
    need some smart way to set the Player's Decisions during the Game's evaulation
    of the actionChain.  Or, we could set-and-forget the Player's Decisions, but
    that wouldn't allow for us to play the same action card two different ways
    within a single turn.  Decisions, decisions... */

    // Update Counter(s)
    actionPhasePlayCount++;

    // Find Original Card and Return
    for (Card card : hand) {
      if (card.getKind() == actionCard.getKind()) {
        return card;
      }
    }
    return null;

  }


  @Override
  public Card doBuy(MoveContext context) {
    actionPhasePlayCount = 0;
    return doBuyEvalSearch(context);
  }

  /*
  ** doBuyEvalSearch - Evaluate Best Card to Buy Using a Cloned Game State,
  ** and evaluating the effect of each Buyable Card against the Current State.
  */
  public Card doBuyEvalSearch(MoveContext context) {

    Card returnCard = null;

    // Buy Card that Improves Player's Evaluation Most
    double currentEvaluation = gameEvaluator.evaluate(context, this.getAllCards());
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
        if (clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
          currentEvaluation = clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards());
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


}
