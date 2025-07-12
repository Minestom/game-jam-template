package net.minestom.jam;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentLoop;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.condition.Conditions;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;

public record Queue(@NotNull Set<UUID> players, boolean isPrivate) implements Audience {

    /**
     * The maximum size of queues.
     */
    public static final int MAX_SIZE = 2;

    /**
     * The number of milliseconds after which invites expire.
     */
    public static final long INVITE_EXPIRE_AFTER_MS = 60_000;

    /**
     * The number of seconds it takes for a game to start after a queue is full.
     */
    public static final int GAME_START_DELAY = 3;

    /**
     * The queue system consists of two types of queues: public queues and private queues.
     * <br>
     * Public queues are joined automatically when a player tries to queue with {@code /queue}. If there doesn't exist one,
     * it will be created. Private queues ("parties") can never be joined automatically, and are created with {@code
     * /party}. Both queues allow {@code /invite <username(s)>}, where the player can invite any number of users.
     */
    public static class Manager {

        private final List<Queue> privateQueues, publicQueues;
        private final Map<UUID, Queue> queueMembership;
        private final Object2LongMap<Pair<UUID, UUID>> invites;

        public Manager() {
            this.privateQueues = new ArrayList<>();
            this.publicQueues = new ArrayList<>();
            this.queueMembership = new HashMap<>();
            this.invites = new Object2LongOpenHashMap<>();
        }


        public void joinPublicQueueWithMessages(@NotNull Player player) {
            final UUID uuid = player.getUuid();
            final boolean success = joinPublicQueue(uuid);

            Queue queue = getQueue(uuid);

            if (success) {
                queue.sendMessage(PLAYER_JOINED_QUEUE.apply(player.getUsername()).append(queue.memberCount()));
            } else {
                player.sendMessage(ALREADY_QUEUED.append(queue.memberCount()));
            }
        }

        public boolean joinPublicQueue(@NotNull UUID uuid) {
            if (isQueued(uuid)) return false;

            addToQueue(nextPublicQueue(), uuid);

            return true;
        }

        public boolean createPrivateQueueWithMessages(@NotNull Player player) {
            final UUID uuid = player.getUuid();
            final boolean success = createPrivateQueue(uuid);

            player.sendMessage((success ? CREATED_PRIVATE_QUEUE : ALREADY_QUEUED).append(getQueue(uuid).memberCount()));

            return success;
        }

        public boolean createPrivateQueue(@NotNull UUID uuid) {
            if (isQueued(uuid)) return false;

            Queue queue = createPrivateQueue();
            addToQueue(queue, uuid);

            return true;
        }

        public void dequeueWithMessages(@NotNull Player player) {
            final Queue leftQueue = dequeue(player);

            player.sendMessage(leftQueue != null ? LEFT_QUEUE : NOT_IN_QUEUE);
        }

        public @Nullable Queue dequeue(@NotNull Player player) {
            final UUID uuid = player.getUuid();

            final Queue queue = getQueue(uuid);

            if (queue == null) return null;

            // Remove the player internally
            queueMembership.remove(uuid);
            queue.players().remove(uuid);

            // Send messages to every other player on the team
            queue.sendMessage(PLAYER_LEFT_QUEUE.apply(player.getUsername()).append(queue.memberCount()));

            return queue;
        }

        public boolean invitePlayers(@NotNull Player inviter, @NotNull Set<Player> invitees) {
            final UUID uuid = inviter.getUuid();
            final Queue queue = getQueue(uuid);

            if (queue == null) {
                inviter.sendMessage(MUST_BE_IN_A_QUEUE_TO_INVITE);
                return false;
            }

            inviter.sendMessage(INVITED_PLAYERS.apply(invitees.size()).append(queue.memberCount()));

            for (var invitee : invitees) {
                sendInvite(inviter, invitee, queue);
            }

            return true;
        }

        private boolean sendInvite(@NotNull Player inviter, @NotNull Player invitee, @NotNull Queue queue) {
            if (inviter.getUuid().equals(invitee.getUuid())) {
                inviter.sendMessage(CANNOT_INVITE_YOURSELF);
                return false;
            } else if (queue.players().contains(invitee.getUuid())) {
                inviter.sendMessage(ALREADY_IN_PARTY.apply(invitee.getUsername()));
                return false;
            }

            long currentTime = System.currentTimeMillis();

            var key = Pair.of(inviter.getUuid(), invitee.getUuid());
            long lastInvite = invites.getLong(key);

            if (currentTime - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                String name = inviter.getUsername();

                invitee.sendMessage((queue.isPrivate ? INVITED_PRIVATE_QUEUE : INVITED_PUBLIC_QUEUE).apply(name));
                invitee.sendMessage(CLICK_TO_ACCEPT_INVITE.apply(name));

                invites.put(key, currentTime);
                return true;
            } else {
                inviter.sendMessage(ALREADY_INVITED.apply(invitee.getUsername()));
                return false;
            }
        }

