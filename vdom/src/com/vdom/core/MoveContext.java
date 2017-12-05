package com.vdom.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.vdom.api.Card;
import com.vdom.api.CardCostComparator;
import com.vdom.api.GameEventListener;
import com.vdom.api.GameType;
import com.vdom.core.Cards.Kind;

public class MoveContext {

  public Game   game;
  public String message;
  public Player player;
  public Player attackedPlayer;

  public  int actions = 1;
  public  int buys    = 1;
  private int coins   = 0;
  public  int potions = 0;

  public int actionsPlayedSoFar = 0;
  public int merchantsPlayed    = 0;
  public int silversPlayed      = 0;
  public int coppersmithsPlayed = 0;
  public int schemesPlayed      = 0;

  public int overpayAmount  = 0; // The number of extra coins paid for a card
  public int overpayPotions = 0; // The number of potions paid for an overpay card

  public int foolsGoldPlayed = 0;
  public int golemInEffect = 0;
  public int freeActionInEffect = 0;

  public int cardCostModifier = 0;

  public int victoryCardsBoughtThisTurn = 0;
  public int totalCardsBoughtThisTurn = 0;
  public int totalEventsBoughtThisTurn = 0;
  public int totalExpeditionBoughtThisTurn = 0;

  public boolean canBuyCards = true;
  public boolean startOfTurn = false;

  public enum TurnPhase { Action, Buy, CleanUp }
  public TurnPhase phase = TurnPhase.Action;

  public boolean blackMarketBuyPhase = false;  // this is not a really buyPhase (peddler costs 8, you can't spend Guilds coin tokens)
  public boolean returnToActionPhase = false;

  public ArrayList<Card> cantBuy = new ArrayList<Card>();

  public int     beggarSilverIsOnTop = 0;
  public boolean graverobberGainedCardOnTop = false;
  public boolean travellingFairBought = false;
  public boolean missionBought = false;
  public boolean enchantressAlreadyAffected = false;
  public boolean hasDoubledCoins = false;
  public int     donatesBought = 0;
  public int     charmsNextBuy = 0;

  public enum PileSelection { DISCARD, HAND, DECK, ANY };
  public PileSelection hermitTrashCardPile = PileSelection.ANY;

  // For checking Achievements
  public int vpsGainedThisTurn = 0;
  public int cardsTrashedThisTurn = 0;


  public MoveContext(Game game, Player player) {
    this(game, player, true);
  }

  public MoveContext(Game game, Player player, boolean canBuyCards) {
    this.game = game;
    this.player = player;
    this.canBuyCards = canBuyCards;
    if (player.getInheritance() != null) {
      cantBuy.add(Cards.inheritance);
    }
  }

  public MoveContext(MoveContext context, Game game, Player player) {
    this.actions = context.actions;
    this.buys = context.buys;
    this.coins = context.coins;
    this.game = game;
    this.player = player;
    if (player.getInheritance() != null) {
      cantBuy.add(Cards.inheritance);
    }
  }

  public Player getPlayer() {
    return player;
  }

  public Player getOpponent() {
    for (Player opponent : game.players) {
      if (opponent.getPlayerName() != player.getPlayerName()) {
        return opponent;
      }
    }
    return null;
  }

  public int getPotions() {
    return potions;
  }

  public ArrayList<Card> getCantBuy() {
    return cantBuy;
  }

  public CardList getPlayedCards() {
    return player.playedCards;
  }

  public int countCardsInPlay(Card card) {
    int cardsInPlay = 0;
    for(Card c : getPlayedCards()) {
      if(c.behaveAsCard().equals(card)) {
        cardsInPlay++;
      }
    }
    return cardsInPlay + countCardsInNextTurn(card);
  }

  public int countCardsInPlayByName(Card card) {
    int cardsInPlay = 0;
    for(Card c : getPlayedCards()) {
      if(!c.equals(Cards.estate) && c.behaveAsCard().equals(card)) {
        cardsInPlay++;
      }
    }
    return cardsInPlay + countCardsInNextTurnByName(card);
  }

  public CardList getCardsInNextTurn() {
    return player.nextTurnCards;
  }

