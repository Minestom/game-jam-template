package net.minestom.jam;

import net.minestom.jam.instance.BlockHandlers;
import net.minestom.jam.instance.Lobby;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;

public class Main {
    public static void main(String[] args) {
        MinecraftServer.init();

        MinecraftServer minecraftServer = MinecraftServer.init();

        MinecraftServer.getBlockManager().registerHandler(BlockHandlers.Sign.KEY, BlockHandlers.Sign::new);

        MinecraftServer.process().eventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(Lobby.INSTANCE);
            player.setRespawnPoint(new Pos(0, 40, 0));
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}