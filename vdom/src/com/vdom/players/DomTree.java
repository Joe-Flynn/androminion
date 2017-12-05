package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
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
  Evaluator   evaluator;
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
    curNode = curNode.getNextExpandOption();
    if(curNode == null) {return null;}
    return (T)(curNode.choice);
  }

  @SuppressWarnings("unchecked")
  public <T> T get_next_option(ArrayList<T> options)
  {
    // This is the big puzzle... what if something happens between plays and the options are different??
    if(curNode == null) {return null;}
    curNode = curNode.getNextExpandOption(options);
    if(curNode == null) {return null;}
    return (T)(curNode.choice);
  }

  public void expand()
  {
    ArrayList<DomTreeNode> states;
    do {
      states = root.getLeafStates();
      for(DomTreeNode state : states) {
        expand_state(state);
      }
    } while (states.size() > 0);
  }


  public void expand_state(DomTreeNode state) {

    state.evaluation = evaluator.evaluateActionPhase(state.context);

    // Log state in TT; if we've been here before, don't expand.
    if(!tt.add(new TranspositionEntry(state.context))) {
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

          curNode = state.newCardNode(playerCard);
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
            curNode.newStateNode(contextClone);

            optionCount++;
          }
          while(curPlay.getNextExpandOption() != null && optionCount < 100);

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


    public DomTreeNode(MoveContext gameContext) {
      type = DomNodeType.state;
      context = gameContext;
    }

    public DomTreeNode(MoveContext gameContext, DomTreeNode parent) {
      type = DomNodeType.state;
      context = gameContext;
      this.parent = parent;
    }

    public DomTreeNode(Card c) {
      type = DomNodeType.play;
      card = c;
    }

    public DomTreeNode(Card c, DomTreeNode parent) {
      type = DomNodeType.play;
      card = c;
      this.parent = parent;
    }

    public DomTreeNode(Object playOption) {
      type = DomNodeType.option;
      choice = playOption;
    }

    public DomTreeNode(Object playOption, DomTreeNode parent) {
      type = DomNodeType.option;
      choice = playOption;
      this.parent = parent;
    }

    public ArrayList<DomTreeNode> getLeafStates() {
      ArrayList<DomTreeNode> leaves = new ArrayList<>();
      if(type == DomNodeType.state && children.size() == 0 && evaluation < neg_infinity) {
        leaves.add(this);
      } else {
        for(DomTreeNode tn : children) {
          leaves.addAll(tn.getLeafStates());
        }
      }
      return leaves;
    }

    public <T> DomTreeNode getNextExpandOption(ArrayList<T> options)
    {
      if (children.size() == 0){
        addOptionNodes(options);
      }
      return getNextExpandOption();
    }

    // getNextExpandOption - pick the first child that hasn't been completely expanded (ending in a state)
    public DomTreeNode getNextExpandOption()
    {
      DomTreeNode ret = null;
      for(DomTreeNode tn : children) {
        if(tn.type == DomNodeType.option && tn.children.size() == 0) {
          return tn;
        } else if(tn.type != DomNodeType.state && tn.type != DomNodeType.dead) {
          // if child has an unexpanded option somewhere, return child
          ret = tn.getNextExpandOption();
          if(ret != null) {return tn;}
        }
      }
      return null;
    }

    public <T> void addOptionNodes(ArrayList<T> options)
    {
      for(Object o : options) {
        children.add(new DomTreeNode(o, this));
      }
    }

    public DomTreeNode newCardNode(Card c) {
      DomTreeNode newNode = new DomTreeNode(c, this);
      children.add(newNode);
      return newNode;
    }

    public DomTreeNode newStateNode(MoveContext gameContext) {
      DomTreeNode newNode = new DomTreeNode(gameContext, this);
      children.add(newNode);
      return newNode;
    }

    public DomTreeNode find_best_state_node() {
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

    public Card getTopPlay()
    {
      Card ret = null;
      DomTreeNode tn = this;
      while(tn.parent != null) {
        if(tn.type == DomNodeType.play) {
          ret = tn.card;
        }
        tn = tn.parent;
      }
      return ret;
    }


  }
}
