package cn.blockforge.shuangshoujiushishuangshoulo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShuangShouJiushishuangshouloMod.MOD_ID, value = Dist.CLIENT)
public final class OffhandLeftClickClient {
    private static final int ATTACK_INTERVAL_TICKS = 10;

    private static int attackCooldown;
    private static boolean wasAttackDown;
    private static boolean destroyingBlock;
    private static BlockPos destroyingBlockPos;

    private OffhandLeftClickClient() {
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)
                || !player.isUsingItem()
                || player.isPassenger()) {
            return;
        }

        event.getInput().leftImpulse *= 5.0F;
        event.getInput().forwardImpulse *= 5.0F;
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() == 0 && event.getAction() == 0) {
            wasAttackDown = false;
            attackCooldown = 0;
            destroyingBlock = false;
            destroyingBlockPos = null;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        boolean attackDown = minecraft.options.keyAttack.isDown();

        if (player == null || gameMode == null || minecraft.screen != null || !attackDown) {
            stopDestroyingBlock(gameMode);
            wasAttackDown = attackDown;
            attackCooldown = 0;
            return;
        }

        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.OFF_HAND || player.isSpectator()) {
            stopDestroyingBlock(gameMode);
            wasAttackDown = attackDown;
            return;
        }

        if (!wasAttackDown) {
            attackCooldown = 0;
        } else if (attackCooldown > 0) {
            attackCooldown--;
        }

        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            stopDestroyingBlock(gameMode);
        } else if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
            stopDestroyingBlock(gameMode);
            if (attackCooldown == 0) {
                gameMode.attack(player, entityHit.getEntity());
                attackCooldown = ATTACK_INTERVAL_TICKS;
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK && hitResult instanceof BlockHitResult blockHit) {
            continueDestroyingBlock(gameMode, blockHit);
        }

        wasAttackDown = true;
    }

    private static void continueDestroyingBlock(MultiPlayerGameMode gameMode, BlockHitResult blockHit) {
        BlockPos blockPos = blockHit.getBlockPos();
        if (!destroyingBlock || !blockPos.equals(destroyingBlockPos)) {
            if (destroyingBlock) {
                gameMode.stopDestroyBlock();
            }
            destroyingBlock = gameMode.startDestroyBlock(blockPos, blockHit.getDirection());
            destroyingBlockPos = destroyingBlock ? blockPos : null;
        } else {
            gameMode.continueDestroyBlock(blockPos, blockHit.getDirection());
        }
    }

    private static void stopDestroyingBlock(MultiPlayerGameMode gameMode) {
        if (destroyingBlock && gameMode != null) {
            gameMode.stopDestroyBlock();
        }
        destroyingBlock = false;
        destroyingBlockPos = null;
    }
}
