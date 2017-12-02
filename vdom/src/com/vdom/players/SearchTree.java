package com.vdom.players;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;

import com.vdom.core.*;

import java.util.ArrayList;

public class SearchTree {

  /* PlayerDecisions are based on the Option Enumerations (in Player.java):
  **  - AmuletOption
  **  - CharmOption
  **  - CountFirstOption
  **  - CountSecondOption
  **  - CourtierOption
  **  - DoctorOverpayOption  <-- BUY PHASE OPTION
  **  - EncampmentOption
  **  - ExtraTurnOption      <-- EXTRA TURN OPTION (I.E. OUTPOST / MISSIOn)
  **  - FoolsGoldOption      <-- REACTION OPTION
  **  - GovernorOption
  **  - GraverobberOption
  **  - HuntingGroundsOption
  **  - JesterOption
  **  - LurkerOption
  **  - MinionOption
  **  - NoblesOption
  **  - PawnOption
  **  - PutBackOption
  **  - QuestOption
  **  - SentryOption
  **  - SpiceMerchantOption
  **  - SquireOption
  **  - StewardOption
  **  - TorturerOption
  **  - TournamentOption
  **  - TrustySteedOption
  **  - WatchTowerOption
  **  - WildHuntOption
  */

  // protected AmuletOption amuletOption;
  // protected CharmOption charmOption;
  // protected CountFirstOption countFirstOption;
  // protected CountSecondOption countSecondOption;
  // protected CourtierOption courtierOption;
  // protected DoctorOverpayOption doctorOverpayOption;
  // protected EncampmentOption encampmentOption;
  // protected ExtraTurnOption extraTurnOption;
  // protected FoolsGoldOption foolsGoldOption;
  // protected GovernorOption governorOption;
  // protected GraverobberOption graverobberOption;
  // protected HuntingGroundsOption huntingGroundsOption;
  // protected JesterOption jesterOption;
  // protected LurkerOption lurkerOption;
  // protected MinionOption minionOption;
  // protected NoblesOption noblesOption;
  // protected PawnOption pawnOption;
  // protected PutBackOption putBackOption;
  // protected QuestOption questOption;
  // protected SentryOption sentryOption;
  // protected SpiceMerchantOption spiceMerchantOption;
  // protected SquireOption squireOption;
  // protected StewardOption stewardOption;
  // protected TorturerOption torturerOption;
  // protected TournamentOption tournamentOption;
  // protected TrustySteedOption trustySteedOption;
  // protected WatchTowerOption watchTowerOption;
  // protected WildHuntOption wildHuntOption;


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

    protected MoveContext context;          // Current Game State (i.e. MoveContext, Game, and Player)
    protected Card actionCard;              // Action Card Played to get to Current Game State
    protected PlayerDecision decision;      // Player Decision Selected for the Action Card Played
    protected double evaluation;            // Player's Evaluation about the Current Game State
    protected ArrayList<TreeNode> children; // Contains Future Game States from the Current Game State

    public TreeNode(MoveContext inputContext) {
      context    = inputContext.cloneContext();
      actionCard = null;
      decision   = PlayerDecision.NoDecision;
      evaluation = ((VDomPlayerJarvis)context.player).gameEvaluator.evaluateActionPhase(context);
      children   = new ArrayList<TreeNode>();
    }

    public MoveContext getContext() {
      return context;
    }

    public Card getActionCard() {
      return actionCard;
    }

    public PlayerDecision getPlayerDecision() {
      return decision;
    }

    public double getPlayerEvaluation() {
      return evaluation;
    }

    public void addChild(TreeNode child) {
      children.add(child);
    }

    public boolean isLeaf() {
      return (children.size() == 0);
    }

    /*
    ** getLeaves - Returns all leaf TreeNodes which are down-lined from this
    ** TreeNode (i.e. an ArrayList of all the children, grandchildren, etc.)
    */
    public ArrayList<TreeNode> getLeaves() {
      ArrayList<TreeNode> leafNodes = new ArrayList<TreeNode>();
      if (this.isLeaf()) {
        leafNodes.add(this);
      } else {
        for (TreeNode child : this.children) {
          leafNodes.addAll(child.getLeaves());
        }
      }
      return leafNodes;
    }


    /*
    ** expandNode - Expands this TreeNode by one action step.  This expansion includes
    ** includes cloning the Game state within the current TreeNode, selecting player
    ** decisions, playing the action card, and then adding the new Game State as a new
    ** child of this TreeNode.
    */
    public boolean expandNode() {

      boolean nodeExpanded = false;

      Player player = context.player;
      for (int i = 0; i < player.getHand().size(); i++) {
        Card playerCard = player.getHand().get(i);
        if (playerCard.is(Type.Action)) {

          // Select Player Decisions
          ArrayList<PlayerDecision> decisions = new ArrayList<PlayerDecision>();
          decisions.add(PlayerDecision.NoDecision); // TODO: IMPLEMENT OTHER DECISIONS!!!!!!!!!!!!!!!!!!!!!!!

          for (PlayerDecision decision : decisions) {

            // Clone Parent Node
            TreeNode child = new TreeNode(context);

            // Set Clone's Parameters
            child.actionCard = playerCard.clone();
            child.decision = decision;

            // Play The Action in the Cloned Game State
            if (child.context.game.isValidAction(child.context, child.context.player.getHand().get(i))) {
              child.context.game.broadcastEvent(new GameEvent(GameEvent.EventType.Status, child.context));
              child.context.player.getHand().get(i).play(child.context.game, child.context, true);
            }

            // Add New State to the Tree
            addChild(child);
            nodeExpanded = true;

          }
        }
      }
      return nodeExpanded;
    }


    /*
    ** getMaxEval - Returns the maximum Action Phase Evaluation, within the
    ** subtree including this Node and its down-line.
    */
    public double getMaxEval() {
      double maxEval = getPlayerEvaluation();
      for (TreeNode child : children) {
        maxEval = Math.max(maxEval, child.getMaxEval());
      }
      return maxEval;
    }


    /*
    ** getMaxEval - Returns an ArrayList that is the first path found to a
    ** TreeNode (within the subtree including this Node and its down-line),
    ** while performing a Depth First Search (DFS) of the subtree.
    */
    public ArrayList<TreeNode> getPathToEvalValue(double value) {
      ArrayList<TreeNode> path = null;
      if (evaluation >= value) {
        path = new ArrayList<TreeNode>();
        path.add(this);
      } else {
        for (TreeNode child : children) {
          ArrayList<TreeNode> subPath = child.getPathToEvalValue(value);
          if (subPath != null)
          path.addAll(subPath);
          break;
        }
      }
      return path;
    }

  }


  protected TreeNode root = null;

  public SearchTree(MoveContext inputContext) {

    // Set Root
    this.root = new TreeNode(inputContext.cloneContext());

    // Expand Tree
    boolean doneExpanding = false;
    while (!doneExpanding) {
      doneExpanding = true;
      ArrayList<TreeNode> leafNodes = this.root.getLeaves();
      for (TreeNode node : leafNodes) {
        if (node.expandNode()) { doneExpanding = false; }
      }
    }
  }

  /*
  ** getRoot - Tree Root Accessor
  */
  public TreeNode getRoot() {
    return this.root;
  }

  /*
  ** getPathToMaxEval - Returns a path to the TreeNode with the maximum
  ** Action Phase Evaluation.
  */
  public ArrayList<TreeNode> getPathToMaxEval() {
    double maxEval = this.root.getMaxEval();
    return this.root.getPathToEvalValue(maxEval);
  }


}
