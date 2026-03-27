package dev.smto.servertraders.trading;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.MerchantGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import dev.smto.servertraders.command.CommandManager;
import dev.smto.servertraders.ServerTraders;
import dev.smto.servertraders.util.CodecUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

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
            MasterMenu.type = MenuType.GENERIC_9x1;
        } else if (TraderManager.TRADERS.size() <= 18) {
            MasterMenu.type = MenuType.GENERIC_9x2;
        } else if (TraderManager.TRADERS.size() <= 27) {
            MasterMenu.type = MenuType.GENERIC_9x3;
        } else if (TraderManager.TRADERS.size() <= 36) {
            MasterMenu.type = MenuType.GENERIC_9x4;
        } else if (TraderManager.TRADERS.size() <= 45) {
            MasterMenu.type = MenuType.GENERIC_9x5;
        } else {
            MasterMenu.type = MenuType.GENERIC_9x6;
        }

        if (TraderManager.TRADERS.size() > 54) {
            ServerTraders.LOGGER.warn("More than 54 traders have been loaded! Menus cannot currently display more than that, so these will be ignored.");
        }

        if (TraderManager.TRADERS.isEmpty()) {
            var t = new TraderDefinition("Example Trader", "example_trader", false, TraderVillagerDefinition.DEFAULT, List.of(
                    SimpleOffer.simple(Items.EMERALD.getDefaultInstance().copyWithCount(1), Items.DIRT.getDefaultInstance().copyWithCount(64)),
                    SimpleOffer.simple(Items.EMERALD.getDefaultInstance().copyWithCount(2), Items.COBBLESTONE.getDefaultInstance().copyWithCount(64)),
                    SimpleOffer.simple(Items.EMERALD.getDefaultInstance().copyWithCount(4), Items.OAK_LOG.getDefaultInstance().copyWithCount(64))
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

    public static boolean checkInteractedVillager(Player player, Villager villager) {
        for (String commandTag : villager.entityTags()) {
            if (commandTag.contains(TraderManager.COMMANDTAG_KEY) && !commandTag.contains("Fallback")) {
                var tr = TraderManager.getTraderByName(commandTag.replaceAll(TraderManager.COMMANDTAG_KEY + "=", ""));
                if (tr != null) {
                    try {
                        TraderManager.updateVillager(villager, tr);
                        TraderManager.openTraderMenuFor((ServerPlayer) player, tr);
                    } catch (Throwable ignored) {}
                } else player.sendSystemMessage(Component.literal(ServerTraders.Config.invalidTraderText).withStyle(ChatFormatting.DARK_RED));
                return true;
            }
        }
        return false;
    }

    public static void updateVillager(Villager villager, TraderDefinition data) {
        villager.setCustomName(Component.literal(data.name()));
        villager.setVillagerData(data.villager().toVanilla());
    }

    public static void openMasterMenuFor(ServerPlayer player) {
        new MasterMenu(player).open();
    }

    private static BiConsumer<ServerPlayer, TraderDefinition> traderMenuFunc = (ServerPlayer player, TraderDefinition trades) -> {
        new TraderMenu(player, trades).open();
    };

    public static void setCustomShopScreen(BiConsumer<ServerPlayer, TraderDefinition> func) {
        TraderManager.traderMenuFunc = func;
    }

    public static void restoreDefaultShopScreen() {
        TraderManager.traderMenuFunc = (ServerPlayer player, TraderDefinition trades) -> {
            new TraderMenu(player, trades).open();
        };
    }

    public static void openTraderMenuFor(ServerPlayer player, TraderDefinition trades) {
        TraderManager.traderMenuFunc.accept(player, trades);
    }

    public static void openTraderPlacingMenuFor(ServerPlayer player) {
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
        public static MenuType<ChestMenu> type = MenuType.GENERIC_9x1;
        public MasterMenu(ServerPlayer player) {
            super(MasterMenu.type, player, false);
        }
        @Override
        public void beforeOpen() {
            super.beforeOpen();
            for (var trader : TraderManager.TRADERS) {
                try {
                    if (!trader.hidden()) {
                        if (trader.trades().isEmpty()) continue;
                        this.addSlot(GuiElementBuilder.from(trader.trades().getFirst().sell())
                                .setName((Component.literal(TraderManager.prettifyName(trader.identifier())).withStyle(ChatFormatting.GOLD).append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)).append(Component.literal(trader.name()).withStyle(ChatFormatting.AQUA))))
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
                } catch (Throwable ignored) {}
            }
        }

        @Override
        public Component getTitle() {
            return Component.literal(ServerTraders.Config.shopMenuTitleText);
        }
    }

    private static class TraderMenu extends MerchantGui {
        private final TraderDefinition trades;
        public TraderMenu(ServerPlayer player, TraderDefinition trades) {
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
        public Component getTitle() {
            return Component.literal(this.trades.name());
        }

        @Override
        public void onSelectTrade(MerchantOffer offer) {
            this.merchantInventory.setChanged();
            this.player.getInventory().setChanged();
            this.sendUpdate();
        }
    }

    public static void placeTrader(Level world, Player player, @Nullable TraderDefinition trader) {
        if (trader == null) return;
        var villagerData = trader.villager();
        var ent = new Villager(EntityType.VILLAGER, world);
        world.addFreshEntity(ent);

        ent.setVillagerData(villagerData.toVanilla());
        ent.setPos(player.position());
        ent.setYHeadRot(player.getYHeadRot());
        ent.setXRot(player.getXRot());
        ent.setNoAi(true);
        ent.setNoGravity(true);
        ent.setInvulnerable(true);
        ent.setDiscardFriction(true);
        ent.setPersistenceRequired();
        ent.setSilent(true);
        ent.setCustomName(Component.literal(trader.name()));
        ent.setCustomNameVisible(true);
        ent.addTag(TraderManager.COMMANDTAG_KEY + "=" + trader.identifier());
        ent.addTag(TraderManager.COMMANDTAG_KEY + "Fallback=" + TraderManager.getTraders().indexOf(trader));

        int random = world.getRandom().nextInt();
        ent.addTag(TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random);
        try {
            if (player.level().getServer() != null) CommandManager.CACHED_DISPATCHER.execute("tp @e[type=minecraft:villager,tag=" + TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random + "] " + ent.getX() + " " + ent.getY() + " " + ent.getZ() + " facing " + player.getX() + " " + player.getY()+1 + " " + player.getZ(), player.level().getServer().createCommandSourceStack());
        } catch (Throwable ignored) {}
        ent.removeTag(TraderManager.COMMANDTAG_KEY + "FreshlyPlaced=" + random);
    }

    private static class TraderPlacingGui extends SimpleGui {
        private TraderPlacingGui(MenuType<ChestMenu> type, ServerPlayer player) {
            super(type, player, false);
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();
            for (TraderDefinition t : TraderManager.getTraders()) {
                try {
                    ItemStack icon = Items.BARRIER.getDefaultInstance();
                    if (!t.trades().isEmpty()) icon = t.trades().getFirst().sell();
                    this.addSlot(GuiElementBuilder.from(icon)
                            .setName(Component.literal("").append(Component.literal(TraderManager.prettifyName(t.identifier())).withStyle(ChatFormatting.GOLD)).append(Component.literal(" - ").withStyle(ChatFormatting.GRAY)).append(Component.literal(t.name()).withStyle(ChatFormatting.AQUA)))
                            .setRarity(Rarity.COMMON)
                            .hideDefaultTooltip()
                            .setCount(1)
                            .setCallback(() -> {
                                this.close();
                                TraderManager.placeTrader(this.player.level(), this.player, t);
                            })
                            .build()
                    );
                } catch (Throwable ignored) {}
            }
        }

        @Override
        public Component getTitle() {
            return Component.literal(ServerTraders.Config.shopTraderPlacingTitleText);
        }
    }
}
