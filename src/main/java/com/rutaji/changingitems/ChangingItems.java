package com.rutaji.changingitems;

import com.rutaji.changingitems.cards.DialogClickListener;
import com.rutaji.changingitems.cards.GameDialog;
import com.rutaji.changingitems.cards.GameManager;
import com.rutaji.changingitems.cards.Glyphs;
import com.rutaji.changingitems.cards.TextGameDialog;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChangingItems extends JavaPlugin {

    @Override
    public void onEnable() {
        GameManager games = new GameManager();
        Glyphs glyphs = new Glyphs(getLogger());
        GameDialog buttonView = new GameDialog(glyphs);
        TextGameDialog textView = new TextGameDialog(glyphs);

        getServer().getPluginManager().registerEvents(games, this);
        getServer().getPluginManager().registerEvents(new DialogClickListener(this, games), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        Commands.literal("qwent")
                                .requires(source -> source.getExecutor() instanceof Player)
                                .executes(ctx -> {
                                    Player player = (Player) ctx.getSource().getExecutor();
                                    textView.show(player, games.start(player, textView));
                                    return 1;
                                })
                                // The earlier button-grid version, for comparison.
                                .then(Commands.literal("buttons")
                                        .executes(ctx -> {
                                            Player player = (Player) ctx.getSource().getExecutor();
                                            buttonView.show(player, games.start(player, buttonView));
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
