package net.minestom.jam.command;

import net.minestom.jam.QueueManager;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLoop;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Commands relevant to the {@link QueueManager}.
 */
public final class QueueCommands {

    /**
     * Registers every handler to a given command manager.
     * This should probably be {@code MinecraftServer.getCommandManager()}.
     */
    public static void register(@NotNull CommandManager manager) {
        manager.register(
                new Queue(),
                new Party(),
                new Invite(),
                new Leave(),
                new Accept()
        );
    }

    private static final Argument<EntityFinder> PLAYER = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

    private static final ArgumentLoop<EntityFinder> PLAYERS = ArgumentType.Loop("players", PLAYER);

    private static @Nullable Player coalescePlayer(@NotNull Player player, @NotNull EntityFinder finder) {
        List<Entity> found = finder.find(player);

        return found.isEmpty() ? null : (Player) found.getFirst();
    }

    private static Set<Player> coalescePlayers(@NotNull Player player, @NotNull List<EntityFinder> finders) {
        Set<Player> players = new HashSet<>();

        for (var finder : finders) {
            @Nullable Player invited = coalescePlayer(player, finder);

            if (invited != null) players.add(invited);
        }

        return players;
    }

    /**
     * Tries to join a public queue, creating one if it does not exist.
     */
    public static final class Queue extends Command {
        public Queue() {
            super("queue");

            setCondition(Conditions::playerOnly);

            setDefaultExecutor((sender, context) -> {
                final Player player = (Player) sender;

                QueueManager.get().joinPublicQueueWithMessages(player);
            });
        }
    }

    /**
     * Creates a private queue, optionally inviting some users.
     */
    public static final class Party extends Command {
        public Party() {
            super("party");

            setCondition(Conditions::playerOnly);

            setDefaultExecutor((sender, context) -> QueueManager.get().createPrivateQueueWithMessages((Player) sender));
            addSyntax((sender, context) -> {
                final Player player = (Player) sender;

                if (QueueManager.get().createPrivateQueueWithMessages(player)) {
                    QueueManager.get().invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)));
                }
            }, PLAYERS);
        }
    }

    /**
     * Tries to invite the provided user(s) to the current private or public queue.
     */
    public static final class Invite extends Command {
        public Invite() {
            super("invite");

            setCondition(Conditions::playerOnly);

            // TODO: Error in default executor

            addSyntax((sender, context) -> {
                final Player player = (Player) sender;

                QueueManager.get().invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)));
            }, PLAYERS);
        }
    }

    /**
     * Leaves the current queue, if possible.
     */
    public static final class Leave extends Command {
        public Leave() {
            super("leave", "dequeue");

            setCondition(Conditions::playerOnly);

            setDefaultExecutor((sender, context) -> {
                final Player player = (Player) sender;

                QueueManager.get().dequeueWithMessages(player);
            });
        }
    }

    /**
     * Accepts a player's invite to another queue.
     */
    public static final class Accept extends Command {
        public Accept() {
            super("accept");

            setCondition(Conditions::playerOnly);

            // TODO: Error in default executor

            addSyntax((sender, context) -> {
                final Player player = (Player) sender;

                QueueManager.get().acceptWithMessages(player, coalescePlayer(player, context.get(PLAYER)));
            }, PLAYER);
        }
    }

}

