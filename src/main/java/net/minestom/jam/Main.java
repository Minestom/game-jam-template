package net.minestom.jam;

import net.minestom.jam.instance.BlockHandlers;
import net.minestom.jam.instance.Lobby;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;

public class Main {
    public static void main(String[] args) {
        MinecraftServer.init();

        MinecraftServer minecraftServer = MinecraftServer.init();

        var process = MinecraftServer.process();

        BlockHandlers.register(process.block());

        MinecraftServer.process().eventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(Lobby.INSTANCE);
            player.setRespawnPoint(Lobby.SPAWN_POINT);
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}