  private int countCardsInNextTurn(Card card) {
    int cardsInNextTurn = 0;
    for(Card c : getCardsInNextTurn()) {
      if(c.behaveAsCard().equals(card)) {
        cardsInNextTurn++;
      }
    }
    return cardsInNextTurn;
  }

  private int countCardsInNextTurnByName(Card card) {
    int cardsInNextTurn = 0;
    for(Card c : getCardsInNextTurn()) {
      if(!c.equals(Cards.estate) && c.behaveAsCard().equals(card)) {
        cardsInNextTurn++;
      }
    }
    return cardsInNextTurn;
  }

  public boolean isRoyalSealInPlay() {
    return (countCardsInPlay(Cards.royalSeal) > 0);
  }

  public int countGoonsInPlay() {
    return countCardsInPlay(Cards.goons);
  }

  public enum CardsInPlay {ACTION,ATTACK,TRAVELLER,VICTORY,TREASURE};

  public int countActionCardsInPlay() {
    return countTypedCardsInPlay(Type.Action);
  }

  public int countAttackCardsInPlay() {
    return countTypedCardsInPlay(Type.Attack);
  }

  public int countTreasureCardsInPlay() {
    return countTypedCardsInPlay(Type.Treasure);
  }

  public int countTravellerCardsInPlay() {
    return countTypedCardsInPlay(Type.Traveller);
  }

  public int countVictoryCardsInPlay() {
    return countTypedCardsInPlay(Type.Victory);
  }

  public int countTypedCardsInPlay(Type type) {
    int numInPlay = 0;
    for (Card c : getPlayedCards()) {
      if (c.is(type, player)) {
        numInPlay++;
      }
    }
    for (Card c : player.nextTurnCards) {
      if (c instanceof CardImpl && ((CardImpl)c).trashAfterPlay)
        continue;
      if (c.is(type, player)) {
        numInPlay++;
      }
    }
    return numInPlay;
  }

  public int countUniqueCardsInPlay() {
    HashSet<String> distinctCardsInPlay = new HashSet<String>();

    for (Card cardInPlay : player.playedCards) {
      if (cardInPlay.getControlCard().equals(Cards.estate)) {
        distinctCardsInPlay.add(cardInPlay.getName());
      } else {
        distinctCardsInPlay.add(cardInPlay.behaveAsCard().getName());
      }
    }
    for (Card cardInPlay : player.nextTurnCards) {
      if (cardInPlay.getControlCard().equals(Cards.estate)) {
        distinctCardsInPlay.add(cardInPlay.getName());
      } else {
        distinctCardsInPlay.add(cardInPlay.behaveAsCard().getName());
      }
    }

    return distinctCardsInPlay.size();
  }

  public int countMerchantGuildsInPlayThisTurn() {
    return countCardsInPlay(Cards.merchantGuild);
  }

  public int getVictoryCardsBoughtThisTurn() {
    return victoryCardsBoughtThisTurn;
  }

  public int getTotalCardsBoughtThisTurn() {
    return totalCardsBoughtThisTurn;
  }

  public int getTotalEventsBoughtThisTurn() {
    return totalEventsBoughtThisTurn;
  }

  public boolean buyWouldEndGame(Card card) {
    return game.buyWouldEndGame(card);
  }

  public int countThroneRoomsInEffect() {
    return freeActionInEffect;
  }

  public int getPileSize(Card card) {
    return game.pileSize(card);
  }

  public int emptyPileCount() {
    return game.emptyPiles();
  }

  public int getEmbargos(Card card) {
    return game.getEmbargos(card);
  }

  public int getPileVpTokens(Card card) {
    return game.getPileVpTokens(card);
  }

  public int getPileDebtTokens(Card card) {
    return game.getPileDebtTokens(card);
  }

  public int getPileTradeRouteTokens(Card card) {
    return game.getPileTradeRouteTokens(card);
  }

  public int getEmbargosIfCursesLeft(Card card) {
    int embargos = game.getEmbargos(card);
    if (!card.is(Type.Event, null)) {
      embargos += game.swampHagAttacks(player);
    }
    return Math.min(embargos, game.pileSize(Cards.curse));
  }

  public ArrayList<Card> getCardsObtainedByLastPlayer() {
    return game.getCardsObtainedByLastPlayer();
  }

