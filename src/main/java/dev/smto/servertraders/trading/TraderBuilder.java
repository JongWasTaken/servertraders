package dev.smto.servertraders.trading;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TraderBuilder {
    private static final HashMap<PlayerEntity, TraderBuilder> BUILDERS = new HashMap<>();
    public static TraderBuilder getBuilderFor(PlayerEntity player) {
        if (TraderBuilder.BUILDERS.containsKey(player)) return TraderBuilder.BUILDERS.get(player);
        var builder = new TraderBuilder(player);
        TraderBuilder.BUILDERS.put(player, builder);
        return builder;
    }

    public static void removeBuilderFor(PlayerEntity player) {
        TraderBuilder.BUILDERS.remove(player);
    }

    public static void openGuiBuilderFor(PlayerEntity player) {
        new TraderBuilderGui((ServerPlayerEntity) player).open();
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
        this.offers.add(SimpleOffer.simple(buy.getDefaultStack().copyWithCount(buyCount), sell.getDefaultStack().copyWithCount(sellCount)));
        return this;
    }
    public TraderBuilder addSimpleOffer(ItemStack buy, ItemStack sell) {
        this.offers.add(SimpleOffer.simple(buy, sell));
        return this;
    }
    public TraderBuilder addFullOffer(Item buy1, int buy1Count, Item buy2, int buy2Count, Item sell, int sellCount) {
        this.offers.add(new SimpleOffer(buy1.getDefaultStack().copyWithCount(buy1Count), buy2.getDefaultStack().copyWithCount(buy2Count), sell.getDefaultStack().copyWithCount(sellCount)));
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

    private final PlayerEntity host;
    private TraderBuilder(PlayerEntity player) {
        this.host = player;
    }

    private static class TraderBuilderGui extends SimpleGui {
        private final TraderBuilder builderRef;
        private final ServerPlayerEntity host;
        private TraderBuilderGui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X1, player, false);
            this.builderRef = TraderBuilder.getBuilderFor(player);
            this.host = player;
        }

        @Override
        public void beforeOpen() {
            super.beforeOpen();
            this.setSlot(0, GuiElementBuilder.from(Items.NAME_TAG.getDefaultStack())
                    .setName(Text.literal("Set Trader Name"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getName() == null ? List.of(Text.literal("Click to set!").formatted(Formatting.BLUE)) : List.of(Text.literal("Set to: ").formatted(Formatting.BLUE).append(Text.literal(this.builderRef.getName()).formatted(Formatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, this.builderRef::setName, this.builderRef.getName());
                    })
                    .build()
            );
            this.setSlot(1, GuiElementBuilder.from(Items.COMMAND_BLOCK.getDefaultStack())
                    .setName(Text.literal("Set Trader Identifier"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getIdentifier() == null ? List.of(Text.literal("Click to set!").formatted(Formatting.BLUE)) : List.of(Text.literal("Set to: ").formatted(Formatting.BLUE).append(Text.literal(this.builderRef.getIdentifier()).formatted(Formatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, this.builderRef::setIdentifier, this.builderRef.getIdentifier());
                    })
                    .build()
            );
            this.setSlot(2, GuiElementBuilder.from(Items.SHULKER_BOX.getDefaultStack())
                    .setName(Text.literal("Set Hidden from Shop Menu"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(List.of(Text.literal("Set to: ").formatted(Formatting.BLUE).append(Text.literal(String.valueOf(this.builderRef.getHidden())).formatted(Formatting.DARK_GREEN))))
                    .glow(this.builderRef.getHidden())
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        this.builderRef.setHidden(!this.builderRef.getHidden());
                        this.close();
                        this.open();
                    })
                    .build()
            );
            this.setSlot(3, GuiElementBuilder.from(Items.BOOK.getDefaultStack())
                    .setName(Text.literal("Set File Name"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.getFilename() == null ? List.of(Text.literal("Click to set!").formatted(Formatting.BLUE)) : List.of(Text.literal("Set to: ").formatted(Formatting.BLUE).append(Text.literal(this.builderRef.getFilename()).formatted(Formatting.DARK_GREEN))))
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        InputGui.openInputGui(this, this.host, (s -> {
                            if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
                            this.builderRef.setFilename(s);
                        }), this.builderRef.getFilename());
                    })
                    .build()
            );
            this.setSlot(4, GuiElementBuilder.from(Items.EMERALD.getDefaultStack())
                    .setName(Text.literal("Offers"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.offers.isEmpty() ? List.of(
                            Text.literal("No offers set!").formatted(Formatting.RED),
                            Text.literal("Hover over the piece of paper for instructions!").formatted(Formatting.BLUE)
                    ) : List.of(Text.literal("Offers: ").formatted(Formatting.BLUE).append(Text.literal("x" + this.builderRef.offers.size()).formatted(Formatting.DARK_GREEN))))
                    .setCount(1)
                    .build()
            );
            this.setSlot(5, GuiElementBuilder.from(Items.VILLAGER_SPAWN_EGG.getDefaultStack())
                    .setName(Text.literal("Appearance"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.builderRef.villager == null ? List.of(
                            Text.literal("No appearance set!").formatted(Formatting.RED),
                            Text.literal("Hover over the piece of paper for instructions!").formatted(Formatting.BLUE)
                    ) : List.of(
                            Text.literal("Set to: ").formatted(Formatting.BLUE),
                            Text.literal("Biome: ").formatted(Formatting.BLUE).append(Text.literal(this.builderRef.villager.type().toString()).formatted(Formatting.DARK_GREEN)),
                            Text.literal("Profession: ").formatted(Formatting.BLUE).append(Text.literal(this.builderRef.villager.profession().toString()).formatted(Formatting.DARK_GREEN))
                            )
                    )
                    .setCount(1)
                    .build()
            );
            this.setSlot(7, GuiElementBuilder.from(Items.PAPER.getDefaultStack())
                    .setName(Text.literal("Important").formatted(Formatting.GOLD, Formatting.BOLD))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(List.of(
                            Text.literal("Villager Appearance and Offers cannot be set using this menu,").formatted(Formatting.GOLD),
                            Text.literal("as using commands is quicker and easier due to autocompletion.").formatted(Formatting.GOLD),
                            Text.literal(""),
                            Text.literal("Adding offers:").formatted(Formatting.BLUE),
                            Text.literal("/servertraders builder add-simple-offer [buy] [count] [sell] [count]").formatted(Formatting.GREEN, Formatting.ITALIC),
                            Text.literal("/servertraders builder add-full-offer [buy1] [count] [buy2] [count] [sell] [count]").formatted(Formatting.GREEN, Formatting.ITALIC),
                            Text.literal("Set villager appearance:").formatted(Formatting.BLUE),
                            Text.literal("/servertraders builder set-appearance [biome] [profession]").formatted(Formatting.GREEN, Formatting.ITALIC),
                            Text.literal(""),
                            Text.literal("Don't worry!").formatted(Formatting.RED, Formatting.BOLD),
                            Text.literal("Builder progress is saved until the next server restart.").formatted(Formatting.RED, Formatting.BOLD)
                    ))
                    .setCount(1)
                    .glow(true)
                    .build()
            );
            this.setSlot(8, GuiElementBuilder.from(Items.CRAFTING_TABLE.getDefaultStack())
                    .setName(Text.literal("Finish Building Trader"))
                    .setRarity(Rarity.COMMON)
                    .hideDefaultTooltip()
                    .setLore(this.checkForErrors())
                    .setCount(1)
                    .setCallback((i, ct, a, slot) -> {
                        if(this.builderRef.validate().isEmpty()) {
                            String filename = this.builderRef.getFilename();
                            if (this.builderRef.build()) {
                                this.host.sendMessage(Text.literal("Your trader was successfully built and saved as " + filename + ".json").formatted(Formatting.GREEN), false);
                            } else {
                                this.host.sendMessage(Text.literal("An unexpected error occurred while building the trader! This might be caused by illegal characters or offers.").formatted(Formatting.RED), false);
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

        public List<Text> checkForErrors() {
            var out = new ArrayList<Text>();
            var errors = this.builderRef.validate();
            if (errors.isEmpty()) {
                out.add(Text.literal("All done! Click to finalize!").formatted(Formatting.BLUE));
            } else {
                out.add(Text.literal("Cannot build due to errors:").formatted(Formatting.BLUE));
                errors.stream().map(i -> (Text) Text.literal(i).formatted(Formatting.RED, Formatting.ITALIC)).forEach(out::add);
            }
            return out;
        }

        @Override
        public Text getTitle() {
            return Text.literal("Trader Builder");
        }

        private static class InputGui extends AnvilInputGui {
            private final SimpleGui parent;
            private final Consumer<String> callback;
            public static void openInputGui(SimpleGui parent, ServerPlayerEntity player, Consumer<String> callback, @Nullable String currentText) {
                if (currentText == null) currentText = "";
                parent.close();
                new InputGui(parent, player, callback, currentText).open();
            }
            private InputGui(SimpleGui parent, ServerPlayerEntity player, Consumer<String> callback, String currentText) {
                super(player, false);
                this.parent = parent;
                this.callback = callback;
                this.setDefaultInputValue(currentText);
            }

            @Override
            public void onClose() {
                this.parent.open();
            }

            @Override
            public void beforeOpen() {
                super.beforeOpen();
                this.setSlot(1, GuiElementBuilder.from(Items.BARRIER.getDefaultStack())
                        .setName(Text.literal("Cancel Input").formatted(Formatting.RED))
                        .setRarity(Rarity.COMMON)
                        .hideDefaultTooltip()
                        .setCount(1)
                        .setCallback((i, ct, a, slot) -> {
                            this.close();
                        })
                        .build()
                );
                this.setSlot(2, GuiElementBuilder.from(Items.ANVIL.getDefaultStack())
                        .setName(Text.literal("Confirm Input").formatted(Formatting.DARK_GREEN))
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
            public Text getTitle() {
                return Text.literal("Text Input");
            }
        }
    }
}
