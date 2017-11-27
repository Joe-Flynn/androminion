package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.core.*;

import java.util.ArrayList;

public class SearchTree {

  public enum PlayerDecision {

    Pawn_AddCardAndAction,
    Pawn_AddCardAndBuy,
    Pawn_AddCardAndGold,
    Pawn_AddActionAndBuy,
    Pawn_AddActionAndGold,
    Pawn_AddBuyAndGold,

    NoDecision // Default - i.e. for Tree Root
  }

  public class TreeNode {

    protected Game gameState;
    protected PlayerDecision decision;
    protected ArrayList children;

    protected TreeNode(Game inputGame) {
      gameState = inputGame.cloneGame();
      decision  = NoDecision;
      children  = new ArrayList<TreeNode>();
    }

  }

  protected TreeNode root;

  public SearchTree(Game inputGame) {
    this.root = TreeNode(inputGame);
  }


  
}
