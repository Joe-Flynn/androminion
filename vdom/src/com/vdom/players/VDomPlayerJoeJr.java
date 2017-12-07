package com.vdom.players;

import java.util.ArrayList;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.*;

public class VDomPlayerJoeJr extends VDomPlayerJoe {

  public VDomPlayerJoeJr() {
		super();
		this.setName("Joe Jr");
	}

  @Override
  public String getPlayerName() {
    return getPlayerName(game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Joe Jr";
  }

}
