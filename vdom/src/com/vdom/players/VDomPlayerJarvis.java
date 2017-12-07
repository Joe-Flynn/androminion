package com.vdom.players;

import java.util.ArrayList;
import java.util.HashMap;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;

import com.vdom.core.*;


public class VDomPlayerJarvis extends BasePlayer {

  // Customize Jarvis by selecting these:
  protected static final boolean actionPhaseHeuristicOnly = false;
  protected static final boolean buyPhaseHeuristicOnly = false;

  // Turn and Game Evaluator
  protected Evaluator gameEvaluator = new Evaluator(this);

  // Action Phase Search Tree
  protected DomTree searchTree = null;
  protected boolean searching  = false;
  protected Card    bestPlay   = null;

  // Buy Phase Parameters
  protected int numTreasuresBought    = 0;
  protected int numKingdomCardsBought = 0;
  protected int numVictoriesBought    = 0;
  protected double maxKingdomRatio    = 0.5;

  public VDomPlayerJarvis() {
		super();
		this.setName("Jarvis");
    this.isPlanningPlayer = true;
		gameEvaluator = new Evaluator(this);
	}

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
    if (gameEvaluator.isDefaultEvaluator == true) {
      gameEvaluator = new Evaluator(this);
    } else {
      // Copy old params (in case they were tweaked)
      gameEvaluator = new Evaluator(this,
                                    gameEvaluator.coinFactor,
                                    gameEvaluator.potionFactor,
                                    gameEvaluator.threeCostGainFactor,
                                    gameEvaluator.fourCostGainFactor,
                                    gameEvaluator.fiveCostGainFactor,
                                    gameEvaluator.coinTokenFactor,
                                    gameEvaluator.debtTokenFactor,
                                    gameEvaluator.victoryTokenFactor,
                                    gameEvaluator.enemyHandSizeFactor,
                                    gameEvaluator.treasureDeltaFactor,
                                    gameEvaluator.actionDeltaFactor,
                                    gameEvaluator.victoryPointFactor,
                                    gameEvaluator.planEvalActionMultiplier,
                                    gameEvaluator.planEvalTreasureMultiplier,
                                    gameEvaluator.planEvalVictoryPointFactor);
    }
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
    if (buyPhaseHeuristicOnly || (idealDeck == null)) {
      return doBuyHeuristic(context);
    } else {
      return doBuyEvalSearch(context);
    }
  }


  // ----------------------------------------------
  // ACTUAL SMARTS BELOW...
  // ----------------------------------------------


  /*
  ** doActionHeuristic - Action Card Selection, based on Very Simple Heuristics.
  ** Needed to add this in here to see if the Search Improves over a baseline.
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
  ** doActionEvalSearch - Actually uses the Dom-Tree to Evaluate Searches
  */
  public Card doActionEvalSearch(MoveContext context) {
    if(getActionsInHand(this).size() > 0) {
      searchTree = new DomTree(context.cloneContext(), gameEvaluator);
      searching = true;
      bestPlay = searchTree.chooseAction(5,3,5);
      searchTree = null; // release tree
      searching = false;
    }
    if (bestPlay == null) {
      return null;
    } else {
      return fromHand(bestPlay);
    }
  }


  /*
  ** gameProgression - Estimate for how far along the game is progressing
  */
  public double gameProgression(MoveContext context)
  {
    double numProvinces = context.game.piles.get("Province").getCount();
    if (numProvinces > 6) {
      return 0.0;
    } else if (numProvinces > 4) {
      return 0.2;
    } else if (numProvinces > 2) {
      return 0.4;
    } else {
      return 1.0;
    }
  }


  /*
  ** evaluateDeckAgainstIdeal - Compares against the Ideal Deck
  */
  public double evaluateDeckAgainstIdeal(MoveContext context)
  {
    if(idealDeck == null){return -1000.0;}

    HashMap<String, Integer> counts = getCardCounts(context.player.getAllCards());
    double deckSize = context.player.getAllCards().size();
    double eval = 0.0;
    double costMultiplier;
    double percent;
    double percentDelta;
    double tolerance = .15;
    double dampening = .80;
    double totalTreasure = 0;
    double idealTreasure = 0;
    double treasureDelta;
    double actionMultiplier = 8.0;

    for(Card c : idealDeck.getCardPercentages().keySet())
    {
      if(c.is(Type.Action)) {
        costMultiplier = (c.getCost(null) >= 5) ? 1.5 : 1.0;
        if (counts.containsKey(c.getName())) {
          percent = counts.get(c.getName()) / deckSize;
        } else {
          percent = 0.0;
        }
        // Continue to improve score for additional cards up to a certain tolerance
        // threshold, but dampen this extra score.
        percent = Math.min(percent, idealDeck.getCardPercentages().get(c) * (1.0 + tolerance));
        percentDelta = percent - idealDeck.getCardPercentages().get(c);
        if (percentDelta > 0.0) {
          percentDelta *= dampening;
        }
        percentDelta *= costMultiplier;

        eval += percentDelta;
      }
    }

    eval *= gameEvaluator.planEvalActionMultiplier; // actionMultiplier

    // Not buying enough gold... lets try this: count sum of COST of treasure in deck.
    // subtract 2 to further increase relative value of gold, and to give copper a negative score.
    for(Card c : idealDeck.getCards())
    {
      if (c.is(Type.Treasure)){
        idealTreasure  += c.getCost(null) - 1;
      }
    }
    for(Card c : getAllCards())
    {
      if (c.is(Type.Treasure) ){
        totalTreasure  += c.getCost(null) - 1;
      }
    }

    idealTreasure = idealTreasure * (deckSize / idealDeck.getCards().size());
    totalTreasure = Math.min(totalTreasure, idealTreasure * (1.0 + tolerance));
    treasureDelta = totalTreasure - idealTreasure;
    if(treasureDelta > 0) {treasureDelta *= dampening;}

    eval += (treasureDelta * gameEvaluator.planEvalTreasureMultiplier);  // treasureMultiplier

    return ((1.0 - gameProgression(context)) * eval) +
           ((gameProgression(context)) * context.player.getTotalVictoryPoints() * gameEvaluator.planEvalVictoryPointFactor);
  }


  private HashMap<String, Integer> getCardCounts(ArrayList<Card> cards)
  {
    HashMap<String, Integer> counts = new HashMap<String, Integer>();

    for (Card c : cards)
    {
      String name = c.getName();
      if (counts.containsKey(name))
      {
        counts.put(name, counts.get(name) + 1);
      }
      else
      {
        counts.put(name, 1);
      }
    }
    return counts;
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
    double currentEvaluation = evaluateDeckAgainstIdeal(context);
    Card cardToBuy = null;

    for (Card c : context.game.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true)) {

      // Clone Game and Players
      Game clonedGame = context.game.cloneGame();
      VDomPlayerJarvis clonedSelf = null;
      for (Player clonedPlayer : clonedGame.players) {
        if (clonedPlayer.getPlayerName() == "Jarvis") {
          clonedSelf = (VDomPlayerJarvis) clonedPlayer;
        }
      }

      MoveContext clonedContext = new MoveContext(context, clonedGame, clonedSelf);

      // Try the Buy
      Card buyCard = clonedGame.getPile(c).topCard();

      if (buyCard != null && clonedContext.canBuy(buyCard)) {
        clonedGame.broadcastEvent(new GameEvent(GameEvent.EventType.Status, clonedContext));
        clonedGame.playBuy(clonedContext, buyCard);
        clonedGame.playerPayOffDebt(clonedSelf, clonedContext);
        if (evaluateDeckAgainstIdeal(clonedContext) > currentEvaluation) {
          currentEvaluation = evaluateDeckAgainstIdeal(clonedContext);
          cardToBuy = buyCard;
        }
      }
    }

    // Update Card to Buy
    if (cardToBuy != null) {
      returnCard = cardToBuy;
    } else if (context.canBuy(Cards.gold)) {
      returnCard = Cards.gold;
    } else if (context.canBuy(Cards.silver)) {
      returnCard = Cards.silver;
    } else {
      returnCard = null;
    }

    // Update Game Evaluator
    return returnCard;

  }


  // ---------------------------------------------------------------------
  // Overrides to BasePlayer Functions (Helper to Search)
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
      return null; // Let's assume this won't happen for now.
    }
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
