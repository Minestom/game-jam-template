package net.minestom.jam.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.anvil.AnvilLoader;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * The lobby world. Loads an anvil world from the path {@code ./lobby}.
 */
public final class Lobby {

    /**
     * The spawn point in the instance. Make sure to change this when changing the world!
     */
    public static final Pos SPAWN_POINT = new Pos(0.5, 67, 0.5, 0, 0);

    public static final Instance INSTANCE = createLobbyInstance();

    private static Instance createLobbyInstance() {
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                new AnvilLoader(Path.of("lobby"))
        );

        instance.setTimeRate(0);
        instance.setTime(6000); // Noon

        instance.eventNode().addListener(AddEntityToInstanceEvent.class, event -> {
            if (!(event.getEntity() instanceof Player player)) return;

            onJoin(player);
        }).addListener(PlayerMoveEvent.class, event -> {
            final Player player = event.getPlayer();

            if (player.getPosition().y() < 0) {
                player.teleport(SPAWN_POINT);
            }
        }).addListener(PlayerBlockBreakEvent.class, event -> {
            event.setCancelled(true);
        });

        return instance;
    }

    private static void onJoin(@NotNull Player player) {

    }


}
