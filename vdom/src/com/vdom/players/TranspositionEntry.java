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

    HashMap<String, Integer> deckCounts = new HashMap<>();
    HashMap<String, Integer> actionCounts = new HashMap<>();

    TranspositionEntry()
    {

    }

    TranspositionEntry(MoveContext context, ArrayList<Card> actionsPlayed)
    {
        this.actionCount = context.actionsPlayedSoFar;
        this.handSize = context.player.getHand().size();

        ArrayList<Card> deck = context.player.getAllCards();

        this.deckSize = deck.size();

        deckCounts = getCardCounts(deck);
        actionCounts = getCardCounts(actionsPlayed);
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
}
