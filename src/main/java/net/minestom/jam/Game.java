package net.minestom.jam;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class Game implements Audience {

    /**
     * The spawn point in the instance. Make sure to change this when changing the world!
     */
    public static final Pos SPAWN_POINT = new Pos(0.5, 65, 0.5, 0, 0);

    /**
     * The game that a player is in.
     */
    public static final Tag<Game> GAME = Tag.Transient("Game");

    private static final @NotNull Set<Game> GAMES = new HashSet<>();

    private static InstanceContainer createGameInstance() {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(
                new AnvilLoader(Path.of("game"))
        );

        instance.setTimeRate(0);
        instance.setTime(6000); // Noon

        return instance;
    }

    private final InstanceContainer instance;
    private final Set<Player> players = new HashSet<>();

    public Game(@NotNull Set<UUID> players) {
        this.instance = createGameInstance();

        for (UUID uuid : players) {
            Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid);
            if (player == null) continue;

            this.players.add(player);
            player.setTag(GAME, this);

            player.setInstance(instance, SPAWN_POINT);
        }

        GAMES.add(this);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    public void onDisconnect(@NotNull Player player) {
        players.remove(player);

        sendMessage(PLAYER_HAS_LEFT.apply(player.getUsername()));
    }

    private static final Function<String, Component> PLAYER_HAS_LEFT = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" ", NamedTextColor.GRAY),
            Component.text(username, NamedTextColor.GRAY),
            Component.text(" has left the game!", NamedTextColor.GRAY)
    );

}
