package com.nullarchive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Slow-burn overworld horror. Events are deliberately rare and cosmetic:
 * this first layer never edits terrain, damages players, or deletes builds.
 */
@Mod.EventBusSubscriber(modid = NullArchive.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldAnomalyEvents {
    private static final String PLAY_TIME = "nullarchive_world_ticks";
    private static final String NEXT_EVENT = "nullarchive_world_event_next";

    private WorldAnomalyEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;

        var data = player.getPersistentData();
        long played = data.getLong(PLAY_TIME) + 1L;
        data.putLong(PLAY_TIME, played);

        // Let the player settle into an ordinary world before the first anomaly.
        if (played < 2400L) return;
        long now = level.getGameTime();
        if (now < data.getLong(NEXT_EVENT)) return;
        if (player.getRandom().nextInt(12000) != 0) return;

        int tier = played < 12000L ? 0 : played < 36000L ? 1 : 2;
        triggerEvent(player, level, tier);
        data.putLong(NEXT_EVENT, now + 2400L + player.getRandom().nextInt(3600));
    }

    private static void triggerEvent(Player player, ServerLevel level, int tier) {
        int event = player.getRandom().nextInt(tier == 0 ? 2 : 5);
        float yaw = player.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 behind = player.position().add(-Math.sin(yaw) * 5.0, 0.0, Math.cos(yaw) * 5.0);
        Vec3 side = player.position().add(Math.cos(yaw) * 6.0, 0.0, Math.sin(yaw) * 6.0);

        switch (event) {
            case 0 -> {
                level.playSound(null, behind.x, behind.y, behind.z,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.35F, 0.45F);
                player.displayClientMessage(Component.literal("SIGNAL // You hear footsteps behind you. When you turn, they stop."), true);
            }
            case 1 -> {
                level.playSound(null, side.x, side.y + 2.0, side.z,
                        SoundEvents.NOTE_BLOCK_BIT, SoundSource.AMBIENT, 0.28F, 0.35F);
                player.displayClientMessage(Component.literal("WORLD // A sound came from somewhere that should be empty."), true);
            }
            case 2 -> {
                level.sendParticles(ParticleTypes.SMOKE, side.x, side.y + 1.0, side.z,
                        7, 0.25, 0.55, 0.25, 0.01);
                level.sendParticles(ParticleTypes.PORTAL, side.x, side.y + 1.0, side.z,
                        5, 0.2, 0.45, 0.2, 0.02);
                player.displayClientMessage(Component.literal("ANOMALY // Something stood between the trees. You blinked. It was gone."), true);
            }
            case 3 -> {
                level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE,
                        SoundSource.AMBIENT, 0.22F, 0.55F);
                player.displayClientMessage(Component.literal("ARCHIVE // Your world has been recorded."), true);
            }
            default -> {
                level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 2.0, player.getZ(),
                        18, 0.8, 0.5, 0.8, 0.005);
                player.displayClientMessage(Component.literal("NULL // The air briefly fills with something like static."), true);
            }
        }
    }
}
