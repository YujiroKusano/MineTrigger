package com.sigulog.minetrigger.weapon.defense;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * エスクード — 障壁展開とカタパルト射出の防御系トリガー。
 *
 * 通常発動: 真下のブロックと同種のブロックを正面に 3×3 で建てる
 * 特殊技  : カタパルト（プレイヤーを上方向＋前方に射出）
 */
public class EscudoItem extends WeaponItem {

    public EscudoItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        ServerWorld world = player.getServerWorld();

        // ── 真下のブロックを素材として使用 ──────────────────────────
        BlockPos below = player.getBlockPos().down();
        BlockState material = world.getBlockState(below);

        // 空気・液体・ベドロック等はオブシディアンで代替
        if (material.isAir()
                || !material.isOpaqueFullCube(world, below)
                || material.getHardness(world, below) < 0) {
            material = Blocks.OBSIDIAN.getDefaultState();
        }

        // ── プレイヤーの向きに応じて 3×3 壁を建設 ──────────────────
        Direction facing  = player.getHorizontalFacing();
        Direction lateral = facing.rotateYClockwise();

        int placed = 0;
        for (int height = 0; height < 3; height++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos wallPos = player.getBlockPos()
                    .offset(facing,  1)
                    .offset(lateral, side)
                    .add(0, height, 0);
                if (world.getBlockState(wallPos).isAir()) {
                    world.setBlockState(wallPos, material);
                    placed++;
                }
            }
        }
        player.sendMessage(Text.literal("§9[ エスクード ]§r 障壁展開 (" + placed + "ブロック)"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f);
        Vec3d boost = new Vec3d(look.x * 0.5, p.catapultVelocity, look.z * 0.5);
        player.setVelocity(boost);
        player.velocityModified = true;
        player.sendMessage(Text.literal("§b[ エスクード ]§r カタパルト発射"), true);
    }
}
