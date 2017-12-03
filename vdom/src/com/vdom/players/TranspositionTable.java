package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.MoveContext;

import java.util.HashMap;
import java.util.ArrayList;

public class TranspositionTable {

    protected HashMap<Integer, HashMap<Integer, ArrayList<TranspositionEntry>>> tt = new HashMap<>();

    public boolean add(MoveContext context, ArrayList<Card> actionsPlayed)
    {
        return add(new TranspositionEntry(context, actionsPlayed));
    }

    public boolean add(TranspositionEntry te)
    {
        if (!tt.containsKey(te.actionCount))
        {
            tt.put(te.actionCount, new HashMap<>());
        }

        HashMap<Integer, ArrayList<TranspositionEntry>> cluster = tt.get(te.actionCount);

        if(!cluster.containsKey(te.handSize))
        {
            cluster.put(te.handSize, new ArrayList<>());
        }

        ArrayList<TranspositionEntry> entries = cluster.get(te.handSize);

        for (TranspositionEntry entry : entries)
        {
            int cmp = entry.compare(te);

            if(cmp == -1)
            {
                // not equal, do nothing and keep looking
            }
            else if(cmp == 0)
            {
                // equal, quit without inserting
                return false;
            }
            else if(cmp == 1)
            {
                // strictly better, replace this one
                entries.remove(entry);
                break;
            }
        }

        // check complete, insert into tt now
        entries.add(te);
        return true;
    }


}
