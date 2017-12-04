package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.Cards;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;

import java.util.ArrayList;

public class DomTree {

    private enum DomNodeType {
        state, play, option, dead
    }

    DomTreeNode root;
    DomTreeNode curPlay;
    DomTreeNode curNode;
    Evaluator evaluator;
    TranspositionTable tt;
    static double neg_infinity = -100000.0;

    DomTree(MoveContext context, Evaluator evaluator)
    {
        tt = new TranspositionTable();
        root = new DomTreeNode(context);
        curNode = root;
        this.evaluator = evaluator;
    }

    @SuppressWarnings("unchecked")
    public <T> T get_next_option()
    {
        // This is the big puzzle... what if something happens between plays and the options are different??
        if(curNode == null) {return null;}
        curNode = curNode.get_next_expand_option();
        if(curNode == null) {return null;}
        return (T)(curNode.choice);
    }

    @SuppressWarnings("unchecked")
    public <T> T get_next_option(ArrayList<T> options)
    {
        // This is the big puzzle... what if something happens between plays and the options are different??
        if(curNode == null) {return null;}
        curNode = curNode.get_next_expand_option(options);
        if(curNode == null) {return null;}
        return (T)(curNode.choice);
    }

    public Card chooseAction() {return chooseAction(10, 3, 10);}

    public Card chooseAction(int depth, int width, int iterations)
    {
        for(int i = 0; i < iterations; i++)
        {
            searchAction(depth, width);
        }

        double maxEval = neg_infinity;
        DomTreeNode maxAction = null;

        for(DomTreeNode tn : root.children)
        {
            if(tn.evaluation > maxEval) {maxAction = tn;}
        }

        return maxAction.card;
    }

    public Card searchAction(){return searchAction(10, 3);}

    public Card searchAction(int depth, int width)
    {
        // Currently assuming that any reshuffling has been done already.
        // Belay that... but probably still a good idea.

        // Keep only existing first level children (if any)
        for(DomTreeNode tn : root.children)
        {
            tn.children = new ArrayList<>();
        }

        // Shuffle and Expand
        //  on second thought... should it be the player's responsibility to reshuffle? The player can track known cards.
        //  hacky, but I'm pretty sure responsible card should never matter..
        root.context.player.shuffleDeck(root.context, Cards.copper);
        expand(depth, width);

        // Add the top evaluation in each main branch to the branch evaluation
        for(DomTreeNode tn : root.children)
        {
            if (tn.evaluation < neg_infinity) {tn.evaluation = 0;}
            tn.evaluation += tn.find_best_state_node().evaluation;
        }

        return null;
    }

    public void expand() {expand(10, 3);}

    public void expand(int depth, int width)
    {
        ArrayList<DomTreeNode> states;
        int i = 0;

        do {
            states = root.get_leaf_states(width);

            for(DomTreeNode state : states)
            {
                expand_state(state);
            }

            i++;
        }while (states.size() > 0 && i < depth);

    }

    public void expand_state(DomTreeNode state) {

        state.evaluation = evaluator.evaluateActionPhase(state.context);

        // log state in TT; if we've been here before, don't expand.
        if(!tt.add(new TranspositionEntry(state.context)))
        {
            state.type = DomNodeType.dead;
            return;
        }

        // Examine Possible Cards to Play from Hand
        Player player = state.context.player;

        for (int i = 0; i < player.getHand().size(); i++) {
            Card playerCard = player.getHand().get(i);
            if (playerCard.is(Type.Action)) {

                // Check if Another of the Same Type of Action Card was ALREADY Expanded
                boolean alreadyChecked = false;
                for (int j = 0; j < i; j++) {
                    if (player.getHand().get(i).getKind() == player.getHand().get(j).getKind()) {
                        alreadyChecked = true;
                    }
                }

                if (!alreadyChecked) { // NOTE: We can make this more elegant by just having a list of unique actions.

                    curNode = state.new_card_node(playerCard);
                    curPlay = curNode;
                    int optionCount = 0;

                    do {
                        curNode = curPlay;

                        MoveContext contextClone = state.context.cloneContext();

                        // Play the Action in the Cloned Game State
                        if (contextClone.game.isValidAction(contextClone, contextClone.player.getHand().get(i))) {
                            contextClone.game.broadcastEvent(new GameEvent(GameEvent.EventType.Status, contextClone));
                            contextClone.player.getHand().get(i).play(contextClone.game, contextClone, true);
                        }

                        // In the play action call, if any interactions are chosen by the tree in the play call, then curNode position is updated
                        curNode.new_state_node(contextClone);

                        optionCount++;
                    }
                    while(curPlay.get_next_expand_option() != null && optionCount < 100);

                }
            }
        }

        if(state.children.size() == 0)
        {
            state.type = DomNodeType.dead;
        }

    }



