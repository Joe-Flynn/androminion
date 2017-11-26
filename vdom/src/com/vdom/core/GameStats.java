package com.vdom.core;

import com.vdom.api.Card;
import com.vdom.api.GameType;

// Stats to Track Per Game

public class GameStats {
  public GameType gameType;
  public Card[]   cards;
  public int      aveTurns;
  public int      aveVictoryPoints;
  public int      aveNumCards;
}
