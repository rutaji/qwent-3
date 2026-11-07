package com.rutaji.qwent3.cards.deck;

import com.rutaji.qwent3.cards.cardsRepository.CardType;
import org.bukkit.entity.Player;

import java.util.List;

public interface DeckSave {

    public List<CardType> get(Player player);
    public void setDeck(Player player, List<CardType> deck);
}
