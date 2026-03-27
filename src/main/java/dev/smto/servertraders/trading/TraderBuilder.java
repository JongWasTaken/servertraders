package dev.smto.servertraders.trading;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TraderBuilder {
    private static final HashMap<Player, TraderBuilder> BUILDERS = new HashMap<>();
    public static TraderBuilder getBuilderFor(Player player) {
        if (TraderBuilder.BUILDERS.containsKey(player)) return TraderBuilder.BUILDERS.get(player);
        var builder = new TraderBuilder(player);
        TraderBuilder.BUILDERS.put(player, builder);
        return builder;
    }

    public static void removeBuilderFor(Player player) {
        TraderBuilder.BUILDERS.remove(player);
    }

    public static void openGuiBuilderFor(Player player) {
        new TraderBuilderGui((ServerPlayer) player).open();
    }

    private String name = null;
    private String identifier = null;
    private String filename = null;
    private boolean isHidden = false;
    private TraderVillagerDefinition villager = null;
    private final List<SimpleOffer> offers = new ArrayList<>();

    public TraderBuilder setName(String name) {
        this.name = name;
        return this;
    }
    public String getName() {
        return this.name;
    }
    public TraderBuilder setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }
    public String getIdentifier() {
        return this.identifier;
    }
    public TraderBuilder setFilename(String filename) {
        this.filename = filename;
        return this;
    }
    public String getFilename() {
        return this.filename;
    }
    public TraderBuilder setHidden() {
        this.isHidden = true;
        return this;
    }
    public TraderBuilder setHidden(boolean hidden) {
        this.isHidden = hidden;
        return this;
    }
    public boolean getHidden() {
        return this.isHidden;
    }
    public TraderBuilder setVillagerData(TraderVillagerDefinition def) {
        this.villager = def;
        return this;
    }
    public TraderVillagerDefinition getVillagerData() {
        return this.villager;
    }
    public TraderBuilder addSimpleOffer(Item buy, int buyCount, Item sell, int sellCount) {
        this.offers.add(SimpleOffer.simple(buy.getDefaultInstance().copyWithCount(buyCount), sell.getDefaultInstance().copyWithCount(sellCount)));
        return this;
    }
    public TraderBuilder addSimpleOffer(ItemStack buy, ItemStack sell) {
        this.offers.add(SimpleOffer.simple(buy, sell));
        return this;
    }
    public TraderBuilder addFullOffer(Item buy1, int buy1Count, Item buy2, int buy2Count, Item sell, int sellCount) {
        this.offers.add(new SimpleOffer(buy1.getDefaultInstance().copyWithCount(buy1Count), buy2.getDefaultInstance().copyWithCount(buy2Count), sell.getDefaultInstance().copyWithCount(sellCount)));
        return this;
    }
    public TraderBuilder addFullOffer(ItemStack buy1, ItemStack buy2, ItemStack sell) {
        this.offers.add(new SimpleOffer(buy1, buy2, sell));
        return this;
    }
    public SimpleOffer removeLastOffer() {
        return this.offers.removeLast();
    }
    public List<SimpleOffer> getOffers() {
        return this.offers;
    }
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if(this.name == null) {
            errors.add("No name set!");
        }
        if(this.identifier == null) {
            errors.add("No identifier set!");
        }
        if(this.filename == null) {
            errors.add("No filename set!");
        }
        if(this.villager == null) {
            errors.add("No appearance set!");
        }
        if(this.offers.isEmpty()) {
            errors.add("No offers set!");
        }
        return errors;
    }
    public boolean build() {
        if(
                this.name != null ||
                    this.identifier != null ||
                    this.villager != null ||
                    this.filename != null ||
                    this.offers.isEmpty()
        ) {
            if(TraderManager.acceptNewTraderDefinition(new TraderDefinition(this.name, this.identifier, this.isHidden, this.villager, this.offers), this.filename)) {
                TraderBuilder.removeBuilderFor(this.host);
                return true;
            };
        }
        return false;
    }

    private final Player host;
    private TraderBuilder(Player player) {
        this.host = player;
    }

    private static class TraderBuilderGui extends SimpleGui {
        private final TraderBuilder builderRef;
        private final ServerPlayer host;
        private TraderBuilderGui(ServerPlayer player) {
            super(MenuType.GENERIC_9x1, player, false);
            this.builderRef = TraderBuilder.getBuilderFor(player);
            this.host = player;
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();
            this.setSlot(0, GuiElementBuilder.from(Items.NAME_TAG.getDefaultInstance())
                    .setName(Component.literal("Set Trader Name"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getName() == null ? List.of(Component.literal("Click to set!").withStyle(ChatFormatting.BLUE)) : List.of(Component.literal("Set to: ").withStyle(ChatFormatting.BLUE).append(Component.literal(this.builderRef.getName()).withStyle(ChatFormatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, this.builderRef::setName, this.builderRef.getName());
                    })
                    .build()
            );
            this.setSlot(1, GuiElementBuilder.from(Items.COMMAND_BLOCK.getDefaultInstance())
                    .setName(Component.literal("Set Trader Identifier"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getIdentifier() == null ? List.of(Component.literal("Click to set!").withStyle(ChatFormatting.BLUE)) : List.of(Component.literal("Set to: ").withStyle(ChatFormatting.BLUE).append(Component.literal(this.builderRef.getIdentifier()).withStyle(ChatFormatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, this.builderRef::setIdentifier, this.builderRef.getIdentifier());
                    })
                    .build()
            );
            this.setSlot(2, GuiElementBuilder.from(Items.SHULKER_BOX.getDefaultInstance())
                    .setName(Component.literal("Set Hidden from Shop Menu"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(List.of(Component.literal("Set to: ").withStyle(ChatFormatting.BLUE).append(Component.literal(String.valueOf(this.builderRef.getHidden())).withStyle(ChatFormatting.DARK_GREEN))))
                    .glow(this.builderRef.getHidden())
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        this.builderRef.setHidden(!this.builderRef.getHidden());
                        this.close();
                        this.open();
                    })
                    .build()
            );
            this.setSlot(3, GuiElementBuilder.from(Items.BOOK.getDefaultInstance())
                    .setName(Component.literal("Set File Name"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getFilename() == null ? List.of(Component.literal("Click to set!").withStyle(ChatFormatting.BLUE)) : List.of(Component.literal("Set to: ").withStyle(ChatFormatting.BLUE).append(Component.literal(this.builderRef.getFilename()).withStyle(ChatFormatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, (s -> {
                            if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
                            this.builderRef.setFilename(s);
                        }), this.builderRef.getFilename());
                    })
                    .build()
            );
            this.setSlot(4, GuiElementBuilder.from(Items.EMERALD.getDefaultInstance())
                    .setName(Component.literal("Offers"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.offers.isEmpty() ? List.of(
                            Component.literal("No offers set!").withStyle(ChatFormatting.RED),
                            Component.literal("Hover over the piece of paper for instructions!").withStyle(ChatFormatting.BLUE)
                    ) : List.of(Component.literal("Offers: ").withStyle(ChatFormatting.BLUE).append(Component.literal("x" + this.builderRef.offers.size()).withStyle(ChatFormatting.DARK_GREEN))))
                    .setCount(1)
                    .build()
            );
            this.setSlot(5, GuiElementBuilder.from(Items.VILLAGER_SPAWN_EGG.getDefaultInstance())
                    .setName(Component.literal("Appearance"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.villager == null ? List.of(
                            Component.literal("No appearance set!").withStyle(ChatFormatting.RED),
                            Component.literal("Hover over the piece of paper for instructions!").withStyle(ChatFormatting.BLUE)
                    ) : List.of(
                            Component.literal("Set to: ").withStyle(ChatFormatting.BLUE),
                            Component.literal("Biome: ").withStyle(ChatFormatting.BLUE).append(Component.literal(this.builderRef.villager.type().toString()).withStyle(ChatFormatting.DARK_GREEN)),
                            Component.literal("Profession: ").withStyle(ChatFormatting.BLUE).append(Component.literal(this.builderRef.villager.profession().toString()).withStyle(ChatFormatting.DARK_GREEN))
                            )
                    )
                    .setCount(1)
                    .build()
            );
            this.setSlot(7, GuiElementBuilder.from(Items.PAPER.getDefaultInstance())
                    .setName(Component.literal("Important").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(List.of(
                            Component.literal("Villager Appearance and Offers cannot be set using this menu,").withStyle(ChatFormatting.GOLD),
                            Component.literal("as using commands is quicker and easier due to autocompletion.").withStyle(ChatFormatting.GOLD),
                            Component.literal(""),
                            Component.literal("Adding offers:").withStyle(ChatFormatting.BLUE),
                            Component.literal("/servertraders builder add-simple-offer [buy] [count] [sell] [count]").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC),
                            Component.literal("/servertraders builder add-full-offer [buy1] [count] [buy2] [count] [sell] [count]").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC),
                            Component.literal("Set villager appearance:").withStyle(ChatFormatting.BLUE),
                            Component.literal("/servertraders builder set-appearance [biome] [profession]").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC),
                            Component.literal(""),
                            Component.literal("Don't worry!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            Component.literal("Builder progress is saved until the next server restart.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    ))
                    .setCount(1)
                    .glow(true)
                    .build()
            );
            this.setSlot(8, GuiElementBuilder.from(Items.CRAFTING_TABLE.getDefaultInstance())
                    .setName(Component.literal("Finish Building Trader"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.checkForErrors())
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        if(this.builderRef.validate().isEmpty()) {
                            String filename = this.builderRef.getFilename();
                            if (this.builderRef.build()) {
                                this.host.sendSystemMessage(Component.literal("Your trader was successfully built and saved as " + filename + ".json").withStyle(ChatFormatting.GREEN), false);
                            } else {
                                this.host.sendSystemMessage(Component.literal("An unexpected error occurred while building the trader! This might be caused by illegal characters or offers.").withStyle(ChatFormatting.RED), false);
                            }
                            this.close();
                        } else {
                            this.close();
                            this.open();
                        }
                    })
                    .build()
            );
        }

        public List<Component> checkForErrors() {
            var out = new ArrayList<Component>();
            var errors = this.builderRef.validate();
            if (errors.isEmpty()) {
                out.add(Component.literal("All done! Click to finalize!").withStyle(ChatFormatting.BLUE));
            } else {
                out.add(Component.literal("Cannot build due to errors:").withStyle(ChatFormatting.BLUE));
                errors.stream().map(i -> (Component) Component.literal(i).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)).forEach(out::add);
            }
            return out;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Trader Builder");
        }

        private static class InputGui extends AnvilInputGui {
            private final SimpleGui parent;
            private final Consumer<String> callback;
            public static void openInputGui(SimpleGui parent, ServerPlayer player, Consumer<String> callback, @Nullable String currentText) {
                if (currentText == null) currentText = "";
                parent.close();
                new InputGui(parent, player, callback, currentText).open();
            }
            private InputGui(SimpleGui parent, ServerPlayer player, Consumer<String> callback, String currentText) {
                super(player, false);
                this.parent = parent;
                this.callback = callback;
                this.setDefaultInputValue(currentText);
            }

            @Override
            public void onManualClose() {
                this.parent.open();
            }

            @Override
            public void beforeOpen() {
                super.beforeOpen();
                this.setSlot(1, GuiElementBuilder.from(Items.BARRIER.getDefaultInstance())
                        .setName(Component.literal("Cancel Input").withStyle(ChatFormatting.RED))
                        .setRarity(Rarity.COMMON)
                        .hideDefaultTooltip()
                        .setCount(1)
                        .setCallback((i, ct, a, slot) -> {
                            this.close();
                        })
                        .build()
                );
                this.setSlot(2, GuiElementBuilder.from(Items.ANVIL.getDefaultInstance())
                        .setName(Component.literal("Confirm Input").withStyle(ChatFormatting.DARK_GREEN))
                        .setRarity(Rarity.COMMON)
                        .hideDefaultTooltip()
                        .setCount(1)
                        .setCallback((i, ct, a, slot) -> {
                            this.callback.accept(this.getInput());
                            this.close();
                        })
                        .build()
                );

            }

            @Override
            public Component getTitle() {
                return Component.literal("Text Input");
            }
        }
    }
}