        public boolean acceptWithMessages(@NotNull Player player, @NotNull Player allegedInviter) {
            var pair = Pair.of(allegedInviter.getUuid(), player.getUuid());
            long lastInvite = invites.getLong(pair);

            if (lastInvite == 0) {
                player.sendMessage(HAS_NOT_INVITED.apply(allegedInviter.getUsername()));
            } else if (System.currentTimeMillis() - lastInvite > INVITE_EXPIRE_AFTER_MS) {
                player.sendMessage(INVITE_HAS_EXPIRED.apply(allegedInviter.getUsername()));
            } else if (isQueued(player.getUuid())) {
                player.sendMessage(ALREADY_QUEUED);
            } else if (!isQueued(allegedInviter.getUuid())) {
                player.sendMessage(INVITER_IS_NOT_QUEUED.apply(allegedInviter.getUsername()));
            } else {
                invites.removeLong(pair);

                Queue queue = queueMembership.get(allegedInviter.getUuid());

                addToQueue(queue, player.getUuid());
                queue.sendMessage(PLAYER_JOINED_QUEUE.apply(player.getUsername()).append(queue.memberCount()));

                return true;
            }

            return false;
        }

        /***
         * Returns whether or not the given player is currently queued.
         */
        public boolean isQueued(@NotNull UUID player) {
            return queueMembership.containsKey(player);
        }

        /**
         * Returns the queue that the player is in, or null if there does not exist one.
         */
        public @Nullable Queue getQueue(@NotNull UUID player) {
            return queueMembership.get(player);
        }

        /**
         * Gets a public queue that a player can join.
         */
        private @NotNull Queue nextPublicQueue() {
            // Find a non-full queue
            for (Queue queue : publicQueues) {
                if (queue.players().size() < MAX_SIZE) {
                    return queue;
                }
            }

            // Return an empty queue
            Queue queue = new Queue(new CopyOnWriteArraySet<>(), false);
            publicQueues.add(queue);
            return queue;
        }

        /**
         * Always creates a new private queue specifically for the player.
         */
        private @NotNull Queue createPrivateQueue() {
            Queue queue = new Queue(new CopyOnWriteArraySet<>(), true);
            privateQueues.add(queue);
            return queue;
        }

        /***
         * Attempts to add a player to a queue.
         */
        private void addToQueue(@NotNull Queue queue, @NotNull UUID player) {
            queue.players().add(player);
            queueMembership.put(player, queue);

            if (queue.players().size() < MAX_SIZE) return;

            AtomicInteger counter = new AtomicInteger(GAME_START_DELAY + 1); // one second before actually starting
            MinecraftServer.getSchedulerManager().submitTask(() -> {
                if (queue.players().size() < MAX_SIZE) return TaskSchedule.stop();

                int time = counter.getAndDecrement();
                if (time > GAME_START_DELAY) return TaskSchedule.seconds(1);

                if (time > 0) {
                    queue.sendMessage(GAME_STARTING_IN.apply(time));
                    return TaskSchedule.seconds(1);
                }

                // Start a new game
                queue.sendMessage(STARTING_GAME);
                new Game(queue.players());

                // Remove the queue
                (queue.isPrivate ? privateQueues : publicQueues).remove(queue);
                for (UUID member : queue.players()) {
                    queueMembership.remove(member);
                }
                queue.players().clear(); // Clear queue just in case

                return TaskSchedule.stop();
            }, ExecutionType.TICK_END);
        }
    }