  public int getNumCardsGainedThisTurn() {
    return game.getCardsObtainedByPlayer().size();
  }

  public int getNumCardsGainedThisTurn(Kind kind) {
    int result = 0;
    for (Card c : game.getCardsObtainedByPlayer()) {
      if (c.getKind() == kind) {
        result++;
      }
    }
    return result;
  }

  public HashMap<String, Integer> getCardCounts() {
    HashMap<String, Integer> cardCounts = new HashMap<String, Integer>();
    for (String cardName : game.piles.keySet()) {
      int count = game.piles.get(cardName).getCount();
      if (count > 0) {
        cardCounts.put(cardName, count);
      }
    }
    return cardCounts;
  }

  public Card[] getBuyableCards() {
    ArrayList<Card> buyableCards = new ArrayList<Card>();
    for (Card card : getCardsInGame(GetCardsInGameOptions.TopOfPiles, true)) {
      if (canBuy(card)) {
        buyableCards.add(card);
      }
    }

    Collections.sort(buyableCards, new CardCostComparator());
    return buyableCards.toArray(new Card[0]);
  }

  public void addGameListener(GameEventListener listener) {
    if (listener != null && !game.listeners.contains(listener)) {
      game.listeners.add(listener);
    }
  }

  public void removeGameListener(GameEventListener listener) {
    if (listener != null && game.listeners.contains(listener)) {
      game.listeners.remove(listener);
    }
  }

  public GameType getGameType() {
    return game.gameType;
  }

  public boolean canPlay(Card card) {
    if (card.is(Type.Action, player)) {
      return game.isValidAction(this, card);
    } else {
      return false;
    }
  }

  public boolean canBuy(Card card) {
    return game.isValidBuy(this, card);
  }

  public boolean canBuy(Card card, int gold) {
    return game.isValidBuy(this, card, gold);
  }

  public int getActionsLeft() {
    return actions;
  }

  public int getBuysLeft() {
    return buys;
  }

  public int getCoins() {
    return coins;
  }

  public int getCoinAvailableForBuy() {
    return getCoins();
  }

  public void addCoins(int coinsToAdd) {
    addCoins(coinsToAdd, null);
  }

  public void addCoins(int coinsToAdd, Card responsible) {
    if (coinsToAdd == 0) {
      return;
    }
    if (coinsToAdd > 0) {
      if (getPlayer().getMinusOneCoinToken()) {
        --coinsToAdd;
        getPlayer().setMinusOneCoinToken(false, this);
      }
    }

    coins += coinsToAdd;
    if (coins < 0)
      coins = 0;
  }

  public void spendCoins(int coinsToSpend) {
    coins -= coinsToSpend;
  }

  public int getPotionsForStatus(Player p) {
    return potions;
  }

  public String getAttackedPlayer() {
    return (attackedPlayer == null)?null:attackedPlayer.getPlayerName();
  }

  public String getMessage() {
    return message;
  }

  public Card[] getCardsInGame(GetCardsInGameOptions opt) {
    return getCardsInGame(opt, false);
  }

  public Card[] getCardsInGame(GetCardsInGameOptions opt, boolean supplyOnly) {
    return getCardsInGame(opt, supplyOnly, null);
  }

  public Card[] getCardsInGame(GetCardsInGameOptions opt, boolean supplyOnly, Type type) {
    return game.getCardsInGame(opt, supplyOnly, type);
  }

