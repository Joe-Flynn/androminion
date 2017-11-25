package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

public class Evaluator {

    private Player player;

    public Evaluator(Player player) {
        this.player = player;
    }

    public double evaluate(MoveContext context) {

        double totalTreasure = 0;
        double totalActions = 0;
        for (Card card : player.getAllCards()) {
            if (card.is(Type.Treasure)) {
                totalTreasure += card.getAddGold();
            }
            else if (card.is(Type.Action)) {
                totalActions++;
            }
        }

        double avgTreasurePerCard = totalTreasure / (double) player.getAllCards().size();
        double avgTreasurePerHand = avgTreasurePerCard * 5.0;
        double deltaFromProvincing = avgTreasurePerHand - 1.6;

        double provincesInSupply = 0;
        for (Card card : context.getSupply()) {
            if (card.equals(Cards.province)) {
                provincesInSupply++;
            }
        }

		double treasureDeltaImpact = deltaFromProvincing * provincesInSupply;
		//int idealTerminalCount = player.getDeckSize() / 7; do we need this?
		double deltaFromIdealActionCount = (double) player.getDeckSize() / (totalActions - (1.0 /8.0));

		double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);
		double vpImpact = (double) player.getTotalVictoryPoints() * (double) (8.0  - provincesInSupply);

		return treasureDeltaImpact + actionDeltaImpact + (vpImpact / 6.0);
    }

}
