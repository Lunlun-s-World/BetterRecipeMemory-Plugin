package io.github.kuohsuanlo.betterrecipememory;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/** Registers the datapack embedded in this plugin before vanilla registries load. */
@SuppressWarnings("UnstableApiUsage")
public final class BetterRecipeMemoryBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            URL pack = Objects.requireNonNull(
                    BetterRecipeMemoryBootstrap.class.getResource("/better_recipe_memory_pack"),
                    "Embedded BetterRecipeMemory datapack is missing from the plugin jar"
            );

            try {
                if (event.registrar().discoverPack(
                        pack.toURI(),
                        "core",
                        configurer -> configurer.autoEnableOnServerStart(true)
                ) == null) {
                    throw new IllegalStateException("Paper rejected the embedded BetterRecipeMemory datapack");
                }
            } catch (URISyntaxException | IOException exception) {
                throw new IllegalStateException("Could not register the embedded BetterRecipeMemory datapack", exception);
            }
        });
    }
}
