package com.vdom.players;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.*;


public class VDomPlayerJarvis extends BasePlayer  {

  protected Evaluator gameEvaluator = new Evaluator(this);

  // Search Tree Setup and Play
  protected SearchTree actionSearchTree    = null;
  protected ArrayList<SearchTree.TreeNode> actionPath = null;
  protected int actionPhasePlayCount       = 0;

  // Best Card(s) in Play
  protected ArrayList<Card> bestCardsInPlay = null;
  protected Card bestCardInPlaySelected     = null;


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


  // ----------------------------------------------------
  // BASE PLAYER FUNCTIONS TO OVERRIDE VVVVVVVVVVVVVVVVVV
  // ----------------------------------------------------

  @Override
  protected Card bestCardInPlay(final MoveContext context, int maxCost, boolean exactCost, int maxDebtCost, boolean potion, boolean actionOnly, boolean victoryCardAllowed, boolean mustCostLessThanMax, boolean mustPick, Card except) {

    // Set Max Cost Limit
    boolean isBuy = (maxCost == -1);
    if (isBuy) {
      maxCost = COST_MAX;
      maxDebtCost = COST_MAX;
    }

    // Set up Card Lists
    Card[] cards = context.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true);
    ArrayList<Card> cardListGood = new ArrayList<Card>();
    ArrayList<Card> cardListBad = new ArrayList<Card>();
    int maxPotionCost = potion ? 1 : 0;

    // Sort Cards into "Good" and "Bad" Cards
    for (int i = 0; i < cards.length; i++) {
      Card card = cards[i];
      int cardCost = card.getCost(context);
      int cardDebt = card.getDebtCost(context);
      int cardPotion = card.costPotion() ? 1 : 0;
      if (card.is(Type.Landmark, context.player) ||
          card.is(Type.Shelter, context.player) ||
          card.equals(Cards.abandonedMine) || /*choose only virtualRuins*/
          card.equals(Cards.ruinedLibrary) ||
          card.equals(Cards.ruinedMarket) ||
          card.equals(Cards.ruinedVillage) ||
          card.equals(Cards.survivors) ||
          (except != null && card.equals(except)) ||
          (card.is(Type.Knight, null) && !card.equals(Cards.virtualKnight)) || /*choose only virtualKnight*/ //TODO SPLITPILES what here?
          !Cards.isSupplyCard(card) ||
          !context.isCardOnTop(card) ||
          (actionOnly && !(card.is(Type.Action))) ||
          (!victoryCardAllowed && (card.is(Type.Victory)) && !card.equals(Cards.curse)) ||
          (exactCost && (cardCost != maxCost || cardDebt != maxDebtCost || maxPotionCost != cardPotion)) ||
          (cardCost > maxCost || cardDebt > maxDebtCost || cardPotion > maxPotionCost) ||
          (mustCostLessThanMax && (cardCost == maxCost && cardDebt == maxDebtCost && maxPotionCost == cardPotion)) ||
          (isBuy && !context.canBuy(card))) {
        /* card not allowed */
      } else if (card.equals(Cards.curse) ||
                 isTrashCard(card) ||
                 (card.equals(Cards.potion) && !shouldBuyPotion())) {
        cardListBad.add(card);  /* card allowed, but NOT wanted */
      } else {
        cardListGood.add(card); /* card allowed, and IS wanted */
      }
    }

    // Return Best "Good" Card
    if (cardListGood.size() > 0) {
      bestCardsInPlay = cardListGood;
      return bestCardInPlaySelected;
    }

    // Otherwise, pick from the scraps (a.k.a. "Bad" Cards)
    if (mustPick && cardListBad.size() > 0) {
      bestCardsInPlay = cardListBad;
      return bestCardInPlaySelected;
    }

    return null;

  }


  // NOTE: ADD a searchable LowestCard(s) used for 28 different Cards in VDom Engine (total)


  // ----------------------------------------------------
  // BASE PLAYER FUNCTIONS TO OVERRIDE ^^^^^^^^^^^^^^^^^^
  // ----------------------------------------------------


  @Override
  public Card doAction(MoveContext context) {

    System.out.println(">>>> JARVIS: BEGINNING DO_ACTION, HAND = " + hand);

    // Build Tree and Find Path to Best Action Phase Evaluation
    if (actionPhasePlayCount == 0) {
      actionSearchTree = new SearchTree(context);
      actionPath = actionSearchTree.getPathToMaxEval();
    }

    // Get Node Along Best Action Phase Evaluation's Path
    SearchTree.TreeNode actionNode = actionPath.get(actionPhasePlayCount);
    Card actionCard = actionNode.getActionCard();
    if (actionCard == null) {
      return null;
    }

    // Update Player Decision
    SearchTree.PlayerDecision decision = actionNode.getPlayerDecision();
    // TODO: IMPLEMENT DECISIONS BASED ON THIS!!

    // NOTE: I PLAN TO DO THIS BY OVERRIDING THE PLAYEROPTIONS-RELATED
    // FUNCTIONS FROM BASEPLAYER HERE IN JARVIS TO CHECK PLAYER DECISIONS
    // FIRST, AND THEN IF NOT SET, JUST .SUPER() IT.

    // Update Best Card Selected
    bestCardInPlaySelected = ((VDomPlayerJarvis)actionNode.context.player).bestCardInPlaySelected;

    // Update Counter(s)
    actionPhasePlayCount++;

    // Find Original Card and Return
    return fromHand(actionCard);

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

}
