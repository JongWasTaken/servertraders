package dev.smto.servertraders.trading;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.MerchantGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerData;
import dev.smto.servertraders.command.CommandManager;
import dev.smto.servertraders.ServerTraders;
import dev.smto.servertraders.util.CodecUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TraderManager {
    private static final String COMMANDTAG_KEY = "servertradersReference";
    private static final List<TraderDefinition> TRADERS = new ArrayList<>();
    public static List<TraderDefinition> getTraders() { return TraderManager.TRADERS; }

    public static TraderDefinition getTraderByName(String name) {
        return TraderManager.TRADERS.stream().filter((t) -> t.identifier().equals(name)).findFirst().orElse(null);
    }
    
    public static void reloadFromDisk() {
        try {
            if (!ServerTraders.CONFIG_FOLDER_PATH.toFile().exists()) {
                Files.createDirectories(ServerTraders.CONFIG_FOLDER_PATH);
                return;
            }
            if (ServerTraders.CONFIG_FOLDER_PATH.toFile().listFiles() == null) return;
            if (Objects.requireNonNull(ServerTraders.CONFIG_FOLDER_PATH.toFile().listFiles()).length == 0) return;
            TraderManager.TRADERS.clear();
            for (File file : Objects.requireNonNull(ServerTraders.CONFIG_FOLDER_PATH.toFile().listFiles())) {
                if (file.getName().endsWith(".json")) {
                    try {
                        TraderManager.TRADERS.add(CodecUtils.fromJsonString(Files.readString(file.toPath()), TraderDefinition.CODEC));
                        ServerTraders.LOGGER.info("Loaded trader {} from disk", TraderManager.TRADERS.getLast().identifier());
                    } catch (Throwable e) {
                        ServerTraders.LOGGER.error("Failed to load trader from {}", file);
                    }
                }
            }
        } catch (Throwable ignored) {
            ServerTraders.LOGGER.error("Failed to load traders from disk!");
        }
        if (TraderManager.TRADERS.size() <= 9) {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X1;
        } else if (TraderManager.TRADERS.size() <= 18) {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X2;
        } else if (TraderManager.TRADERS.size() <= 27) {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X3;
        } else if (TraderManager.TRADERS.size() <= 36) {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X4;
        } else if (TraderManager.TRADERS.size() <= 45) {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X5;
        } else {
            MasterMenu.type = ScreenHandlerType.GENERIC_9X6;
        }

        if (TraderManager.TRADERS.size() > 54) {
            ServerTraders.LOGGER.warn("More than 54 traders have been loaded! Menus cannot currently display more than that, so these will be ignored.");
        }

        if (TraderManager.TRADERS.isEmpty()) {
            var t = new TraderDefinition("Example Trader", "example_trader", false, TraderVillagerDefinition.DEFAULT, List.of(
                    SimpleOffer.simple(Items.EMERALD.getDefaultStack().copyWithCount(1), Items.DIRT.getDefaultStack().copyWithCount(64)),
                    SimpleOffer.simple(Items.EMERALD.getDefaultStack().copyWithCount(2), Items.COBBLESTONE.getDefaultStack().copyWithCount(64)),
                    SimpleOffer.simple(Items.EMERALD.getDefaultStack().copyWithCount(4), Items.OAK_LOG.getDefaultStack().copyWithCount(64))
            ));
            if (TraderManager.acceptNewTraderDefinition(t, "example_trader")) {
                ServerTraders.LOGGER.info("Created example trader!");
            }
        }
    }

    public static boolean acceptNewTraderDefinition(TraderDefinition trader, String fileName) {
        try {
            JsonElement jsonElement = JsonParser.parseString(CodecUtils.toJsonString(trader, TraderDefinition.CODEC));
            Files.writeString(ServerTraders.CONFIG_FOLDER_PATH.resolve(fileName + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(jsonElement));
            if (!TraderManager.TRADERS.contains(trader)) TraderManager.TRADERS.add(trader);
        } catch (IOException e) {
            ServerTraders.LOGGER.error("Failed to write trader {} to disk!\n{}", fileName, e);
            return false;
        }
        return true;
    }

    public static boolean checkInteractedVillager(PlayerEntity player, VillagerEntity villager) {
        for (String commandTag : villager.getCommandTags()) {
            if (commandTag.contains(TraderManager.COMMANDTAG_KEY) && !commandTag.contains("Fallback")) {
                var tr = TraderManager.getTraderByName(commandTag.replaceAll(TraderManager.COMMANDTAG_KEY + "=", ""));
                if (tr != null) {
                    try {
                        TraderManager.updateVillager(villager, tr);
                        TraderManager.openTraderMenuFor((ServerPlayerEntity) player, tr);
                    } catch (Throwable ignored) {}
                } else player.sendMessage(Text.literal(ServerTraders.Config.invalidTraderText).formatted(Formatting.DARK_RED), false);
                return true;
            }
        }
        return false;
    }

    public static void updateVillager(VillagerEntity villager, TraderDefinition data) {
        villager.setCustomName(Text.literal(data.name()));
        villager.setVillagerData(new VillagerData(data.villager().type(), data.villager().profession(), 5));
    }

    public static void openMasterMenuFor(ServerPlayerEntity player) {
        new MasterMenu(player).open();
    }

    public static void openTraderMenuFor(ServerPlayerEntity player, TraderDefinition trades) {
        new TraderMenu(player, trades).open();
    }

    public static void openTraderPlacingMenuFor(ServerPlayerEntity player) {
        new TraderPlacingGui(MasterMenu.type, player).open();
    }

    public static String prettifyName(String name) {
        StringBuilder out = new StringBuilder();
        for (String s : name.replaceAll("-", "_").replaceAll(" ", "_").split("_")) {
            out.append(s.substring(0, 1).toUpperCase()).append(s.substring(1));
            out.append(" ");
        }
        out = new StringBuilder(out.toString().trim());
        return out.toString();
    }

    private static class MasterMenu extends SimpleGui {
        public static ScreenHandlerType<GenericContainerScreenHandler> type = ScreenHandlerType.GENERIC_9X1;
        public MasterMenu(ServerPlayerEntity player) {
            super(MasterMenu.type, player, false);
        }
        @Override
        public void beforeOpen() {
            super.beforeOpen();
            for (var trader : TraderManager.TRADERS) {
                if (!trader.hidden()) {
                    this.addSlot(GuiElementBuilder.from(trader.trades().getFirst().sell())
                            .setName((Text.literal(TraderManager.prettifyName(trader.identifier())).formatted(Formatting.GOLD).append(Text.literal(" - ").formatted(Formatting.GRAY)).append(Text.literal(trader.name()).formatted(Formatting.AQUA))))
                            .setRarity(Rarity.COMMON)
                            .hideDefaultTooltip()
                            .setCount(1)
                            .setCallback(() -> {
                                this.close();
                                TraderManager.openTraderMenuFor(this.player, trader);
                            })
                            .build()
                    );
                }
            }
        }

        @Override
        public Text getTitle() {
            return Text.literal(ServerTraders.Config.shopMenuTitleText);
        }
    }

    private static class TraderMenu extends MerchantGui {
        private final TraderDefinition trades;
        public TraderMenu(ServerPlayerEntity player, TraderDefinition trades) {
            super(player, false);
            this.trades = trades;
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();
            for (SimpleOffer trade : this.trades.trades()) {
                this.addTrade(trade.toVanilla());
            }
        }

        @Override
        public Text getTitle() {
            return Text.literal(this.trades.name());
        }

        @Override
        public void onSelectTrade(TradeOffer offer) {
            this.merchantInventory.markDirty();
            this.player.getInventory().markDirty();
            this.sendUpdate();
        }
    }

    private static class TraderPlacingGui extends SimpleGui {
        private TraderPlacingGui(ScreenHandlerType<GenericContainerScreenHandler> type, ServerPlayerEntity player) {
            super(type, player, false);
        }

        public void placeTrader(int sel) {
            var t = TraderManager.getTraders().get(sel);
            var villagerData = t.villager();

            var ent = new VillagerEntity(EntityType.VILLAGER, this.player.getWorld());
            this.player.getWorld().spawnEntity(ent);

            ent.setVillagerData(new VillagerData(villagerData.type(), villagerData.profession(), 5));
            //ent.setPosition(target.toCenterPos().offset(Direction.UP, 0.51));
            ent.setPosition(this.player.getPos());
            ent.setHeadYaw(this.player.getHeadYaw());
            ent.setPitch(this.player.getPitch());
            ent.setAiDisabled(true);
            ent.setNoGravity(true);
            ent.setInvulnerable(true);
            ent.setNoDrag(true);
            ent.setPersistent();
            ent.setSilent(true);
            ent.setCustomName(Text.literal(t.name()));
            ent.setCustomNameVisible(true);
            ent.addCommandTag(TraderManager.COMMANDTAG_KEY + "=" + t.identifier());
            ent.addCommandTag(TraderManager.COMMANDTAG_KEY + "Fallback=" + sel);

            int random = this.player.getWorld().getRandom().nextInt();
            ent.addCommandTag(TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random);
            try {
                if (this.player.getServer() != null) CommandManager.CACHED_DISPATCHER.execute("tp @e[type=minecraft:villager,tag=" + TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random + "] " + ent.getX() + " " + ent.getY() + " " + ent.getZ() + " facing " + this.player.getX() + " " + this.player.getY()+1 + " " + this.player.getZ(), this.player.getServer().getCommandSource());
            } catch (Throwable ignored) {}
            ent.removeCommandTag(TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random);
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();
                for (int i = 0; i < TraderManager.getTraders().size(); i++) {
                    try {
                        var t = TraderManager.getTraders().get(i);
                        int finalI = i;
                        this.addSlot(GuiElementBuilder.from(t.trades().getFirst().sell())
                                .setName(Text.literal("").append(Text.literal(TraderManager.prettifyName(t.identifier())).formatted(Formatting.GOLD)).append(Text.literal(" - ").formatted(Formatting.GRAY)).append(Text.literal(t.name()).formatted(Formatting.AQUA)))
                                .setRarity(Rarity.COMMON)
                                .hideDefaultTooltip()
                                .setCount(1)
                                .setCallback(() -> {
                                    this.close();
                                    this.placeTrader(finalI);
                                })
                                .build()
                        );
                    } catch (Throwable ignored) {}
                }
        }

        @Override
        public Text getTitle() {
            return Text.literal(ServerTraders.Config.shopTraderPlacingTitleText);
        }
    }
}
