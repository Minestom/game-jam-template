package net.minestom.jam.instance;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.block.BlockManager;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Block handlers for signs, hanging signs, player heads, and banners.
 * <br>
 * These don't handle any logic; they just let the server know which tags need to be sent to the client.
 */
public final class BlockHandlers {

    /**
     * Registers every handler to a given block manager.
     * This should probably be {@code MinecraftServer.getBlockManager()}.
     */
    public static void register(@NotNull BlockManager manager) {
        manager.registerHandler(Sign.KEY, Sign::new);
        manager.registerHandler(HangingSign.KEY, HangingSign::new);
        manager.registerHandler(PlayerHead.KEY, PlayerHead::new);
        manager.registerHandler(Banner.KEY, Banner::new);
    }

    public static class Sign implements BlockHandler {

        private static final Key KEY = Key.key("sign");

        private static final List<Tag<?>> TAGS = List.of(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text")
        );

        @Override
        public @NotNull Key getKey() {
            return KEY;
        }

        @Override
        public @NotNull Collection<Tag<?>> getBlockEntityTags() {
            return TAGS;
        }
    }

    public static class HangingSign implements BlockHandler {

        private static final Key KEY = Key.key("hanging_sign");

        private static final List<Tag<?>> TAGS = List.of(
                Tag.Boolean("is_waxed"),
                Tag.NBT("front_text"),
                Tag.NBT("back_text")
        );

        @Override
        public @NotNull Key getKey() {
            return KEY;
        }

        @Override
        public @NotNull Collection<Tag<?>> getBlockEntityTags() {
            return TAGS;
        }
    }

    public static class PlayerHead implements BlockHandler {

        private static final Key KEY = Key.key("skull");

        private static final List<Tag<?>> TAGS = List.of(
                Tag.NBT("profile")
        );

        @Override
        public @NotNull Key getKey() {
            return KEY;
        }

        @Override
        public @NotNull Collection<Tag<?>> getBlockEntityTags() {
            return TAGS;
        }
    }

    public static class Banner implements BlockHandler {

        private static final Key KEY = Key.key("banner");

        private static final List<Tag<?>> TAGS = List.of(
                Tag.NBT("patterns")
        );

        @Override
        public @NotNull Key getKey() {
            return KEY;
        }

        @Override
        public @NotNull Collection<Tag<?>> getBlockEntityTags() {
            return TAGS;
        }
    }

}
