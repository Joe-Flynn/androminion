package com.vdom.players;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

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





  /*
  ** Card Action Interactions - Overridden to enter search tree!
  */
  private <T> T choose(ArrayList <T> options)
  {

    // TODO: interact with search tree
    // Pseudocode:

    // If in search:
      // Check if current node has been expanded already.
      // If expanded, Return option of first untried expanded node. (Should there be checks to make sure the option is still applicable?)
      // Else, call for expansion of the tree, then return option of first untried node (or return option 1?)
    // Else:
      // Initialize search, and return the result.

    // Temporarily return a random option
    return options.get(rand.nextInt(options.size()));
  }


  private int choose_index(int number_of_options)
  {
    ArrayList<Integer> options = new ArrayList<>();
    for(Integer i = 0; i < number_of_options; i++)
    {
      options.add(i);
    }
    return choose(options);
  }

  private boolean choose_bool()
  {
    ArrayList<Boolean> options = new ArrayList<>();
    options.add(Boolean.TRUE);
    options.add(Boolean.FALSE);
    return choose(options);
  }

  private <T> Card[] choose_combination(Card[] cards, int n)
  {
    ArrayList<Card[]> options = new ArrayList<>();

    // temporarily, simply return one possibility
    Card[] option = new Card[n];
    for (int i = 0; i < n; i++)
    {
      option[i] = cards[i];
    }
    options.add(option);
    return choose(options);
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
          (isBuy && !context.canBuy(card)) {
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
      cardListGood.add(null);
      return choose(cardListGood);
    }

    // Otherwise, pick from the scraps (a.k.a. "Bad" Cards)
    if (mustPick && cardListBad.size() > 0) {
      return choose(cardListBad);
    } else {
      return null;
    }
  }


  // Lowest Card used for 28 different Cards in VDom Engine (total)
  // NOTE: Added Option to Choose Combination Here

  @Override
  public Card[] lowestCards(MoveContext context, ArrayList<Card> cards, int num, boolean discard) {

    // Safety Checks
    if (cards == null) { return null; }
    if (cards.size() == 0) { return null; }
    if (cards.size() <= num) { return cards.toArray(new Card[0]); }

    // Create CardList
    CardList cardArray = new CardList(controlPlayer, getPlayerName(false));
    for(Card c : cards) {
      cardArray.add(c);
    }

    ArrayList<Card> ret = new ArrayList<Card>();

    //if discard victory cards first
    if(discard) {
      //Tunnel first
      while (cardArray.contains(Cards.tunnel)) {
        ret.add(Cards.tunnel);
        cardArray.remove(Cards.tunnel);
        if (ret.size() == num) {
          return ret.toArray(new Card[ret.size()]);
        }
      }
      for (int i = cardArray.size() - 1; i >= 0; i--) {
        Card card = cardArray.get(i);
        if (isOnlyVictory(card, context.getPlayer())) {
          ret.add(card);
          cardArray.remove(i);
          if (ret.size() == num) {
            return ret.toArray(new Card[ret.size()]);
          }
        }
      }
    }
    
    //next trash cards
    Card[] trashCards = pickOutCards(cardArray, (num - ret.size()), getTrashCards());
    if (trashCards != null) {
      for(Card c : trashCards) {
        ret.add(c);
        cardArray.remove(c);
        if (ret.size() == num) {
          return ret.toArray(new Card[ret.size()]);
        }
      }
    }

    // By cost...
    int cost = 1; //start with 1 because all cards left with cost=0 should be good (prizes, Madman, spoils, ...)
    while(cost <= COST_MAX) {
      for (int i = cardArray.size() - 1; i >= 0; i--) {
        Card card = cardArray.get(i);
        if (   card.getCost(context) == cost
                || card.getCost(context) == 0 && cost == 7) { //prizes are worth 7, see http://boardgamegeek.com/thread/651180/if-prizes-were-part-supply-how-much-would-they-cos
          ret.add(card);
          cardArray.remove(i);

          if(ret.size() == num) {
            return ret.toArray(new Card[ret.size()]);
          }
        }
      }
      cost++;
    }

    if(cardArray.size() == num){ return cardArray.toArray();}

    return choose_combination(cardArray.toArray(), num);

  }


  // a simple approach for now might be to shuffle a few times and pass the shuffled arrays?
  // ultimately, this should be handled in a function that can also optionally sift and trash.
  @Override
  public Card[] topOfDeck_orderCards(MoveContext context, Card[] cards) {
    return cards;
  }

  @Override
  protected Card[] discardAttackCardsToKeep(MoveContext context, int numToKeep) {

    Card[] discards = lowestCards(context, getHand(), getHand().size() - numToKeep, true);
    ArrayList<Card> hand = getHand().toArrayListClone();

    // TODO: find a cleaner way to do this ??
    for(Card c : discards)
    {
      for( Card d: hand)
      {
        if(c == d)
        {
          hand.remove(d);
          break;
        }
      }
    }

    return hand.toArray(new Card[numToKeep]);

  }


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
    System.out.println(">>>> JARVIS: actionPhasePlayCount: " + actionPhasePlayCount);
    System.out.println(">>>> JARVIS: actionPath length: " + actionPath.size());
    SearchTree.TreeNode actionNode = actionPath.get(actionPhasePlayCount);
    System.out.println(">>>> JARVIS: actionPath length: " + actionPath.size());
    Card actionCard = actionNode.getActionCard();
    if (actionCard == null) {
      return null;
    }

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
