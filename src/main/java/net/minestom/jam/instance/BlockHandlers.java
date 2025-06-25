package net.minestom.jam.instance;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class BlockHandlers {

    public static class Sign implements BlockHandler {

        public static final Key KEY = Key.key("sign");

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

}
