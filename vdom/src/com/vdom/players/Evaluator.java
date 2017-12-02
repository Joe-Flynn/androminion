package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

import java.util.ArrayList;

public class Evaluator {

    private Player player;

    public Evaluator(Player player) {
        this.player = player;
    }

    /*
    ** evaluateActionPhase - Evaluates Action Phase "Turn Economy", which is based
    ** on the buyability / gainability of the MoveContext after playing several actions.
    */
    public double evaluateActionPhase(MoveContext context) {

      int usableCoin      = Math.min(context.getCoins(), context.getBuysLeft() * 8);
      /// TODO: ^----- Get Available Coins???
      /// TODO: ^----- Update 8 to most expensive buy

      int potionGains     = Math.min(context.getPotions(), Math.min(context.getBuysLeft(), context.getCoins() / 3));
      int threeCostGains  = Math.min(context.getCoins() / 3, context.getBuysLeft());
      int fiveCostGains   = Math.min(context.getCoins() / 5, context.getBuysLeft());

      int coinTokenFactor = context.player.getGuildsCoinTokenCount();
      int debtTokenFactor = context.player.getDebtTokenCount();
      int victTokenFactor = context.player.getVictoryTokens();

      // "Turn Economy" is a tunable weighted sum
      double turnEconomy  = usableCoin + (0.5 * potionGains) +
                            threeCostGains + (1.25 * fiveCostGains) +
                            (1.5 * coinTokenFactor) - (1.0 * debtTokenFactor) +
                            (0.5 * victTokenFactor);

      // Scale by Inverse of Opponent's Hand Size
      double enemyHandSize = context.getOpponent().getHand().size();

      // TODO: Add something that evaluates how close your deck becomes to the Original Plan

      return turnEconomy - enemyHandSize;
    }


    /*
    ** evaluate - Probably want to rename this?  Since we need an evaluate
    ** function for action phase and a separate one for buy phase.  Also,
    ** what is the cardPile input needed for?
    */
    public double evaluate(MoveContext context, CardList cardPile) {
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

//        double avgTreasurePerCard = totalTreasure / (double) cardPile.size();
//        double avgTreasurePerHand = avgTreasurePerCard * 5.0;
//        double deltaFromProvincing = avgTreasurePerHand - 1.6;

//        double provincesInSupply = 0;
//        for (Card card : context.getSupply()) {
//            if (card.equals(Cards.province)) {
//                provincesInSupply++;
//            }
//        }

        double deltaFromProvincing = totalTreasure / cardPile.size() - 1.6;
        double provincesInSupply = context.game.piles.get("Province").getCount();

		double treasureDeltaImpact = deltaFromProvincing * provincesInSupply;
		//int idealTerminalCount = player.getDeckSize() / 7; do we need this?
		double deltaFromIdealActionCount = (double) cardPile.size() / 8.0 - totalActions;

		double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);
		double vpImpact = (double) player.getTotalVictoryPoints() * (8.0  - provincesInSupply);

		return treasureDeltaImpact - actionDeltaImpact + (vpImpact / 6.0);
    }

    public double evaluate(MoveContext context, ArrayList<Card> cardPile) {
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

//        double avgTreasurePerCard = totalTreasure / (double) cardPile.size();
//        double avgTreasurePerHand = avgTreasurePerCard * 5.0;
//        double deltaFromProvincing = avgTreasurePerHand - 1.6;

//        double provincesInSupply = 0;
//        for (Card card : context.getSupply()) {
//            if (card.equals(Cards.province)) {
//                provincesInSupply++;
//            }
//        }

        double deltaFromProvincing = totalTreasure / cardPile.size() - 1.6;
        double provincesInSupply = context.game.piles.get("Province").getCount();

        double treasureDeltaImpact = deltaFromProvincing * provincesInSupply;
        //int idealTerminalCount = player.getDeckSize() / 7; do we need this?
        double deltaFromIdealActionCount = (double) cardPile.size() / 8.0 - totalActions;

        double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);
        double vpImpact = (double) player.getTotalVictoryPoints() * (8.0  - provincesInSupply);

        return treasureDeltaImpact - actionDeltaImpact + (vpImpact / 6.0);
    }

}
