package net.minestom.jam;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class Main {
    public static void main(String[] args) {
        MinecraftServer.init();

        MinecraftServer minecraftServer = MinecraftServer.init();

        Instance instance = MinecraftServer.process().instance().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.STONE));

        MinecraftServer.process().eventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();

            event.setSpawningInstance(instance);
            player.setRespawnPoint(new Pos(0, 40, 0));
        });

        minecraftServer.start("0.0.0.0", 25565);
    }
}