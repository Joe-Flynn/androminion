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

    Amulet_ChooseSilver,
    Amulet_ChooseCoin,
    Amulet_ChooseTrash,

    NoDecision // Default - i.e. for Tree Root
  }

  public class TreeNode {

    protected MoveContext context;          // Current Game State (i.e. MoveContext, Game, and Player)

    protected Card actionCard;              // Action Card Played to get to Current Game State
    protected PlayerDecision decision;      // Player Decision Selected for the Action Card Played

    // TODO: IMPLEMENT OTHER CRITICAL DECISIONS (Review Structure from AI Player's Choose Function)

    protected double evaluation;            // Player's Evaluation about the Current Game State
    protected ArrayList<TreeNode> children; // Contains Future Game States from the Current Game State

    protected int treeDepth;                     // Used to perform Depth-Limited Searches
    protected static final int maxTreeDepth = 4; // Limit for Depth-Limited Searches

    public TreeNode(MoveContext inputContext) {
      context    = inputContext.cloneContext();
      actionCard = null;
      decision   = PlayerDecision.NoDecision;
      evaluation = ((VDomPlayerJarvis)context.player).gameEvaluator.evaluateActionPhase(context);
      children   = new ArrayList<TreeNode>();
      treeDepth  = 0;
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

      System.out.println(">>>> SEARCH TREE: Expanding Node: " + actionCard);

      boolean nodeExpanded = false;

      // Do NOT Expand, if Limits are Reached
      if (context.getActionsLeft() <= 0 || treeDepth >= maxTreeDepth) {
        return nodeExpanded;
      }

      // Get Possible Cards to Play from Hand
      ArrayList<Card> handActions = context.player.uniqueHandActions();

      for (Card playerCard : handActions) {

        // Select Player Decisions
        ArrayList<PlayerDecision> decisions = getPlayerDecisions(playerCard);
        for (PlayerDecision decision : decisions) {

          // Clone Parent Node
          TreeNode child = cloneAndPlay(playerCard, null);
          VDomPlayerJarvis childPlayer = (VDomPlayerJarvis) child.context.player;

          // Add a Single Node if No "Best Card in Play" Decision is Needed
          if (childPlayer.bestCardsInPlay == null) {
            addChild(child);
            nodeExpanded = true;
          }

          // Otherwise, Add Nodes for each "Best Card in Play" Decision Needed
          if (childPlayer.bestCardsInPlay != null) {

            System.out.println(">>>> SEARCH TREE: bestCardsInPlay not null.");

            for (Card card : childPlayer.bestCardsInPlay) {
              TreeNode anotherChild = cloneAndPlay(playerCard, card);
              addChild(anotherChild);
              nodeExpanded = true;
            }

            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

          }

          /// PROBABLY GOING TO HAVBE PROBLEMS WHEN BEST CARD IN PLAY CALLED MULIPLE TIMES...

        }
      }
      return nodeExpanded;
    }


    /*
    ** cloneAndPlay - Helper Function to expandNode.  This function clones the
    ** current TreeNode and Plays the Action Card Selected with the Player
    ** Decisions made as per the current TreeNode.
    */
    protected TreeNode cloneAndPlay(Card actionCard, Card bestCard) {

      // Clone Parent Node
      TreeNode child = new TreeNode(context);

      // Set Clone's Parameters
      child.actionCard = actionCard.clone();
      child.decision = decision;
      child.treeDepth = treeDepth + 1;

      // Choose Best Card in Play
      ((VDomPlayerJarvis)child.context.player).bestCardInPlaySelected = bestCard;

      // Play the Action in the Cloned Game State
      if (child.context.game.isValidAction(child.context, child.context.player.fromHand(actionCard))) {
        child.context.game.broadcastEvent(new GameEvent(GameEvent.EventType.Status, child.context));
        child.context.player.fromHand(actionCard).play(child.context.game, child.context, true);
      }

      // Update the Cloned Game State's Evaluation
      child.evaluation = ((VDomPlayerJarvis)child.context.player).gameEvaluator.evaluateActionPhase(child.context);

      return child;

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

      System.out.println(">>>> SEARCH TREE: getPathToEvalValue for Node: " + actionCard + ", Node Eval: " + evaluation);

      ArrayList<TreeNode> path = new ArrayList<TreeNode>();
      if (evaluation >= value) {
        path.add(this);
      } else {
        for (TreeNode child : children) {
          ArrayList<TreeNode> subPath = child.getPathToEvalValue(value);
          if (subPath.size() > 0) {
            path.add(this);
            path.addAll(subPath);
            break;
          }
        }
      }
      return path;
    }


    protected ArrayList<PlayerDecision> getPlayerDecisions(Card playerCard) {

      ArrayList<PlayerDecision> decisions = new ArrayList<PlayerDecision>();

      switch (playerCard.getKind()) {
        case Amulet:
          decisions.add(PlayerDecision.Amulet_ChooseSilver);
          decisions.add(PlayerDecision.Amulet_ChooseCoin);
          decisions.add(PlayerDecision.Amulet_ChooseTrash);
          break;
        case Pawn:
          decisions.add(PlayerDecision.Pawn_AddCardAndAction);
          decisions.add(PlayerDecision.Pawn_AddCardAndBuy);
          decisions.add(PlayerDecision.Pawn_AddCardAndGold);
          decisions.add(PlayerDecision.Pawn_AddActionAndBuy);
          decisions.add(PlayerDecision.Pawn_AddActionAndGold);
          decisions.add(PlayerDecision.Pawn_AddBuyAndGold);
          break;
        default:
          break;
      }
      return decisions;
    }

  }

  // Search Tree Root
  protected TreeNode root = null;

  // Search Tree Constructor
  public SearchTree(MoveContext inputContext) {

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
