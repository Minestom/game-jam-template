package net.minestom.jam;

import net.minestom.jam.instance.BlockHandlers;
import net.minestom.jam.instance.Lobby;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.extras.velocity.VelocityProxy;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();

        String secret = System.getenv("VELOCITY_SECRET");
        if (secret != null) {
            VelocityProxy.enable(secret);
        }

        BlockHandlers.register(MinecraftServer.getBlockManager());

        Queue.Manager queues = new Queue.Manager();

        Queue.Commands.register(queues, MinecraftServer.getCommandManager());

        var events = MinecraftServer.getGlobalEventHandler();

        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(Lobby.INSTANCE);
            player.setRespawnPoint(Lobby.SPAWN_POINT);
        });

        events.addListener(PlayerDisconnectEvent.class, event -> {
            final Player player = event.getPlayer();

            queues.dequeue(player);

            Game game = player.getTag(Game.GAME);
            if (game != null) game.onDisconnect(player);
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}