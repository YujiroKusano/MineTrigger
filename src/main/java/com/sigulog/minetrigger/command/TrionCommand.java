package com.sigulog.minetrigger.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.sigulog.minetrigger.core.TrionSystem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * トリオン操作コマンド。
 *
 * /trion heal [player]           → トリオンを全回復
 * /trion set <amount> [player]   → トリオンを指定量にセット
 * /trion add <amount> [player]   → トリオンを加算
 * /trion get [player]            → 現在のトリオン量を表示
 */
public final class TrionCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("trion")
                    .requires(src -> src.hasPermissionLevel(2))

                    // /trion heal
                    .then(CommandManager.literal("heal")
                        .executes(ctx -> healSelf(ctx))
                        .then(CommandManager.argument("target", EntityArgumentType.players())
                            .executes(ctx -> healTargets(ctx))))

                    // /trion set <amount>
                    .then(CommandManager.literal("set")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0))
                            .executes(ctx -> setSelf(ctx, FloatArgumentType.getFloat(ctx, "amount")))
                            .then(CommandManager.argument("target", EntityArgumentType.players())
                                .executes(ctx -> setTargets(ctx, FloatArgumentType.getFloat(ctx, "amount"))))))

                    // /trion add <amount>
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                            .executes(ctx -> addSelf(ctx, FloatArgumentType.getFloat(ctx, "amount")))
                            .then(CommandManager.argument("target", EntityArgumentType.players())
                                .executes(ctx -> addTargets(ctx, FloatArgumentType.getFloat(ctx, "amount"))))))

                    // /trion get
                    .then(CommandManager.literal("get")
                        .executes(ctx -> getSelf(ctx))
                        .then(CommandManager.argument("target", EntityArgumentType.players())
                            .executes(ctx -> getTargets(ctx))))
            );
        });
    }

    // ──────────────────────────────────────────────────────────────
    // heal
    // ──────────────────────────────────────────────────────────────

    private static int healSelf(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("プレイヤーから実行してください")); return 0; }
        heal(player);
        src.sendFeedback(() -> Text.literal("§b" + player.getName().getString() + "§r のトリオンを全回復しました"), false);
        return 1;
    }

    private static int healTargets(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        for (ServerPlayerEntity p : targets) {
            heal(p);
        }
        ctx.getSource().sendFeedback(() ->
            Text.literal("§b" + targets.size() + "人§r のトリオンを全回復しました"), true);
        return targets.size();
    }

    private static void heal(ServerPlayerEntity player) {
        TrionSystem.setTrion(player, TrionSystem.getMaxTrion(player));
    }

    // ──────────────────────────────────────────────────────────────
    // set
    // ──────────────────────────────────────────────────────────────

    private static int setSelf(CommandContext<ServerCommandSource> ctx, float amount) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("プレイヤーから実行してください")); return 0; }
        TrionSystem.setTrion(player, amount);
        src.sendFeedback(() -> Text.literal(
            "§b" + player.getName().getString() + "§r のトリオンを §e" + (int) amount + "§r にセットしました"), false);
        return 1;
    }

    private static int setTargets(CommandContext<ServerCommandSource> ctx, float amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        for (ServerPlayerEntity p : targets) {
            TrionSystem.setTrion(p, amount);
        }
        ctx.getSource().sendFeedback(() ->
            Text.literal("§b" + targets.size() + "人§r のトリオンを §e" + (int) amount + "§r にセットしました"), true);
        return targets.size();
    }

    // ──────────────────────────────────────────────────────────────
    // add
    // ──────────────────────────────────────────────────────────────

    private static int addSelf(CommandContext<ServerCommandSource> ctx, float amount) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("プレイヤーから実行してください")); return 0; }
        float newVal = TrionSystem.getTrion(player) + amount;
        TrionSystem.setTrion(player, newVal);
        src.sendFeedback(() -> Text.literal(
            "§b" + player.getName().getString() + "§r のトリオンに §e" + (int) amount + "§r 加算しました"), false);
        return 1;
    }

    private static int addTargets(CommandContext<ServerCommandSource> ctx, float amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        for (ServerPlayerEntity p : targets) {
            TrionSystem.setTrion(p, TrionSystem.getTrion(p) + amount);
        }
        ctx.getSource().sendFeedback(() ->
            Text.literal("§b" + targets.size() + "人§r のトリオンに §e" + (int) amount + "§r 加算しました"), true);
        return targets.size();
    }

    // ──────────────────────────────────────────────────────────────
    // get
    // ──────────────────────────────────────────────────────────────

    private static int getSelf(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("プレイヤーから実行してください")); return 0; }
        float cur = TrionSystem.getTrion(player);
        float max = TrionSystem.getMaxTrion(player);
        src.sendFeedback(() -> Text.literal(
            "§b" + player.getName().getString() + "§r のトリオン: §e" + (int) cur + " / " + (int) max), false);
        return 1;
    }

    private static int getTargets(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
        ServerCommandSource src = ctx.getSource();
        for (ServerPlayerEntity p : targets) {
            float cur = TrionSystem.getTrion(p);
            float max = TrionSystem.getMaxTrion(p);
            src.sendFeedback(() -> Text.literal(
                "§b" + p.getName().getString() + "§r のトリオン: §e" + (int) cur + " / " + (int) max), false);
        }
        return targets.size();
    }

    private TrionCommand() {}
}
