package com.vdom.players;

import java.util.ArrayList;

import com.vdom.api.Card;
import com.vdom.api.GameEvent;
import com.vdom.core.*;

public class VDomPlayerJarvisJr extends VDomPlayerJarvis {

  public VDomPlayerJarvisJr() {
		super();
		this.setName("Jarvis Jr");
	}

  @Override
  public String getPlayerName() {
    return getPlayerName(game.maskPlayerNames);
  }

  @Override
  public String getPlayerName(boolean maskName) {
    return maskName ? "Player " + (playerNumber + 1) : "Jarvis Jr";
  }

}
