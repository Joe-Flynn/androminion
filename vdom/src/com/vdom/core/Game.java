package com.vdom.core;

import java.util.*;
import java.util.Map.Entry;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

import com.vdom.api.Card;
import com.vdom.api.CardCostComparator;
import com.vdom.api.GameEvent;
import com.vdom.api.GameEvent.EventType;
import com.vdom.api.GameEventListener;
import com.vdom.api.GameType;

import com.vdom.core.MoveContext.TurnPhase;
import com.vdom.core.Player.ExtraTurnOption;
import com.vdom.core.Player.HuntingGroundsOption;
import com.vdom.core.Player.WatchTowerOption;
import com.vdom.core.Player.FoolsGoldOption;

import com.vdom.players.*;

public class Game {

  // The CARD SET to use for the game (See com.vdom.api.GameType)
  public    GameType gameType;
  protected List<Expansion> randomExpansions;
  protected List<Expansion> randomExcludedExpansions;

  // Prefixes for Specially Named Cards (Specified at Launch)
  protected static final String BANE = "bane+";
  protected static final String OBELISK = "obelisk+";
  protected static final String BLACKMARKET = "blackMarket+";

  // Array to Hold Specially Named Cards (Specified-at-Launch and Un-Found)
  protected String[] cardsSpecifiedAtLaunch;
  protected ArrayList<String> unfoundCards;
  protected String cardListText;
  protected String unfoundCardText;

  // Game Configurations for Platinum and Colony (from "Properity" Expansion)
  protected boolean platColonyNotPassedIn;
  protected boolean platColonyPassedIn;
  protected double  chanceForPlatColony;

  // Game Configurations for Shelters (from "Dark Ages" Expansion)
  protected boolean sheltersNotPassedIn;
  protected boolean sheltersPassedIn;
  protected double  chanceForShelters;

  // Game Configurations for Events & Landmarks (from "Adventures" / "Empires Expansion")
  protected boolean randomIncludesEvents;
  protected boolean randomIncludesLandmarks;
  protected int     numRandomEvents;
  protected int     numRandomLandmarks;
  protected boolean splitMaxEventsAndLandmarks;

  // Game Configurations for Black Market (from "Promo" Expansion)
  protected static enum BlackMarketSplitPileOptions { NONE, ONE, ANY, ALL }
  protected static final int blackMarketCount = 25;
  protected boolean blackMarketOnlyCardsFromUsedExpansions;
  protected BlackMarketSplitPileOptions blackMarketSplitPileOptions;

  // General Game Configurations
  protected boolean quickPlay;       // Simple Treasure Selection
  protected boolean actionChains;    // Allow Multiple Actions
  protected boolean equalStartHands; // Start Players with Equal Hands
  public    boolean maskPlayerNames; // Mask Player Name on Output

  // Game Errata Configurations
  public static final boolean errataPossessedTakesTokens    = false; // Introduced May 2016 - True enables old behavior
  public static final boolean errataMasqueradeAlwaysAffects = false; // Introduced Oct 2016 - True enables old behavior
  public static final boolean errataMineForced              = false; // Introduced Oct 2016 - True enables old behavior
  public static final boolean errataMoneylenderForced       = false; // Introduced Oct 2016 - True enables old behavior
  public static final boolean errataShuffleDeckEmptyOnly    = false; // Introduced Oct 2016 - True enables old behavior

  // Selection Randomizer
  public static Random rand = new Random(System.currentTimeMillis());

  // Game Pile Size Configurations
  protected static final int kingdomCardPileSize = 10;
  protected int victoryCardPileSize;

  // Number of Total Games and Players
  protected int numGames;
  public    int numPlayers;   // Number of Players Per Game
  public    Player[] players; // Array of Players in Each Game

  // Game's PLayer's Turns
  protected int playersTurn;                         // Index of Current Player
  protected int gameTurnCount;                       // Current Game Turn Counter
  protected int consecutiveTurnCounter;              // Current Player Consecutive Turn Counter
  protected ArrayList<Card>[] cardsObtainedLastTurn; // Cards obtained per Player

  // Game Card Piles
  public HashMap<String, CardPile> piles;            // Card Piles in play in Game
  public HashMap<String, CardPile> placeholderPiles; // Placeholder Piles for Setup
  public ArrayList<Card> trashPile;                  // Trash Pile

  // Special Card-Specific Card Piles
  public ArrayList<Card> possessedTrashPile;
  public ArrayList<Card> possessedBoughtPile;
  public ArrayList<Card> blackMarketPile;
  public ArrayList<Card> blackMarketPileShuffled;

  // Game Tokens
  public HashMap<String, Integer> embargos;       // Number of Embargo Tokens per Card Pile
  public HashMap<String, Integer> pileVpTokens;   // Number of VP Tokens per Card Pile (i.e. Landmarks)
  public HashMap<String, Integer> pileDebtTokens; // Number of Debt Tokens (a.k.a. "Tax") per Card Pile
  protected HashMap<String, HashMap<Player, List<PlayerSupplyToken>>> playerSupplyTokens; // Adventures Tokens

  // Special Card-Specific Game Values
  public int    possessionsToProcess;         // Needed for Possession (from "Alchemy" Expansion)
  public Player possessingPlayer;             // Needed for Possession (from "Alchemy" Expansion)
  public int    nextPossessionsToProcess;     // Needed for Possession (from "Alchemy" Expansion)
  public Player nextPossessingPlayer;         // Needed for Possession (from "Alchemy" Expansion)

  public int    tradeRouteValue;              // Trade Route's Value (from "Prosperity" Expansion)
  public Card   baneCard;                     // Young Witch's Bane (from "Cornucopia" Expansion)
  public Card   obeliskCard;                  // Obelisk's Action Card (from "Empires" Expansion)

  public boolean sheltersInPlay;              // Shelters in Game (from "Dark Ages" Expansion)
  public boolean bakerInPlay;                 // Baker Supply in Game (from "Guilds" Expansion)
  public boolean journeyTokenInPlay;          // Journey Token in Game (from "Adventures" Expansion)

  public boolean firstProvinceWasGained;      // Needed for Mountain Pass (from "Empires" Expansion)
  public boolean doMountainPassAfterThisTurn; // Needed for Mountain Pass (from "Empires" Expansion)
  public int     firstProvinceGainedBy;       // Needed for Mountain Pass (from "Empires" Expansion)

  // Error Logging Options
  public static boolean ignoreAllPlayerErrors;
  public static boolean ignoreSomePlayerErrors;
  public static HashSet<String> ignoreList;

  // Game Listeners (For Logging Game Data)
  public ArrayList<GameEventListener> listeners;
  public GameEventListener gameListener;

  // Win Stat Trackers (Player to # Wins Over ALL Games and GameTypes)
  protected HashMap<String, Double> overallWins;

  // Overall Stat Tracker per GameType
  protected ArrayList<GameStats> gameTypeStats;

  // Extra Turn Info Class
  protected static class ExtraTurnInfo {
    public ExtraTurnInfo() { ; }
    public ExtraTurnInfo(boolean canBuyCards) { this.canBuyCards = canBuyCards; }
    public boolean canBuyCards = true;
  }

  // -----------------------------------------------
  // Evaluator Parameters to Tune (or Machine Learn)
  // -----------------------------------------------

  protected double coinFactor                 =  1.0;
  protected double potionFactor               =  0.5;
  protected double threeCostGainFactor        =  1.0;
  protected double fourCostGainFactor         =  1.1;
  protected double fiveCostGainFactor         =  1.2;
  protected double coinTokenFactor            =  1.0;
  protected double debtTokenFactor            = -1.0;
  protected double victoryTokenFactor         =  1.0;
  protected double enemyHandSizeFactor        = -1.0;

  protected double treasureDeltaFactor        =  1.0;
  protected double actionDeltaFactor          = -1.0;
  protected double victoryPointFactor         =  0.17;

  protected double planEvalActionMultiplier   = 8.0;
  protected double planEvalTreasureMultiplier = 1.0;
  protected double planEvalVictoryPointFactor = 0.17;


  /*
  ** Game Constructor
  */
  public Game() {

    // Num Games and Players
    numGames   = 10;
    numPlayers = 2;

    // CARD SET to use for the game (See com.vdom.api.GameType)
    gameType                 = GameType.RandomBaseGame;
    randomExpansions         = null;
    randomExcludedExpansions = null;

    // Array to Hold Specially Named Cards (Specified-at-Launch and Un-Found)
    cardsSpecifiedAtLaunch = null;
    unfoundCards           = new ArrayList<String>();
    cardListText           = "";
    unfoundCardText        = "";

    // Game Configurations for Platinum and Colony (from "Prosperity" Expansion)
    platColonyNotPassedIn = false;
    platColonyPassedIn    = false;
    chanceForPlatColony   = -1;

    // Game Configurations for Shelters (from "Dark Ages" Expansion)
    sheltersNotPassedIn   = false;
    sheltersPassedIn      = false;
    chanceForShelters     = 0.0;

    // Game Configurations for Events & Landmarks (from "Adventures" / "Empires Expansion")
    randomIncludesEvents       = false;
    randomIncludesLandmarks    = false;
    numRandomEvents            = 0;
    numRandomLandmarks         = 0;
    splitMaxEventsAndLandmarks = true;

    // Game Configurations for Black Market (from "Promo" Expansion)
    blackMarketOnlyCardsFromUsedExpansions = false;
    blackMarketSplitPileOptions = BlackMarketSplitPileOptions.NONE;

    // General Game Configurations
    quickPlay       = false; // Simple Treasure Selection
    actionChains    = false; // Allow Multiple Actions
    equalStartHands = false; // Start Players with Equal Hands
    maskPlayerNames = false; // Mask Player Name on Output

    // Game's PLayer's Turns
    playersTurn   = 0;
    gameTurnCount = 0;
    consecutiveTurnCounter = 0;
    cardsObtainedLastTurn  = null;

    // Initialize Game Card Piles
    piles            = new HashMap<String, CardPile>(); // Card Piles in play in Game
    placeholderPiles = new HashMap<String, CardPile>(); // Placeholder Piles for Setup
    trashPile        = new ArrayList<Card>();           // Trash Pile

    // Initialize Special Card-Specific Card Piles
    possessedTrashPile      = new ArrayList<Card>();
    possessedBoughtPile     = new ArrayList<Card>();
    blackMarketPile         = new ArrayList<Card>();
    blackMarketPileShuffled = new ArrayList<Card>();

    // Initialize Game Tokens
    embargos = new HashMap<String, Integer>();       // Number of Embargo Tokens per Card Pile
    pileVpTokens = new HashMap<String, Integer>();   // Number of VP Tokens per Card Pile (i.e. Landmarks)
    pileDebtTokens = new HashMap<String, Integer>(); // Number of Debt Tokens (a.k.a. "Tax") per Card Pile
    playerSupplyTokens = new HashMap<String, HashMap<Player, List<PlayerSupplyToken>>>();

    // Initialize Special Card-Specific Game Values
    possessionsToProcess        = 0;     // Needed for Possession (from "Alchemy" Expansion)
    possessingPlayer            = null;  // Needed for Possession (from "Alchemy" Expansion)
    nextPossessionsToProcess    = 0;     // Needed for Possession (from "Alchemy" Expansion)
    nextPossessingPlayer        = null;  // Needed for Possession (from "Alchemy" Expansion)
    tradeRouteValue             = 0;     // Trade Route's Value (from "Prosperity" Expansion)
    baneCard                    = null;  // Young Witch's Bane (from "Cornucopia" Expansion)
    obeliskCard                 = null;  // Obelisk's Action Card (from "Empires" Expansion)
    sheltersInPlay              = false; // Shelters in Game (from "Dark Ages" Expansion)
    bakerInPlay                 = false; // Baker Supply in Game (from "Guilds" Expansion)
    journeyTokenInPlay          = false; // Journey Token in Game (from "Adventures" Expansion)
    firstProvinceWasGained      = false; // Needed for Mountain Pass (from "Empires" Expansion)
    doMountainPassAfterThisTurn = false; // Needed for Mountain Pass (from "Empires" Expansion)
    firstProvinceGainedBy       = -1;    // Needed for Mountain Pass (from "Empires" Expansion)


    // Game Listeners (For Logging Game Data)
    listeners = new ArrayList<GameEventListener>();

    // Initialize Overall Stat Trackers
    overallWins   = new HashMap<String, Double>();
    gameTypeStats = new ArrayList<GameStats>();

  }

  // NOTE: The remainder of this file is organized into 5 Secions, as such:
  //   - SECTION 1: MAIN FUNCTION (entry point to game engine)
  //   - SECTION 2: GAME SETUP & INITIALIZATION FUNCTIONS (Sets up game parameters)
  //   - SECTION 3: GAME'S TURN HANDLING FUNCTIONS (Handles Turn Mechanics)
  //   - SECTION 4: GAME END & STATS CALCULATION FUNCTIONS
  //   - SECTION 5: CARD-SPECIFIC FUNCTIONS (kicks off game engine)


  /***************************************
  ** SECTION 1: MAIN FUNCTION
  ***************************************/

