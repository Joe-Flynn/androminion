package com.vdom.players;

// ??? - KEEP WHAT YOU NEED

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.api.GameType;

import com.vdom.core.BasePlayer;
import com.vdom.core.CardPile;
import com.vdom.core.Cards;
import com.vdom.core.Game;
import com.vdom.core.Expansion;
import com.vdom.core.GetCardsInGameOptions;
import com.vdom.core.MoveContext;
import com.vdom.core.Player;
import com.vdom.core.Type;
import com.vdom.core.Util;


public class VDomPlayerPhew extends BasePlayer  {

    Evaluator gameEvaluator = new Evaluator(this);

    @Override
    public String getPlayerName() {
        return getPlayerName(game.maskPlayerNames);
    }

    @Override
    public String getPlayerName(boolean maskName) {
        return maskName ? "Player " + (playerNumber + 1) : "Phew";
    }

    @Override
    public boolean isAi() {
        return true;
    }

    @Override
    public void newGame(MoveContext context) {
        super.newGame(context);
        gameEvaluator = new Evaluator(this);
    }


    @Override
    public Card doAction(MoveContext context) {
        return doActionHeuristic(context);
    }

    @Override
    public Card doBuy(MoveContext context) {
        return doBuyEvalSearch(context);
    }

    protected Card[] favoriteTerminals = {
            Cards.goons,
            Cards.grandMarket,
            Cards.huntingGrounds,
            Cards.hoard,
            Cards.mountebank,
            Cards.wharf,
            Cards.cultist,
            Cards.witch,
            Cards.torturer,
            Cards.margrave,
            Cards.ghostShip,
            Cards.bridgeTroll,
            Cards.hireling,
            Cards.giant,
            Cards.gear,
            Cards.jackOfAllTrades,
            Cards.catacombs,
            Cards.rabble,
            Cards.journeyman,
            Cards.councilRoom,
            Cards.vault,
            Cards.magpie,
            Cards.militia,
            Cards.monument,
            Cards.hauntedWoods,
            Cards.youngWitch,
            Cards.soothsayer,
            Cards.wildHunt,
            Cards.swampHag,
            Cards.bank,
            Cards.jester,
            Cards.masquerade,
            Cards.smithy,
            Cards.bank,
            Cards.treasureTrove,
            Cards.envoy,
            Cards.ranger,
            Cards.library,
            Cards.merchantShip,
            Cards.mine,
            Cards.marauder,
            Cards.watchTower,
            Cards.cutpurse,
            Cards.taxman,
            Cards.nobleBrigand,
            Cards.oracle,
            Cards.courtyard,
            Cards.bureaucrat,
            Cards.moat,
            Cards.nobles,
            Cards.harem
    };

