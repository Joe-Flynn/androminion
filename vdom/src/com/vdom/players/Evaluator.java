package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

import java.util.ArrayList;

public class Evaluator {

  protected Player player;

  // Action Phase Evaluator Tunable Parameters
  protected double coinFactor          =  1.0;
  protected double potionFactor        =  0.5;
  protected double threeCostGainFactor =  1.0;
  protected double fourCostGainFactor  =  0.0;
  protected double fiveCostGainFactor  =  1.25;
  protected double coinTokenFactor     =  1.0;
  protected double debtTokenFactor     = -1.0;
  protected double victoryTokenFactor  =  1.0;
  protected double enemyHandSizeFactor = -1.0;

  // Buy Phase Evaluator Tunable Parameters
  protected double treasureDeltaFactor =  1.0;
  protected double actionDeltaFactor   = -1.0;
  protected double victoryPointFactor  =  0.17;

  // Buy Phase Evaluator-Compared-To-Plan Parameters
  protected double planEvalActionMultiplier   = 8.0;
  protected double planEvalTreasureMultiplier = 1.0;
  protected double planEvalVictoryPointFactor = 0.17;

  protected boolean isDefaultEvaluator = true;

  // DEFAULT Constructor
  public Evaluator(Player player) {
    this.player = player;
    this.isDefaultEvaluator = true;
  }

  // TUNABLE Constructor (i.e. For Machine Learning)
  public Evaluator(Player player, double coinFactor, double potionFactor, double threeCostGainFactor,
                   double fourCostGainFactor, double fiveCostGainFactor, double coinTokenFactor,
                   double debtTokenFactor, double victoryTokenFactor, double enemyHandSizeFactor,
                   double treasureDeltaFactor, double actionDeltaFactor, double victoryPointFactor,
                   double planEvalActionMultiplier, double planEvalTreasureMultiplier,
                   double planEvalVictoryPointFactor) {

    this.player = player;
    this.isDefaultEvaluator = false;

    // ACTION PHASE Parameters (PHIL)
    this.coinFactor          = coinFactor;
    this.potionFactor        = potionFactor;
    this.threeCostGainFactor = threeCostGainFactor;
    this.fourCostGainFactor  = fourCostGainFactor;
    this.fiveCostGainFactor  = fiveCostGainFactor;
    this.coinTokenFactor     = coinTokenFactor;
    this.debtTokenFactor     = debtTokenFactor;
    this.victoryTokenFactor  = victoryTokenFactor;
    this.enemyHandSizeFactor = enemyHandSizeFactor;

    // BUY PHASE Parameters (PHIL)
    this.treasureDeltaFactor = treasureDeltaFactor;
    this.actionDeltaFactor   = actionDeltaFactor;
    this.victoryPointFactor  = victoryPointFactor;

    // BUY PHASE Parameters (JARVIS), i.e. These compare to the Player's Plan
    this.planEvalActionMultiplier   = planEvalActionMultiplier;
    this.planEvalTreasureMultiplier = planEvalTreasureMultiplier;
    this.planEvalVictoryPointFactor = planEvalVictoryPointFactor;

  }

  /*
  ** evaluateActionPhase - Evaluates Action Phase "Turn Economy", which is based
  ** on the buyability / gainability of the MoveContext after playing several actions.
  */
  public double evaluateActionPhase(MoveContext context) {

    int coin = getCoinEstimate(context);

    int buyFactor = 8;
    if (context.game.isPlatInGame()) { buyFactor = 11; }

    int usableCoin       = Math.min(coin, context.getBuysLeft() * buyFactor);
    int potionGains      = Math.min(context.getPotions(), Math.min(context.getBuysLeft(), coin / 3));
    int threeCostGains   = Math.min(coin / 3, context.getBuysLeft());
    int fourCostGains    = Math.min(coin / 4, context.getBuysLeft());
    int fiveCostGains    = Math.min(coin / 5, context.getBuysLeft());
    int numCoinTokens    = context.player.getGuildsCoinTokenCount();
    int numDebtTokens    = context.player.getDebtTokenCount();
    int numVictoryTokens = context.player.getVictoryTokens();
    int enemyHandSize    = context.getOpponent().getHand().size();

    double turnEconomy = (usableCoin * coinFactor) +
                         (potionGains * potionFactor) +
                         (threeCostGains * threeCostGainFactor) +
                         (fourCostGains * fourCostGainFactor) +
                         (fiveCostGains * fiveCostGainFactor) +
                         (numCoinTokens * coinTokenFactor) +
                         (numDebtTokens * debtTokenFactor) +
                         (numVictoryTokens * victoryTokenFactor) +
                         (enemyHandSize * enemyHandSizeFactor);

    // TODO: ADD A TURN DIMENSION (i.e. player.getTurnCount())

    return turnEconomy;

  }


  /*
  ** evaluateBuyPhase - Evaluates Buy Phase "Game Economy", which is based
  ** on several factors the MoveContext and Game State.
  */
  public double evaluateBuyPhase(MoveContext context, ArrayList<Card> cardPile) {

    double totalTreasure = 0;
    double totalActions = 0;

    for (Card card : cardPile) {
      if (card.is(Type.Treasure)) {
        totalTreasure += card.getAddGold();
      }
      else if (card.is(Type.Action)) {
        totalActions++;
      }
    }

    double deltaFromProvincing = totalTreasure / cardPile.size() - 1.6;
    double provincesInSupply = context.game.piles.get("Province").getCount();
    double treasureDeltaImpact = deltaFromProvincing * provincesInSupply;

    double deltaFromIdealActionCount = (double) cardPile.size() / 8.0 - totalActions;
    double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);

    double vpImpact = (double) player.getTotalVictoryPoints() * (8.0  - provincesInSupply);

    // TODO: Adjust Above Numbers for Colony and Platinum games?

    double gameEconomy = (treasureDeltaImpact * treasureDeltaFactor) +
                         (actionDeltaImpact * actionDeltaFactor) +
                         (vpImpact * victoryPointFactor);

    return gameEconomy;

  }


  // -----------------------------------------------
  // Helper Functions, Below:
  // -----------------------------------------------

  protected int getCoinEstimate(MoveContext context) {
    int coin = 0;
    int treasurecards = 0;
    int foolsgoldcount = 0;
    int bankcount = 0;
    int venturecount = 0;
    for (Card card : context.player.getHand()) {
      if (card.is(Type.Treasure, context.player)) {
        coin += card.getAddGold();
        if (card.getKind() != Cards.Kind.Spoils) {
          treasurecards++;
        }
        if (card.getKind() == Cards.Kind.FoolsGold) {
          foolsgoldcount++;
          if (foolsgoldcount > 1) {
            coin += 3;
          }
        }
        if (card.getKind() == Cards.Kind.PhilosophersStone) {
          coin += (context.player.getDeckSize() + context.player.getDiscardSize()) / 5;
        }
        if (card.getKind() == Cards.Kind.Bank) {
          bankcount++;
        }
        if (card.getKind() == Cards.Kind.Venture) {
          venturecount++;
          coin += 1; // Estimate: could draw potion or hornOfPlenty but also platinum
                     // Patrick estimates in getCurrencyTotal(list) coin += 1
        }
      }
    }
    coin += bankcount * (treasurecards + venturecount) - (bankcount*bankcount + bankcount) / 2;
    coin += context.player.getGuildsCoinTokenCount();
    if(context.player.getMinusOneCoinToken() && coin > 0) {
      coin--;
    }
    coin += context.getCoins();
    return coin;
  }
}
