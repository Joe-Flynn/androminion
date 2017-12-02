package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

import java.util.ArrayList;

public class Evaluator {

    private Player player;

    public Evaluator(Player player) {
        this.player = player;
    }

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


        double deltaFromProvincing = totalTreasure / cardPile.size() - 1.6;
        double provincesInSupply = context.game.piles.get("Province").getCount();

        double treasureDeltaImpact = deltaFromProvincing * provincesInSupply;
        double deltaFromIdealActionCount = (double) cardPile.size() / 8.0 - totalActions;

        double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);
        double vpImpact = (double) player.getTotalVictoryPoints() * (8.0  - provincesInSupply);

        return treasureDeltaImpact - actionDeltaImpact + (vpImpact / 6.0);
    }

}
