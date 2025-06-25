package net.minestom.jam.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.anvil.AnvilLoader;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class Lobby {

    public static final Instance INSTANCE = createLobbyInstance();

    private static Instance createLobbyInstance() {
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                new AnvilLoader(Path.of("lobby"))
        );

        instance.setTimeRate(0);
        instance.setTime(18000); // Midnight

        instance.eventNode().addListener(AddEntityToInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;

            onJoin(player);
        });

        return instance;
    }

    private static void onJoin(@NotNull Player player) {

    }


}