    /**
     * Commands relevant to the {@link Manager}.
     */
    public record Commands(@NotNull Queue.Manager manager) {
        /**
         * Registers every handler to a given queue manager and command manager.
         * The command manager should probably be {@code MinecraftServer.getCommandManager()}.
         */
        public static void register(@NotNull Queue.Manager queues, @NotNull CommandManager manager) {
            var commands = new Commands(queues);

            manager.register(
                    commands.new JoinQueue(),
                    commands.new Party(),
                    commands.new Invite(),
                    commands.new Leave(),
                    commands.new Accept()
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
        public final class JoinQueue extends Command {
            public JoinQueue() {
                super("queue");

                setCondition(Conditions::playerOnly);

                setDefaultExecutor((sender, context) -> {
                    final Player player = (Player) sender;

                    manager.joinPublicQueueWithMessages(player);
                });
            }
        }

        /**
         * Creates a private queue, optionally inviting some users.
         */
        public final class Party extends Command {
            public Party() {
                super("party");

                setCondition(Conditions::playerOnly);

                setDefaultExecutor((sender, context) -> manager.createPrivateQueueWithMessages((Player) sender));
                addSyntax((sender, context) -> {
                    final Player player = (Player) sender;

                    if (player.hasTag(Game.GAME)) {
                        player.sendMessage(CANNOT_QUEUE_IN_GAME);
                        return;
                    }

                    if (manager.createPrivateQueueWithMessages(player)) {
                        manager.invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)));
                    }
                }, PLAYERS);
            }
        }

        /**
         * Tries to invite the provided user(s) to the current private or public queue.
         */
        public final class Invite extends Command {
            public Invite() {
                super("invite");

                setCondition(Conditions::playerOnly);

                setDefaultExecutor((sender, context) -> sender.sendMessage(INVITE_SYNTAX));

                addSyntax((sender, context) -> {
                    final Player player = (Player) sender;

                    if (player.hasTag(Game.GAME)) {
                        player.sendMessage(CANNOT_QUEUE_IN_GAME);
                        return;
                    }

                    manager.invitePlayers(player, coalescePlayers(player, context.get(PLAYERS)));
                }, PLAYERS);
            }
        }

        /**
         * Leaves the current queue, if possible.
         */
        public final class Leave extends Command {
            public Leave() {
                super("leave", "dequeue");

                setCondition(Conditions::playerOnly);

                setDefaultExecutor((sender, context) -> {
                    final Player player = (Player) sender;

                    if (player.hasTag(Game.GAME)) {
                        player.sendMessage(CANNOT_QUEUE_IN_GAME);
                        return;
                    }

                    manager.dequeueWithMessages(player);
                });
            }
        }

