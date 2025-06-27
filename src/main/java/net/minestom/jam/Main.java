package net.minestom.jam;

import net.minestom.jam.command.QueueCommands;
import net.minestom.jam.instance.BlockHandlers;
import net.minestom.jam.instance.Lobby;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        BlockHandlers.register(MinecraftServer.getBlockManager());
        QueueCommands.register(MinecraftServer.getCommandManager());

        var events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(Lobby.INSTANCE);
            player.setRespawnPoint(Lobby.SPAWN_POINT);
        });

        events.addListener(PlayerDisconnectEvent.class, event -> {
            QueueManager.get().dequeue(event.getPlayer());
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}