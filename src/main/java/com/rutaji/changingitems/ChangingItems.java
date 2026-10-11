package com.rutaji.changingitems;

import com.rutaji.changingitems.cards.DialogClickListener;
import com.rutaji.changingitems.cards.GameDialog;
import com.rutaji.changingitems.cards.GameManager;
import com.rutaji.changingitems.cards.Glyphs;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChangingItems extends JavaPlugin {

    @Override
    public void onEnable() {
        GameManager games = new GameManager();
        GameDialog dialog = new GameDialog(new Glyphs(getLogger()));

        getServer().getPluginManager().registerEvents(games, this);
        getServer().getPluginManager().registerEvents(new DialogClickListener(this, games, dialog), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("qwent")
                                .requires(source -> source.getExecutor() instanceof Player)
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getExecutor();
                                    dialog.show(player, games.start(player));
                                    return 1;
                                })
                                .build(),
                        "Start a Qwent card game"));

        getLogger().info("ChangingItems enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChangingItems disabled!");
    }
}
