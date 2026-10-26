package com.rutaji.qwent3;

import com.rutaji.qwent3.cards.deck.DeckEditor;
import com.rutaji.qwent3.cards.deck.DeckStore;
import com.rutaji.qwent3.cards.dialog.DialogClickListener;
import com.rutaji.qwent3.cards.dialog.GameDialog;
import com.rutaji.qwent3.cards.dialog.Glyphs;
import com.rutaji.qwent3.cards.gameLogic.GameManager;
import com.rutaji.qwent3.cards.items.CardItems;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class Qwent3 extends JavaPlugin {
    private @Nullable DeckEditor deckEditor;

    @Override
    public void onEnable() {
        DeckStore decks = new DeckStore(this);
        CardItems cardItems = new CardItems(this);
        DeckEditor deckEditor = new DeckEditor(decks, cardItems);
        this.deckEditor = deckEditor;
        GameManager games = new GameManager(decks, new GameDialog(new Glyphs(getLogger())), getLogger());

        getServer().getPluginManager().registerEvents(games, this);
        getServer().getPluginManager().registerEvents(deckEditor, this);
        getServer().getPluginManager().registerEvents(new DialogClickListener(games), this);

        QwentCommands commands = new QwentCommands(games, decks, deckEditor, cardItems);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> event.registrar().register(commands.build(), "Qwent card game"));

        getLogger().info("qwent3 enabled!");
    }

    @Override
    public void onDisable() {
        if (deckEditor != null) {
            deckEditor.closeAll();
        }
        getLogger().info("qwent3 disabled!");
    }
}
