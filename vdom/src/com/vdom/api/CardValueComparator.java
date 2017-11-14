package com.vdom.api;

import java.util.Comparator;

public class CardValueComparator implements Comparator<Card> {

  /*
  ** compare - Compares two cards' AddGold Value, returning -1 (if
  ** cardOne is greater), +1 (if cardTwo is greater), or 0 (if both
  ** cards' AddGold Values are equal.
  */
  public int compare(Card cardOne, Card cardTwo) {
    if (cardOne.getAddGold() == cardTwo.getAddGold()) {
      return 0;
    } else if (cardOne.getAddGold() > cardTwo.getAddGold()) {
      return -1;
    } else {
      return 1;
    }
  }
}
