package com.vdom.players;

import java.util.ArrayList;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;

import com.vdom.core.*;


public class VDomPlayerJarvis extends BasePlayer {

  // Turn and Game Evaluator
  Evaluator gameEvaluator = new Evaluator(this);

  // Action Phase Search Tree
  DomTree searchTree = null;
  boolean searching  = false;
  Card    bestPlay   = null;

  // Buy Phase Parameters
  int numTreasuresBought    = 0;
  int numKingdomCardsBought = 0;
  int numVictoriesBought    = 0;
  double maxKingdomRatio    = 0.2;

  @Override
  public BasePlayer clone(Game inputGame)
  {
    VDomPlayerJarvis player = (VDomPlayerJarvis) super.clone(inputGame);
    player.gameEvaluator = gameEvaluator;
    player.searchTree = searchTree;
    player.searching = searching;
    player.bestPlay = bestPlay;
    return player;
  }

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

      if(getActionsInHand(this).size() > 0) {
      searchTree = new DomTree(context.cloneContext(), gameEvaluator);
      searching = true;
      bestPlay = searchTree.chooseAction(6,3,10);
      searching = false;
    }

    if (bestPlay == null) {
      return null;
    } else {
      return fromHand(bestPlay);
    }

  }

  @Override
  public Card doBuy(MoveContext context) {
//    Card returnCard = doBuyHeuristic(context);
//    System.out.println(">>>> JARVIS: ACTUALLY BUYING CARD: " + returnCard);
//    return returnCard;
    return null;
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
      numVictoriesBought++;
      returnCard = Cards.province;
    } else if (context.canBuy(Cards.gold)) {
      numTreasuresBought++;
      returnCard = Cards.gold;
    } else {

      // Buy Highest Value Card the Player can Buy
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
        double totalCardsBought = (double) (numTreasuresBought + numKingdomCardsBought + numVictoriesBought + 1);
        double kingdomRatio = (double) numKingdomCardsBought / totalCardsBought;

        System.out.println(">>>> JARVIS: totalCardsBought = " + totalCardsBought);
        System.out.println(">>>> JARVIS: numKingdomCardsBought = " + numKingdomCardsBought);
        System.out.println(">>>> JARVIS: kingdomRatio = " + kingdomRatio);
        System.out.println("-----------------------------------------------");

        if (kingdomRatio <= maxKingdomRatio) {
          numKingdomCardsBought++;
          returnCard = highestValueCard;
        }
      }
    }

    // Else, Buy a Silver
    if (returnCard == null && context.canBuy(Cards.silver)) {
      numTreasuresBought++;
      returnCard = Cards.silver;
    }

    // Return Final Selection
    return returnCard;

  }


  /*
  ** doBuyEvalSearch - Evaluate Best Card to Buy Using a Cloned Game State,
  ** and evaluating the effect of each Buyable Card against the Current State.
  */
  public Card doBuyEvalSearch(MoveContext context) {

    Card returnCard = null;

    // Buy Card that Improves Player's Evaluation Most
    double currentEvaluation = gameEvaluator.evaluateBuyPhase(context, this.getAllCards());
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
  // Overrides to BasePlayer Functions
  // ---------------------------------------------------------------------

  @Override
  protected Card bestCardInPlay(final MoveContext context, int maxCost, boolean exactCost, int maxDebtCost,
                                boolean potion, boolean actionOnly, boolean victoryCardAllowed,
                                boolean mustCostLessThanMax, boolean mustPick, Card except) {

    boolean isBuy = (maxCost == -1);
    if (isBuy) {
      maxCost = COST_MAX;
      maxDebtCost = COST_MAX;
    }

    Card[] cards = context.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true);
    ArrayList<Card> cardListGood = new ArrayList<Card>();
    ArrayList<Card> cardListBad = new ArrayList<Card>();
    int maxPotionCost = potion ? 1 : 0;

    for (int i = 0; i < cards.length; i++) {
      Card card = cards[i];
      int cardCost = card.getCost(context);
      int cardDebt = card.getDebtCost(context);
      int cardPotion = card.costPotion() ? 1 : 0;
      if (card.is(Type.Landmark, context.player) ||
          card.is(Type.Shelter, context.player) ||
          card.equals(Cards.abandonedMine) ||
          card.equals(Cards.ruinedLibrary) ||
          card.equals(Cards.ruinedMarket) ||
          card.equals(Cards.ruinedVillage) ||
          card.equals(Cards.survivors) ||
          (except != null && card.equals(except)) ||
          (card.is(Type.Knight, null) && !card.equals(Cards.virtualKnight)) ||
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
        /* card allowed, but not wanted */
        cardListBad.add(card);
      } else {
        cardListGood.add(card);
      }
    }

    if (cardListGood.size() > 0) {
      return choose(cardListGood);
    }

    if (mustPick && cardListBad.size() > 0) {
      return choose(cardListBad);
    }

    return null;
  }



  private <T> T choose(ArrayList<T> options) {

    // Reduce width of Tree
    while (options.size() > 3) { options.remove(3); }

    // If in search:
    // Check if current node has been expanded already.
    if(this.searching)
    {
      // If expanded, Return option of first untried expanded node. (Should there be checks to make sure the option is still applicable?)
      // Else, call for expansion of the tree, then return option of first untried node (or return option 1?)
      T choice = searchTree.get_next_option(options);
      return searchTree.get_next_option(options);
    } else {
      return null; // lets assume this won't happen for now.
    }
  }


}