  public static void main(String[] args) {

    // Variables for holding game results
    HashMap<String, Double> gameResults;
    double player1_totalWins = 0.0;
    double player2_totalWins = 0.0;

    // Set up game(s) and Start
    Game game = new Game();

    // Write Time Stamp to Log
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("evaluation_output.txt", true), "utf-8"))) {
       writer.write("-----------" + new java.util.Date() + "-----------\n");
    } catch (Exception e) {
      System.out.println("ERROR:" + e);
    }

    // Players to Play
    String playerName1 = "Jarvis";
    String playerName2 = "Joe";

    for (int i = 0; i < 100; i++) {

      gameResults = game.start(playerName1, playerName2);
      double player1wins = gameResults.get(playerName1);
      double player2wins = gameResults.get(playerName2);

      player1_totalWins += player1wins;
      player2_totalWins += player2wins;

      // Write to Log
      try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("evaluation_output.txt", true), "utf-8"))) {
         writer.write("CUMULATIVE RESULTS:\t" + playerName1 + ":\t" + player1_totalWins + "\t" + playerName2+ ":\t" + player2_totalWins + "\n");
      } catch (Exception e) {
        System.out.println("ERROR:" + e);
      }

    }

    // Print Overall Game Stats
    game.printStats(game.overallWins, game.numGames * GameType.values().length, "Total");

    // Print Overall Game Stats
    game.printGameTypeStats();

    System.out.println("OVERALL WINS.............");
    System.out.println(" --> Player 1: " + player1_totalWins + ", Player 2: " + player2_totalWins);


  }

  /*
  ** start - Starts the Dominion game simulator
  */
  public HashMap<String, Double> start(String playerName1, String playerName2) {

    HashMap<String, Double> playerToWins = new HashMap<>();

    playerToWins.put(playerName1, 0.0);
    playerToWins.put(playerName2, 0.0);

    // Variables for Overall Stats over all Games
    long turnCountTotal = 0;
    long vpTotal        = 0;
    long numCardsTotal  = 0;

    // Play <numGames> Games
    for (int gameCount = 0; gameCount < numGames; gameCount++) {

      Util.debug("---------------------", false);
      Util.debug("New Game: " + gameType);

      // Initialize the Game's Players (yes this is hacky)
      Player player1 = new VDomPlayerJoe();
      if (playerName1 == "Andrew")    { player1 = new VDomPlayerAndrew(); }
      if (playerName1 == "Phil")      { player1 = new VDomPlayerPhil(); }
      if (playerName1 == "Flynn")     { player1 = new VDomPlayerFlynn(); }
      if (playerName1 == "Jarvis")    { player1 = new VDomPlayerJarvis(); }
      if (playerName1 == "Joe Jr")    { player1 = new VDomPlayerJoeJr(); }
      if (playerName1 == "Jarvis Jr") { player1 = new VDomPlayerJarvisJr(); }

      Player player2 = new VDomPlayerJoe();
      if (playerName2 == "Andrew")    { player2 = new VDomPlayerAndrew(); }
      if (playerName2 == "Phil")      { player2 = new VDomPlayerPhil(); }
      if (playerName2 == "Flynn")     { player2 = new VDomPlayerFlynn(); }
      if (playerName2 == "Jarvis")    { player2 = new VDomPlayerJarvis(); }
      if (playerName2 == "Joe Jr")    { player2 = new VDomPlayerJoeJr(); }
      if (playerName2 == "Jarvis Jr") { player2 = new VDomPlayerJarvisJr(); }

      // ((VDomPlayerJarvis)player1).setEvaluator(coinFactor, potionFactor, threeCostGainFactor,
      //                                          fourCostGainFactor, fiveCostGainFactor, coinTokenFactor,
      //                                          debtTokenFactor, victoryTokenFactor, enemyHandSizeFactor,
      //                                          treasureDeltaFactor, actionDeltaFactor, victoryPointFactor,
      //                                          planEvalActionMultiplier, planEvalTreasureMultiplier,
      //                                          planEvalVictoryPointFactor);

      // Initialize the Game (incl. GameEventListeners, Players, and Cards)
      initGameBoard(player1, player2);

      // Set Up Planning Player1 ONLY IF the Player1 has Planning (Screw Player 2, lol).
      if (player1.isPlanningPlayer) {
        DeckPlanner planner = new DeckPlanner(this.cloneGame(), 30);
        player1.idealDeck = planner.findBestDeck(player1);
      }

      // Set up Player's Turn Information
      playersTurn = 0;
      gameTurnCount = 1;
      Util.debug("Turn " + gameTurnCount + " --------------------");
      Queue<ExtraTurnInfo> extraTurnsInfo = new LinkedList<ExtraTurnInfo>();

      // Play Turns until Game Ends
      boolean gameOver = false;
      while (!gameOver) {

        // Create text for New Turn
        Player player = players[playersTurn];
        boolean canBuyCards = extraTurnsInfo.isEmpty() ? true : extraTurnsInfo.remove().canBuyCards;
        MoveContext context = new MoveContext(this, player, canBuyCards);

        // Begin Phase of Turn
        context.phase = TurnPhase.Action;
        context.startOfTurn = true;
        playerBeginTurn(player, context);
        context.startOfTurn = false;

        do {

          // Action Phase of Turn
          context.phase = TurnPhase.Action;
          context.returnToActionPhase = false;
          playerAction(player, context);

          // Buy Phase of Turn
          context.phase = TurnPhase.Buy;
          playerBeginBuy(player, context);
          playTreasures(player, context, -1, null);
          playGuildsTokens(player, context);
          playerBuy(player, context);

        } while (context.returnToActionPhase);

        // Broadcast No-Buy Event
        if (context.totalCardsBoughtThisTurn + context.totalEventsBoughtThisTurn == 0) {
          GameEvent event = new GameEvent(GameEvent.EventType.NoBuy, context);
          broadcastEvent(event);
          Util.debug(player.getPlayerName() + " did not buy a card with coins:" + context.getCoinAvailableForBuy());
        }

        // Discard and Draw New Hand
        context.phase = TurnPhase.CleanUp;
        player.cleanup(context);

        // Clean up other players cards in play without future duration effects, e.g. Duplicate
        for (Player otherPlayer : getPlayersInTurnOrder()) {
          if (otherPlayer != player) {
            otherPlayer.cleanupOutOfTurn(new MoveContext(this, otherPlayer));
          }
        }

        // Update Turn Information
        extraTurnsInfo.addAll(playerEndTurn(player, context));
        gameOver = checkGameOver();

        // Check if Game has ended
        if (!gameOver) {
          playerAfterTurn(player, context);
          if (player.isControlled()) {
            player.stopBeingControlled();
          }
          setPlayersTurn(!extraTurnsInfo.isEmpty());
        }
      }

      // Update Overall Stats over all Games
      turnCountTotal += gameTurnCount;
      int vps[] = gameOver(playerToWins);
      for (int i = 0; i < vps.length; i++) {
        vpTotal += vps[i];
        numCardsTotal += players[i].getAllCards().size();
      }

    }

    // Mark Game Winner and Print Results
    Util.log("");
    Util.log("THE RESULTS: -------------------");
    printStats(playerToWins, numGames, gameType.toString());
    Util.log("--------------------------------");

    // Complete Overall Stats over all Games
    ArrayList<Card> gameCards = new ArrayList<Card>();
    for (CardPile pile : piles.values()) {
      Card card = pile.placeholderCard();
      if (!card.equals(Cards.copper) && !card.equals(Cards.silver) && !card.equals(Cards.gold) && !card.equals(Cards.platinum) &&
          !card.equals(Cards.estate) && !card.equals(Cards.duchy) && !card.equals(Cards.province) && !card.equals(Cards.colony) &&
          !card.equals(Cards.curse)) {
        gameCards.add(card);
      }
    }
    GameStats stats = new GameStats();
    stats.gameType         = gameType;
    stats.cards            = gameCards.toArray(new Card[0]);
    stats.aveTurns         = (int) (turnCountTotal / numGames);
    stats.aveNumCards      = (int) (numCardsTotal / (numGames * numPlayers));
    stats.aveVictoryPoints = (int) (vpTotal / (numGames * numPlayers));

    gameTypeStats.add(stats);

    // Return Player-To-Wins Mapping
    return playerToWins;

  }

  // only call from games cloned by DeckPlanner
  @SuppressWarnings("unchecked")
  public double playPlanningGame(int numTurns, Deck deck, Player planningPlayer) {

    Util.debug("---------------------", false);
    Util.debug("New Planning Game: " + gameType);

    // Initialize Plannings Players
    initPlayersPlanning(2, planningPlayer);

    // Set planningPlayer's deck , draw, and shuffle deck
    Player pPlayer = players[0];
    if (!pPlayer.isPlanningPlayer) { pPlayer = players[1]; }

    pPlayer.idealDeck = deck;
    pPlayer.setDeck(deck);
    pPlayer.shuffleDeck(new MoveContext(this, pPlayer), null);
    while (pPlayer.hand.size() < 5)
      drawToHand(new MoveContext(this, pPlayer), null, 5 - pPlayer.hand.size(), false);

    // Get the Planning Player's Evaluator
    Evaluator evaluator = ((VDomPlayerJarvis)planningPlayer).getEvaluator();

    // Set Dummy's deck and hand with init starting cards to stop errors with cards
    // played by the planning player, but that require dummy's hand/deck to exist
    Player dummy = players[1];
    ArrayList<Card> dummyDeck  = new ArrayList<>();
    for (int i = 0; i < 7; i++) {
      if (i < 3) {
        dummyDeck.add(this.getGamePile(Cards.estate).topCard());
      }
      dummyDeck.add(this.getGamePile(Cards.copper).topCard());
    }
    ((VDomPlayerDummy) dummy).setDeck(dummyDeck);

    dummy.shuffleDeck(new MoveContext(this, dummy), null);
    while (dummy.hand.size() < 5)
      drawToHand(new MoveContext(this, dummy), null, 5 - dummy.hand.size(), false);


    // Set up Player's Turn Information
    playersTurn = 0;
    gameTurnCount = 1;
    Util.debug("Turn " + gameTurnCount + " --------------------");
    Queue<ExtraTurnInfo> extraTurnsInfo = new LinkedList<ExtraTurnInfo>();


    // Play Turns until Game Ends
    boolean gameOver = false;
    int turnsPlayed = 0;
    double turnEconomySummation = 0;
    while (!gameOver && turnsPlayed < numTurns) {

      // Create text for New Turn
      Player player = players[playersTurn];
      boolean canBuyCards = extraTurnsInfo.isEmpty() ? true : extraTurnsInfo.remove().canBuyCards;
      MoveContext context = new MoveContext(this, player, canBuyCards);

      // Begin Phase of Turn
      context.phase = TurnPhase.Action;
      context.startOfTurn = true;
      playerBeginTurn(player, context);
      context.startOfTurn = false;

      do {

        // Action Phase of Turn
        context.phase = TurnPhase.Action;
        context.returnToActionPhase = false;
        playerAction(player, context);

        if (player.getPlayerName().equals(planningPlayer.getPlayerName())) {
          turnEconomySummation += evaluator.evaluateActionPhase(context);
          //turnsPlayed++;
        }

        // Buy Phase of Turn
        context.phase = TurnPhase.Buy;
        playerBeginBuy(player, context);
        playTreasures(player, context, -1, null);
        playGuildsTokens(player, context);
        playerBuy(player, context);

      } while (context.returnToActionPhase);

      if (player.getPlayerName().equals(planningPlayer.getPlayerName())) {
        //turnEconomySummation += evaluator.evaluateActionPhase(context);
        turnsPlayed++;
      }

      // Broadcast No-Buy Event
      if (context.totalCardsBoughtThisTurn + context.totalEventsBoughtThisTurn == 0) {
        GameEvent event = new GameEvent(GameEvent.EventType.NoBuy, context);
        broadcastEvent(event);
        Util.debug(player.getPlayerName() + " did not buy a card with coins:" + context.getCoinAvailableForBuy());
      }

      // Discard and Draw New Hand
      context.phase = TurnPhase.CleanUp;
      player.cleanup(context);

      // Clean up other players cards in play without future duration effects, e.g. Duplicate
      for (Player otherPlayer : getPlayersInTurnOrder()) {
        if (otherPlayer != player) {
          otherPlayer.cleanupOutOfTurn(new MoveContext(this, otherPlayer));
        }
      }


      // Update Turn Information
      extraTurnsInfo.addAll(playerEndTurn(player, context));
      gameOver = checkGameOver();

      // Check if Game has ended
      if (!gameOver) {
        playerAfterTurn(player, context);
        if (player.isControlled()) {
          player.stopBeingControlled();
        }
        setPlayersTurn(!extraTurnsInfo.isEmpty());
      }


    }


    return turnEconomySummation / (double) turnsPlayed;
  }


  /***************************************
  ** SECTION 2: GAME SETUP & INITIALIZATION FUNCTIONS
  ***************************************/


  /*
  ** initGameBoard - Sets up the game's cards, players, and game listeners
  */
  void initGameBoard(Player player1, Player player2) {
    baneCard = null;
    firstProvinceWasGained = false;
    doMountainPassAfterThisTurn = false;
    initGameListener();
    initCards();
    initPlayers(numPlayers, player1, player2);
    initPlayerCards();
  }


  /*
  ** initPlayers - Sets up the Game's players
  */
  @SuppressWarnings("unchecked")
  public void initPlayers(int numPlayers, Player player1, Player player2) {

    players = new Player[numPlayers];
    playersTurn = 0;

    cardsObtainedLastTurn = new ArrayList[numPlayers];
    for (int i = 0; i < numPlayers; i++) {
      cardsObtainedLastTurn[i] = new ArrayList<Card>();
    }

    // Randomize Player Order
    int playSwap = rand.nextInt(numPlayers);

    for (int i = 0; i < numPlayers; i++) {

      if (i == playSwap) {
        players[i] = player1;
      } else {
        players[i] = player2;
      }

      players[i].game = this;
      players[i].playerNumber = i;

      // Interactive player needs this called once for each player on startup so internal counts work properly.
      players[i].getPlayerName();

      MoveContext context = new MoveContext(this, players[i]);
      players[i].newGame(context);
      players[i].initCards();

      context = new MoveContext(this, players[i]);
      String s = cardListText + "\n---------------\n\n";

      if (platColonyPassedIn || chanceForPlatColony > 0.9999) {
        s += "Platinum/Colony included...\n";
      } else if (platColonyNotPassedIn || Math.round(chanceForPlatColony * 100) == 0) {
        s += "Platinum/Colony not included...\n";
      } else {
        s += "Chance for Platinum/Colony\n   " + (Math.round(chanceForPlatColony * 100)) + "% ... " + (isPlatInGame() ? "included\n" : "not included\n");
      }

      if (baneCard != null) {
        s += "Bane card: " + baneCard.getName() + "\n";
      }

      // When Baker is included in the game, each Player starts with 1 coin token
      if (bakerInPlay) {
        players[i].gainGuildsCoinTokens(1);
      }

      /* The journey token is face up at the start of a game.
      ** It can be turned over by Ranger, Giant and Pilgrimage. */
      if (journeyTokenInPlay) {
        players[i].flipJourneyToken(null);
      }

      if (sheltersPassedIn || chanceForShelters > 0.9999) {
        s += "Shelters included...\n";
      } else if (sheltersNotPassedIn || Math.round(chanceForShelters * 100) == 0) {
        s += "Shelters not included...\n";
      } else {
        s += "Chance for Shelters\n   " + (Math.round(chanceForShelters * 100)) + "% ... " + (sheltersInPlay ? "included\n" : "not included\n");
      }

      s += unfoundCardText;
      context.message = s;
      broadcastEvent(new GameEvent(GameEvent.EventType.GameStarting, context));

    }
  }


  @SuppressWarnings("unchecked")
  public void initPlayersPlanning(int numPlayers, Player player) {

    players = new Player[numPlayers];
    playersTurn = 0;

    cardsObtainedLastTurn = new ArrayList[numPlayers];
    for (int i = 0; i < numPlayers; i++) {
      cardsObtainedLastTurn[i] = new ArrayList<Card>();
    }

    for (int i = 0; i < numPlayers; i++) {

      if (i == 0) {
        players[i] = player;
      }
      else {
        players[i] = new VDomPlayerDummy();
      }

      players[i].game = this;
      players[i].playerNumber = i;

      // Interactive player needs this called once for each player on startup so internal counts work properly.
      players[i].getPlayerName();

      MoveContext context = new MoveContext(this, players[i]);
      players[i].newGame(context);
      players[i].initCards();

      context = new MoveContext(this, players[i]);
      String s = cardListText + "\n---------------\n\n";

      if (platColonyPassedIn || chanceForPlatColony > 0.9999) {
        s += "Platinum/Colony included...\n";
      } else if (platColonyNotPassedIn || Math.round(chanceForPlatColony * 100) == 0) {
        s += "Platinum/Colony not included...\n";
      } else {
        s += "Chance for Platinum/Colony\n   " + (Math.round(chanceForPlatColony * 100)) + "% ... " + (isPlatInGame() ? "included\n" : "not included\n");
      }

      if (baneCard != null) {
        s += "Bane card: " + baneCard.getName() + "\n";
      }

      // When Baker is included in the game, each Player starts with 1 coin token
      if (bakerInPlay) {
        players[i].gainGuildsCoinTokens(1);
      }

      /* The journey token is face up at the start of a game.
      ** It can be turned over by Ranger, Giant and Pilgrimage. */
      if (journeyTokenInPlay) {
        players[i].flipJourneyToken(null);
      }

      if (sheltersPassedIn || chanceForShelters > 0.9999) {
        s += "Shelters included...\n";
      } else if (sheltersNotPassedIn || Math.round(chanceForShelters * 100) == 0) {
        s += "Shelters not included...\n";
      } else {
        s += "Chance for Shelters\n   " + (Math.round(chanceForShelters * 100)) + "% ... " + (sheltersInPlay ? "included\n" : "not included\n");
      }

      s += unfoundCardText;
      context.message = s;
      broadcastEvent(new GameEvent(GameEvent.EventType.GameStarting, context));

    }
  }

  /*
  ** initPlayerCards - Sets up the Players' Starting Decks
  */
  public void initPlayerCards() {
    Player player;
    for (int i = 0; i < numPlayers; i++) {

      player = players[i];
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);
      player.discard(takeFromPile(Cards.copper), null, null);

      if (sheltersInPlay) {
        player.discard(takeFromPile(Cards.necropolis), null, null);
        player.discard(takeFromPile(Cards.overgrownEstate), null, null);
        player.discard(takeFromPile(Cards.hovel), null, null);
        // Also need to remove the Estates that were put in the pile prior to
        // determining if Shelters would be used
        takeFromPile(Cards.estate);
        takeFromPile(Cards.estate);
        takeFromPile(Cards.estate);
      } else {
        player.discard(takeFromPile(Cards.estate), null, null);
        player.discard(takeFromPile(Cards.estate), null, null);
        player.discard(takeFromPile(Cards.estate), null, null);
      }

      if (!equalStartHands || i == 0) {
        while (player.hand.size() < 5)
        drawToHand(new MoveContext(this, player), null, 5 - player.hand.size(), false);
      } else {
        // make subsequent player hands equal
        for (int j = 0; j < 5; j++) {
          Card card = players[0].hand.get(j);
          player.discard.remove(card);
          player.hand.add(card);
        }
        player.replenishDeck(null, null, 0);
      }
    }

    // Add tradeRoute tokens if tradeRoute in play
    tradeRouteValue = 0;
    if (cardInGame(Cards.tradeRoute)) {
      for (CardPile pile : piles.values()) {
        if ((pile.placeholderCard().is(Type.Victory)) && pile.isSupply()) {
          pile.setTradeRouteToken();
        }
      }
    }

  }

  /*
  ** initCards - Sets up the various card piles needed for a Game
  */
  public void initCards() {

    piles.clear();
    embargos.clear();
    pileVpTokens.clear();
    playerSupplyTokens.clear();
    trashPile.clear();
    blackMarketPile.clear();
    blackMarketPileShuffled.clear();

    platColonyNotPassedIn = false;
    platColonyPassedIn    = false;
    sheltersNotPassedIn   = false;
    sheltersPassedIn      = false;

    int provincePileSize  = -1;
    int curseCount        = -1;
    int treasureMultiplier = 1;

    switch (numPlayers) {
      case 1:
      case 2:
        curseCount = 10;
        provincePileSize = 8;
        victoryCardPileSize = 8;
      break;
      case 3:
        curseCount = 20;
        provincePileSize = 12;
        victoryCardPileSize = 12;
      break;
      case 4:
        curseCount = 30;
        provincePileSize = 12;
        victoryCardPileSize = 12;
      break;
      case 5:
        curseCount = 40;
        provincePileSize = 15;
        victoryCardPileSize = 12;
        treasureMultiplier = 2;
      break;
      case 6:
        curseCount = 50;
        provincePileSize = 18;
        victoryCardPileSize = 12;
        treasureMultiplier = 2;
      break;
    }

    // Create Treasure Piles
    addPile(Cards.gold,   30 * treasureMultiplier);
    addPile(Cards.silver, 40 * treasureMultiplier);
    addPile(Cards.copper, 60 * treasureMultiplier);

    // Create Victory Card Piles
    addPile(Cards.curse,    curseCount);
    addPile(Cards.province, provincePileSize);
    addPile(Cards.duchy,    victoryCardPileSize);
    addPile(Cards.estate,   victoryCardPileSize + (3 * numPlayers));

    unfoundCards.clear();
    int added = 0;

    // Handles Special Cards Specified at Launch
    if (cardsSpecifiedAtLaunch != null) {

      platColonyNotPassedIn = true;
      sheltersNotPassedIn = true;

      for (String cardName : cardsSpecifiedAtLaunch) {

        Card card = null;
        boolean bane = false;
        boolean obelisk = false;
        boolean blackMarket = false;

        if (cardName.startsWith(BANE)) {
          bane = true;
          cardName = cardName.substring(BANE.length());
        }
        if (cardName.startsWith(OBELISK)) {
          obelisk = true;
          cardName = cardName.substring(OBELISK.length());
        }
        if (cardName.startsWith(BLACKMARKET)) {
          blackMarket = true;
          cardName = cardName.substring(BLACKMARKET.length());
        }
        String s = cardName.replace("/", "").replace(" ", "");
        for (Card c : Cards.actionCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        for (Card c : Cards.eventsCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        for (Card c : Cards.landmarkCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        // Handle split pile / knights cards being passed in incorrectly
        for (Card c : Cards.variablePileCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        for (Card c : Cards.castleCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        for (Card c : Cards.knightsCards) {
          if (c.getSafeName().equalsIgnoreCase(s)) {
            card = c;
            break;
          }
        }
        if (card != null && Cards.variablePileCardToRandomizer.containsKey(card)) {
          card = Cards.variablePileCardToRandomizer.get(card);
        }

        if (card != null && bane) {
          baneCard = card;
        }
        if (card != null && obelisk) {
          obeliskCard = card;
        }
        if (card != null && blackMarket) {
          blackMarketPile.add(card);
        }
        if (cardName.equalsIgnoreCase("Knights")) {
          card = Cards.virtualKnight;
        }

        if (card != null && !piles.containsKey(card.getName())) {
          addPile(card);
          added += 1;
        } else if (s.equalsIgnoreCase(Cards.curse.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.estate.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.duchy.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.province.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.copper.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.silver.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.potion.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.gold.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.diadem.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.followers.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.princess.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.trustySteed.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.bagOfGold.getSafeName())) {
          // do nothing
        } else if (s.equalsIgnoreCase(Cards.platinum.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.colony.getSafeName())) {
          platColonyPassedIn = true;
        } else if (s.equalsIgnoreCase("Shelter") ||
                   s.equalsIgnoreCase("Shelters") ||
                   s.equalsIgnoreCase(Cards.hovel.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.overgrownEstate.getSafeName()) ||
                   s.equalsIgnoreCase(Cards.necropolis.getSafeName())) {
          sheltersPassedIn = true;
        } else {
          unfoundCards.add(s);
          Util.debug("ERROR::Could not find card:" + s);
        }
      }

      for (String s : unfoundCards) {
        if (added >= 10)
        break;
        Card c = null;
        int replacementCost = -1;

        if (replacementCost != -1) {
          ArrayList<Card> cardsWithSameCost = new ArrayList<Card>();
          for (Card card : Cards.actionCards) {
            if (card.getCost(null) == replacementCost && !cardInGame(card)) {
              cardsWithSameCost.add(card);
            }
          }

          if (cardsWithSameCost.size() > 0) {
            c = cardsWithSameCost.get(rand.nextInt(cardsWithSameCost.size()));
          }
        }

        while (c == null) {
          c = Cards.actionCards.get(rand.nextInt(Cards.actionCards.size()));
          if (cardInGame(c)) {
            c = null;
          }
        }

        Util.debug("Adding replacement for " + s + ": " + c);
        addPile(c);
        added += 1;
      }

      gameType = GameType.Specified;

    } else {

      CardSet cardSet = CardSet.getCardSet(gameType, -1, randomExpansions, randomExcludedExpansions, randomIncludesEvents, numRandomEvents, randomIncludesLandmarks, numRandomLandmarks, !splitMaxEventsAndLandmarks, true);

      if (cardSet == null) {
        cardSet = CardSet.getCardSet(CardSet.defaultGameType, -1);
      }

      for (Card card : cardSet.getCards()) {
        this.addPile(card);
      }

      if (cardSet.getBaneCard() != null) {
        this.baneCard = cardSet.getBaneCard();
        //Adding the bane card could probably be done in the CardSet class, but it seems better to call it out explicitly.
        this.addPile(this.baneCard);
      }
    }

    // Black Market
    Cards.blackMarketCards.clear();
    if (piles.containsKey(Cards.blackMarket.getName())) {
      List<Card> allCards;

      // get 10 cards more then needed. Extract the cards in supply
      int count = Math.max(blackMarketCount - blackMarketPile.size(), 0);
      if (blackMarketOnlyCardsFromUsedExpansions) {
        List<Expansion> expansions = new ArrayList<Expansion>();
        if (randomExpansions != null && randomExpansions.size() > 0) {
          expansions.addAll(randomExpansions);
        } else {
          for (CardPile pile : placeholderPiles.values()) {
            if (pile != null &&
            pile.placeholderCard() != null &&
            pile.placeholderCard().getExpansion() != null &&
            Cards.isKingdomCard(pile.placeholderCard()) &&
            !expansions.contains(pile.placeholderCard().getExpansion()) &&
            pile.placeholderCard().getExpansion() != Expansion.Promo) {
              expansions.add(pile.placeholderCard().getExpansion());
            }
          }
        }
        allCards = CardSet.getCardSet(GameType.Random, count + 10, expansions, randomExcludedExpansions, false, 0, false, 0, false, false).getCards();
      } else {
        allCards = CardSet.getCardSet(GameType.Random, count + 10).getCards();
      }

      List<Card> remainingCards = new ArrayList<Card>();
      for (int i = 0; i < allCards.size(); i++) {
        if (!piles.containsKey(allCards.get(i).getName())) {
          CardPile tempPile = allCards.get(i).getPileCreator().create(allCards.get(i), 12); //count doesn't matter as we only need templates
          ArrayList<Card> templates = tempPile.getTemplateCards();
          if (Cards.variablePileCards.contains(templates.get(0)) || Cards.castleCards.contains(templates.get(0))) {
            if (blackMarketSplitPileOptions == BlackMarketSplitPileOptions.ANY) {
              for (Card card : templates) {
                remainingCards.add(card);
              }
            } else if (blackMarketSplitPileOptions == BlackMarketSplitPileOptions.ONE) {
              remainingCards.add(Util.randomCard(templates));
            } else if (blackMarketSplitPileOptions == BlackMarketSplitPileOptions.ALL) {
              remainingCards.add(allCards.get(i));
            }
          } else {
            remainingCards.add(Util.randomCard(templates));
          }
        }
      }

      // Take count cards from the rest
      List<Card> cards = CardSet.getRandomCardSet(remainingCards, count).getCards();

      //Force remaining split pile cards into black market deck if one from a split pile is in
      if (blackMarketSplitPileOptions == BlackMarketSplitPileOptions.ALL) {
        ArrayList<Card> extraCards = new ArrayList<Card>();
        for (int i = 0; i < cards.size(); ++i) {
          Card c = cards.get(i);
          ArrayList<Card> templates = c.getPileCreator().create(allCards.get(i), 12).getTemplateCards(); //count doesn't matter as we only need templates
          if (templates.size() > 1) {
            cards.set(i, templates.get(0));
            for (int j = 1; j < templates.size(); ++j) {
              extraCards.add(templates.get(j));
            }
          }
        }
        int cardsToRemove = extraCards.size() - (count - cards.size());
        for (int n = 0; n < cardsToRemove; ++n) {
          for (int i = 0; i < cards.size(); ++i) {
            if (!Cards.variablePileCards.contains(cards.get(i)) && !Cards.castleCards.contains(cards.get(i))) {
              cards.remove(i);
              break;
            }
          }
        }
        for (Card c : extraCards) {
          if (cards.size() < count)
          cards.add(c);
        }
      }

      for (int i = 0; i < cards.size(); i++) {
        remainingCards.remove(cards.get(i));
        blackMarketPile.add(cards.get(i).instantiate());
      }

      if (this.baneCard == null && blackMarketPile.contains(Cards.youngWitch)) {
        this.baneCard = CardSet.getBaneCard(remainingCards);
        if (this.baneCard != null) {
          this.addPile(this.baneCard);
        }
      }
      // sort
      Collections.sort(blackMarketPile, new Util.CardCostNameComparator());
      // put all in piles
      cards.clear();
      for (int i = 0; i < blackMarketPile.size(); i++) {
        cards.add(blackMarketPile.get(i));
        addPile(blackMarketPile.get(i).getTemplateCard(), 1, false, true);
        Cards.blackMarketCards.add(blackMarketPile.get(i));
      }
      // shuffle
      while (cards.size() > 0) {
        blackMarketPileShuffled.add(cards.remove(this.rand.nextInt(cards.size())));
      }
    }

    if (obeliskCard != null && !piles.containsKey(Cards.obelisk.getName())) {
      addPile(Cards.obelisk);
    }

    //determine shelters & plat/colony use
    boolean alreadyCountedKnights = false;
    int darkAgesCards = 0;
    int prosperityCards = 0;
    int kingdomCards = 0;
    for (CardPile pile : placeholderPiles.values()) {
      if (pile != null &&
      pile.placeholderCard() != null &&
      pile.placeholderCard().getExpansion() != null &&
      Cards.isKingdomCard(pile.placeholderCard())) {
        kingdomCards++;
        if (pile.placeholderCard.getExpansion() == Expansion.DarkAges) {
          darkAgesCards++;
        }
        if (pile.placeholderCard().getExpansion() == Expansion.Prosperity) {
          prosperityCards++;
        }
      }
    }

    sheltersInPlay = false;
    if (sheltersPassedIn) {
      sheltersInPlay = true;
      chanceForShelters = 1;
    } else if (!(sheltersNotPassedIn && cardsSpecifiedAtLaunch != null)) {
      if (chanceForShelters > -0.0001) {
        sheltersInPlay = rand.nextDouble() < chanceForShelters;
      } else {
        chanceForShelters = darkAgesCards / (double) kingdomCards;

        if (rand.nextDouble() < chanceForShelters) {
          sheltersInPlay = true;
        }
      }
    }

    if (sheltersInPlay) {
      addPile(Cards.necropolis, numPlayers, false);
      addPile(Cards.overgrownEstate, numPlayers, false);
      addPile(Cards.hovel, numPlayers, false);
    }

    // Check for PlatColony
    boolean addPlatColony = false;
    if (platColonyPassedIn) {
      addPlatColony = true;
    } else if (!(platColonyNotPassedIn && cardsSpecifiedAtLaunch != null)) {
      if (chanceForPlatColony > -0.0001) {
        addPlatColony = rand.nextDouble() < chanceForPlatColony;
      } else {
        chanceForPlatColony = prosperityCards / (double) kingdomCards;

        if (rand.nextDouble() < chanceForPlatColony) {
          addPlatColony = true;
        }
      }
    }

    if (addPlatColony) {
      addPile(Cards.platinum, 12);
      addPile(Cards.colony);
    }

    // Add the potion if there are any cards that need them.
    outerloop:
    for (CardPile pile : piles.values()) {
      for (Card cardInPile : pile.getTemplateCards()) {
        if (cardInPile.costPotion()) {
          addPile(Cards.potion, 16);
          break outerloop;
        }
      }
    }

    boolean looter = false;
    for (CardPile pile : piles.values()) {
      for (Card cardInPile : pile.getTemplateCards()) {
        if (cardInPile.is(Type.Looter, null)) {
          looter = true;
        }
      }
    }
    if (looter) {
      CardPile rp = (CardPile) this.addPile(Cards.virtualRuins, Math.max(10, (numPlayers * 10) - 10));
    }


    if (piles.containsKey(Cards.tournament.getName()) && !piles.containsKey(Cards.bagOfGold.getName())) {
      addPile(Cards.bagOfGold, 1, false);
      addPile(Cards.diadem, 1, false);
      addPile(Cards.followers, 1, false);
      addPile(Cards.princess, 1, false);
      addPile(Cards.trustySteed, 1, false);
    }

    // If Bandit Camp, Pillage, or Marauder is in play, we'll need Spoils (non-supply)
    if (piles.containsKey(Cards.banditCamp.getName()) ||
    piles.containsKey(Cards.pillage.getName()) ||
    piles.containsKey(Cards.marauder.getName())) {
      addPile(Cards.spoils, 15, false);
    }

    // If Urchin is in play, we'll need Mercenary (non-supply)
    if (piles.containsKey(Cards.urchin.getName())) {
      addPile(Cards.mercenary, 10, false);
    }

    // If Hermit is in play, we'll need Madman (non-supply)
    if (piles.containsKey(Cards.hermit.getName())) {
      addPile(Cards.madman, 10, false);
    }

    // If Page is in play, we'll need treasureHunter, warrior, hero, champion (non-supply)
    if (piles.containsKey(Cards.page.getName())) {
      addPile(Cards.treasureHunter, 5, false);
      addPile(Cards.warrior, 5, false);
      addPile(Cards.hero, 5, false);
      addPile(Cards.champion, 5, false);
    }

    // If Peasant is in play, we'll need soldier, fugitive, disciple, teacher (non-supply)
    if (piles.containsKey(Cards.peasant.getName())) {
      addPile(Cards.soldier, 5, false);
      addPile(Cards.fugitive, 5, false);
      addPile(Cards.disciple, 5, false);
      addPile(Cards.teacher, 5, false);
    }

    // If Baker is in play, each player starts with one coin token
    if (piles.containsKey(Cards.baker.getName())) {
      bakerInPlay = true;
    }

    // Setup for Landmarks starting with tokens
    Card[] landmarksWithTokens = {Cards.arena, Cards.basilica, Cards.battlefield, Cards.baths, Cards.colonnade, Cards.labyrinth};
    for (Card c : landmarksWithTokens) {
      if (piles.containsKey(c.getName())) {
        addPileVpTokens(c, 6 * numPlayers, null);
      }
    }

    // Setup for Aqueduct
    if (piles.containsKey(Cards.aqueduct.getName())) {
      addPileVpTokens(Cards.silver, 8, null);
      addPileVpTokens(Cards.gold, 8, null);
    }

    // Setup for Defiled Shrine
    if (piles.containsKey(Cards.defiledShrine.getName())) {
      for (CardPile pile : placeholderPiles.values()) {
        Card c = pile.placeholderCard();
        if (pile.isSupply() && c.is(Type.Action) && !c.is(Type.Gathering)) {
          addPileVpTokens(c, 2, null);
        }
      }
    }

    // Setup for Obelisk
    if (piles.containsKey(Cards.obelisk.getName())) {
      if (obeliskCard == null) {
        ArrayList<Card> validObeliskCards = new ArrayList<Card>();
        for (String p : placeholderPiles.keySet()) {
          CardPile pile = placeholderPiles.get(p);
          Card placeholder = pile.placeholderCard();
          if (pile.isSupply() && placeholder.is(Type.Action) && !validObeliskCards.contains(placeholder)) {
            validObeliskCards.add(placeholder);
          }
        }
        if (validObeliskCards.size() > 0) {
          obeliskCard = validObeliskCards.get(rand.nextInt(validObeliskCards.size()));
        }
      }
    }

    // Setup for Tax
    if (piles.containsKey(Cards.tax.getName())) {
      for (String cardName : placeholderPiles.keySet()) {
        Card c = piles.get(cardName).placeholderCard();
        if (Cards.isSupplyCard(c)) {
          addPileDebtTokens(c, 1, null);
        }
      }
    }

    // If Ranger, Giant or Pilgrimage are in play, each player starts with a journey token faced up
    if (piles.containsKey(Cards.ranger.getName()) ||
        piles.containsKey(Cards.giant.getName()) ||
        piles.containsKey(Cards.pilgrimage.getName())) {
      journeyTokenInPlay = true;
    } else {
      journeyTokenInPlay = false;
    }

    Util.debug("");
    Util.debug("Cards in Play", true);
    Util.debug("---------------", true);
    cardListText += "Cards in play\n---------------\n";

    ArrayList<Card> cards = new ArrayList<Card>();
    ArrayList<Card> events = new ArrayList<Card>();
    ArrayList<Card> landmarks = new ArrayList<Card>();
    for (CardPile pile : placeholderPiles.values()) {
      Card c = pile.placeholderCard();
      if (Cards.isKingdomCard(c)) {
        cards.add(c);
      } else if (Cards.eventsCards.contains(c)) {
        events.add(c);
      } else if (Cards.landmarkCards.contains(c)) {
        landmarks.add(c);
      }
    }
    Collections.sort(cards, new Util.CardCostNameComparator());
    Collections.sort(events, new Util.CardCostNameComparator());
    Collections.sort(landmarks, new Util.CardCostNameComparator());

    for (Card c : cards) {
      cardListText += Util.getShortText(c) + ((baneCard != null && c.equals(baneCard)) ? " (Bane)" + baneCard.getName() : "") + "\n";
    }
    if (!events.isEmpty()) {
      cardListText += "\nEvents in play\n---------------\n";
      for (Card c : events) {
        cardListText += Util.getShortText(c) + "\n";
      }
    }
    if (!landmarks.isEmpty()) {
      cardListText += "\nLandmarks in play\n---------------\n";
      for (Card c : landmarks) {
        cardListText += c.getName() + (c.equals(Cards.obelisk) && obeliskCard != null ? " (" + obeliskCard.getName() + ")" : "") + "\n";
      }
    }

    for (Entry<String, CardPile> cEntry : piles.entrySet()) {
      if (cEntry.getKey().equals(cEntry.getValue().placeholderCard().getName())) {
        Util.debug(cEntry.getKey() + ": " + cEntry.getValue().cards.toString());
      } else {

      }
    }

    if (unfoundCards != null && unfoundCards.size() > 0) {
      unfoundCardText += "\n";
      String cardList = "";
      boolean first = true;
      for (String s : unfoundCards) {
        if (first) {
          first = false;
        } else {
          cardList += "\n";
        }
        cardList += s;
      }
      cardList += "\n\n";
      unfoundCardText += "The following cards are not \navailable, so replacements \nhave been used:\n" + cardList;
    }
  }

  /*
  ** initGameListener - Defines and instantiates a new GameEventListner,
  ** which is used for logging GameEvents during a single game.
  */
  public void initGameListener() {

    listeners.clear();

    gameListener = new GameEventListener() {

      // Main gameEvent Function of the Listener Interface
      public void gameEvent(GameEvent event) {

        // If Game Start or End, then just return
        if (event.getType() == GameEvent.EventType.GameStarting ||
            event.getType() == GameEvent.EventType.GameOver) {
          return;
        }

        // Otherwise, if GameEvent type is 'CardObtained' or 'BuyingCard'...
        if ((event.getType() == GameEvent.EventType.CardObtained ||
             event.getType() == GameEvent.EventType.BuyingCard) &&
            !event.card.is(Type.Event, null)) {

          MoveContext context = event.getContext();
          Player player = context.getPlayer();

          // Handle Possessed Player
          if (player.isPossessed()) {
            possessedBoughtPile.add(event.card);
            MoveContext controlContext = new MoveContext(context.game, context.getPlayer().controlPlayer);
            controlContext.getPlayer().gainCardAlreadyInPlay(event.card, Cards.possession, controlContext);
            return;
          }

          // Start inheriting newly gained estate
          if (event.card.equals(Cards.estate) && event.player.getInheritance() != null) {
            ((CardImpl) event.card).startInheritingCardAbilities(player.getInheritance().getTemplateCard().instantiate());
          }

          // Handle Victory Points gained per this Turn
          if (context != null && event.card.is(Type.Victory)) {
            context.vpsGainedThisTurn += event.card.getVictoryPoints();
          }

          // See rules explanation of Tunnel for what commandedDiscard means.
          boolean commandedDiscard = true;
          if (event.getType() == GameEvent.EventType.BuyingCard ||
              event.getType() == GameEvent.EventType.CardObtained) {
            commandedDiscard = false;
          } else if (event.responsible != null) {
            Card r = event.responsible;
            if (r.equals(Cards.estate) && player.getInheritance() != null) {
              r = player.getInheritance();
            }
            if (r.equals(Cards.borderVillage) ||
                r.equals(Cards.feast) ||
                r.equals(Cards.remodel) ||
                r.equals(Cards.swindler) ||
                r.equals(Cards.ironworks) ||
                r.equals(Cards.saboteur) ||
                r.equals(Cards.upgrade) ||
                r.equals(Cards.ambassador) ||
                r.equals(Cards.smugglers) ||
                r.equals(Cards.talisman) ||
                r.equals(Cards.expand) ||
                r.equals(Cards.forge) ||
                r.equals(Cards.remake) ||
                r.equals(Cards.hornOfPlenty) ||
                r.equals(Cards.jester) ||
                r.equals(Cards.develop) ||
                r.equals(Cards.haggler) ||
                r.equals(Cards.workshop) ||
                r.equals(Cards.hermit) ||
                r.equals(Cards.dameNatalie)) {
              commandedDiscard = false;
            }
          }
          boolean handled = false;

          // Not sure if this is exactly right for the Trader, but it seems to be based
          // on detailed card explanation in the rules.  The handling for new cards is
          // done before taking the card from the pile in a different method below.

          if (!event.newCard) {
            boolean hasInheritedTrader = Cards.trader.equals(context.getPlayer().getInheritance()) && context.getPlayer().hand.contains(Cards.estate);
            boolean hasTrader = context.getPlayer().hand.contains(Cards.trader);
            Card traderCard = hasTrader ? Cards.trader : Cards.estate;
            if (hasTrader || hasInheritedTrader) {
              if (player.controlPlayer.trader_shouldGainSilverInstead((MoveContext) context, event.card)) {
                player.reveal(traderCard, null, context);
                player.trash(event.card, Cards.trader, (MoveContext) context);
                event.card = Cards.silver;
                player.gainNewCard(Cards.silver, Cards.trader, context);
                return;
              }
            }
          }

          if (event.getPlayer() == players[playersTurn]) {
            cardsObtainedLastTurn[playersTurn].add(event.card);
          }

          if (cardsObtainedLastTurn[playersTurn].size() == 2) {
            if (cardInGame(Cards.labyrinth)) {
              int tokensLeft = getPileVpTokens(Cards.labyrinth);
              if (tokensLeft > 0) {
                int tokensToTake = Math.min(tokensLeft, 2);
                removePileVpTokens(Cards.labyrinth, tokensToTake, context);
                player.addVictoryTokens(context, tokensToTake, Cards.labyrinth);
              }
            }
          }

          boolean hasInheritedWatchtower = Cards.watchTower.equals(player.getInheritance()) && player.hand.contains(Cards.estate);
          boolean hasWatchtower = player.hand.contains(Cards.watchTower);
          Card watchTowerCard = hasWatchtower ? Cards.watchTower : Cards.estate;
          if (hasWatchtower || hasInheritedWatchtower) {
            WatchTowerOption choice = context.player.controlPlayer.watchTower_chooseOption((MoveContext) context, event.card);

            if (choice == WatchTowerOption.TopOfDeck) {
              handled = true;
              GameEvent watchTowerEvent = new GameEvent(GameEvent.EventType.CardRevealed, context);
              watchTowerEvent.card = watchTowerCard;
              watchTowerEvent.responsible = null;
              context.game.broadcastEvent(watchTowerEvent);

              player.putOnTopOfDeck(event.card, context, true);
            } else if (choice == WatchTowerOption.Trash) {
              handled = true;
              GameEvent watchTowerEvent = new GameEvent(GameEvent.EventType.CardRevealed, context);
              watchTowerEvent.card = watchTowerCard;
              watchTowerEvent.responsible = null;
              context.game.broadcastEvent(watchTowerEvent);

              player.trash(event.card, Cards.watchTower, context);
            }
          }

          Card gainedCardAbility = event.card;
          if (gainedCardAbility.equals(Cards.estate) && player.getInheritance() != null) {
            gainedCardAbility = player.getInheritance();
          }

          if (!handled) {
            if (context.isRoyalSealInPlay() && context.player.controlPlayer.royalSealTravellingFair_shouldPutCardOnDeck((MoveContext) context, Cards.royalSeal, event.card)) {
              player.putOnTopOfDeck(event.card, context, true);
            } else if (context.travellingFairBought && context.player.controlPlayer.royalSealTravellingFair_shouldPutCardOnDeck((MoveContext) context, Cards.travellingFair, event.card)) {
              player.putOnTopOfDeck(event.card, context, true);
            } else if (event.responsible != null && event.responsible.equals(Cards.summon)
            && (!event.card.equals(Cards.inn))
            && (!event.card.equals(Cards.borderVillage) || (event.card.equals(Cards.borderVillage) && Cards.borderVillage.getCost(context) == 0))
            && (!event.card.equals(Cards.deathCart) || (event.card.equals(Cards.deathCart) && context.game.isPileEmpty(Cards.virtualRuins)))
            ) {
              //TODO: figure out better way to handle not Summoning Death Cart or Border Village (or other cards) due to lose track rule
              //      may have missed some esoteric cases here (e.g. Inn's when-gain ability doesn't have to have Summon lose track)
              context.player.summon.add(event.card);
              GameEvent summonEvent = new GameEvent(GameEvent.EventType.CardSetAsideSummon, context);
              summonEvent.card = event.card;
              context.game.broadcastEvent(summonEvent);
            } else if (gainedCardAbility.equals(Cards.nomadCamp)) {
              player.putOnTopOfDeck(event.card, context, true);
            } else if (gainedCardAbility.equals(Cards.villa)) {
              player.hand.add(event.card);
              if (context.game.getCurrentPlayer() == player) {
                context.actions += 1;
                if (context.phase == TurnPhase.Buy) {
                  context.returnToActionPhase = true;
                }
              }
            } else if (event.responsible != null) {
              Card r = event.responsible;
              if (r.equals(Cards.estate) && player.getInheritance() != null) {
                r = player.getInheritance();
              }

              r = r.behaveAsCard(); //Get impersonated card

              if (r.equals(Cards.armory) ||
                  r.equals(Cards.artificer) ||
                  r.equals(Cards.bagOfGold) ||
                  r.equals(Cards.bureaucrat) ||
                  r.equals(Cards.develop) ||
                  r.equals(Cards.foolsGold) ||
                  (r.equals(Cards.graverobber) && context.graverobberGainedCardOnTop == true) ||
                  r.equals(Cards.seaHag) ||
                  r.equals(Cards.taxman) ||
                  r.equals(Cards.tournament) ||
                  r.equals(Cards.treasureMap) ||
                  r.equals(Cards.replace) && (context.attackedPlayer != player && (gainedCardAbility.is(Type.Action) || gainedCardAbility.is(Type.Treasure)))) {
                player.putOnTopOfDeck(event.card, context, true);
              } else if (r.equals(Cards.beggar)) {
                if (event.card.equals(Cards.copper)) {
                  player.hand.add(event.card);
                } else if (event.card.equals(Cards.silver) && context.beggarSilverIsOnTop++ == 0) {
                  player.putOnTopOfDeck(event.card, context, true);
                } else if (event.card.equals(Cards.silver)) {
                  player.discard.add(event.card);
                }
              } else if (r.equals(Cards.tradingPost) || r.equals(Cards.mine) || r.equals(Cards.explorer) || r.equals(Cards.torturer) || r.equals(Cards.transmogrify) || r.equals(Cards.artisan)) {
                player.hand.add(event.card);
              } else if (r.equals(Cards.illGottenGains) && event.card.equals(Cards.copper)) {
                player.hand.add(event.card);
              } else if (r.equals(Cards.rocks)) {
                if (context.phase == TurnPhase.Buy && context.game.getCurrentPlayer() == player) {
                  player.putOnTopOfDeck(event.card, context, true);
                } else {
                  player.hand.add(event.card);
                }
              } else {
                player.discard(event.card, null, null, commandedDiscard, false);
              }
            } else {
              player.discard(event.card, null, null, commandedDiscard, false);
            }
          }

          // check for when-gain callable cards
          // NOTE: Technically this should be done in a loop, as you can call multiple cards for one when-gain.
          //   However, since the only card here, Duplicate, will trigger another on-gain anyway
          //   we don't need to.
          ArrayList<Card> callableCards = new ArrayList<Card>();
          for (Card c : player.tavern) {
            if (c.behaveAsCard().isCallableWhenCardGained()) {
              int callCost = c.behaveAsCard().getCallableWhenGainedMaxCost();
              if (callCost == -1 || (event.card.getCost(context) <= callCost && event.card.getDebtCost(context) == 0 && !event.card.costPotion())) {
                callableCards.add(c);
              }
            }
          }
          if (!callableCards.isEmpty()) {
            //ask player which card to call
            Collections.sort(callableCards, new Util.CardCostComparator());
            // we want null entry at the end for None
            Card[] cardsAsArray = callableCards.toArray(new Card[callableCards.size() + 1]);
            Card toCall = player.controlPlayer.call_whenGainCardToCall(context, event.card, cardsAsArray);
            if (toCall != null || callableCards.contains(toCall)) {
              toCall.behaveAsCard().callWhenCardGained(context, event.card);
            }
          }

          if (event.card.equals(Cards.province) && !firstProvinceWasGained) {
            doMountainPassAfterThisTurn = true;
            firstProvinceWasGained = true;
            firstProvinceGainedBy = playersTurn;
          }

          if (event.card.is(Type.Treasure, player)) {
            if (cardInGame(Cards.aqueduct)) {
              //TODO?: you can technically choose the order of resolution for moving the VP
              //       tokens from the treasure after taking the tokens, but why would you ever do this?
              int tokensLeft = getPileVpTokens(event.card);
              if (tokensLeft > 0) {
                removePileVpTokens(event.card, 1, context);
                addPileVpTokens(Cards.aqueduct, 1, context);
              }
            }
          }
          if (event.card.is(Type.Victory, player)) {
            if (cardInGame(Cards.battlefield)) {
              int tokensLeft = getPileVpTokens(Cards.battlefield);
              if (tokensLeft > 0) {
                int tokensToTake = Math.min(tokensLeft, 2);
                removePileVpTokens(Cards.battlefield, tokensToTake, context);
                player.addVictoryTokens(context, tokensToTake, Cards.battlefield);
              }
            }
            if (cardInGame(Cards.aqueduct)) {
              int tokensLeft = getPileVpTokens(Cards.aqueduct);
              if (tokensLeft > 0) {
                removePileVpTokens(Cards.aqueduct, tokensLeft, context);
                player.addVictoryTokens(context, tokensLeft, Cards.aqueduct);
              }
            }
            int groundsKeepers = context.countCardsInPlay(Cards.groundskeeper);
            player.addVictoryTokens(context, groundsKeepers, Cards.groundskeeper);
          }

          if (gainedCardAbility.equals(Cards.illGottenGains)) {
            for (Player targetPlayer : getPlayersInTurnOrder()) {
              if (targetPlayer != player) {
                MoveContext targetContext = new MoveContext(Game.this, targetPlayer);
                targetPlayer.gainNewCard(Cards.curse, event.card, targetContext);
              }
            }
          } else if (event.card.equals(Cards.province)) {
            for (Player targetPlayer : getPlayersInTurnOrder()) {
              if (targetPlayer != player) {
                int foolsGoldCount = 0;
                // Check all of the cards, not just for existence, since there may be more than 1
                for (Card c : targetPlayer.hand) {
                  if (c.equals(Cards.foolsGold)) {
                    foolsGoldCount++;
                  }
                }

                while (foolsGoldCount-- > 0) {
                  MoveContext targetContext = new MoveContext(Game.this, targetPlayer);
                  FoolsGoldOption option = targetPlayer.controlPlayer.foolsGold_chooseOption(targetContext);
                  if (option == FoolsGoldOption.TrashForGold) {
                    targetPlayer.hand.remove(Cards.foolsGold);
                    targetPlayer.trash(Cards.foolsGold, Cards.foolsGold, targetContext);
                    targetPlayer.gainNewCard(Cards.gold, Cards.foolsGold, targetContext);
                  } else if (option == FoolsGoldOption.PassAll) {
                    break;
                  }
                }
              }
            }
          } else if (event.card.equals(Cards.duchy)) {
            if (Cards.isSupplyCard(Cards.duchess) && isCardOnTop(Cards.duchess)) {
              if (player.controlPlayer.duchess_shouldGainBecauseOfDuchy((MoveContext) context)) {
                player.gainNewCard(Cards.duchess, Cards.duchess, context);
              }
            }
          } else if (gainedCardAbility.equals(Cards.embassy)) {
            for (Player targetPlayer : getPlayersInTurnOrder()) {
              if (targetPlayer != player) {
                MoveContext targetContext = new MoveContext(Game.this, targetPlayer);
                targetPlayer.gainNewCard(Cards.silver, event.card, targetContext);
              }
            }
          } else if (gainedCardAbility.equals(Cards.cache)) {
            for (int i = 0; i < 2; i++) {
              player.gainNewCard(Cards.copper, event.card, context);
            }
          } else if (gainedCardAbility.equals(Cards.inn)) {
            ArrayList<Card> cards = new ArrayList<Card>();
            int actionCardsFound = 0;
            for (int i = player.discard.size() - 1; i >= 0; i--) {
              Card c = player.discard.get(i);
              if (c.is(Type.Action, player)) {
                actionCardsFound++;
                if (player.controlPlayer.inn_shuffleCardBackIntoDeck(event.getContext(), c)) {
                  cards.add(c);
                }
              }
            }

            Util.debug((String.format("Inn: %d action(s) found in %d-card discard pile", actionCardsFound, player.discard.size())), true);
            if (cards.size() > 0) {
              for (Card c : cards) {
                player.discard.remove(c);
                player.deck.add(c);
              }
            }
            player.shuffleDeck(context, Cards.inn);
          } else if (gainedCardAbility.equals(Cards.borderVillage)) {
            boolean validCard = false;
            int gainedCardCost = event.card.getCost(context);
            for (Card c : event.context.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true)) {
              if (Cards.isSupplyCard(c) && c.getCost(context) < gainedCardCost && !c.costPotion() && c.getDebtCost(context) <= 0 && event.context.isCardOnTop(c)) {
                validCard = true;
                break;
              }
            }

            if (validCard) {
              Card card = context.player.controlPlayer.borderVillage_cardToObtain(context, gainedCardCost - 1);
              if (card != null) {
                if (card.getCost(context) < gainedCardCost && card.getDebtCost(context) == 0 && !card.costPotion()) {
                  player.gainNewCard(card, event.card, (MoveContext) context);
                } else {
                  Util.playerError(player, "Border Village returned invalid card, ignoring.");
                }
              }
            }
          } else if (gainedCardAbility.equals(Cards.mandarin)) {
            CardList playedCards = context.getPlayedCards();
            CardList nextTurnCards = context.player.nextTurnCards;
            ArrayList<Card> treasureCardsInPlay = new ArrayList<Card>();

            for (Card c : playedCards) {
              if (c.is(Type.Treasure, player)) {
                treasureCardsInPlay.add(c);
              }
            }
            for (Card c : nextTurnCards) {
              if (c.is(Type.Treasure, player)) {
                treasureCardsInPlay.add(c);
              }
            }

            if (treasureCardsInPlay.size() > 0) {
              Card[] order;
              if (treasureCardsInPlay.size() == 1)
              order = treasureCardsInPlay.toArray(new Card[treasureCardsInPlay.size()]);
              else
              order = player.controlPlayer.mandarin_orderCards(context, treasureCardsInPlay.toArray(new Card[treasureCardsInPlay.size()]));

              for (int i = order.length - 1; i >= 0; i--) {
                Card c = order[i];
                player.putOnTopOfDeck(c);
                if (!playedCards.remove(c))
                nextTurnCards.remove(c);
              }
            }
          } else if (gainedCardAbility.equals(Cards.deathCart)) {
            context.player.controlPlayer.gainNewCard(Cards.virtualRuins, event.card, context);
            context.player.controlPlayer.gainNewCard(Cards.virtualRuins, event.card, context);
          } else if (gainedCardAbility.equals(Cards.lostCity)) {
            for (Player targetPlayer : getPlayersInTurnOrder()) {
              if (targetPlayer != player) {
                drawToHand(new MoveContext(Game.this, targetPlayer), Cards.lostCity, 1, true);
              }
            }
          } else if (gainedCardAbility.equals(Cards.emporium)) {
            if (context.countActionCardsInPlay() >= 5) {
              player.addVictoryTokens(context, 2, Cards.emporium);
            }
          } else if (gainedCardAbility.equals(Cards.fortune)) {
            int gladiators = context.countCardsInPlayByName(Cards.gladiator);
            for (int i = 0; i < gladiators; ++i) {
              player.gainNewCard(Cards.gold, event.card, context);
            }
          } else if (gainedCardAbility.equals(Cards.rocks)) {
            player.gainNewCard(Cards.silver, event.card, context);
          } else if (gainedCardAbility.equals(Cards.crumblingCastle)) {
            player.addVictoryTokens(context, 1, Cards.crumblingCastle);
            player.gainNewCard(Cards.silver, event.card, context);
          } else if (gainedCardAbility.equals(Cards.hauntedCastle) && context.game.getCurrentPlayer() == player) {
            player.gainNewCard(Cards.gold, event.card, context);
            for (Player targetPlayer : context.game.getPlayersInTurnOrder()) {
              if (targetPlayer == player) continue;
              if (targetPlayer.hand.size() >= 5) {
                MoveContext playerContext = new MoveContext(context.game, targetPlayer);
                playerContext.attackedPlayer = targetPlayer;
                Card[] cards = targetPlayer.controlPlayer.hauntedCastle_gain_cardsToPutBackOnDeck(playerContext);
                boolean bad = false;
                if (cards == null || cards.length == 2) {
                  bad = true;
                } else {
                  ArrayList<Card> copy = Util.copy(targetPlayer.hand);
                  for (Card card : cards) {
                    if (!copy.remove(card)) {
                      bad = true;
                      break;
                    }
                  }
                }
                if (bad) {
                  Util.playerError(targetPlayer, "Haunted Castle put back cards error, putting back the first 2 cards.");
                  cards = new Card[2];
                  cards[0] = targetPlayer.hand.get(0);
                  cards[1] = targetPlayer.hand.get(1);
                }
                GameEvent topDeckEvent = new GameEvent(GameEvent.EventType.CardOnTopOfDeck, playerContext);
                topDeckEvent.setPlayer(targetPlayer);
                for (int i = cards.length - 1; i >= 0; i--) {
                  targetPlayer.hand.remove(cards[i]);
                  targetPlayer.putOnTopOfDeck(cards[i]);
                  playerContext.game.broadcastEvent(topDeckEvent);
                }
              }
            }
          } else if (gainedCardAbility.equals(Cards.temple)) {
            int numTokens = context.game.getPileVpTokens(Cards.temple);
            context.game.removePileVpTokens(Cards.temple, numTokens, context);
            player.addVictoryTokens(context, numTokens, Cards.temple);
          } else if (gainedCardAbility.equals(Cards.sprawlingCastle)) {
            int duchyCount = context.game.getPile(Cards.duchy).getCount();
            int estateCount = context.game.getPile(Cards.estate).getCount();
            if (duchyCount == 0 && estateCount == 0) return;

            Player.HuntingGroundsOption option = context.player.controlPlayer.sprawlingCastle_chooseOption(context);
            if (option == null) option = HuntingGroundsOption.GainEstates;
            switch (option) {
              case GainDuchy:
              context.player.controlPlayer.gainNewCard(Cards.duchy, event.card, context);
              break;
              case GainEstates:
              context.player.controlPlayer.gainNewCard(Cards.estate, event.card, context);
              context.player.controlPlayer.gainNewCard(Cards.estate, event.card, context);
              context.player.controlPlayer.gainNewCard(Cards.estate, event.card, context);
              break;
              default:
              break;
            }
          } else if (gainedCardAbility.equals(Cards.grandCastle)) {
            int victoryCards = 0;
            for (Card c : player.getHand()) {
              player.reveal(c, event.card, context);
              if (c.is(Type.Victory, player)) {
                victoryCards++;
              }
            }

            for (Player opponent : context.game.getPlayersInTurnOrder()) {
              if (opponent != player) {
                for (Card c : opponent.nextTurnCards) {
                  if (c.is(Type.Victory, opponent)) {
                    victoryCards++;
                  }
                }
              }
            }

            victoryCards += context.countVictoryCardsInPlay();

            player.addVictoryTokens(context, victoryCards, Cards.grandCastle);
          }

          if (event.card.is(Type.Action, player)) {
            if (cardInGame(Cards.defiledShrine)) {
              //TODO?: you can technically choose the order of resolution for moving the VP
              //       tokens from the action to before taking the ones from Temple when it,
              //       but why would you ever do this outside of old possession rules?
              int tokensLeft = getPileVpTokens(event.card);
              if (tokensLeft > 0) {
                removePileVpTokens(event.card, 1, context);
                addPileVpTokens(Cards.defiledShrine, 1, context);
              }
            }
          }

        }

        StringBuilder msg = new StringBuilder();
        msg.append(event.getPlayer().getPlayerName() + ": " + event.getType());
        if (event.card != null) {
          msg.append(":" + event.card.getName());
          if (event.card.getControlCard() != event.card) {
            msg.append(" <" + event.card.getControlCard().getName() + ">");
          }
          if (event.card.isImpersonatingAnotherCard()) {
            msg.append(" (as " + event.card.behaveAsCard().getName() + ")");
          }
        }
        if (event.getType() == GameEvent.EventType.TurnBegin && event.getPlayer().isPossessed()) {
          msg.append(" possessed by " + event.getPlayer().controlPlayer.getPlayerName() + "!");
        }
        if (event.attackedPlayer != null) {
          msg.append(", attacking:" + event.attackedPlayer.getPlayerName());
        }

        if (event.getType() == GameEvent.EventType.BuyingCard) {
          msg.append(" (gold remaining: " + event.getContext().getCoinAvailableForBuy() + ", buys remaining: " + event.getContext().getBuysLeft());
        }
        Util.debug(msg.toString(), true);

      }

    };
  }


  /***************************************
  ** SECTION 3: GAME'S TURN HANDLING FUNCTIONS
  ***************************************/

  /*
  ** setPlayersTurn - Sets up turn for the Next Player
  */
  public void setPlayersTurn(boolean takeAnotherTurn) {
    if (!takeAnotherTurn && consecutiveTurnCounter > 0) {
      consecutiveTurnCounter = 0;
      playersTurn++;
      if (playersTurn >= numPlayers) {
        playersTurn = 0;
        gameTurnCount++;
        Util.debug("Turn " + gameTurnCount + " --------------------", true);
      }
    }
  }

  /*
  ** printPlayerTurn - Print Players' Turn Information
  */
  public void printPlayerTurn() {
    for (Player player : players) {
      Util.debug("", true);
      ArrayList<Card> allCards = player.getAllCards();
      StringBuilder msg = new StringBuilder();
      msg.append(" " + allCards.size() + " Cards: ");

      final HashMap<String, Integer> cardCounts = new HashMap<String, Integer>();
      for (Card card : allCards) {
        String key = card.getName() + " -> " + card.getDescription();
        Integer count = cardCounts.get(key);
        if (count == null) {
          cardCounts.put(key, 1);
        } else {
          cardCounts.put(key, count + 1);
        }
      }

      ArrayList<Card> removeDuplicates = new ArrayList<Card>();
      for (Card card : allCards) {
        if (!removeDuplicates.contains(card)) {
          removeDuplicates.add(card);
        }
      }
      allCards = removeDuplicates;

      Collections.sort(allCards, new Comparator<Card>() {
        public int compare(Card o1, Card o2) {
          String keyOne = o1.getName() + " -> " + o1.getDescription();
          String keyTwo = o2.getName() + " -> " + o2.getDescription();
          return cardCounts.get(keyTwo) - cardCounts.get(keyOne);
        }
      });

      boolean first = true;
      for (Card card : allCards) {
        String key = card.getName() + " -> " + card.getDescription();
        if (first) {
          first = false;
        } else {
          msg.append(", ");
        }
        msg.append("" + cardCounts.get(key) + " " + card.getName());
      }

      Util.debug(player.getPlayerName() + ": " + msg, true);
    }
    Util.debug("", true);
  }


  /*
  ** playerEndTurn - ???
  */
  public List<ExtraTurnInfo> playerEndTurn(Player player, MoveContext context) {
    int handCount = 5;

    List<ExtraTurnInfo> result = new ArrayList<Game.ExtraTurnInfo>();
    // Can only have at most two consecutive turns
    for (Card card : player.nextTurnCards) {
      Card behaveAsCard = card.behaveAsCard();
      if (behaveAsCard.takeAnotherTurn()) {
        handCount = behaveAsCard.takeAnotherTurnCardCount();
        if (consecutiveTurnCounter <= 1) {
          result.add(new ExtraTurnInfo());
          break;
        }
      }
    }

    handCount += context.totalExpeditionBoughtThisTurn;

    // draw next hand
    for (int i = 0; i < handCount; i++) {
      drawToHand(context, null, handCount - i, false);
    }

    if (player.save != null) {
      player.hand.add(player.save);
      player.save = null;
    }

    // /////////////////////////////////
    // Reset context for status update
    // /////////////////////////////////
    context.actionsPlayedSoFar = 0;
    context.actions = 1;
    context.buys = 1;
    context.coppersmithsPlayed = 0;

    GameEvent event = new GameEvent(GameEvent.EventType.NewHand, context);
    broadcastEvent(event);
    event = null;

    // /////////////////////////////////
    // Turn End
    // /////////////////////////////////

    event = new GameEvent(GameEvent.EventType.TurnEnd, context);
    broadcastEvent(event);

    if (cardsObtainedLastTurn[playersTurn].size() == 0 && cardInGame(Cards.baths)) {
      int tokensLeft = getPileVpTokens(Cards.baths);
      if (tokensLeft > 0) {
        int tokensToTake = Math.min(tokensLeft, 2);
        removePileVpTokens(Cards.baths, tokensToTake, context);
        player.addVictoryTokens(context, tokensToTake, Cards.baths);
      }
    }

    if (player.isPossessed()) {
      while (!possessedTrashPile.isEmpty()) {
        player.discard(possessedTrashPile.remove(0), null, null, false, false);
      }
      possessedBoughtPile.clear();
    }

    if (player.isPossessed()) {
      if (--possessionsToProcess == 0)
      possessingPlayer = null;
      player.controlPlayer = player;
    } else if (nextPossessionsToProcess > 0) {
      possessionsToProcess = nextPossessionsToProcess;
      possessingPlayer = nextPossessingPlayer;
      nextPossessionsToProcess = 0;
      nextPossessingPlayer = null;
    }

    if (context.missionBought && consecutiveTurnCounter <= 1) {
      if (!result.isEmpty()) {
        //ask player if they want to do mission turn with three cards or do outpost turn first then mission turn
        // player not possessed because we are between turns

        //TODO: dominionator - integrate this with Possession turn logic
        ExtraTurnOption[] options = new ExtraTurnOption[]{ExtraTurnOption.OutpostFirst, ExtraTurnOption.MissionFirst};
        switch (player.extraTurn_chooseOption(context, options)) {
          case MissionFirst:
          result.get(0).canBuyCards = false;
          break;
          case OutpostFirst:
          result.add(new ExtraTurnInfo(false));
          break;
          case PossessionFirst:
          //TODO
          break;
          default:
          break;
        }
      } else {
        result.add(new ExtraTurnInfo(false));
      }
    }

    return result;
  }


  /*
  ** playerAfterTurn - ???
  */
  public void playerAfterTurn(Player player, MoveContext context) {
    while (context.donatesBought-- > 0) {
      while (!player.deck.isEmpty()) {
        player.hand.add(player.deck.removeLastCard());
      }
      while (!player.discard.isEmpty()) {
        player.hand.add(player.discard.removeLastCard());
      }
      Card[] cardsToTrash = player.donate_cardsToTrash(context);
      if (cardsToTrash != null) {
        for (Card c : cardsToTrash) {
          Card toTrash = player.hand.get(c);
          if (toTrash == null) {
            Util.playerError(player, "Donate error, tried to trash card not in hand: " + c);
          } else {
            player.hand.remove(toTrash);
            player.trash(toTrash, Cards.donate, context);
          }
        }
      }
      while (!player.hand.isEmpty()) {
        player.deck.add(player.hand.removeLastCard());
      }
      player.shuffleDeck(context, Cards.donate);
      for (int i = 0; i < 5; ++i) {
        drawToHand(context, Cards.donate, 5 - i);
      }
    }

    // Mountain Pass bidding
    if (cardInGame(Cards.mountainPass) && doMountainPassAfterThisTurn) {
      doMountainPassAfterThisTurn = false;

      int highestBid = 0;
      Player highestBidder = null;
      final int MAX_BID = 40;
      int playersLeftToBid = numPlayers;
      for (Player biddingPlayer : getPlayersInTurnOrder((firstProvinceGainedBy + 1) % numPlayers)) {
        MoveContext bidContext = new MoveContext(this, biddingPlayer);
        int bid = biddingPlayer.mountainPass_getBid(context, highestBidder, highestBid, --playersLeftToBid);
        if (bid > MAX_BID) bid = MAX_BID;
        if (bid < 0) bid = 0;
        if (bid != 0 && bid > highestBid) {
          highestBid = bid;
          highestBidder = biddingPlayer;
        }
        GameEvent event = new GameEvent(GameEvent.EventType.MountainPassBid, bidContext);
        event.setAmount(bid);
        event.card = Cards.mountainPass;
        context.game.broadcastEvent(event);
        if (bid == MAX_BID) {
          break;
        }
      }
      if (highestBidder != null) {
        MoveContext bidContext = new MoveContext(this, highestBidder);
        highestBidder.addVictoryTokens(bidContext, 8, Cards.mountainPass);
        highestBidder.gainDebtTokens(highestBid);
        GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensObtained, context);
        event.setAmount(highestBid);
        context.game.broadcastEvent(event);
      }
      GameEvent winEvent = new GameEvent(GameEvent.EventType.MountainPassWinner, context);
      winEvent.setPlayer(highestBidder == null ? context.getPlayer() : highestBidder);
      winEvent.setAmount(highestBid);
      context.game.broadcastEvent(winEvent);
    }
  }


  /*
  ** - playerBeginTurn - ???
  */
  @SuppressWarnings("unchecked")
  public void playerBeginTurn(Player player, MoveContext context) {

    if (context.game.possessionsToProcess > 0) {
      player.controlPlayer = context.game.possessingPlayer;
    } else {
      player.controlPlayer = player;
      consecutiveTurnCounter++;
    }

    cardsObtainedLastTurn[playersTurn].clear();
    if (consecutiveTurnCounter == 1) {
      player.newTurn();
    }

    player.clearDurationEffectsOnOtherPlayers();

    GameEvent gevent = new GameEvent(GameEvent.EventType.TurnBegin, context);
    broadcastEvent(gevent);

    /* Duration cards (i.e. horse traders, haven, gear, archive, wharf, cards on prince, etc. */
    boolean allDurationAreSimple = true;
    ArrayList<Object> durationEffects = new ArrayList<Object>();
    ArrayList<Boolean> durationEffectsAreCards = new ArrayList<Boolean>();
    int archiveNum = 0;

    for (Card card : player.nextTurnCards) {

      Card thisCard = card.behaveAsCard();
      if (thisCard.is(Type.Duration, player)) {

        /* NOTE: Effects that resolve at the start of your turn can be resolved in any order;
        ** this includes multiple plays of the same Duration card by a Throne Room variant.
        ** For example, if you played a Wharf and then a Throne Room on an Amulet last turn,
        ** on this turn you could choose to first gain a Silver from the first Amulet play,
        ** then draw 2 cards from Wharf (perhaps triggering a reshuffle and maybe drawing
        ** that Silver), and then choose to trash a card from the second Amulet play,
        ** now that you have more cards to choose from.
        */

        int cloneCount = ((CardImpl) card).getControlCard().cloneCount;

        for (int clone = cloneCount; clone > 0; clone--) {

          if (thisCard.equals(Cards.amulet) ||
              thisCard.equals(Cards.dungeon)) {
            allDurationAreSimple = false;
          }

          if (thisCard.equals(Cards.haven)) {
            if (player.haven != null && player.haven.size() > 0) {
              durationEffects.add(thisCard);
              durationEffects.add(player.haven.remove(0));
              durationEffectsAreCards.add(clone == cloneCount && !((CardImpl) card.behaveAsCard()).trashAfterPlay);
              durationEffectsAreCards.add(false);
            }
          } else if (thisCard.equals(Cards.gear)) {
            if (player.gear.size() > 0) {
              durationEffects.add(thisCard);
              durationEffects.add(player.gear.remove(0));
              durationEffectsAreCards.add(clone == cloneCount && !((CardImpl) card.behaveAsCard()).trashAfterPlay);
              durationEffectsAreCards.add(false);
            }
          } else if (thisCard.equals(Cards.archive)) {
            if (player.archive.size() > 0 && player.archive.size() != (archiveNum + 1)) {
              durationEffects.add(thisCard);
              durationEffects.add(player.archive.get(archiveNum++));
              durationEffectsAreCards.add(clone == cloneCount && !((CardImpl) card.behaveAsCard()).trashAfterPlay);
              durationEffectsAreCards.add(false);
            }
          } else {
            durationEffects.add(thisCard);
            durationEffects.add(Cards.curse); /*dummy*/
            durationEffectsAreCards.add(clone == cloneCount && !((CardImpl) card.behaveAsCard()).trashAfterPlay);
            durationEffectsAreCards.add(false);
          }
        }
      } else if (isModifierCard(thisCard.behaveAsCard())) {
        GameEvent event = new GameEvent(GameEvent.EventType.PlayingDurationAction, context);
        event.card = card;
        event.newCard = true;
        broadcastEvent(event);
      }
    }

    for (Card card : player.horseTraders) {
      durationEffects.add(card);
      durationEffects.add(Cards.curse); /*dummy*/
      durationEffectsAreCards.add(true);
      durationEffectsAreCards.add(false);
    }
    for (Card card : player.prince) {
      if (!card.equals(Cards.prince)) {
        allDurationAreSimple = false;
        durationEffects.add(Cards.prince);
        durationEffects.add(card);
        durationEffectsAreCards.add(true);
        durationEffectsAreCards.add(false);
      }
    }
    for (Card card : player.summon) {
      if (!card.equals(Cards.summon)) {
        allDurationAreSimple = false;
        durationEffects.add(Cards.summon);
        durationEffects.add(card);
        durationEffectsAreCards.add(true);
        durationEffectsAreCards.add(false);
      }
    }
    while (!player.haven.isEmpty()) {
      durationEffects.add(Cards.haven);
      durationEffects.add(player.haven.remove(0));
      durationEffectsAreCards.add(false);
      durationEffectsAreCards.add(false);
    }
    while (archiveNum < player.archive.size()) {
      durationEffects.add(Cards.archive);
      durationEffects.add(player.archive.get(archiveNum++));
      durationEffectsAreCards.add(false);
      durationEffectsAreCards.add(false);
    }
    int numOptionalItems = 0;
    ArrayList<Card> callableCards = new ArrayList<Card>();
    for (Card c : player.tavern) {
      if (c.behaveAsCard().isCallableWhenTurnStarts()) {
        callableCards.add((Card) c);
      }
    }
    if (!callableCards.isEmpty()) {
      Collections.sort(callableCards, new Util.CardCostComparator());
      for (Card c : callableCards) {
        if (c.behaveAsCard().equals(Cards.guide)
        || c.behaveAsCard().equals(Cards.ratcatcher)
        || c.behaveAsCard().equals(Cards.transmogrify)) {
          allDurationAreSimple = false;
        }
      }
    }
    if (!allDurationAreSimple) {
      // Add cards callable at start of turn
      for (Card c : callableCards) {
        durationEffects.add(c);
        durationEffects.add(Cards.curse);
        durationEffectsAreCards.add(false);
        durationEffectsAreCards.add(false);
        numOptionalItems += 2;
      }
    }
    while (durationEffects.size() > numOptionalItems) {
      int selection = 0;
      if (allDurationAreSimple) {
        selection = 0;
      } else {
        selection = 2 * player.controlPlayer.duration_cardToPlay(context, durationEffects.toArray(new Object[durationEffects.size()]));
      }
      Card card = (Card) durationEffects.get(selection);
      boolean isRealCard = durationEffectsAreCards.get(selection);
      if (card == null) {
        Util.log("ERROR: duration_cardToPlay returned " + selection);
        selection = 0;
        card = (Card) durationEffects.get(selection);
      }
      Card card2 = null;
      if (durationEffects.get(selection + 1) instanceof Card) {
        card2 = (Card) durationEffects.get(selection + 1);
      }
      ArrayList<Card> setAsideCards = null;
      if (durationEffects.get(selection + 1) instanceof ArrayList<?>) {
        setAsideCards = (ArrayList<Card>) durationEffects.get(selection + 1);
      }
      if (card2 == null) {
        Util.log("ERROR: duration_cardToPlay returned " + selection);
        card2 = card;
      }

      durationEffects.remove(selection + 1);
      durationEffects.remove(selection);
      durationEffectsAreCards.remove(selection + 1);
      durationEffectsAreCards.remove(selection);

      if (card.equals(Cards.prince)) {

        if (!(card2.is(Type.Duration, player))) {
          player.playedByPrince.add(card2);
        }
        player.prince.remove(card2);
        context.freeActionInEffect++;
        try {
          card2.play(this, context, false);
        } catch (RuntimeException e) {
          e.printStackTrace();
        }
        context.freeActionInEffect--;

      } else if (card.equals(Cards.summon)) {

        player.summon.remove(card2);
        context.freeActionInEffect++;
        try {
          card2.play(this, context, false);
        } catch (RuntimeException e) {
          e.printStackTrace();
        }
        context.freeActionInEffect--;

      } else if (card.behaveAsCard().equals(Cards.horseTraders)) {

        //BUG: this doesn't let you call estates inheriting horse trader differently
        Card horseTrader = player.horseTraders.remove(0);
        player.hand.add(horseTrader);
        drawToHand(context, horseTrader, 1);

      } else if (card.behaveAsCard().is(Type.Duration, player)) {

        if (card.behaveAsCard().equals(Cards.haven)) {
          player.hand.add(card2);
        }
        if (card.behaveAsCard().equals(Cards.gear)) {
          for (Card c : setAsideCards)
          player.hand.add(c);
        }
        if (card.behaveAsCard().equals(Cards.archive)) {
          CardImplEmpires.archiveSelect(this, context, player, setAsideCards);
        }

        Card thisCard = card.behaveAsCard();

        GameEvent event = new GameEvent(GameEvent.EventType.PlayingDurationAction, context);
        event.card = card;
        event.newCard = isRealCard;
        broadcastEvent(event);

        context.actions += thisCard.getAddActionsNextTurn();
        context.addCoins(thisCard.getAddGoldNextTurn());
        context.buys += thisCard.getAddBuysNextTurn();
        int addCardsNextTurn = thisCard.getAddCardsNextTurn();

        /* addCardsNextTurn are displayed like addCards but sometimes the text differs */
        if (thisCard.getKind() == Cards.Kind.Tactician) {
          context.actions += 1;
          context.buys += 1;
          addCardsNextTurn = 5;
        }
        if (thisCard.getKind() == Cards.Kind.Dungeon) {
          addCardsNextTurn = 2;
        }
        if (thisCard.getKind() == Cards.Kind.Hireling) {
          addCardsNextTurn = 1;
        }

        for (int i = 0; i < addCardsNextTurn; i++) {
          drawToHand(context, thisCard, addCardsNextTurn - i, true);
        }

        if (thisCard.getKind() == Cards.Kind.Amulet
        || thisCard.getKind() == Cards.Kind.Dungeon) {
          context.freeActionInEffect++;
          try {
            ((CardImpl) thisCard).additionalCardActions(context.game, context, player);
          } catch (RuntimeException e) {
            e.printStackTrace();
          }
          context.freeActionInEffect--;
        }
      } else if (card.behaveAsCard().isCallableWhenTurnStarts()) {
        numOptionalItems -= 2;
        card.behaveAsCard().callAtStartOfTurn(context);
      } else {
        Util.log("ERROR: nextTurnCards contains " + card);
      }
    }

    ArrayList<Card> staysInPlayCards = new ArrayList<Card>();
    archiveNum = 0;
    while (!player.nextTurnCards.isEmpty()) {
      Card card = player.nextTurnCards.remove(0);
      if (isModifierCard(card.behaveAsCard())) {
        if (!player.nextTurnCards.isEmpty()) {
          Card nextCard = player.nextTurnCards.get(0);
          int additionalModifierCards = 0;
          while (nextCard != null && isModifierCard(nextCard.behaveAsCard())) {
            additionalModifierCards++;
            if (player.nextTurnCards.size() > additionalModifierCards)
            nextCard = player.nextTurnCards.get(additionalModifierCards);
            else
            nextCard = null;
          }
          if (nextCard != null && (nextCard.behaveAsCard().equals(Cards.hireling) || nextCard.behaveAsCard().equals(Cards.champion) ||
          (nextCard.behaveAsCard().equals(Cards.archive) && player.archive.get(archiveNum++).size() > 0))) {
            staysInPlayCards.add(card);
            for (int i = 0; i < additionalModifierCards; ++i) {
              staysInPlayCards.add(player.nextTurnCards.remove(0));
            }
            player.nextTurnCards.remove(0);
            staysInPlayCards.add(nextCard);
            continue;
          }
        }
      }

      if (card.behaveAsCard().equals(Cards.hireling) || card.behaveAsCard().equals(Cards.champion) ||
      (card.behaveAsCard().equals(Cards.archive) && player.archive.size() > archiveNum && player.archive.get(archiveNum++).size() > 0)) {
        staysInPlayCards.add(card);
      } else {
        CardImpl behaveAsCard = (CardImpl) card.behaveAsCard();
        behaveAsCard.cloneCount = 1;
        ((CardImpl) card).cloneCount = 1;
        if (!(behaveAsCard.trashAfterPlay || ((CardImpl) card).trashAfterPlay)) {
          player.playedCards.add(card);
        } else {
          behaveAsCard.trashAfterPlay = false;
          ((CardImpl) card).trashAfterPlay = false;
        }
      }
    }
    while (!staysInPlayCards.isEmpty()) {
      player.nextTurnCards.add(staysInPlayCards.remove(0));
    }
    //Clean up empty Archive lists
    Iterator<ArrayList<Card>> it = player.archive.iterator();
    while (it.hasNext()) {
      if (it.next().isEmpty())
      it.remove();
    }

    //TODO: Dominionator - Will require tracking duration effects independent of cards
    //       to do correctly or replacing real card with a dummy card - do this later.

    //TODO: integrate this into the main action selection UI if possible to make it more seamless
    //check for start-of-turn callable cards
    callableCards = new ArrayList<Card>();
    Card toCall = null;
    for (Card c : player.tavern) {
      if (c.behaveAsCard().isCallableWhenTurnStarts()) {
        callableCards.add(c);
      }
    }
    if (!callableCards.isEmpty()) {
      Collections.sort(callableCards, new Util.CardCostComparator());
      do {
        toCall = null;
        // we want null entry at the end for None
        Card[] cardsAsArray = callableCards.toArray(new Card[callableCards.size() + 1]);
        //ask player which card to call
        toCall = player.controlPlayer.call_whenTurnStartCardToCall(context, cardsAsArray);
        if (toCall != null && callableCards.contains(toCall)) {
          toCall = callableCards.remove(callableCards.indexOf(toCall));
          toCall.behaveAsCard().callAtStartOfTurn(context);
        }
        // loop while we still have cards to call
      } while (toCall != null && !callableCards.isEmpty());
    }
  }


  public Player getNextPlayer() {
    int next = playersTurn + 1;
    if (next >= numPlayers) {
      next = 0;
    }
    return players[next];
  }

  public Player getCurrentPlayer() {
    return players[playersTurn];
  }

  public Player[] getPlayersInTurnOrder() {
    return getPlayersInTurnOrder(playersTurn);
  }

  public Player[] getPlayersInTurnOrder(Player startingPlayer) {
    int at = 0;
    for (int i = 0; i < numPlayers; i++) {
      if (players[i] == startingPlayer) {
        at = i;
        break;
      }
    }
    return getPlayersInTurnOrder(at);
  }

  public Player[] getPlayersInTurnOrder(int startingPlayerIdx) {
    Player[] ordered = new Player[numPlayers];

    int at = startingPlayerIdx;
    for (int i = 0; i < numPlayers; i++) {
      ordered[i] = players[at];
      at++;
      if (at >= numPlayers) {
        at = 0;
      }
    }

    return ordered;
  }


  /*
  ** playTreasures - Selects Treasure Cards from Player's Hand and Plays Them (During Buy Phase)
  */
  public void playTreasures(Player player, MoveContext context, int maxCards, Card responsible) {

    // Determine if Player should Select Treasures to Play
    boolean selectingCoins = playerShouldSelectCoinsToPlay(context, player.getHand());
    if (maxCards != -1) {
      selectingCoins = true; // i.e. Storyteller sets maxCards != -1
    }

    // Get list of Treasures to Play
    ArrayList<Card> treasures = null;
    treasures = (selectingCoins) ? player.controlPlayer.treasureCardsToPlayInOrder(context, maxCards, responsible) : player.getTreasuresInHand();

    // Play Treasure Cards
    while (treasures != null && !treasures.isEmpty() && maxCards != 0) {
      while (!treasures.isEmpty() && maxCards != 0) {
        Card card = treasures.remove(0);
        if (player.hand.contains(card)) { // Needed due to Counterfeit which trashes cards during this loop
          card.play(context.game, context, true, true);
          maxCards--;
        }
      }
      if (maxCards != 0) {
        treasures = (selectingCoins) ? player.controlPlayer.treasureCardsToPlayInOrder(context, maxCards, responsible) : player.getTreasuresInHand();
      }
    }
  }


  /*
  ** playGuildsTokens - - Selects Guilds Tokens from Player's Stash and Plays Them (During Buy Phase)
  */
  public void playGuildsTokens(Player player, MoveContext context) {

    int coinTokenTotal = player.getGuildsCoinTokenCount();
    if (coinTokenTotal > 0) {

      // Offer the player the option of "spending" Guilds coin tokens prior to buying cards
      int numTokensToSpend = player.controlPlayer.numGuildsCoinTokensToSpend(context, coinTokenTotal, false/*!butcher*/);

      if (numTokensToSpend > 0 && numTokensToSpend <= coinTokenTotal) {
        player.spendGuildsCoinTokens(numTokensToSpend);
        context.addCoins(numTokensToSpend);
        if (numTokensToSpend > 0) {
          GameEvent event = new GameEvent(GameEvent.EventType.GuildsTokenSpend, context);
          event.setComment(": " + numTokensToSpend);
          context.game.broadcastEvent(event);
        }

        Util.debug(player, "Spent " + numTokensToSpend + " Guilds coin tokens");

      }
    }
  }


  public void playerAction(Player player, MoveContext context) {

    Card action = null;

    do {

      action = null;
      ArrayList<Card> actionCards = null;

      if (!actionChains || player.controlPlayer.isAi()) {
        action = player.controlPlayer.doAction(context);
        if (action != null) {
          actionCards = new ArrayList<Card>();
          actionCards.add(action);
        }
      } else {
        Card[] cs = player.controlPlayer.actionCardsToPlayInOrder(context);
        if (cs != null && cs.length != 0) {
          actionCards = new ArrayList<Card>();
          for (int i = 0; i < cs.length; i++) {
            actionCards.add(cs[i]);
          }
        }
      }


      while (context.actions > 0 && actionCards != null && !actionCards.isEmpty()) {

        action = actionCards.remove(0);
        if (action != null) {
          if (isValidAction(context, action)) {
            GameEvent event = new GameEvent(GameEvent.EventType.Status, (MoveContext) context);
            broadcastEvent(event);
            try {
              action.play(this, (MoveContext) context, true);
            } catch (RuntimeException e) {
              e.printStackTrace();
            }
          } else {
            Util.debug("Error:Invalid action selected");
          }
        }
      }
    } while (context.actions > 0 && action != null);
  }

  public void playerPayOffDebt(Player player, MoveContext context) {
    if (player.getDebtTokenCount() > 0 && context.getCoins() > 0) {
      int payOffNum = player.controlPlayer.numDebtTokensToPayOff(context);
      if (payOffNum > context.getCoins() || payOffNum < 0) {
        payOffNum = 0;
      }
      if (payOffNum > player.getDebtTokenCount()) {
        payOffNum = player.getDebtTokenCount();
      }
      if (payOffNum > 0) {
        context.spendCoins(payOffNum);
        player.payOffDebtTokens(payOffNum);
        GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensPaidOff, context);
        event.setAmount(payOffNum);
        context.game.broadcastEvent(event);
      }
    }
  }

  public void playerBeginBuy(Player player, MoveContext context) {
    if (cardInGame(Cards.arena)) {
      arena(player, context);
    }
  }

  /*
  ** playerBuy - Implements main BUY phase for the Player
  */
  public void playerBuy(Player player, MoveContext context) {

    Card buy = null; // Card to Buy

    do {
      if (context.buys <= 0) { break; }
      buy = null;
      try {
        playerPayOffDebt(player, context);
        if (player.getDebtTokenCount() == 0) {
          buy = player.controlPlayer.doBuy(context);
          if (buy != null) {
            buy = getPile(buy).topCard(); //Swap in the actual top card of the pile
          }
        }
      } catch (Throwable t) {
        Util.playerError(player, t);
      }

      if (buy != null) {
        if (isValidBuy(context, buy)) {
          if (buy.is(Type.Event, null)) {
            context.totalEventsBoughtThisTurn++;
          } else {
            context.totalCardsBoughtThisTurn++;
          }
          GameEvent statusEvent = new GameEvent(GameEvent.EventType.Status, (MoveContext) context);
          broadcastEvent(statusEvent);
          playBuy(context, buy);
          playerPayOffDebt(player, context);
          if (context.returnToActionPhase)
          return;
        } else {
          buy = null;
        }
      }
    } while (context.buys > 0 && buy != null);

    //Discard Wine Merchants from Tavern
    if (context.getCoinAvailableForBuy() >= 2) {
      int wineMerchants = 0;
      for (Card card : player.getTavern()) {
        if (Cards.wineMerchant.equals(card)) {
          wineMerchants++;
        }
      }
      if (wineMerchants > 0) {
        int wineMerchantsTotal = player.controlPlayer.cleanup_wineMerchantToDiscard(context, wineMerchants);
        if (wineMerchants < 0 || wineMerchantsTotal > wineMerchants) {
          Util.playerError(player, "Wine Merchant discard error, invalid number of Wine Merchants. Discarding all Wine Merchants.");
          wineMerchantsTotal = wineMerchants;
        }
        if (wineMerchantsTotal > 0) {
          for (int i = 0; i < wineMerchantsTotal; i++) {
            Card card = player.getTavern().get(Cards.wineMerchant);
            player.getTavern().remove(card);
            player.discard(card, null, context, true, false); //set commandedDiscard=true and cleanup=false to force GameEvent
          }
        }
      }
    }

    //Discard Wine Merchants Estates from Tavern
    if (context.getCoinAvailableForBuy() >= 2) {
      int wineMerchants = 0;
      for (Card card : player.getTavern()) {
        if (Cards.estate.equals(card) && Cards.wineMerchant.equals(card.behaveAsCard())) {
          wineMerchants++;
        }
      }
      if (wineMerchants > 0) {
        int wineMerchantsTotal = player.controlPlayer.cleanup_wineMerchantEstateToDiscard(context, wineMerchants);
        if (wineMerchants < 0 || wineMerchantsTotal > wineMerchants) {
          Util.playerError(player, "Wine Merchant estate discard error, invalid number of Wine Merchants. Discarding all Wine Merchants.");
          wineMerchantsTotal = wineMerchants;
        }
        if (wineMerchantsTotal > 0) {
          for (int i = 0; i < wineMerchantsTotal; i++) {
            Card card = player.getTavern().get(Cards.estate);
            player.getTavern().remove(card);
            player.discard(card, null, context, true, false); //set commandedDiscard=true and cleanup=false to force GameEvent
          }
        }
      }
    }
  }


  /*
  ** isValidAction - Returns True if Action is valid, and False otherwise
  */
  public boolean isValidAction(MoveContext context, Card action) {
    if (action == null) {
      return false;
    }
    if (!(action.is(Type.Action, context.player))) {
      return false;
    }
    for (Card card : context.getPlayer().hand) {
      if (action.equals(card)) {
        return true;
      }
    }


    return false;
  }


  /*
  ** isValidBuy - Returns True if Buy is valid, and False otherwise
  */
  public boolean isValidBuy(MoveContext context, Card card) {
    return isValidBuy(context, card, context.getCoinAvailableForBuy());
  }


  /*
  ** isValidBuy - Returns True if Buy is valid, and False otherwise
  */
  public boolean isValidBuy(MoveContext context, Card card, int gold) {
    if (card == null) {
      return true;
    }

    // TODO: Temp hack to prevent AI from buying possession, even though human player can, since it only half works
    //       (AI will make decisions while possessed, but will try to make "good" ones)
    //        if(card.equals(Cards.possession) && context != null && context.getPlayer() != null && context.getPlayer().isAi()) {
    //            return false;
    //        }

    CardPile thePile = getPile(card);
    if (thePile == null) {
      return false;
    }
    if (context.getPlayer().getDebtTokenCount() > 0) {
      return false;
    }
    if (!context.canBuyCards && !card.is(Type.Event, null)) {
      return false;
    }
    if (context.blackMarketBuyPhase) {
      if (thePile.isBlackMarket() == false) {
        return false;
      }
      if (Cards.isSupplyCard(card)) {
        return false;
      }
    } else if (card.is(Type.Event, null) && context.phase != TurnPhase.Buy) {
      return false;
    } else if (!card.is(Type.Event, null)) {
      if (thePile.isSupply() == false) {
        return false;
      }
      if (!Cards.isSupplyCard(card)) {
        return false;
      }
    }

    if (isPileEmpty(card)) {
      return false;
    }

    if (context.cantBuy.contains(card)) {
      return false;
    }

    if (card.equals(Cards.grandMarket) && (context.countCardsInPlay(Cards.copper) > 0)) {
      return false;
    }

    int cost = card.getCost(context, !context.blackMarketBuyPhase);

    int potions = context.getPotions();
    if (cost <= gold && (!card.costPotion() || potions > 0)) {
      return true;
    }

    return false;
  }

  /*
  ** playBuy - ???
  */
  public Card playBuy(MoveContext context, Card buy) {

    // Use up one Buy
    Player player = context.getPlayer();
    if (!context.blackMarketBuyPhase) {
      context.buys--;
    }

    // Spend Coins, or Potion, or Gain Debt
    context.spendCoins(buy.getCost(context));
    if (buy.costPotion()) {
      context.potions--;
    } else if (buy.getDebtCost(context) > 0) {
      int debtCost = buy.getDebtCost(context);
      context.getPlayer().controlPlayer.gainDebtTokens(debtCost);
      GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensObtained, context);
      event.setAmount(debtCost);
      context.game.broadcastEvent(event);
    }

    // Check for Embargos on the Supply Pile (and Gain Curses if there are)
    int embargos = getEmbargos(buy);
    for (int i = 0; i < embargos; i++) {
      player.gainNewCard(Cards.curse, Cards.embargo, context);
    }

    // Check for "Tax" Debt Tokens on the Supply Pile (and Gain Debt if there are)
    int numDebtTokensOnPile = getPileDebtTokens(buy);
    if (numDebtTokensOnPile > 0) {
      removePileDebtTokens(buy, numDebtTokensOnPile, context);
      context.getPlayer().controlPlayer.gainDebtTokens(numDebtTokensOnPile);
      GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensObtained, context);
      event.setAmount(numDebtTokensOnPile);
      context.game.broadcastEvent(event);
    }

    // Calculate Cost adjusted based on other Cards in play or Card being bought
    int cost = buy.getCost(context);

    Card card = buy;
    if (!buy.is(Type.Event, null)) {
      card = takeFromPileCheckTrader(buy, context);
    }

    // If card can be overpaid for, do so now
    if (buy.isOverpay(player)) {
      int coinOverpay = player.amountToOverpay(context, buy);
      coinOverpay = Math.max(0, coinOverpay);
      coinOverpay = Math.min(coinOverpay, context.getCoinAvailableForBuy());
      context.overpayAmount = coinOverpay;

      context.spendCoins(context.overpayAmount);

      if (context.potions > 0) {
        int potionOverpay = player.overpayByPotions(context, context.potions);
        potionOverpay = Math.max(0, potionOverpay);
        potionOverpay = Math.min(potionOverpay, context.getPotions());
        context.overpayPotions = potionOverpay;
        context.potions -= context.overpayPotions;
      }

      if (context.overpayAmount > 0 || context.overpayPotions > 0) {
        GameEvent event = new GameEvent(GameEvent.EventType.OverpayForCard, (MoveContext) context);
        event.card = card;
        event.newCard = true;
        broadcastEvent(event);
      }
    } else {
      context.overpayAmount = 0;
      context.overpayPotions = 0;
    }

    buy.isBuying(context);

    if (!buy.is(Type.Event, null)) {
      if (player.getHand().size() > 0 && isPlayerSupplyTokenOnPile(buy, player, PlayerSupplyToken.Trashing)) {
        Card cardToTrash = player.controlPlayer.trashingToken_cardToTrash((MoveContext) context);
        if (cardToTrash != null) {
          if (!player.getHand().contains(cardToTrash)) {
            Util.playerError(player, "Trashing token error, invalid card to trash, ignoring.");
          } else {
            player.hand.remove(cardToTrash);
            player.trash(cardToTrash, null, context);
          }
        }
      }

      for (int i = 0; i < swampHagAttacks(player); i++) {
        player.gainNewCard(Cards.curse, Cards.swampHag, context);
      }

      if (hauntedWoodsAttacks(player)) {
        if (player.hand.size() > 0) {
          Card[] order;
          if (player.hand.size() == 1)
          order = player.hand.toArray();
          else
          order = player.controlPlayer.mandarin_orderCards(context, player.hand.toArray());

          for (int i = order.length - 1; i >= 0; i--) {
            Card c = order[i];
            player.putOnTopOfDeck(c);
            player.hand.remove(c);
          }
        }
      }
    }


    /* GameEvent.Type.BuyingCard must be after overpaying! */

    if (card != null) {
      GameEvent event = new GameEvent(GameEvent.EventType.BuyingCard, (MoveContext) context);
      event.card = card;
      event.newCard = true;
      broadcastEvent(event);
    }

    if (!buy.costPotion() && buy.getDebtCost(context) == 0 && !(buy.is(Type.Victory)) && cost < 5 && !buy.is(Type.Event)) {
      for (int i = 1; i <= context.countCardsInPlay(Cards.talisman); i++) {
        if (card.equals(getPile(card).topCard())) {
          context.getPlayer().gainNewCard(buy, Cards.talisman, context);
        }
      }
    }

    if (!buy.is(Type.Event)) {
      player.addVictoryTokens(context, context.countGoonsInPlay(), Cards.goons);
    }

    if (!buy.is(Type.Event) && context.countMerchantGuildsInPlayThisTurn() > 0) {
      player.gainGuildsCoinTokens(context.countMerchantGuildsInPlayThisTurn());
      GameEvent event = new GameEvent(GameEvent.EventType.GuildsTokenObtained, context);
      broadcastEvent(event);
    }

    if (buy.is(Type.Victory)) {
      context.victoryCardsBoughtThisTurn++;
      for (int i = 1; i <= context.countCardsInPlay(Cards.hoard); i++) {
        player.gainNewCard(Cards.gold, Cards.hoard, context);
      }
    }

    buy.isBought(context);
    if (!buy.is(Type.Event)) {
      haggler(context, buy);
      charmWhenBuy(context, buy);
      basilicaWhenBuy(context);
      colonnadeWhenBuy(context, buy);
      defiledShrineWhenBuy(context, buy);
    }

    return card;
  }


  // Use drawToHand when "drawing" or "+ X cards" when -1 Card token could be drawn instead
  boolean drawToHand(MoveContext context, Card responsible, int cardsLeftToDraw) {
    return drawToHand(context, responsible, cardsLeftToDraw, true);
  }

  boolean drawToHand(MoveContext context, Card responsible, int cardsLeftToDraw, boolean showUI) {
    Player player = context.player;
    if (player.getMinusOneCardToken()) {
      player.setMinusOneCardToken(false, context);
      return true;
    }
    Card card = draw(context, responsible, cardsLeftToDraw);
    if (card == null)
    return false;

    if (responsible != null) {
      Util.debug(player, responsible.getName() + " draw:" + card.getName(), true);
    }

    player.hand.add(card, showUI);

    return true;
  }

  // Use draw when removing a card from the top of the deck without "drawing" it (e.g. look at or reveal)
  Card draw(MoveContext context, Card responsible, int cardsLeftToDraw) {
    if (errataShuffleDeckEmptyOnly) {
      if (context.player.deck.isEmpty()) {
        if (context.player.discard.isEmpty()) {
          return null;
        } else {
          replenishDeck(context, responsible, cardsLeftToDraw);
        }
      }
    } else {
      if (cardsLeftToDraw > 0 && context.player.deck.size() < cardsLeftToDraw) {
        ArrayList<Card> cardsToDraw = new ArrayList<Card>();
        for (Card c : context.player.deck) {
          cardsToDraw.add(c);
        }
        context.player.deck.clear();
        if (!context.player.discard.isEmpty()) {
          replenishDeck(context, responsible, cardsLeftToDraw - cardsToDraw.size());
        }
        if (context.player.deck.isEmpty() && cardsToDraw.isEmpty()) {
          return null;
        }
        Collections.reverse(cardsToDraw);
        for (Card c : cardsToDraw) {
          context.player.deck.add(0, c);
        }
      } else if (context.player.deck.isEmpty()) {
        if (context.player.discard.isEmpty()) {
          return null;
        } else {
          replenishDeck(context, responsible, cardsLeftToDraw);
        }
      }
    }
    return context.player.deck.remove(0);
  }

  public void replenishDeck(MoveContext context, Card responsible, int cardsLeftToDraw) {
    context.player.replenishDeck(context, responsible, cardsLeftToDraw);

    GameEvent event = new GameEvent(GameEvent.EventType.DeckReplenished, context);
    broadcastEvent(event);
  }

  CardPile addPileVpTokens(Card card, int num, MoveContext context) {
    if (Cards.isBlackMarketCard(card)) {
      return null;
    }
    String name = card.getName();
    pileVpTokens.put(name, getPileVpTokens(card) + num);
    if (context != null) {
      GameEvent event = new GameEvent(GameEvent.EventType.VPTokensPutOnPile, context);
      event.setAmount(num);
      event.card = card;
      context.game.broadcastEvent(event);
    }
    return piles.get(name);
  }

  CardPile removePileVpTokens(Card card, int num, MoveContext context) {
    if (Cards.isBlackMarketCard(card)) {
      return null;
    }
    if (getPile(card) != null)
    card = getPile(card).placeholderCard();

    num = Math.min(num, getPileVpTokens(card));
    String name = card.getName();
    if (num > 0) {
      pileVpTokens.put(name, getPileVpTokens(card) - num);
      if (context != null) {
        GameEvent event = new GameEvent(GameEvent.EventType.VPTokensTakenFromPile, context);
        event.setAmount(num);
        event.card = card;
        context.game.broadcastEvent(event);
      }
    }
    return piles.get(name);
  }

  CardPile addPileDebtTokens(Card card, int num, MoveContext context) {
    card = getPile(card).placeholderCard();
    String name = card.getName();
    pileDebtTokens.put(name, getPileDebtTokens(card) + num);
    if (context != null) {
      GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensPutOnPile, context);
      event.setAmount(num);
      event.card = card;
      context.game.broadcastEvent(event);
    }
    return piles.get(name);
  }

  CardPile removePileDebtTokens(Card card, int num, MoveContext context) {
    card = getPile(card).placeholderCard();
    num = Math.min(num, getPileDebtTokens(card));
    String name = card.getName();
    if (num > 0) {
      pileDebtTokens.put(name, getPileDebtTokens(card) - num);
      if (context != null) {
        GameEvent event = new GameEvent(GameEvent.EventType.DebtTokensTakenFromPile, context);
        event.setAmount(num);
        event.card = card;
        context.game.broadcastEvent(event);
      }
    }
    return piles.get(name);
  }


  public int getPileVpTokens(Card card) {
    if (Cards.isBlackMarketCard(card)) {
      return 0;
    }
    if (getPile(card) != null)
    card = getPile(card).placeholderCard();

    Integer count = pileVpTokens.get(card.getName());
    return (count == null) ? 0 : count;
  }

  public int getPileDebtTokens(Card card) {
    card = getPile(card).placeholderCard();
    Integer count = pileDebtTokens.get(card.getName());
    return (count == null) ? 0 : count;
  }

  public int getPileTradeRouteTokens(Card card) {
    return piles.get(card.getName()).hasTradeRouteToken() ? 1 : 0;
  }

  public List<PlayerSupplyToken> getPlayerSupplyTokens(Card card, Player player) {
    card = card.getTemplateCard();
    if (player == null || !playerSupplyTokens.containsKey(card.getName()))
    return new ArrayList<PlayerSupplyToken>();

    if (!playerSupplyTokens.get(card.getName()).containsKey(player)) {
      playerSupplyTokens.get(card.getName()).put(player, new ArrayList<PlayerSupplyToken>());
    }
    return playerSupplyTokens.get(card.getName()).get(player);
  }

  public boolean isPlayerSupplyTokenOnPile(Card card, Player player, PlayerSupplyToken token) {
    return getPlayerSupplyTokens(card, player).contains(token);
  }

  public void movePlayerSupplyToken(Card card, Player player, PlayerSupplyToken token) {
    removePlayerSupplyToken(player, token);
    getPlayerSupplyTokens(card, player).add(token);

    MoveContext context = new MoveContext(this, player);
    GameEvent.EventType eventType = null;
    if (token == PlayerSupplyToken.PlusOneCard) {
      eventType = EventType.PlusOneCardTokenMoved;
    } else if (token == PlayerSupplyToken.PlusOneAction) {
      eventType = EventType.PlusOneActionTokenMoved;
    } else if (token == PlayerSupplyToken.PlusOneBuy) {
      eventType = EventType.PlusOneBuyTokenMoved;
    } else if (token == PlayerSupplyToken.PlusOneCoin) {
      eventType = EventType.PlusOneCoinTokenMoved;
    } else if (token == PlayerSupplyToken.MinusTwoCost) {
      eventType = EventType.MinusTwoCostTokenMoved;
    } else if (token == PlayerSupplyToken.Trashing) {
      eventType = EventType.TrashingTokenMoved;
    }
    GameEvent event = new GameEvent(eventType, context);
    event.card = card;
    broadcastEvent(event);
  }

  protected void removePlayerSupplyToken(Player player, PlayerSupplyToken token) {
    for (String cardName : playerSupplyTokens.keySet()) {
      if (playerSupplyTokens.get(cardName).containsKey(player)) {
        playerSupplyTokens.get(cardName).get(player).remove(token);
      }
    }
  }

  public Card takeFromPile(Card card) {
    return takeFromPile(card, null);
  }

  public Card takeFromPile(Card card, MoveContext context) {
    CardPile pile = getPile(card);
    if (pile == null || pile.getCount() <= 0) {
      return null;
    }
    tradeRouteValue += pile.takeTradeRouteToken();
    return pile.removeCard();
  }

  /*
  ** takeFromPileCheckTrader - Takes <cardToGain> from Supply Pile, as long as the
  ** player doens't have a Trader (card) in hand to counter-act a bad <cardToGain>
  */
  public Card takeFromPileCheckTrader(Card cardToGain, MoveContext context) {

    // If the pile was specified instead of a card, take the top card from that pile.
    if (cardToGain.isPlaceholderCard() || cardToGain.isTemplateCard()) {
      cardToGain = getPile(cardToGain).topCard();

    // If the desired card is not on top of the pile, don't take the card
    } else if (!isCardOnTop(cardToGain)) {
      return null;
    }

    // Check if Trader (card) should be used to pick up a Silver instead
    boolean hasInheritedTrader = Cards.trader.equals(context.getPlayer().getInheritance()) && context.getPlayer().hand.contains(Cards.estate);
    boolean hasTrader = context.getPlayer().hand.contains(Cards.trader);
    Card traderCard = hasTrader ? Cards.trader : Cards.estate;
    if (!isPileEmpty(cardToGain) && (hasTrader || hasInheritedTrader) && !cardToGain.equals(Cards.silver)) {
      if (context.player.controlPlayer.trader_shouldGainSilverInstead((MoveContext) context, cardToGain)) {
        cardToGain = Cards.silver;
        context.player.reveal(traderCard, null, context);
      }
    }

    // Picks up the Card
    return takeFromPile(cardToGain, context);
  }


  /*
  ** pileSize - Returns the size of the <card> pile
  */
  public int pileSize(Card card) {
    CardPile pile = getPile(card);
    if (pile == null) {
      return -1;
    }
    return pile.getCount();
  }


  /*
  ** isPileEmpty - Returns True if the <card> pile is empty
  */
  public boolean isPileEmpty(Card card) {
    return pileSize(card) <= 0;
  }


  /*
  ** emptyPiles - Returns the number of empty Supply Piles
  */
  public int emptyPiles() {
    int emptyPiles = 0;
    ArrayList<CardPile> alreadyCounted = new ArrayList<CardPile>();
    for (CardPile pile : piles.values()) {
      if (pile.getCount() <= 0 && pile.isSupply() && !alreadyCounted.contains(pile)) {
        emptyPiles++;
        alreadyCounted.add(pile);
      }
    }
    return emptyPiles;
  }


  public Card[] getCardsInGame(GetCardsInGameOptions opt) {
    return getCardsInGame(opt, false);
  }

  public Card[] getCardsInGame(GetCardsInGameOptions opt, boolean supplyOnly) {
    return getCardsInGame(opt, supplyOnly, null);
  }

  public Card[] getCardsInGame(GetCardsInGameOptions opt, boolean supplyOnly, Type type) {
    ArrayList<Card> cards = new ArrayList<Card>();
    for (CardPile pile : piles.values()) {

      if (supplyOnly && !pile.isSupply) continue;

      if (opt == GetCardsInGameOptions.All || opt == GetCardsInGameOptions.Placeholders) {
        if ((type == null || pile.placeholderCard().is(type, null))
        && !cards.contains(pile.placeholderCard()))
        cards.add(pile.placeholderCard());
      }
      if (opt == GetCardsInGameOptions.All || opt == GetCardsInGameOptions.Templates) {
        for (Card c : pile.getTemplateCards()) {
          if ((type == null || c.is(type, null))
          && !cards.contains(c)) {
            cards.add(c);
          }
        }
      }
      if (opt == GetCardsInGameOptions.TopOfPiles) {
        if (pile.topCard() != null && (type == null || pile.topCard().is(type))
        && !cards.contains(pile.topCard())) {
          cards.add(pile.topCard());
        }
      }
      if (opt == GetCardsInGameOptions.Buyables) {
        if (pile.topCard() != null && (type == null || pile.topCard().is(type))
        && !cards.contains(pile.topCard()) && (pile.isSupply() || pile.topCard().is(Type.Event))) {
          cards.add(pile.topCard());
        }
      }
    }
    return cards.toArray(new Card[0]);
  }

  public boolean cardInGame(Card c) {
    for (CardPile pile : piles.values()) {
      if (c.equals(pile.placeholderCard())) {
        return true;
      }
      for (Card template : pile.getTemplateCards()) {
        if (c.equals(template)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isCardOnTop(Card card) {
    CardPile pile = getPile(card);
    if (pile == null) return false;
    Card top = pile.topCard();
    return top != null && top.equals(card);
  }

  public boolean pileInGame(CardPile p) {
    for (CardPile pile : piles.values()) {
      if (pile.equals(p)) {
        return true;
      }
    }
    return false;
  }

  public boolean isPlatInGame() {
    return cardInGame(Cards.platinum);
  }

  public boolean isColonyInGame() {
    return cardInGame(Cards.colony);
  }

  public int getCardsLeftInPile(Card card) {
    CardPile pile = getPile(card);
    if (pile == null || pile.getCount() < 0) {
      return 0;
    }
    return pile.getCount();
  }

  public ArrayList<Card> GetTrashPile() {
    return trashPile;
  }

  public ArrayList<Card> GetBlackMarketPile() {
    return blackMarketPile;
  }

  public CardPile addPile(Card card) {
    boolean isSupply = true;
    int count = kingdomCardPileSize;
    if (card.is(Type.Victory)) count = victoryCardPileSize;
    if (card.equals(Cards.rats)) count = 20;
    if (card.equals(Cards.port)) count = 12;
    if (card.is(Type.Event) || card.is(Type.Landmark)) {
      count = 1;
      isSupply = false;
    }
    return addPile(card, count, isSupply);
  }

  public CardPile addPile(Card card, int count) {
    return addPile(card, count, true);
  }

  public CardPile addPile(Card card, int count, boolean isSupply) {
    return addPile(card, count, isSupply, false);
  }

  public CardPile addPile(Card card, int count, boolean isSupply, boolean isBlackMarket) {
    CardPile pile = card.getPileCreator().create(card, count);

    if (!isSupply) {
      pile.notInSupply();
    }
    if (isBlackMarket) {
      pile.inBlackMarket();
    }

    piles.put(card.getName(), pile);
    placeholderPiles.put(card.getName(), pile);
    HashMap<Player, List<PlayerSupplyToken>> tokenMap = new HashMap<Player, List<PlayerSupplyToken>>();
    playerSupplyTokens.put(card.getName(), tokenMap);

    // Add the to the list for each templateCard used (this replaces addLinkedPile)
    // Also add the an entry for each templateCardName to the playerSupplyTokens because at some places in the code
    // the token is checked with the actual card and not the placeholder.

    for (Card templateCard : pile.getTemplateCards()) {
      if (!piles.containsKey(templateCard.getName())) {
        piles.put(templateCard.getName(), pile);
      }
      if (!playerSupplyTokens.containsKey(templateCard.getName())) {
        playerSupplyTokens.put(templateCard.getName(), tokenMap);
      }
    }

    return pile;
  }

  protected ArrayList<Card> getCardsObtainedByPlayer(int PlayerNumber) {
    return cardsObtainedLastTurn[PlayerNumber];
  }

  public ArrayList<Card> getCardsObtainedByPlayer() {
    return getCardsObtainedByPlayer(playersTurn);
  }

  public ArrayList<Card> getCardsObtainedByLastPlayer() {
    int playerOnRight = playersTurn - 1;
    if (playerOnRight < 0) {
      playerOnRight = numPlayers - 1;
    }
    return getCardsObtainedByPlayer(playerOnRight);
  }

  /*
  ** broadcastEvent - Broadcasts the GameEvent to all Listeners
  */
  public void broadcastEvent(GameEvent event) {
    // Notify all GameEventListeners
    for (GameEventListener listener : listeners) {
      listener.gameEvent(event);
    }
    // Notify this class' listener last for proper action/logging order
    if (gameListener != null) {
      gameListener.gameEvent(event);
    }
  }


  // getHandString - Returns a string of Card Names in a player's hand
  String getHandString(Player player) {
    String handString = null;
    Card[] hand = player.getHand().toArray();
    Arrays.sort(hand, new CardCostComparator());
    for (Card card : hand) {
      if (card == null) {
        continue;
      }
      if (handString == null) {
        handString = card.getName();
      } else {
        handString += ", " + card.getName();
      }
    }

    return handString;
  }


  /*
  ** playerShouldSelectCoinsToPlay - Returns True if player should select Treasures to play
  */
  public boolean playerShouldSelectCoinsToPlay(MoveContext context, CardList cards) {
    if (!quickPlay) {
      return true;
    }

    if (cards == null) {
      return false;
    }

    CardPile grandMarket = getPile(Cards.grandMarket);
    for (Card card : cards) {
      if (
      card.equals(Cards.philosophersStone) ||
      card.equals(Cards.bank) ||
      card.equals(Cards.contraband) ||
      card.equals(Cards.loan) ||
      card.equals(Cards.quarry) ||
      card.equals(Cards.talisman) ||
      card.equals(Cards.hornOfPlenty) ||
      card.equals(Cards.diadem) ||
      (card.equals(Cards.copper) && grandMarket != null && grandMarket.getCount() > 0)
      ) {
        return true;
      }
    }

    return false;
  }


  public CardPile getPile(Card card) {
    if (card == null) return null;
    return piles.get(card.getName());
  }

  public CardPile getGamePile(Card card) {
    return getPile(card);
  }

  public boolean cardsInSamePile(Card first, Card second) {
    return getPile(first).equals(getPile(second));
  }



  /***************************************
  ** SECTION 4: GAME END & STATS CALCULATION FUNCTIONS
  ***************************************/


  /*
  ** gameOver - Determines Game's Winner and Broadcasts "Game Over" Event
  */
  public int[] gameOver(HashMap<String, Double> gameTypeSpecificWins) {

    if (Util.debug_on) {
      printPlayerTurn();
    }

    int[] vps = calculateVps();

    for (int i = 0; i < numPlayers; i++) {
      int tieCount = 0;
      boolean loss = false;
      for (int j = 0; j < numPlayers; j++) {
        if (i == j) {
          continue;
        }
        if (vps[i] < vps[j]) {
          loss = true;
          break;
        }
        if (vps[i] == vps[j]) {
          tieCount++;
        }
      }

      if (!loss) {
        String s = players[i].getPlayerName();
        double num = gameTypeSpecificWins.get(players[i].getPlayerName());
        Double overall = overallWins.get(players[i].getPlayerName());
        boolean trackOverall = (overall != null);
        if (tieCount == 0) {
          num += 1.0;
          if (trackOverall) {
            overall += 1.0;
          }
        } else {
          num += 1.0 / (tieCount + 1);
          if (trackOverall) {
            overall += 1.0 / (tieCount + 1);
          }
        }
        gameTypeSpecificWins.put(players[i].getPlayerName(), num);
        if (trackOverall) {
          overallWins.put(players[i].getPlayerName(), overall);
        }
      }

      Player player = players[i];
      player.vps = vps[i];
      player.win = !loss;
      MoveContext context = new MoveContext(this, player);
      broadcastEvent(new GameEvent(GameEvent.EventType.GameOver, context));

    }
    int index = 0;
    for (Player player : players) {
      int vp = vps[index++];
      Util.debug(player.getPlayerName() + ": Victory Points = " + vp, true);
      GameEvent event = new GameEvent(GameEvent.EventType.VictoryPoints, null);
      event.setPlayer(player);
      event.setComment(":" + vp);
      broadcastEvent(event);
    }
    return vps;

  }


  /*
  ** printStats - Prints Results of Playing Games
  */
  protected void printStats(HashMap<String, Double> wins, int gameCount, String gameType) {

    double totalGameCount = 0;
    Iterator<Entry<String, Double>> it = wins.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, Double> e = it.next();
      totalGameCount += e.getValue();
    }
    gameCount = (int) totalGameCount;

    StringBuilder sb = new StringBuilder();

    String s = gameType + ": ";

    String start = "" + gameCount;

    if (gameCount > 1) {
      s = start + (gameType.equals("Types") ? " types " : " games ") + s;
    }

    if (!Util.debug_on) {
      while (s.length() < 30) {
        s += " ";
      }
    }
    sb.append(s);
    String winner = null;
    double high = 0.0;
    Iterator<String> keyIter = wins.keySet().iterator();

    while (keyIter.hasNext()) {
      String playerName = keyIter.next();
      double num = wins.get(playerName);
      double val = Math.round((num * 100 / gameCount));
      if (val > high) {
        high = val;
        winner = playerName;
      }
    }

    keyIter = wins.keySet().iterator();
    while (keyIter.hasNext()) {
      String playerName = keyIter.next();
      double num = wins.get(playerName);
      double val = Math.round((num * 100 / gameCount));
      String numStr = "" + (int) val;
      while (numStr.length() < 3) {
        numStr += " ";
      }
      sb.append(" ");
      if (playerName.equals(winner)) {
        sb.append("*");
      } else {
        sb.append(" ");
      }
      sb.append(playerName + " " + numStr + "%");
    }

    Util.log(sb.toString());
  }


  /*
  ** printGameTypeStats - Prints the summary stats of each GameType played
  */
  protected void printGameTypeStats() {
    for (int i = 0; i < gameTypeStats.size(); i++) {
      GameStats stats = gameTypeStats.get(i);
      StringBuilder sb = new StringBuilder();
      sb.append(stats.gameType);
      if (stats.gameType.toString().length() < 8) {
        sb.append("\t");
      }
      if (stats.gameType.toString().length() < 16) {
        sb.append("\t");
      }
      sb.append("Avgs: " +
      "VP:" + stats.aveVictoryPoints + ", " +
      "# Cards:" + stats.aveNumCards + ", " +
      "# Turns:" + stats.aveTurns + ", " +
      "Card Set={" + Util.cardArrayToString(stats.cards) + "}");
      Util.log(sb.toString());
    }
  }


  // calculateVps - Returns each player's total Victory Points sum
  protected int[] calculateVps() {
    int[] vps = new int[numPlayers];
    for (int i = 0; i < players.length; i++) {
      vps[i] = players[i].getVPs();
    }
    return vps;
  }

  public boolean buyWouldEndGame(Card card) {
    if (isColonyInGame() && card.equals(Cards.colony)) {
      if (pileSize(card) <= 1) {
        return true;
      }
    }

    if (card.equals(Cards.province)) {
      if (pileSize(card) <= 1) {
        return true;
      }
    }

    if (emptyPiles() >= 2 && pileSize(card) <= 1) {
      return true;
    }

    return false;
  }

  protected boolean checkGameOver() {
    if (isColonyInGame() && isPileEmpty(Cards.colony)) {
      return true;
    }

    if (isPileEmpty(Cards.province)) {
      return true;
    }

    switch (numPlayers) {
      case 1:
      case 2:
      case 3:
      case 4:
      /* Ends game for 1, 2, 3 or 4 players */
      if (emptyPiles() >= 3) {
        return true;
      }
      break;
      case 5:
      case 6:
      /* Ends game for 5 or 6 players */
      if (emptyPiles() >= 4) {
        return true;
      }
    }

    return false;
  }


  /***************************************
  ** SECTION 5: CARD-SPECIFIC FUNCTIONS
  ***************************************/

  /*
  ** isModifierCard - Returns True if the card is a Modifier
  */
  public static boolean isModifierCard(Card card) {
    return card.equals(Cards.throneRoom) ||
           card.equals(Cards.disciple) ||
           card.equals(Cards.kingsCourt) ||
           card.equals(Cards.procession) ||
           card.equals(Cards.royalCarriage) ||
           card.equals(Cards.crown);
  }

  /*
  ** haggler - Implements Haggler (from Hinterlands
  */
  protected void haggler(MoveContext context, Card cardBought) {
    if (!context.game.piles.containsKey(Cards.haggler.getName()))
    return;
    int hagglers = context.countCardsInPlay(Cards.haggler);

    int cost = cardBought.getCost(context);
    int debt = cardBought.getDebtCost(context);
    boolean potion = cardBought.costPotion();
    int potionCost = potion ? 1 : 0;
    List<Card> validCards = new ArrayList<Card>();

    for (int i = 0; i < hagglers; i++) {
      validCards.clear();
      for (Card card : getCardsInGame(GetCardsInGameOptions.TopOfPiles, true)) {
        if (!(card.is(Type.Victory)) && Cards.isSupplyCard(card) && isCardOnTop(card)) {
          int gainCardCost = card.getCost(context);
          int gainCardPotionCost = card.costPotion() ? 1 : 0;
          int gainCardDebt = card.getDebtCost(context);

          if ((gainCardCost < cost || gainCardDebt < debt || gainCardPotionCost < potionCost) &&
          (gainCardCost <= cost && gainCardDebt <= debt && gainCardPotionCost <= potionCost)) {
            validCards.add(card);
          }
        }
      }

      if (validCards.size() > 0) {
        Card toGain = context.getPlayer().controlPlayer.haggler_cardToObtain(context, cost, debt, potion);
        if (toGain != null) {
          if (!validCards.contains(toGain)) {
            Util.playerError(context.getPlayer(), "Invalid card returned from Haggler, ignoring.");
          } else {
            context.getPlayer().gainNewCard(toGain, Cards.haggler, context);
          }
        }
      }
    }

  }

  /*
  ** charmWhenBuy - Implements Charmed
  */
  protected void charmWhenBuy(MoveContext context, Card buy) {
    Player player = context.getPlayer();
    if (context.charmsNextBuy > 0) {
      //is there another valid card to gain?
      boolean validCard = validCharmCardLeft(context, buy);
      while (context.charmsNextBuy-- > 0) {
        if (validCard) {
          Card toGain = player.controlPlayer.charm_cardToObtain(context, buy);
          if (toGain != null) {
            if (!isValidCharmCard(context, buy, toGain)) {
              Util.playerError(player, "Charm card to gain invalid, ignoring");
            } else {
              player.gainNewCard(toGain, Cards.charm, context);
            }
          }
          validCard = validCharmCardLeft(context, buy);
        }
      }
    }
  }

  /*
  ** validCharmCardLeft - Implements Charmed
  */
  protected boolean validCharmCardLeft(MoveContext context, Card buy) {
    for (Card c : context.game.getCardsInGame(GetCardsInGameOptions.TopOfPiles, true)) {
      if (isValidCharmCard(context, buy, c)) {
        return true;
      }
    }
    return false;
  }

  /*
  ** isValidCharmCard - Implements Charmed
  */
  protected boolean isValidCharmCard(MoveContext context, Card buy, Card c) {
    return !buy.equals(c) && context.game.isCardOnTop(c) &&
    !context.game.isPileEmpty(c) &&
    Cards.isSupplyCard(c) &&
    buy.getCost(context) == c.getCost(context) &&
    buy.getDebtCost(context) == c.getDebtCost(context) &&
    (buy.costPotion() == c.costPotion());
  }

  /*
  ** arena - Implements Arena Landmark
  */
  protected void arena(Player player, MoveContext context) {
    boolean hasAction = false;
    for (Card c : player.getHand()) {
      if (c.is(Type.Action, player)) {
        hasAction = true;
        break;
      }
    }
    if (!hasAction) return;
    Card toDiscard = player.controlPlayer.arena_cardToDiscard(context);
    if (toDiscard != null && (!player.getHand().contains(toDiscard) || toDiscard.is(Type.Action, player))) {
      Util.playerError(player, "Arena - invalid card specified, ignoring.");
    }
    if (toDiscard == null) return;
    player.discard(player.getHand().remove(player.getHand().indexOf(toDiscard)), Cards.arena, context);
    int tokensToTake = Math.min(getPileVpTokens(Cards.arena), 2);
    removePileVpTokens(Cards.arena, tokensToTake, context);
    player.addVictoryTokens(context, tokensToTake, Cards.arena);
  }


  /*
  ** basilicaWhenBuy - Implements Basilica Landmark
  */
  protected void basilicaWhenBuy(MoveContext context) {
    //TODO?: Can resolve Basilica before overpay to not get tokens in some cases (would matter with old Possession rules)
    if (cardInGame(Cards.basilica) && (context.getCoins() + context.overpayAmount) >= 2) {
      int tokensLeft = getPileVpTokens(Cards.basilica);
      if (tokensLeft > 0) {
        int tokensToTake = Math.min(tokensLeft, 2);
        removePileVpTokens(Cards.basilica, tokensToTake, context);
        context.getPlayer().addVictoryTokens(context, tokensToTake, Cards.basilica);
      }
    }
  }

  /*
  ** colonnadeWhenBuy - Implements Colonnade Landmark
  */
  protected void colonnadeWhenBuy(MoveContext context, Card buy) {
    if (buy.is(Type.Action, context.getPlayer())) {
      if (cardInGame(Cards.colonnade)) {
        Player player = context.getPlayer();
        if (player.playedCards.contains(buy) || player.nextTurnCards.contains(buy)) {
          int tokensLeft = getPileVpTokens(Cards.colonnade);
          if (tokensLeft > 0) {
            int tokensToTake = Math.min(tokensLeft, 2);
            removePileVpTokens(Cards.colonnade, tokensToTake, context);
            player.addVictoryTokens(context, tokensToTake, Cards.colonnade);
          }
        }
      }
    }
  }

  /*
  ** defiledShrineWhenBuy - Implements Defiled Shrine Landmark
  */
  protected void defiledShrineWhenBuy(MoveContext context, Card buy) {
    //TODO?: Can resolve Basilica before overpay to not get tokens in some cases (would matter with old Possession rules)
    if (buy.equals(Cards.curse)) {
      if (cardInGame(Cards.defiledShrine)) {
        int tokensLeft = getPileVpTokens(Cards.defiledShrine);
        if (tokensLeft > 0) {
          removePileVpTokens(Cards.defiledShrine, tokensLeft, context);
          context.getPlayer().addVictoryTokens(context, tokensLeft, Cards.defiledShrine);
        }
      }
    }
  }


  /*
  ** hasLighthouse - Return True if player has a Lighthouse, else returns False
  */
  boolean hasLighthouse(Player player) {
    for (Card card : player.nextTurnCards) {
      if (card.behaveAsCard().equals(Cards.lighthouse) && !((CardImpl) card).trashAfterPlay)
      return true;
    }
    return false;
  }


  /*
  ** countChampionsInPlay - Returns the number of Champion cards in play
  */
  int countChampionsInPlay(Player player) {
    int count = 0;
    for (Card card : player.nextTurnCards) {
      if (card.behaveAsCard().equals(Cards.champion))
      count += ((CardImpl) card).cloneCount;
    }
    return count;
  }


  /*
  Note that any cards in the supply can have Embargo coins added.
  This includes the basic seven cards (Victory, Curse, Treasure),
  any of the 10 game piles, and Colony/Platinum when included.
  However, this does NOT include any Prizes from Cornucopia.
  */

  CardPile addEmbargo(Card card) {
    if (isValidEmbargoPile(card)) {
      String name = card.getName();
      embargos.put(name, getEmbargos(card) + 1);
      return piles.get(name);
    }
    return null;
  }

  public boolean isValidEmbargoPile(Card card) {
    return !(card == null || !pileInGame(getPile(card)) || !Cards.isSupplyCard(card));
  }

  public int getEmbargos(Card card) {
    Integer count = embargos.get(getPile(card).placeholderCard().getName());
    return (count == null) ? 0 : count;
  }


  /*
  ** trashHovelsInHandOption - Implements Hovel Trashing
  */
  public void trashHovelsInHandOption(Player player, MoveContext context, Card responsible) {
    // If player has a Hovel (or multiple Hovels), offer the option to trash...
    ArrayList<Card> hovelsToTrash = new ArrayList<Card>();

    for (Card c : player.hand) {
      if (c.getKind() == Cards.Kind.Hovel && player.controlPlayer.hovel_shouldTrash(context)) {
        hovelsToTrash.add(c);
      }
    }

    if (hovelsToTrash.size() > 0) {
      for (Card c : hovelsToTrash) {
        player.hand.remove(c);
        player.trash(c, responsible, context);
      }
    }
  }

  /*
  ** hauntedWoodsAttacks - Implements Haunted Woods
  */
  public boolean hauntedWoodsAttacks(Player player) {
    for (Player otherPlayer : players) {
      if (otherPlayer != null && otherPlayer != player) {
        if (otherPlayer.getDurationEffectsOnOtherPlayer(player, Cards.Kind.HauntedWoods) > 0) {
          return true;
        }
      }
    }
    return false;
  }

  /*
  ** enchantressAttacks - Implements Enchantress
  */
  public boolean enchantressAttacks(Player player) {
    if (getCurrentPlayer() != player) return false;
    for (Player otherPlayer : players) {
      if (otherPlayer != null && otherPlayer != player) {
        if (otherPlayer.getDurationEffectsOnOtherPlayer(player, Cards.Kind.Enchantress) > 0) {
          return true;
        }
      }
    }
    return false;
  }

  /*
  ** swampHagAttacks - Implements Swamp Hag
  */
  public int swampHagAttacks(Player player) {
    int swampHags = 0;
    for (Player otherPlayer : players) {
      if (otherPlayer != null && otherPlayer != player) {
        swampHags += otherPlayer.getDurationEffectsOnOtherPlayer(player, Cards.Kind.SwampHag);
      }
    }
    return swampHags;
  }


  /***************************************
  ** SECTION 6: GAME CLONING FUNCTIONS
  ***************************************/

  @SuppressWarnings("unchecked")
  public Game cloneGame() {

    Game clone = new Game();

    clone.numGames   = numGames;
    clone.numPlayers = numPlayers;

    // Initialize GameListener
    clone.initGameListener();

    // Clone Game's Players and Listeners
    clone.players = new BasePlayer[numPlayers];
    clone.listeners = new ArrayList<GameEventListener>();
    for (int i = 0; i < numPlayers; i++) {
      clone.players[i] = ((BasePlayer) players[i]).clone(clone);
      clone.listeners.add((GameEventListener)players[i]);
    }

    // Update Cloned Game's Parameters
    clone.gameType                   = gameType;
    clone.randomExpansions           = (randomExpansions == null) ? null : new ArrayList(randomExpansions);
    clone.randomExcludedExpansions   = (randomExcludedExpansions == null) ? null : new ArrayList(randomExcludedExpansions);
    clone.cardsSpecifiedAtLaunch     = cardsSpecifiedAtLaunch;
    clone.unfoundCards               = new ArrayList<String>(unfoundCards);
    clone.cardListText               = cardListText;
    clone.unfoundCardText            = unfoundCardText;

    // Clone Expansion-specific Options
    clone.platColonyNotPassedIn      = platColonyNotPassedIn;
    clone.platColonyPassedIn         = platColonyPassedIn;
    clone.chanceForPlatColony        = chanceForPlatColony;
    clone.sheltersNotPassedIn        = sheltersNotPassedIn;
    clone.sheltersPassedIn           = sheltersPassedIn;
    clone.chanceForShelters          = chanceForShelters;
    clone.randomIncludesEvents       = randomIncludesEvents;
    clone.randomIncludesLandmarks    = randomIncludesLandmarks;
    clone.numRandomEvents            = numRandomEvents;
    clone.numRandomLandmarks         = numRandomLandmarks;
    clone.splitMaxEventsAndLandmarks = splitMaxEventsAndLandmarks;
    clone.blackMarketOnlyCardsFromUsedExpansions = blackMarketOnlyCardsFromUsedExpansions;
    clone.blackMarketSplitPileOptions = blackMarketSplitPileOptions;

    // Clone General Game Options
    clone.quickPlay                  = quickPlay;
    clone.actionChains               = actionChains;
    clone.equalStartHands            = equalStartHands;
    clone.maskPlayerNames            = maskPlayerNames;

    // Clone Turn Info
    clone.playersTurn                = playersTurn;
    clone.gameTurnCount              = gameTurnCount;
    clone.consecutiveTurnCounter     = consecutiveTurnCounter;
    clone.cardsObtainedLastTurn      = cardsObtainedLastTurn;

    // Clone Game Card Piles
    clone.piles = new HashMap<String, CardPile>();
    for (String key : piles.keySet()) {
      clone.piles.put(key, piles.get(key).clone());
    }

    clone.placeholderPiles = new HashMap<String, CardPile>();
    for (String key : placeholderPiles.keySet()) {
      clone.placeholderPiles.put(key, placeholderPiles.get(key).clone());
    }

    clone.trashPile = new ArrayList<Card>();
    for (Card card : trashPile) { clone.trashPile.add(card.clone()); }

    clone.possessedTrashPile = new ArrayList<Card>();
    for (Card card : possessedTrashPile) { clone.possessedTrashPile.add(card.clone()); }

    clone.possessedBoughtPile = new ArrayList<Card>();
    for (Card card : possessedBoughtPile) { clone.possessedBoughtPile.add(card.clone()); }

    clone.blackMarketPile = new ArrayList<Card>();
    for (Card card : blackMarketPile) { clone.blackMarketPile.add(card.clone()); }

    clone.blackMarketPileShuffled = new ArrayList<Card>();
    for (Card card : blackMarketPileShuffled) { clone.blackMarketPileShuffled.add(card.clone()); }

    // Clone Game Tokens
    clone.embargos = new HashMap<String, Integer>();
    for (String key : embargos.keySet()) {
      clone.embargos.put(key, embargos.get(key));
    }

    clone.pileVpTokens = new HashMap<String, Integer>();
    for (String key : pileVpTokens.keySet()) {
      clone.pileVpTokens.put(key, pileVpTokens.get(key));
    }

    clone.pileDebtTokens = new HashMap<String, Integer>();
    for (String key : pileDebtTokens.keySet()) {
      clone.pileDebtTokens.put(key, pileDebtTokens.get(key));
    }

    // Clone Adventures Tokens
    clone.playerSupplyTokens = new HashMap<String, HashMap<Player, List<PlayerSupplyToken>>>();
    for (String key : playerSupplyTokens.keySet()) {
      HashMap playerToTokens = new HashMap<Player, List<PlayerSupplyToken>>();
      for (int i = 0; i < 2; i++) {
        if (playerSupplyTokens.get(key).containsKey(players[i])) {
          playerToTokens.put(clone.players[i], new ArrayList(playerSupplyTokens.get(key).get(players[i])));
        }
      }
      clone.playerSupplyTokens.put(key, playerToTokens);
    }

    // Possession is NOT implemented in this version of clone()
    clone.possessionsToProcess     = 0;
    clone.nextPossessionsToProcess = 0;
    clone.possessingPlayer         = null;
    clone.nextPossessingPlayer     = null;

    // Clone Special Card-Specific Game Values
    clone.tradeRouteValue             = tradeRouteValue;
    clone.sheltersInPlay              = sheltersInPlay;
    clone.bakerInPlay                 = bakerInPlay;
    clone.journeyTokenInPlay          = journeyTokenInPlay;
    clone.firstProvinceWasGained      = firstProvinceWasGained;
    clone.doMountainPassAfterThisTurn = doMountainPassAfterThisTurn;
    clone.firstProvinceGainedBy       = firstProvinceGainedBy;

    // Clone Special Cards
    if (baneCard != null) { clone.baneCard = baneCard.clone(); }
    if (obeliskCard != null) { clone.obeliskCard = obeliskCard.clone(); }

    // Don't Clone Stats Trackers
    overallWins   = new HashMap<String, Double>();  // Don't need to clone
    gameTypeStats = new ArrayList<GameStats>();     // Don't need to clone

    return clone;

  }

}