    /*
    ** doActionEvalSearch - Evaluate Best Action to Play Using a Cloned Game
    ** State, and evaluating the effect of each Card in the Player's hand on it.
    */
    public Card doActionEvalSearch(MoveContext context) {

        double currentEvaluation = gameEvaluator.evaluate(context, this.getAllCards());
        int    indexActionToPlay = -1;

        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).is(Type.Action)) {

                // Clone Game and Players
                Game clonedGame = context.game.cloneGame();
                VDomPlayerPhew clonedSelf = null;
                for (Player clonedPlayer : clonedGame.players) {
                    if (clonedPlayer.getPlayerName() == "Phew") {
                        clonedSelf = (VDomPlayerPhew) clonedPlayer;
                    }
                }
                Evaluator clonedEvaluator = clonedSelf.gameEvaluator;
                MoveContext clonedContext = new MoveContext(clonedGame, clonedSelf, true);

                // Try the Indexed Action
                if (clonedGame.isValidAction(clonedContext, clonedSelf.hand.get(i))) {
                    clonedGame.broadcastEvent(new GameEvent(GameEvent.EventType.Status, clonedContext));
                    clonedSelf.hand.get(i).play(clonedGame, clonedContext, true);
                    if (clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
                        currentEvaluation = clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards());
                        indexActionToPlay = i;
                    }
                }
            }
        }

        // If Better State is Found, Return Action Card
        if (indexActionToPlay > -1) {
            return hand.get(indexActionToPlay);
        } else {
            return null;
        }

    }


    /*
    ** doActionHeuristic - Action Card Selection, based on Very Simple Heuristics
    */
    public Card doActionHeuristic(MoveContext context) {

        Card returnCard = null;

        // Get +Action Cards First
        for (int i = 0; i < hand.size(); i++) {
            Card cardInHand = hand.get(i);
            if (cardInHand.is(Type.Action) && cardInHand.getAddActions() > 0) {
                returnCard = cardInHand;
            }
        }

        // Get +Draw Cards Next
        if (returnCard == null && context.actions > 1) {
            for (int i = 0; i < hand.size(); i++) {
                Card cardInHand = hand.get(i);
                if (cardInHand.is(Type.Action) && cardInHand.getAddCards() > 0) {
                    returnCard = cardInHand;
                }
            }
        }

        // Get +Gold Cards Next
        if (returnCard == null && context.actions > 1) {
            for (int i = 0; i < hand.size(); i++) {
                Card cardInHand = hand.get(i);
                if (cardInHand.is(Type.Action) && cardInHand.getAddGold() > 0) {
                    returnCard = cardInHand;
                }
            }
        }

        // Pick a Terminal Card (optimally with the highest value)
        if (returnCard == null) {
            int  highestValue = 0;
            Card highestValueCard = null;
            for (int i = 0; i < hand.size(); i++) {
                Card cardInHand = hand.get(i);
                if (cardInHand.is(Type.Action)) {
                    if (cardInHand.getCost(context) > highestValue) {
                        highestValue = cardInHand.getCost(context);
                        highestValueCard = cardInHand;
                    }
                }
            }
            returnCard = highestValueCard;
        }

        // Update Game Evaluator
        return returnCard;

    }

    /*
    ** doBuyEvalSearch - Evaluate Best Card to Buy Using a Cloned Game State,
    ** and evaluating the effect of each Buyable Card against the Current State.
    */
    public Card doBuyEvalSearch(MoveContext context) {

        Card returnCard = null;

        // Buy Province or Gold, First
//    if (context.getCoinAvailableForBuy() == 0) {
//      returnCard = null;
//    } else if (context.canBuy(Cards.province)) {
//      returnCard = Cards.province;
//    } else if (context.canBuy(Cards.gold)) {
//      returnCard = Cards.gold;
//    } else {

        // Buy Card that Improves Player's Evaluation Most
        //double currentEvaluation = gameEvaluator.evaluate(context, this.getAllCards());
        double currentEvaluation = -1000.0;
        String cardToBuy = "";

        LinkedList<String> orderedPiles = new LinkedList<>(context.game.piles.keySet());
        LinkedList<String> limitedPiles = new LinkedList<>();

        for(int i=favoriteTerminals.length - 1; i >= 0;i--){

            String card_string = favoriteTerminals[i].getName();
            if (orderedPiles.contains(card_string)){
                orderedPiles.remove(card_string);
                orderedPiles.push(card_string);
                limitedPiles.push(card_string);
            }
        }

        limitedPiles.push("Province");
        limitedPiles.push("Duchy");
        limitedPiles.push("Estate");
        limitedPiles.push("Gold");
        limitedPiles.push("Silver");

        for (String pileName : limitedPiles) {

            // Clone Game and Players
            Game clonedGame = context.game.cloneGame();
            VDomPlayerPhew clonedSelf = null;
            for (Player clonedPlayer : clonedGame.players) {
                if (clonedPlayer.getPlayerName() == "Phew") {
                    clonedSelf = (VDomPlayerPhew) clonedPlayer;
                }
            }
            Evaluator clonedEvaluator = clonedSelf.gameEvaluator;
            MoveContext clonedContext = new MoveContext(context, clonedGame, clonedSelf);

            // Try the Buy
            Card supplyCard = clonedContext.game.piles.get(pileName).placeholderCard();
            Card buyCard = clonedGame.getPile(supplyCard).topCard();

            if (clonedGame.isValidBuy(clonedContext, buyCard)) {
                clonedGame.broadcastEvent(new GameEvent(GameEvent.EventType.Status, clonedContext));
                clonedGame.playBuy(clonedContext, buyCard);
                clonedGame.playerPayOffDebt(clonedSelf, clonedContext);
                if (clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards()) > currentEvaluation) {
                    currentEvaluation = clonedEvaluator.evaluate(clonedContext, clonedSelf.getAllCards());
                    cardToBuy = pileName;
                }
            }
        }

        // Update Card to Buy
        if (cardToBuy != "") {
            returnCard = context.game.piles.get(cardToBuy).placeholderCard();
        } else if (context.canBuy(Cards.silver)) {
            returnCard = Cards.silver;
        } else {
            returnCard = null;
        }

        // Update Game Evaluator
        return returnCard;

    }


    /*
    ** doBuyHeuristic - Simple Big Money Strategy, with the option to buy cards if
    ** the price is right.  This can be modified to use the Game State Evaluator.
    */
    public Card doBuyHeuristic(MoveContext context) {

        Card returnCard = null;

        // Buy Province or Gold, First
        if (context.getCoinAvailableForBuy() == 0) {
            returnCard = null;
        } else if (context.canBuy(Cards.province)) {
            returnCard = Cards.province;
        } else if (context.canBuy(Cards.gold)) {
            returnCard = Cards.gold;
        } else {

            // Get Highest Value Card Player can Buy
            int  highestValue = 0;
            Card highestValueCard = null;
            for (String p : context.game.placeholderPiles.keySet()) {
                CardPile pile = context.game.placeholderPiles.get(p);
                Card supplyCard = pile.placeholderCard();
                if (pile.topCard() != null) {
                    if (context.canBuy(supplyCard) && supplyCard.getCost(context) > highestValue) {
                        highestValue = supplyCard.getCost(context);
                        highestValueCard = supplyCard;
                    }
                }
            }
            if (highestValueCard != null) {
                returnCard = highestValueCard;
            } else if (context.canBuy(Cards.silver)) {
                returnCard = Cards.silver;
            } else {
                returnCard = null;
            }
        }

        // Update Game Evaluator
        return returnCard;

    }




    // We man want to modify which cards get considered to be "garbage", differently from the Base Player
    // public Card[] getTrashCards() { return null; }

}
