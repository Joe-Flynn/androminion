package com.vdom.players;

import java.util.ArrayList;
import java.util.HashMap;

import com.vdom.api.Card;
import com.vdom.core.CardList;
import com.vdom.core.MoveContext;

public class TranspositionEntry {

    Integer actionCount = 0;
    Integer handSize = 0;
    Integer deckSize = 0;
    Integer currentCoin = 0;
    Integer vpTokens = 0;
    Integer actionsLeft = 0;
    Integer buys = 0;
    Integer coinToken = 0;
    double evaluation = 0;

    HashMap<String, Integer> handCounts = new HashMap<>();
    HashMap<String, Integer> deckCounts = new HashMap<>();
    HashMap<String, Integer> actionCounts = new HashMap<>();

    TranspositionEntry()
    {

    }

    TranspositionEntry(MoveContext context)
    {
        this(context, context.getPlayedCards().toArrayList(), -1000.0);
    }

    TranspositionEntry(MoveContext context, double eval)
    {
        this(context, context.getPlayedCards().toArrayList(), eval);
    }

    TranspositionEntry(MoveContext context, ArrayList<Card> actionsPlayed)
    {
        this(context, actionsPlayed, -1000.0);
    }

    TranspositionEntry(MoveContext context, ArrayList<Card> actionsPlayed, double eval)
    {
        ArrayList<Card> hand = context.player.getHand().toArrayList();
        ArrayList<Card> deck = context.player.getAllCards();

        this.actionCount = context.actionsPlayedSoFar;
        this.actionsLeft = context.actions;
        this.buys = context.buys;
        this.coinToken = context.player.getGuildsCoinTokenCount();

        // include evaluation of node in case we want to re-implement to save time in subsequent searches
        this.evaluation = eval;

        // this won't count treasure in hand, but the hand is already being compared.
        this.currentCoin = context.getCoins();
        this.vpTokens = context.player.getVictoryTokens();

        this.handSize = hand.size();
        this.deckSize = deck.size();

        deckCounts = getCardCounts(deck);
        actionCounts = getCardCounts(actionsPlayed);
        handCounts = getCardCounts(hand);

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

    int compare(TranspositionEntry te)
    {
        // return -1 for not equal, 0 for equal, 1 for strictly better

        // assume handsize and actionsPlayed are already equal?
        if(te.handSize != this.handSize || te.actionCount != this.actionCount)
        {
            return -1;
        }

        if(te.deckSize == this.deckSize && te.actionsLeft >= this.actionsLeft && te.buys >= this.buys && te.currentCoin >= this.currentCoin && te.vpTokens >= this.vpTokens)
        {
            for (String k : this.actionCounts.keySet())
            {
                if(this.actionCounts.get(k) != te.actionCounts.get(k))
                {
                    return -1;
                }
            }
            for (String k : this.handCounts.keySet())
            {
                if(this.handCounts.get(k) != te.handCounts.get(k))
                {
                    return -1;
                }
            }
            for (String k : this.deckCounts.keySet())
            {
                if(this.deckCounts.get(k) != te.deckCounts.get(k))
                {
                    return -1;
                }
            }
        }
        else
        {
            return -1;
        }

        if(te.deckSize == this.deckSize && te.actionsLeft == this.actionsLeft && te.buys == this.buys && te.currentCoin == this.currentCoin && te.vpTokens == this.vpTokens)
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

}
