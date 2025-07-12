package net.minestom.jam;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.jam.instance.Lobby;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final List<Player> players = new ArrayList<>();
    private final AtomicBoolean ending = new AtomicBoolean(false);

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

    public void onGameEnd() {
        ending.set(true);

        for (Player player : players) {
            player.setInstance(Lobby.INSTANCE, Lobby.SPAWN_POINT);
            player.removeTag(GAME);
        }

        players.clear();
        GAMES.remove(this);
    }

    public void onDisconnect(@NotNull Player player) {
        players.remove(player);

        sendMessage(PLAYER_HAS_LEFT.apply(player.getUsername()));

        // As an example, we end the game if there's one player left
        if (players.size() == 1) {
            onGameEnd();
        }
    }

    private static final Function<String, Component> PLAYER_HAS_LEFT = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" ", NamedTextColor.GRAY),
            Component.text(username, NamedTextColor.GRAY),
            Component.text(" has left the game!", NamedTextColor.GRAY)
    );

}
