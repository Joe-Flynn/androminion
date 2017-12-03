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


public class VDomPlayerJarvis extends BasePlayer {

  Evaluator gameEvaluator = new Evaluator(this);

  int actionPhasePlayCount = 0;
  DomTree searchTree = null;
  Card bestPlay = null;
  boolean searching = false;


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

    System.out.println(">>>> JARVIS: BEGINNING DO_ACTION, HAND = " + hand);

    // Build Tree and Find Path to Best Action Phase Evaluation
    //if (actionPhasePlayCount == 0) {
      searchTree = new DomTree(context, gameEvaluator);
      searchTree.expand();
      bestPlay = searchTree.root.get_top_play();
    //}

    // Get Node Along Best Action Phase Evaluation's Path
    System.out.println(">>>> JARVIS: actionPhasePlayCount: " + actionPhasePlayCount);
    // System.out.println(">>>> JARVIS: actionPath length: " + actionPath.size());
    // SearchTree.TreeNode actionNode = actionPath.get(actionPhasePlayCount);
    // System.out.println(">>>> JARVIS: actionPath length: " + actionPath.size());
    // Card actionCard = actionNode.getActionCard();
    if (bestPlay == null) {
      return null;
    }

    //SearchTree.PlayerDecision decision = actionNode.getPlayerDecision();
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
      if (card.getKind() == bestPlay.getKind()) {
        return card;
      }
    }
    return null;

  }


  @Override
  public Card doBuy(MoveContext context) {
    actionPhasePlayCount = 0;
    Card returnCard = doBuyHeuristic(context);
    System.out.println(">>>> JARVIS: ACTUALLY BUYING CARD: " + returnCard);
    return returnCard;
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
      VDomPlayerJarvis clonedSelf = null;
      for (Player clonedPlayer : clonedGame.players) {
        if (clonedPlayer.getPlayerName() == "Jarvis") {
          clonedSelf = (VDomPlayerJarvis) clonedPlayer;
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
      int highestValue = 0;
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


  private <T> T choose(ArrayList<T> options) {

    // TODO: interact with search tree
    // Pseudocode:

    // If in search:
    // Check if current node has been expanded already.
    if(this.searching)
    {
      // If expanded, Return option of first untried expanded node. (Should there be checks to make sure the option is still applicable?)
      // Else, call for expansion of the tree, then return option of first untried node (or return option 1?)
      return searchTree.get_next_option(options);
    }
    // Else:
    // Initialize search, and return the result.
    else
    {
      // lets assume this won't happen for now...
      return null;
    }

    // Temporarily return a random option
    // return options.get(rand.nextInt(options.size()));
  }

  private int choose_index(int number_of_options) {
    ArrayList<Integer> options = new ArrayList<>();
    for (Integer i = 0; i < number_of_options; i++) {
      options.add(i);
    }

    return choose(options);

  }

  private boolean choose_bool() {
    ArrayList<Boolean> options = new ArrayList<>();
    options.add(Boolean.TRUE);
    options.add(Boolean.FALSE);

    return choose(options);
  }

  private <T> Card[] choose_combination(Card[] cards, int n) {
    ArrayList<Card[]> options = new ArrayList<>();

    // temporarily, simply return one possibility
    Card[] option = new Card[n];
    for (int i = 0; i < n; i++) {
      option[i] = cards[i];
    }
    options.add(option);
    return choose(options);
  }


}