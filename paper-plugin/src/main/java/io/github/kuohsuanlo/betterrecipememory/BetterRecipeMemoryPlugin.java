package io.github.kuohsuanlo.betterrecipememory;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

/**
 * Standalone Paper 1.21.8 plugin.
 *
 * <p>The bootstrapper supplies a small embedded datapack which filters vanilla recipe
 * advancements before registries are built. This runtime class gives each player the
 * complete recipe book once and suppresses the harmless orphan-advancement warnings
 * created by old player data.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public final class BetterRecipeMemoryPlugin extends JavaPlugin implements Listener, BasicCommand {

    static final String AUTHOR_LINE =
            "crafted by 廢土貓大 LogoCat · 廢土 · mcfallout.net";
    static final String REPO =
            "github.com/TW527E/BetterRecipeMemory-Plugin";

    private NamespacedKey recipeBookGrantedKey;

    @Override
    public void onLoad() {
        try {
            OrphanAdvancementLogFilter.install();
            getLogger().info("Orphan-advancement log filter installed.");
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING,
                    "Log filter install failed (non-fatal; orphan warnings will remain)", throwable);
        }
    }

    @Override
    public void onEnable() {
        recipeBookGrantedKey = new NamespacedKey(this, "recipe_book_granted_v1");
        getServer().getPluginManager().registerEvents(this, this);
        registerCommand("brm", "BetterRecipeMemory information and administration", List.of(), this);

        getLogger().info("BetterRecipeMemory for Paper 1.21.8 — " + AUTHOR_LINE);
        getLogger().info("The recipe-advancement filter is bundled in this jar; no separate datapack is needed.");
        getLogger().info(REPO);

        // Also covers plugin reloads. Normal startup has no online players here.
        for (Player player : Bukkit.getOnlinePlayers()) {
            grantRecipesOnce(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        grantRecipesOnce(event.getPlayer());
    }

    private void grantRecipesOnce(Player player) {
        if (player.getPersistentDataContainer().has(recipeBookGrantedKey, PersistentDataType.BYTE)) {
            return;
        }

        int granted = grantAllRecipes(player);
        if (granted >= 0) {
            player.getPersistentDataContainer().set(recipeBookGrantedKey, PersistentDataType.BYTE, (byte) 1);
            getLogger().fine(() -> "Initial recipe-book grant completed for " + player.getName()
                    + " (" + granted + " newly discovered recipes).");
        }
    }

    private int grantAllRecipes(Player player) {
        Set<NamespacedKey> recipeKeys = new LinkedHashSet<>();
        var iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed keyed) {
                recipeKeys.add(keyed.getKey());
            }
        }

        if (recipeKeys.isEmpty()) {
            getLogger().warning("Recipe registry was empty; will retry for " + player.getName()
                    + " on their next join.");
            return -1;
        }

        return player.discoverRecipes(recipeKeys);
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        String subcommand = args.length == 0 ? "about" : args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "help" -> sendHelp(source);
            case "version" -> source.getSender().sendRichMessage(
                    "<gold>BetterRecipeMemory <white>" + getPluginMeta().getVersion());
            case "refill" -> refill(source, args);
            case "about" -> sendAbout(source);
            default -> {
                source.getSender().sendRichMessage("<red>Unknown subcommand. Use <yellow>/brm help</yellow>.");
                sendHelp(source);
            }
        }
    }

    private void refill(CommandSourceStack source, String[] args) {
        if (!source.getSender().hasPermission("betterrecipememory.admin")) {
            source.getSender().sendRichMessage("<red>You do not have permission to use this command.");
            return;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                source.getSender().sendRichMessage("<red>Player is not online: <white>" + args[1]);
                return;
            }
        } else if (source.getExecutor() instanceof Player player) {
            target = player;
        } else {
            source.getSender().sendRichMessage("<red>Console usage: /brm refill <player>");
            return;
        }

        int granted = grantAllRecipes(target);
        if (granted >= 0) {
            target.getPersistentDataContainer().set(recipeBookGrantedKey, PersistentDataType.BYTE, (byte) 1);
            source.getSender().sendRichMessage("<green>Recipe book filled for <white>" + target.getName()
                    + "</white> (<yellow>" + granted + "</yellow> newly discovered).");
        }
    }

    private void sendHelp(CommandSourceStack source) {
        source.getSender().sendRichMessage("<gold>BetterRecipeMemory <gray>commands:");
        source.getSender().sendRichMessage("<yellow>/brm about <gray>— project information");
        source.getSender().sendRichMessage("<yellow>/brm version <gray>— plugin version");
        if (source.getSender().hasPermission("betterrecipememory.admin")) {
            source.getSender().sendRichMessage("<yellow>/brm refill [player] <gray>— grant every recipe again");
        }
    }

    private void sendAbout(CommandSourceStack source) {
        source.getSender().sendRichMessage("<gold><bold>BetterRecipeMemory</bold> <gray>(standalone Paper plugin)");
        source.getSender().sendRichMessage("<yellow>— " + AUTHOR_LINE);
        source.getSender().sendRichMessage("<white>Removes vanilla recipe advancements and keeps the full recipe book.");
        source.getSender().sendRichMessage("<aqua><underlined>https://" + REPO);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            return List.of("about", "help", "version", "refill");
        }
        if (args.length == 1) {
            List<String> choices = new ArrayList<>(List.of("about", "help", "version"));
            if (source.getSender().hasPermission("betterrecipememory.admin")) {
                choices.add("refill");
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return choices.stream().filter(choice -> choice.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("refill")
                && source.getSender().hasPermission("betterrecipememory.admin")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
