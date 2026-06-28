package dev.donetasy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;

public class Fishblocker implements ModInitializer {

    private boolean debugEnabled = false;


    private int stableTicks = 0;
    private boolean armed = false;
    private int waitTicks = 0;
    private boolean recastPending = false;
    private FishingHook lastHook = null;

    private double Sensitivity = 0.015;

    private double cfgSensitivity = 0.015;


    private int cfgStableTicks = 1;


    private int cfgWaitCatch = 20;


    private int cfgWaitRecast = 15;

    @Override
    public void onInitialize() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("fishblocker")
                    // /fishblocker toggle
                    .then(ClientCommandManager.literal("toggle")
                            .executes(context -> {
                                debugEnabled = !debugEnabled;
                                context.getSource().getPlayer().displayClientMessage(
                                        Component.literal("§7[Fishblocker] Auto-Fisher " + (debugEnabled ? "§aActive" : "§cDeactivated")),
                                        false
                                );
                                return 1;
                            })
                    )
                    // /fishblocker config
                    .then(ClientCommandManager.literal("config")
                            // /fishblocker config stableTicks <int>
                            .then(ClientCommandManager.literal("stableTicks")
                                    .then(ClientCommandManager.argument("wert", IntegerArgumentType.integer(1))
                                            .executes(context -> {
                                                cfgStableTicks = IntegerArgumentType.getInteger(context, "int");
                                                context.getSource().getPlayer().displayClientMessage(
                                                        Component.literal("§a[Fishblocker] set StableTick to  " + cfgStableTicks + " Ticks."), false);
                                                return 1;
                                            })
                                    )
                            )
                            // /fishblocker config waitCatch <int>
                            .then(ClientCommandManager.literal("waitCatch")
                                    .then(ClientCommandManager.argument("wert", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                cfgWaitCatch = IntegerArgumentType.getInteger(context, "int");
                                                context.getSource().getPlayer().displayClientMessage(
                                                        Component.literal("§a[Fishblocker] Set WaitCatch to " + cfgWaitCatch + " Ticks ."), false);
                                                return 1;
                                            })
                                    )
                            )
                            // /fishblocker config waitRecast <int>
                            .then(ClientCommandManager.literal("waitRecast")
                                    .then(ClientCommandManager.argument("wert", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                cfgWaitRecast = IntegerArgumentType.getInteger(context, "int");
                                                context.getSource().getPlayer().displayClientMessage(
                                                        Component.literal("§a[Fishblocker] Set WaitRecast to " + cfgWaitRecast + " Ticks."), false);
                                                return 1;
                                            })
                                    )
                            )
                            // /fishblocker config waitRecast <doubble>
                            .then(ClientCommandManager.literal("Sensitivity")
                                    .then(ClientCommandManager.argument("doubble", IntegerArgumentType.integer(0))
                                            .executes(context -> {
                                                cfgSensitivity = IntegerArgumentType.getInteger(context, "int");
                                                context.getSource().getPlayer().displayClientMessage(
                                                        Component.literal("§a[Fishblocker] Set Sensitivity to " + cfgSensitivity + " Ymovement."), false);
                                                return 1;
                                            })
                                    )
                            )
                    )
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null) return;

            if (recastPending) {
                if (waitTicks > 0) {
                    waitTicks--;
                } else {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    recastPending = false;
                    waitTicks = cfgWaitRecast;
                }
                return;
            }

            if (waitTicks > 0) {
                waitTicks--;
                return;
            }

            FishingHook bobber = client.player.fishing;

            if (debugEnabled) {
                String debugMsg = "§eno rod thrown";

                if (bobber != null) {
                    double currentMotionY = bobber.getDeltaMovement().y;
                    debugMsg = "armed=" + armed + " | stableTicks=" + stableTicks +
                            " | motionY=" + String.format("%.3f", currentMotionY);
                }

                client.player.displayClientMessage(Component.literal(debugMsg), true);
            }

            if (bobber == null) {
                lastHook = null;
                armed = false;
                stableTicks = 0;
                return;
            }

            if (lastHook != bobber) {
                lastHook = bobber;
                armed = false;
                stableTicks = 0;
            }

            boolean inFluid = bobber.isInWater() || bobber.isInLava();
            double motionY = bobber.getDeltaMovement().y;


            boolean stable = inFluid && Math.abs(motionY) < Sensitivity;
            boolean drop = motionY < -0.02;

            if (stable) {
                stableTicks++;

                if (stableTicks >= cfgStableTicks) {
                    armed = true;
                }
            } else {
                stableTicks = 0;
            }

            if (armed && drop && debugEnabled) {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);

                client.player.displayClientMessage(
                        Component.literal("§a[fishblocker] hook"),
                        false
                );

                armed = false;
                stableTicks = 0;
                lastHook = null;

                recastPending = true;
                waitTicks = cfgWaitCatch;
            }
        });
    }
}