    public class DomTreeNode {

        DomNodeType type;
        DomTreeNode parent = null;
        ArrayList<DomTreeNode> children = new ArrayList<>();

        // Used exclusively in state nodes
        MoveContext context = null;
        double evaluation = neg_infinity - 1;

        // Used exclusively in play nodes
        Card card = null;

        // Used exclusively in option nodes
        Object choice = null;
        int option_index = 0;


        DomTreeNode(MoveContext gameContext)
        {
            type = DomNodeType.state;
            context = gameContext;
        }

        DomTreeNode(Card c)
        {
            type = DomNodeType.play;
            card = c;
        }

        DomTreeNode(Object playOption)
        {
            type = DomNodeType.option;
            choice = playOption;
        }

        DomTreeNode(MoveContext gameContext, DomTreeNode parent)
        {
            type = DomNodeType.state;
            context = gameContext;
            this.parent = parent;
        }

        DomTreeNode(Card c, DomTreeNode parent)
        {
            type = DomNodeType.play;
            card = c;
            this.parent = parent;
        }

        DomTreeNode(Object playOption, DomTreeNode parent)
        {
            type = DomNodeType.option;
            choice = playOption;
            this.parent = parent;
        }

        public ArrayList<DomTreeNode> get_leaf_states()
        {
            ArrayList<DomTreeNode> leaves = new ArrayList<>();
            if(type == DomNodeType.state && children.size() == 0 && evaluation < neg_infinity)
            {
                leaves.add(this);
            }
            else
            {
                for(DomTreeNode tn : children)
                {
                    leaves.addAll(tn.get_leaf_states());
                }
            }
            return leaves;
        }

        public ArrayList<DomTreeNode> get_leaf_states(int width)
        {
            ArrayList<DomTreeNode> leaves = new ArrayList<>();
            if(type == DomNodeType.state && children.size() == 0 && evaluation < neg_infinity)
            {
                leaves.add(this);
            }
            else
            {
                for(int i = 0; i < Math.min(width, children.size()); i++)
                {
                    leaves.addAll(children.get(i).get_leaf_states());
                }
                /*
                for(DomTreeNode tn : children)
                {
                    leaves.addAll(tn.get_leaf_states());
                }*/
            }
            return leaves;
        }

        public <T> DomTreeNode get_next_expand_option(ArrayList<T> options)
        {
            if (children.size() == 0){add_option_nodes(options);}
            return get_next_expand_option();
        }

        public DomTreeNode get_next_expand_option()
        {
            // pick the first child that hasn't been completely expanded (ending in a state)

            DomTreeNode ret = null;

            for(DomTreeNode tn : children)
            {
                if(tn.type == DomNodeType.option && tn.children.size() == 0)
                {
                    return tn;
                }
                else if(tn.type != DomNodeType.state && tn.type != DomNodeType.dead)
                {
                    // if child has an unexpanded option somewhere, return child
                    ret = tn.get_next_expand_option();
                    if(ret != null) {return tn;}
                }
            }
            return null;
        }

        public <T> void add_option_nodes(ArrayList<T> options)
        {
            for(Object o : options)
            {
                children.add(new DomTreeNode(o, this));
            }
        }

        public DomTreeNode new_card_node(Card c)
        {
            DomTreeNode newNode = new DomTreeNode(c, this);
            children.add(newNode);
            return newNode;
        }

        public DomTreeNode new_state_node(MoveContext gameContext)
        {
            DomTreeNode newNode = new DomTreeNode(gameContext, this);
            children.add(newNode);
            return newNode;
        }

        public DomTreeNode find_best_state_node()
        {
            double best_eval = neg_infinity;
            DomTreeNode best_node = this;

            for(DomTreeNode tn : children)
            {
                if(tn.evaluation > best_eval)
                {
                    best_eval = tn.evaluation;
                    best_node = tn;
                }
            }

            return best_node;
        }

        public Card get_top_play()
        {
            Card ret = null;
            DomTreeNode tn = this;

            while(tn.parent != null)
            {
                if(tn.type == DomNodeType.play)
                {
                    ret = tn.card;
                }
                tn = tn.parent;
            }

            return ret;
        }

    }
}