  public Card[] getSupply() {
    return game.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true);
  }

  public boolean cardInGame(Card card) {
    return game.cardInGame(card);
  }

  public boolean isCardOnTop(Card card) {
    return game.isCardOnTop(card);
  }

  public int getCardsLeftInPile(Card card) {
    return game.getCardsLeftInPile(card);
  }

  protected boolean isNewCardAvailable(int cost, int debt, boolean potion) {
    for(Card c : getCardsInGame(GetCardsInGameOptions.TopOfPiles, true, null)) {
      if(Cards.isSupplyCard(c)&& c.getCost(this) == cost && c.getDebtCost(this) == debt && c.costPotion() == potion && isCardOnTop(c)) {
        return true;
      }
    }
    return false;
  }

  protected Card[] getAvailableCards(int cost, boolean potion) {
    ArrayList<Card> cards = new ArrayList<Card>();
    for(Card c : getCardsInGame(GetCardsInGameOptions.TopOfPiles, true, null)) {
      if(Cards.isSupplyCard(c) && c.getCost(this) == cost && c.costPotion() == potion && isCardOnTop(c)) {
        cards.add(c);
      }
    }
    return cards.toArray(new Card[0]);
  }


  /*
  ** cloneContext - Returns a clone of a MoveContext, including a full clone of
  ** the Game, Player, and all parameters within the MoveContext as well.
  */
  public MoveContext cloneContext() {

    // Clone the Game
    Game clonedGame = game.cloneGame();

    // Find Cloned Player
    Player clonedPlayer = null;
    for (Player clonedGamePlayer : clonedGame.players) {

      if (clonedGamePlayer.getPlayerName() == player.getPlayerName()) {
        clonedPlayer = clonedGamePlayer;
      }
    }

    // Find Cloned "Attacked Player"
    Player clonedAttackedPlayer = null;
    if (attackedPlayer != null) {
      for (Player clonedGamePlayer : clonedGame.players) {
        if (clonedGamePlayer.getPlayerName() == attackedPlayer.getPlayerName()) {
          clonedAttackedPlayer = attackedPlayer;
        }
      }
    }

    // Create Cloned MoveContext
    MoveContext clone = new MoveContext(clonedGame, clonedPlayer, canBuyCards);

    // Copy Over Cloned MoveContext Info
    clone.message = message;
    clone.attackedPlayer = clonedAttackedPlayer;
    clone.actions = actions;
    clone.buys = buys;
    clone.coins = coins;
    clone.potions = potions;
    clone.actionsPlayedSoFar = actionsPlayedSoFar;
    clone.merchantsPlayed = merchantsPlayed;
    clone.silversPlayed = silversPlayed;
    clone.coppersmithsPlayed = coppersmithsPlayed;
    clone.schemesPlayed = schemesPlayed;
    clone.overpayAmount = overpayAmount;
    clone.overpayPotions = overpayPotions;
    clone.foolsGoldPlayed = foolsGoldPlayed;
    clone.golemInEffect = golemInEffect;
    clone.freeActionInEffect = freeActionInEffect;
    clone.cardCostModifier = cardCostModifier;
    clone.victoryCardsBoughtThisTurn = victoryCardsBoughtThisTurn;
    clone.totalCardsBoughtThisTurn = totalCardsBoughtThisTurn;
    clone.totalEventsBoughtThisTurn = totalEventsBoughtThisTurn;
    clone.totalExpeditionBoughtThisTurn = totalExpeditionBoughtThisTurn;
    clone.canBuyCards = canBuyCards;
    clone.startOfTurn = startOfTurn;
    clone.phase = phase;
    clone.blackMarketBuyPhase = blackMarketBuyPhase;
    clone.returnToActionPhase = returnToActionPhase;
    clone.beggarSilverIsOnTop = beggarSilverIsOnTop;
    clone.graverobberGainedCardOnTop = graverobberGainedCardOnTop;
    clone.travellingFairBought = travellingFairBought;
    clone.missionBought = missionBought;
    clone.enchantressAlreadyAffected = enchantressAlreadyAffected;
    clone.hasDoubledCoins = hasDoubledCoins;
    clone.donatesBought = donatesBought;
    clone.charmsNextBuy = charmsNextBuy;
    clone.hermitTrashCardPile = hermitTrashCardPile;
    clone.vpsGainedThisTurn = vpsGainedThisTurn;
    clone.cardsTrashedThisTurn = cardsTrashedThisTurn;

    // Clone Cards in CantBuy ArrayList
    clone.cantBuy = new ArrayList<Card>();
    for (Card card : cantBuy) {
      clone.cantBuy.add(card.clone());
    }

    // Return Clone
    return clone;

  }


  /*
  ** debug - Print out Debug Messages
  */
  public void debug(String msg) {
    debug(msg, true);
  }

  /*
  ** debug - Print out Debug Messages
  */
  private void debug(String msg, boolean prefixWithPlayerName) {
    if (!prefixWithPlayerName || player == null) {
      Util.debug(msg);
    } else {
      player.debug(msg);
    }
  }

}