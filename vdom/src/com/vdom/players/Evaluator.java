package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

public class Evaluator {

    private MoveContext context;
    private Player player;
    private double totalTreasureInDeck;
    private int totalActionsInDeck;

    public Evaluator(Player player) {
        this.player = player;
        totalTreasureInDeck = 7;
        totalActionsInDeck = 0;
    }

    /**
     * Updates the totalTreasureInDeck and totalActionsInDeck from the card just bought during the buy phase.
     * Must be called in doBuy before you return a chosen card.
     * @param card the card just chosen during doBuy
     */
    public void updateWithBuyChoice(Card card) {
        if (card.is(Type.Treasure)) {
            totalTreasureInDeck += card.getAddGold();
        }
        else if (card.is(Type.Action)) {
            totalActionsInDeck++;
        }

    }

    /**
     * Updates the totalTreasureInDeck and totalActionsInDeck from the card just played during the action phase.
     * Must be called in doAction before you return a chosen card.
     * @param card the card just chosen during doAction
     */
    public void updateWithActionChoice(Card card) {
        if (card.is(Type.Treasure)) {
            totalTreasureInDeck += card.getAddGold();
        }
        else if (card.is(Type.Action)) {
            totalActionsInDeck--;
        }
    }

    public double evaluate(MoveContext context) {

        double avgTreasurePerCard = totalTreasureInDeck / (double) player.getDeckSize();
        double avgTreasurePerHand = avgTreasurePerCard * 5.0;
        double deltaFromProvincing = avgTreasurePerHand - 1.6;

        int idealTerminalCount = player.getDeckSize() / 7;
        double deltaFromIdealActionCount = (double) player.getDeckSize() / (totalActionsInDeck - (1.0 /8.0));

        double provincesInSupply = 0;
        for (Card card : context.getSupply()) {
            if (card.equals(Cards.province)) {
                provincesInSupply++;
            }
        }

        double actionDeltaImpact = Math.abs(deltaFromIdealActionCount * provincesInSupply);

        return 0;
    }
}
