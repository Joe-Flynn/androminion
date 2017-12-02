package com.vdom.players;

// ??? - KEEP WHAT YOU NEED

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.*;


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


  @Override
  public Card doAction(MoveContext context) {
    return doActionHeuristic(context);
  }

  @Override
  public Card doBuy(MoveContext context) {
    return doBuyEvalSearch(context);
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

  @Override
  protected Card bestCardInPlay(final MoveContext context, int maxCost, boolean exactCost, int maxDebtCost, boolean potion, boolean actionOnly, boolean victoryCardAllowed, boolean mustCostLessThanMax, boolean mustPick, Card except) {
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
      if (card.is(Type.Landmark, context.player)
              || card.is(Type.Shelter, context.player)
              || card.equals(Cards.abandonedMine) /*choose only virtualRuins*/
              || card.equals(Cards.ruinedLibrary)
              || card.equals(Cards.ruinedMarket)
              || card.equals(Cards.ruinedVillage)
              || card.equals(Cards.survivors)
              || (except != null && card.equals(except))
              || (card.is(Type.Knight, null) && !card.equals(Cards.virtualKnight)) /*choose only virtualKnight*/ //TODO SPLITPILES what here?
              || !Cards.isSupplyCard(card)
              || !context.isCardOnTop(card)
              || (actionOnly && !(card.is(Type.Action)))
              || (!victoryCardAllowed && (card.is(Type.Victory)) && !card.equals(Cards.curse))
              || (exactCost && (cardCost != maxCost || cardDebt != maxDebtCost || maxPotionCost != cardPotion))
              || (cardCost > maxCost || cardDebt > maxDebtCost || cardPotion > maxPotionCost)
              || (mustCostLessThanMax && (cardCost == maxCost && cardDebt == maxDebtCost && maxPotionCost == cardPotion))
              || (isBuy && !context.canBuy(card))
              ) {
        /*card not allowed*/
      } else if (   card.equals(Cards.curse)
              || isTrashCard(card)
              || (card.equals(Cards.potion) && !shouldBuyPotion())
              ) {
        /*card allowed, but not wanted*/
        cardListBad.add(card);
      } else {
        cardListGood.add(card);
      }
    }


    if (cardListGood.size() > 0) {
      cardListGood.add(null);
      return choose(cardListGood);
      //return cardListGood.get(0);
    }

    if (mustPick && cardListBad.size() > 0) {
      // don't add null to this one, we have to choose something if possible
      return choose(cardListBad);
      //return cardListBad.get(0);
    }

    return null;
  }

  // turns out highestCard is only called twice, let it do its thing for now...
  /*
  @Override
  protected Card highestCard(MoveContext context, Iterable<Card> cards) {
    float highestCost = -1;
    Card highestCard = null;
    for (Card c : cards) {
      float cost = c.getCost(context);
      float potionOffset = c.costPotion() ? 2.5f : 0.0f;
      if (cost + potionOffset > highestCost) {
        highestCost = cost + potionOffset;
        highestCard = c;
      }
    }
    return highestCard;
  }
  */

  // 25 usages -> TODO: either funnel to choose here or through lowestCards
  // Also, we might need to do less option filtering here for discard/trash for benefit scenarios.
  @Override
  public Card lowestCard(MoveContext context, CardList cards, boolean discard) {
    Card[] ret = lowestCards(context, cards.toArrayList(), 1, discard);
    if(ret == null) {
      return null;
    }

    return ret[0];
  }

  // 3 usages as is, with additional upstream usages including 25 from lowestCard
  @Override
  public Card[] lowestCards(MoveContext context, ArrayList<Card> cards, int num, boolean discard/*discard or trash?*/) {
    if (cards == null) {
      return null;
    }

    if (cards.size() == 0) {
      return null;
    }

    if (cards.size() <= num) {
      return cards.toArray(new Card[0]);
    }

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

    /*
    // Add all...
    for (int i = cardArray.size() - 1; i >= 0; i--) {
      Card card = cardArray.get(i);
      ret.add(card);
      cardArray.remove(i);

      if(ret.size() == num) {
        return ret.toArray(new Card[ret.size()]);
      }
    }

    // Should never get here, but just in case...
    return ret.toArray(new Card[0]);
  */
  }

  // implement choose combination to use here??
  /*
  @Override
  public Card[] pickOutCards(CardList cards, int num, Card[] cardsToMatch) {
    if(cards == null) {
      return null;
    }

    if(cards.size() == 0) {
      return null;
    }

    ArrayList<Card> ret = new ArrayList<Card>();
    for(Card match : cardsToMatch) {
      for(Card c : cards) {
        if(c.equals(match)) {
          ret.add(c);
        }

        if(ret.size() == num) {
          return ret.toArray(new Card[0]);
        }
      }
    }

    if(ret.size() == 0) {
      return null;
    }

    return ret.toArray(new Card[0]);
  }
  */

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
    /*
    ArrayList<Card> keepers = new ArrayList<Card>();
    ArrayList<Card> discards = new ArrayList<Card>();

    // Just add in the non-victory cards...
    for (Card card : context.attackedPlayer.getHand()) {
      if (!shouldDiscard(card, new MoveContext(context.game, context.attackedPlayer))) {
        keepers.add(card);
      } else {
        discards.add(card);
      }
    }

    while (keepers.size() < numToKeep) {
      keepers.add(discards.remove(0));
    }

    // Still more than numToKeep? Remove all but one action...
    while (keepers.size() > numToKeep) {
      int bestAction = -1;
      boolean removed = false;
      for (int i = 0; i < keepers.size(); i++) {
        if (keepers.get(i).is(Type.Action, context.player)) {
          if (bestAction == -1) {
            bestAction = i;
          } else {
            if(keepers.get(i).getCost(context) > keepers.get(bestAction).getCost(context)) {
              keepers.remove(bestAction);
              bestAction = i;
            }
            else {
              keepers.remove(i);
            }
            removed = true;
            break;
          }
        }
      }
      if (!removed) {
        break;
      }
    }

    // Still more than numToKeep? Start removing copper...
    while (keepers.size() > numToKeep) {
      boolean removed = false;
      for (int i = 0; i < keepers.size(); i++) {
        if (keepers.get(i).equals(Cards.copper)) {
          keepers.remove(i);
          removed = true;
          break;
        }
      }
      if (!removed) {
        break;
      }
    }

    // Still more than numToKeep? Start removing silver...
    while (keepers.size() > numToKeep) {
      boolean removed = false;
      for (int i = 0; i < keepers.size(); i++) {
        if (keepers.get(i).equals(Cards.silver)) {
          keepers.remove(i);
          removed = true;
          break;
        }
      }
      if (!removed) {
        break;
      }
    }

    while (keepers.size() > numToKeep) {
      keepers.remove(0);
    }

    return keepers.toArray(new Card[0]);

    */
  }

  /*
  ** doActionEvalSearch - Evaluate Best Action to Play Using a Cloned Game
  ** State, and evaluating the effect of each Card in the Player's hand on it.
  */
  public Card doActionEvalSearch(MoveContext context) {

    double currentEvaluation = gameEvaluator.evaluate(context, this.getAllCards());
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
          if (clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
            currentEvaluation = clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards());
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

    // Update Game Evaluator
    return returnCard;

  }

  /*
  ** doBuyEvalSearch - Evaluate Best Card to Buy Using a Cloned Game State,
  ** and evaluating the effect of each Buyable Card against the Current State.
  */
  public Card doBuyEvalSearch(MoveContext context) {

    Card returnCard = null;

    // Buy Province or Gold, First
//    if (context.getCoinAvailableForBuy() == 0) {
//      returnCard = null;
//    } else if (context.canBuy(Cards.province)) {
//      returnCard = Cards.province;
//    } else if (context.canBuy(Cards.gold)) {
//      returnCard = Cards.gold;
//    } else {

      // Buy Card that Improves Player's Evaluation Most
      //double currentEvaluation = gameEvaluator.evaluate(context, this.getAllCards());
      double currentEvaluation = -1000.0;
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



  // We man want to modify which cards get considered to be "garbage", differently from the Base Player
  // public Card[] getTrashCards() { return null; }

}
