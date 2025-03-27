package dev.smto.servertraders.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.smto.servertraders.trading.*;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import dev.smto.servertraders.ServerTraders;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandManager {
    public static CommandDispatcher<ServerCommandSource> CACHED_DISPATCHER = new CommandDispatcher<ServerCommandSource>();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        CommandManager.CACHED_DISPATCHER = dispatcher;
        dispatcher.register(literal("servertraders")
            .then(literal("reload")
                    .requires(source -> Permissions.require(CommandManager.getPermission("reload")).test(source) || CommandManager.validatePermissionFallback(source))
                    .executes(context -> {
                        ServerTraders.CONFIG_MANAGER.read();
                        TraderManager.reloadFromDisk();
                        context.getSource().sendFeedback(() -> Text.literal("ServerTraders reloaded."), false);
                        return 1;
                    })
            )
            .then(literal("config")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        context.getSource().sendFeedback(() -> Text.translatable("command.ssu.config.no_argument"), false);
                        return 0;
                    })
                    .then(argument("key", string()).executes(context -> {
                                var key = getString(context, "key");
                                var value = ServerTraders.CONFIG_MANAGER.toMap().getOrDefault(key, "[key does not exist]");
                                context.getSource().sendFeedback(() -> Text.translatable("command.ssu.config.get").append(Text.literal(key)).append(Text.literal(" -> ").append(Text.literal(value))), false);
                                return 0;
                            }).suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(ServerTraders.CONFIG_MANAGER.getKeys(), suggestionsBuilder))
                            .then(argument("value", string()).executes(context -> {
                                        var key = getString(context, "key");
                                        var value = getString(context, "value");
                                        if (ServerTraders.CONFIG_MANAGER.trySet(key, value)) {
                                            ServerTraders.CONFIG_MANAGER.write();
                                            context.getSource().sendFeedback(() -> Text.translatable("command.ssu.config.set").append(Text.literal(key)).append(Text.literal(" -> ").append(Text.literal(value))), false);
                                            return 0;
                                        }
                                        else
                                            context.getSource().sendFeedback(() -> Text.translatable("command.ssu.config.error").formatted(Formatting.RED), false);
                                        return 1;
                                    }).suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(List.of(ServerTraders.CONFIG_MANAGER.toMap().getOrDefault(commandContext.getArgument("key", String.class), "")), suggestionsBuilder))
                            ))
            )
            .then(literal("place")
                    .requires(source -> Permissions.require(CommandManager.getPermission("place")).test(source) || CommandManager.validatePermissionFallback(source))
                    .executes(context -> {
                        TraderManager.openTraderPlacingMenuFor(context.getSource().getPlayer());
                        return 1;
                    }).then(argument("identifier", string()).executes(context -> {
                        if (context.getSource().getPlayer() == null) return 1;
                        var id = getString(context, "identifier");
                        TraderManager.placeTrader(context.getSource().getWorld(), context.getSource().getPlayer(), TraderManager.getTraders().stream().filter((t) -> t.identifier().equals(id)).findFirst().orElse(null));
                        //context.getSource().sendFeedback(() -> Text.translatable("command.ssu.config.set").append(Text.literal(key)).append(Text.literal(" -> ").append(Text.literal(value))), false);
                        return 0;
                    }).suggests((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(TraderManager.getTraders().stream().map(TraderDefinition::identifier).toList(), suggestionsBuilder)))
            )
            .then(literal("builder")
                    .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                    .executes(context -> {
                        TraderBuilder.openGuiBuilderFor(context.getSource().getPlayer());
                        return 0;
                    })
                    .then(literal("reset")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                TraderBuilder.removeBuilderFor(context.getSource().getPlayer());
                                context.getSource().sendFeedback(() -> Text.literal("All builder values have been reset.").formatted(Formatting.DARK_GREEN), false);
                                return 0;
                            })
                    )
                    .then(literal("add-simple-offer")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buy\", \"buyCount\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buy", ItemStackArgumentType.itemStack(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buyCount\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buyCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("sell", ItemStackArgumentType.itemStack(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Argument \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("sellCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                ItemStack buyItem = ItemStackArgumentType.getItemStackArgument(context, "buy").createStack(1, false);
                                int buyCount = IntegerArgumentType.getInteger(context, "buyCount");
                                ItemStack sellItem = ItemStackArgumentType.getItemStackArgument(context, "sell").createStack(1, false);
                                int sellCount = IntegerArgumentType.getInteger(context, "sellCount");
                                if (buyItem.isOf(Items.AIR) || sellItem.isOf(Items.AIR)) {
                                    context.getSource().sendFeedback(() -> Text.literal("One or more items are invalid.").formatted(Formatting.RED), false);
                                    return 1;
                                }
                                if (buyItem.getComponents().get(DataComponentTypes.MAX_STACK_SIZE) < buyCount || sellItem.getComponents().get(DataComponentTypes.MAX_STACK_SIZE) < sellCount) {
                                    context.getSource().sendFeedback(() -> Text.literal("One or more items cannot have the specified item count.").formatted(Formatting.RED), false);
                                    return 1;
                                }
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).addSimpleOffer(buyItem.copyWithCount(buyCount), sellItem.copyWithCount(sellCount));
                                context.getSource().sendFeedback(() -> Text.literal("Offer added: ").formatted(Formatting.DARK_GREEN).append(Text.literal( + buyCount + "x " + getItemName(buyItem) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                return 0;
                            })))
                    )))
                    .then(literal("add-full-offer")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buy1\", \"buy1Count\", \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buy1", ItemStackArgumentType.itemStack(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buy1Count\", \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buy1Count", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buy2", ItemStackArgumentType.itemStack(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"buy2Count\", \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("buy2Count", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"sell\", \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("sell", ItemStackArgumentType.itemStack(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Argument \"sellCount\" missing!").formatted(Formatting.RED), false);
                                return 0;
                            }).then(argument("sellCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                ItemStack buy1Item = ItemStackArgumentType.getItemStackArgument(context, "buy1").createStack(1, false);
                                int buy1Count = IntegerArgumentType.getInteger(context, "buy1Count");
                                ItemStack buy2Item = ItemStackArgumentType.getItemStackArgument(context, "buy2").createStack(1, false);
                                int buy2Count = IntegerArgumentType.getInteger(context, "buy2Count");
                                ItemStack sellItem = ItemStackArgumentType.getItemStackArgument(context, "sell").createStack(1, false);
                                int sellCount = IntegerArgumentType.getInteger(context, "sellCount");
                                if (buy1Item.isOf(Items.AIR) || buy2Item.isOf(Items.AIR) || sellItem.isOf(Items.AIR)) {
                                    context.getSource().sendFeedback(() -> Text.literal("One or more items are invalid.").formatted(Formatting.RED), false);
                                    return 1;
                                }
                                if (buy1Item.getComponents().get(DataComponentTypes.MAX_STACK_SIZE) < buy1Count || buy2Item.getComponents().get(DataComponentTypes.MAX_STACK_SIZE) < buy2Count || sellItem.getComponents().get(DataComponentTypes.MAX_STACK_SIZE).intValue() < sellCount) {
                                    context.getSource().sendFeedback(() -> Text.literal("One or more items cannot have the specified item count.").formatted(Formatting.RED), false);
                                    return 1;
                                }
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).addFullOffer(buy1Item.copyWithCount(buy1Count), buy2Item.copyWithCount(buy2Count), sellItem.copyWithCount(sellCount));
                                context.getSource().sendFeedback(() -> Text.literal("Offer added: ").formatted(Formatting.DARK_GREEN).append(Text.literal( + buy1Count + "x " + getItemName(buy1Item) + " + " + buy2Count + "x " + getItemName(buy2Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                return 0;
                            })))
                    )))))
                    .then(literal("remove-last-offer")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                SimpleOffer offer = TraderBuilder.getBuilderFor(context.getSource().getPlayer()).removeLastOffer();
                                ItemStack sellItem = offer.sell();
                                int sellCount = offer.sell().getCount();
                                ItemStack buy1Item = offer.buy();
                                int buy1Count = offer.buy().getCount();
                                if (offer.isSimple()) {
                                    context.getSource().sendFeedback(() -> Text.literal("Offer removed: ").formatted(Formatting.DARK_GREEN).append(Text.literal( + buy1Count + "x " + getItemName(buy1Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                    return 0;
                                }
                                ItemStack buy2Item = offer.buy2();
                                int buy2Count = offer.buy2().getCount();
                                context.getSource().sendFeedback(() -> Text.literal("Offer removed: ").formatted(Formatting.DARK_GREEN).append(Text.literal( + buy1Count + "x " + getItemName(buy1Item) + " + " + buy2Count + "x " + getItemName(buy2Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                return 0;
                            })
                    )
                    .then(literal("set-name")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Argument \"name\" missing!"), false);
                                return 0;
                            })
                            .then(argument("name", StringArgumentType.string())
                            .executes(context -> {
                                String name = context.getArgument("name", String.class);
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setName(name);
                                context.getSource().sendFeedback(() -> Text.literal("Name set to \"" + name + "\"!"), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-identifier")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Argument \"identifier\" missing!"), false);
                                return 0;
                            })
                            .then(argument("identifier", StringArgumentType.string())
                            .executes(context -> {
                                String id = context.getArgument("identifier", String.class);
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setIdentifier(id);
                                context.getSource().sendFeedback(() -> Text.literal("Identifier set to \"" + id + "\"!"), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-appearance")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Arguments \"type\" and \"profession\" missing!"), false);
                                return 0;
                            })
                            .then(argument("type", IdentifierArgumentType.identifier()).suggests((c, b) -> CommandSource.suggestIdentifiers(Registries.VILLAGER_TYPE.getIds(), b))
                                    .executes(context -> {
                                        context.getSource().sendFeedback(() -> Text.literal("Argument \"profession\" missing!"), false);
                                        return 0;
                                    }).then(argument("profession", IdentifierArgumentType.identifier()).suggests((c, b) -> CommandSource.suggestIdentifiers(Registries.VILLAGER_PROFESSION.getIds(), b))
                                            .executes(context -> {
                                                VillagerType type = Registries.VILLAGER_TYPE.get(context.getArgument("type", Identifier.class));
                                                VillagerProfession profession = Registries.VILLAGER_PROFESSION.get(context.getArgument("profession", Identifier.class));
                                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setVillagerData(new TraderVillagerDefinition(type, profession));
                                                context.getSource().sendFeedback(() -> Text.literal("Villager Appearance set! ").formatted(Formatting.DARK_GREEN).append(Text.literal("Biome: " + type + ", Profession: " + profession)), false);
                                                return 0;
                                            }
                                    ))
                            )
                    )
                    .then(literal("set-hidden")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                var b = TraderBuilder.getBuilderFor(context.getSource().getPlayer());
                                boolean current = b.getHidden();
                                b.setHidden(!current);
                                context.getSource().sendFeedback(() -> Text.literal("\"hidden\" flag has been toggled to: ").formatted(Formatting.DARK_GREEN).append(Text.literal(String.valueOf(!current))), false);
                                return 0;
                            }).then(argument("hidden", BoolArgumentType.bool()).executes(context -> {
                                boolean b = BoolArgumentType.getBool(context, "hidden");
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setHidden(b);
                                context.getSource().sendFeedback(() -> Text.literal("\"hidden\" flag has been set to: ").formatted(Formatting.DARK_GREEN).append(Text.literal(String.valueOf(b))), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-filename")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendFeedback(() -> Text.literal("Argument \"filename\" missing!"), false);
                            return 0;
                            })
                            .then(argument("filename", StringArgumentType.string())
                                .executes(context -> {
                                    var builder = TraderBuilder.getBuilderFor(context.getSource().getPlayer());
                                    String filename = context.getArgument("filename", String.class);
                                    if (filename.endsWith(".json")) filename = filename.substring(0, filename.length() - 5);
                                    builder.setFilename(filename);
                                    String finalFilename = filename;
                                    context.getSource().sendFeedback(() -> Text.literal("Filename set:").formatted(Formatting.DARK_GREEN).append(Text.literal(finalFilename).formatted(Formatting.BLUE)), false);
                                    return 1;
                                }))
                    )
                    .then(literal("save")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                var builder = TraderBuilder.getBuilderFor(context.getSource().getPlayer());
                                var errors = builder.validate();
                                if (errors.isEmpty()) {
                                    if (builder.build()) {
                                        context.getSource().sendFeedback(() -> Text.literal("Saved trader successfully!"), false);
                                        return 0;
                                    }
                                }
                                context.getSource().sendFeedback(() -> Text.literal("Cannot create trader due to the following errors:").formatted(Formatting.BOLD, Formatting.RED), false);
                                errors.forEach(s -> context.getSource().sendFeedback(() -> Text.literal("- " + s).formatted(Formatting.RED), false));
                                return 1;
                            })
                    )
            )
        );
        if (ServerTraders.Config.enableShopCommand) {
            dispatcher.register(literal(ServerTraders.Config.shopCommand)
                    .executes(context -> {
                        TraderManager.openMasterMenuFor(context.getSource().getPlayer());
                        return 0;
                    })
            );
        }
    }

    private static String getPermission(String permission) {
        return ServerTraders.MOD_ID + ".command." + permission;
    }

    private static boolean validatePermissionFallback(ServerCommandSource source) {
        if (ServerTraders.Config.enablePermissionFallback) {
            return source.hasPermissionLevel(ServerTraders.Config.fallbackPermissionLevel);
        }
        return false;
    }

    private static String getItemName(ItemStack stack) {
        if (true) return stack.getOrDefault(DataComponentTypes.CUSTOM_NAME, stack.getItemName()).getString();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return I18n.translate(stack.getItem().getTranslationKey());
        }
        return Registries.ITEM.getId(stack.getItem()).getPath();
    }
}