        /**
         * Accepts a player's invite to another queue.
         */
        public final class Accept extends Command {
            public Accept() {
                super("accept");

                setCondition(Conditions::playerOnly);

                setDefaultExecutor((sender, context) -> sender.sendMessage(ACCEPT_SYNTAX));

                addSyntax((sender, context) -> {
                    final Player player = (Player) sender;

                    if (player.hasTag(Game.GAME)) {
                        player.sendMessage(CANNOT_QUEUE_IN_GAME);
                        return;
                    }

                    Player invited = coalescePlayer(player, context.get(PLAYER));

                    if (invited == null) {
                        player.sendMessage(UNKNOWN_PLAYER);
                        return;
                    }

                    manager.acceptWithMessages(player, invited);
                }, PLAYER);
            }
        }
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        for (UUID member : players()) {
            Player found = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(member);

            if (found != null) found.sendMessage(message);
        }
    }

    private Component memberCount() {
        return MEMBER_COUNT.apply(players.size());
    }

    private static final Component ALREADY_QUEUED = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are already in a queue! Leave it with /leave!", NamedTextColor.RED)
    );

    private static final Component CREATED_PRIVATE_QUEUE = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You created a new ", NamedTextColor.GRAY),
            Component.text("private", NamedTextColor.DARK_PURPLE),
            Component.text(" queue!", NamedTextColor.GRAY)
    );

    private static final Component MUST_BE_IN_A_QUEUE_TO_INVITE = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You must be in a queue to invite players!", NamedTextColor.RED)
    );

    private static final Component PLAYERS_SUFFIX_PLURAL =
            Component.text(" players!", NamedTextColor.GRAY);

    private static final Component PLAYERS_SUFFIX_SINGULAR =
            Component.text(" player!", NamedTextColor.GRAY);

    private static final IntFunction<Component> INVITED_PLAYERS = players -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" Invited ", NamedTextColor.GRAY),
            Component.text(players, NamedTextColor.WHITE),
            players == 1 ? PLAYERS_SUFFIX_SINGULAR : PLAYERS_SUFFIX_PLURAL
    );

    private static final IntFunction<Component> MEMBER_COUNT = count -> Component.textOfChildren(
            Component.text(" (", NamedTextColor.GRAY),
            Component.text(count, NamedTextColor.GRAY),
            Component.text("/" + MAX_SIZE + ")", NamedTextColor.GRAY)
    );

    private static final Component NOT_IN_QUEUE = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You are not in a queue!", NamedTextColor.RED)
    );

    private static final Component LEFT_QUEUE = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" You left the queue!", NamedTextColor.GRAY)
    );

    private static final Function<String, Component> PLAYER_JOINED_QUEUE = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" " + username, NamedTextColor.WHITE),
            Component.text(" joined the queue!", NamedTextColor.GRAY)
    );

    private static final Function<String, Component> PLAYER_LEFT_QUEUE = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" " + username, NamedTextColor.WHITE),
            Component.text(" left the queue!", NamedTextColor.GRAY)
    );

    private static final Function<String, Component> INVITED_PUBLIC_QUEUE = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" " + username, NamedTextColor.WHITE),
            Component.text(" has invited you to join their ", NamedTextColor.GRAY),
            Component.text("public", NamedTextColor.LIGHT_PURPLE),
            Component.text(" queue!", NamedTextColor.GRAY)
    );

    private static final Function<String, Component> INVITED_PRIVATE_QUEUE = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" " + username, NamedTextColor.WHITE),
            Component.text(" has invited you to join their ", NamedTextColor.GRAY),
            Component.text("private", NamedTextColor.DARK_PURPLE),
            Component.text(" queue!", NamedTextColor.GRAY)
    );

    private static final Function<String, Component> CLICK_TO_ACCEPT_INVITE = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.YELLOW, TextDecoration.BOLD),
            Component.text(" Click here or run ", NamedTextColor.GRAY),
            Component.text("/accept " + username, NamedTextColor.WHITE),
            Component.text(" to accept!", NamedTextColor.GRAY)
    ).clickEvent(ClickEvent.runCommand("/accept " + username));

    private static final Function<String, Component> ALREADY_INVITED = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You have already invited ", NamedTextColor.RED),
            Component.text(username, NamedTextColor.RED),
            Component.text(" to your queue!", NamedTextColor.RED)
    );

    private static final Component CANNOT_INVITE_YOURSELF = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot invite yourself!", NamedTextColor.RED)
    );

    private static final Function<String, Component> ALREADY_IN_PARTY = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" ", NamedTextColor.RED),
            Component.text(username, NamedTextColor.RED),
            Component.text(" is already in your queue!", NamedTextColor.RED)
    );

    private static final Function<String, Component> HAS_NOT_INVITED = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" ", NamedTextColor.RED),
            Component.text(username, NamedTextColor.RED),
            Component.text(" has not invited you to their queue!", NamedTextColor.RED)
    );

    private static final Function<String, Component> INVITE_HAS_EXPIRED = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" The invite from ", NamedTextColor.RED),
            Component.text(username, NamedTextColor.RED),
            Component.text(" has expired!", NamedTextColor.RED)
    );

    private static final Function<String, Component> INVITER_IS_NOT_QUEUED = username -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" ", NamedTextColor.RED),
            Component.text(username, NamedTextColor.RED),
            Component.text(" is not currently in a queue!", NamedTextColor.RED)
    );

    private static final IntFunction<Component> GAME_STARTING_IN = seconds -> Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" Game starting in ", NamedTextColor.GRAY),
            Component.text(seconds, NamedTextColor.WHITE),
            Component.text(seconds != 1 ? " seconds!" : " second!", NamedTextColor.GRAY)
    );

    private static final Component STARTING_GAME = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.GREEN, TextDecoration.BOLD),
            Component.text(" Starting game!", NamedTextColor.GRAY)
    );

    private static final Component CANNOT_QUEUE_IN_GAME = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" You cannot run any queue commands as you are in a game!", NamedTextColor.RED)
    );

    private static final Component UNKNOWN_PLAYER = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" Unknown player!", NamedTextColor.RED)
    );

    private static final Component ACCEPT_SYNTAX = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /accept syntax: /accept <player>", NamedTextColor.RED)
    );

    private static final Component INVITE_SYNTAX = Component.textOfChildren(
            Component.text("[!]", NamedTextColor.RED, TextDecoration.BOLD),
            Component.text(" /invite syntax: /invite <player(s)>", NamedTextColor.RED)
    );
}