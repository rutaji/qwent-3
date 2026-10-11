package com.rutaji.changingitems;

import com.rutaji.changingitems.cards.DeckStore;
import com.rutaji.changingitems.cards.DialogClickListener;
import com.rutaji.changingitems.cards.GameDialog;
import com.rutaji.changingitems.cards.GameManager;
import com.rutaji.changingitems.cards.GameState;
import com.rutaji.changingitems.cards.Glyphs;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChangingItems extends JavaPlugin {

    @Override
    public void onEnable() {
        GameManager games = new GameManager(new DeckStore());
        Glyphs glyphs = new Glyphs(getLogger());
        GameDialog dialog = new GameDialog(glyphs);

        getServer().getPluginManager().registerEvents(games, this);
        getServer().getPluginManager().registerEvents(new DialogClickListener(this, games, dialog), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("qwent")
                                .requires(source -> source.getExecutor() instanceof Player)
                                // Resumes the running game (e.g. after Esc), or starts one.
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getExecutor();
                                    GameState state = games.get(player);
                                    games.setViewing(player, null);
                                    dialog.show(player, state != null ? state : games.start(player), null);
                                    return 1;
                                })
                                .then(Commands.literal("new")
                                        .executes(ctx -> {
                                            Player player = (Player) ctx.getSource().getExecutor();
                                            dialog.show(player, games.start(player), null);
                                            return 1;
                                        }))
                                .build(),
                        "Start a Qwent card game"));

        getLogger().info("ChangingItems enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChangingItems disabled!");
    }
}
