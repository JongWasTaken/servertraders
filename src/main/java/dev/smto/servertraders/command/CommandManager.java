package dev.smto.servertraders.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.smto.servertraders.trading.*;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permission;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.smto.servertraders.ServerTraders;

import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandManager {
    public static CommandDispatcher<CommandSourceStack> CACHED_DISPATCHER = new CommandDispatcher<CommandSourceStack>();
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandRegistryAccess) {
        CommandManager.CACHED_DISPATCHER = dispatcher;
        dispatcher.register(literal("servertraders")
            .then(literal("reload")
                    .requires(source -> Permissions.require(CommandManager.getPermission("reload")).test(source) || CommandManager.validatePermissionFallback(source))
                    .executes(context -> {
                        ServerTraders.CONFIG_MANAGER.read();
                        TraderManager.reloadFromDisk();
                        context.getSource().sendSuccess(() -> Component.literal("ServerTraders reloaded."), false);
                        return 1;
                    })
            )
            .then(literal("config")
                    .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("Missing arguments!"), false);
                        return 0;
                    })
                    .then(argument("key", string()).executes(context -> {
                                var key = getString(context, "key");
                                var value = ServerTraders.CONFIG_MANAGER.toMap().getOrDefault(key, "[key does not exist]");
                                context.getSource().sendSuccess(() -> Component.literal("Current config: ").append(Component.literal(key)).append(Component.literal(" -> ").append(Component.literal(value))), false);
                                return 0;
                            }).suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ServerTraders.CONFIG_MANAGER.getKeys(), suggestionsBuilder))
                            .then(argument("value", string()).executes(context -> {
                                        var key = getString(context, "key");
                                        var value = getString(context, "value");
                                        if (ServerTraders.CONFIG_MANAGER.trySet(key, value)) {
                                            ServerTraders.CONFIG_MANAGER.write();
                                            context.getSource().sendSuccess(() -> Component.literal("Config modified: ").append(Component.literal(key)).append(Component.literal(" -> ").append(Component.literal(value))), false);
                                            return 0;
                                        }
                                        else
                                            context.getSource().sendSuccess(() -> Component.literal("An error occurred while setting the given value. Is the type valid?").withStyle(ChatFormatting.RED), false);
                                        return 1;
                                    }).suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(List.of(ServerTraders.CONFIG_MANAGER.toMap().getOrDefault(commandContext.getArgument("key", String.class), "")), suggestionsBuilder))
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
                        TraderManager.placeTrader(context.getSource().getLevel(), context.getSource().getPlayer(), TraderManager.getTraders().stream().filter((t) -> t.identifier().equals(id)).findFirst().orElse(null));
                        return 0;
                    }).suggests((commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(TraderManager.getTraders().stream().map(TraderDefinition::identifier).toList(), suggestionsBuilder)))
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
                                context.getSource().sendSuccess(() -> Component.literal("All builder values have been reset.").withStyle(ChatFormatting.DARK_GREEN), false);
                                return 0;
                            })
                    )
                    .then(literal("add-simple-offer")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buy\", \"buyCount\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buy", ItemArgument.item(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buyCount\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buyCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("sell", ItemArgument.item(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Argument \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("sellCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                ItemStack buyItem = ItemArgument.getItem(context, "buy").createItemStack(1);
                                int buyCount = IntegerArgumentType.getInteger(context, "buyCount");
                                ItemStack sellItem = ItemArgument.getItem(context, "sell").createItemStack(1);
                                int sellCount = IntegerArgumentType.getInteger(context, "sellCount");
                                if (buyItem.is(Items.AIR) || sellItem.is(Items.AIR)) {
                                    context.getSource().sendSuccess(() -> Component.literal("One or more items are invalid.").withStyle(ChatFormatting.RED), false);
                                    return 1;
                                }
                                if (buyItem.getComponents().get(DataComponents.MAX_STACK_SIZE) < buyCount || sellItem.getComponents().get(DataComponents.MAX_STACK_SIZE) < sellCount) {
                                    context.getSource().sendSuccess(() -> Component.literal("One or more items cannot have the specified item count.").withStyle(ChatFormatting.RED), false);
                                    return 1;
                                }
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).addSimpleOffer(buyItem.copyWithCount(buyCount), sellItem.copyWithCount(sellCount));
                                context.getSource().sendSuccess(() -> Component.literal("Offer added: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal( + buyCount + "x " + getItemName(buyItem) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                return 0;
                            })))
                    )))
                    .then(literal("add-full-offer")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buy1\", \"buy1Count\", \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buy1", ItemArgument.item(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buy1Count\", \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buy1Count", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buy2\", \"buy2Count\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buy2", ItemArgument.item(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"buy2Count\", \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("buy2Count", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"sell\", \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("sell", ItemArgument.item(commandRegistryAccess)).executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Argument \"sellCount\" missing!").withStyle(ChatFormatting.RED), false);
                                return 0;
                            }).then(argument("sellCount", IntegerArgumentType.integer(0, 64)).executes(context -> {
                                ItemStack buy1Item = ItemArgument.getItem(context, "buy1").createItemStack(1);
                                int buy1Count = IntegerArgumentType.getInteger(context, "buy1Count");
                                ItemStack buy2Item = ItemArgument.getItem(context, "buy2").createItemStack(1);
                                int buy2Count = IntegerArgumentType.getInteger(context, "buy2Count");
                                ItemStack sellItem = ItemArgument.getItem(context, "sell").createItemStack(1);
                                int sellCount = IntegerArgumentType.getInteger(context, "sellCount");
                                if (buy1Item.is(Items.AIR) || buy2Item.is(Items.AIR) || sellItem.is(Items.AIR)) {
                                    context.getSource().sendSuccess(() -> Component.literal("One or more items are invalid.").withStyle(ChatFormatting.RED), false);
                                    return 1;
                                }
                                if (buy1Item.getComponents().get(DataComponents.MAX_STACK_SIZE) < buy1Count || buy2Item.getComponents().get(DataComponents.MAX_STACK_SIZE) < buy2Count || sellItem.getComponents().get(DataComponents.MAX_STACK_SIZE).intValue() < sellCount) {
                                    context.getSource().sendSuccess(() -> Component.literal("One or more items cannot have the specified item count.").withStyle(ChatFormatting.RED), false);
                                    return 1;
                                }
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).addFullOffer(buy1Item.copyWithCount(buy1Count), buy2Item.copyWithCount(buy2Count), sellItem.copyWithCount(sellCount));
                                context.getSource().sendSuccess(() -> Component.literal("Offer added: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal( + buy1Count + "x " + getItemName(buy1Item) + " + " + buy2Count + "x " + getItemName(buy2Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
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
                                    context.getSource().sendSuccess(() -> Component.literal("Offer removed: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal( + buy1Count + "x " + getItemName(buy1Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                    return 0;
                                }
                                ItemStack buy2Item = offer.buy2();
                                int buy2Count = offer.buy2().getCount();
                                context.getSource().sendSuccess(() -> Component.literal("Offer removed: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal( + buy1Count + "x " + getItemName(buy1Item) + " + " + buy2Count + "x " + getItemName(buy2Item) + " -> " + sellCount + "x " + getItemName(sellItem))), false);
                                return 0;
                            })
                    )
                    .then(literal("set-name")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Argument \"name\" missing!"), false);
                                return 0;
                            })
                            .then(argument("name", StringArgumentType.string())
                            .executes(context -> {
                                String name = context.getArgument("name", String.class);
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setName(name);
                                context.getSource().sendSuccess(() -> Component.literal("Name set to \"" + name + "\"!"), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-identifier")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Argument \"identifier\" missing!"), false);
                                return 0;
                            })
                            .then(argument("identifier", StringArgumentType.string())
                            .executes(context -> {
                                String id = context.getArgument("identifier", String.class);
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setIdentifier(id);
                                context.getSource().sendSuccess(() -> Component.literal("Identifier set to \"" + id + "\"!"), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-appearance")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Arguments \"type\" and \"profession\" missing!"), false);
                                return 0;
                            })
                            .then(argument("type", IdentifierArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.VILLAGER_TYPE.keySet(), b))
                                    .executes(context -> {
                                        context.getSource().sendSuccess(() -> Component.literal("Argument \"profession\" missing!"), false);
                                        return 0;
                                    }).then(argument("profession", IdentifierArgument.id()).suggests((c, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.VILLAGER_PROFESSION.keySet(), b))
                                            .executes(context -> {
                                                VillagerType type = BuiltInRegistries.VILLAGER_TYPE.getValue(context.getArgument("type", Identifier.class));
                                                VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getValue(context.getArgument("profession", Identifier.class));
                                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setVillagerData(new TraderVillagerDefinition(type, profession));
                                                context.getSource().sendSuccess(() -> Component.literal("Villager Appearance set! ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal("Biome: " + type + ", Profession: " + profession)), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("\"hidden\" flag has been toggled to: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(String.valueOf(!current))), false);
                                return 0;
                            }).then(argument("hidden", BoolArgumentType.bool()).executes(context -> {
                                boolean b = BoolArgumentType.getBool(context, "hidden");
                                TraderBuilder.getBuilderFor(context.getSource().getPlayer()).setHidden(b);
                                context.getSource().sendSuccess(() -> Component.literal("\"hidden\" flag has been set to: ").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(String.valueOf(b))), false);
                                return 0;
                            }))
                    )
                    .then(literal("set-filename")
                            .requires(source -> Permissions.require(CommandManager.getPermission("builder")).test(source) || CommandManager.validatePermissionFallback(source))
                            .executes(context -> {
                                context.getSource().sendSuccess(() -> Component.literal("Argument \"filename\" missing!"), false);
                            return 0;
                            })
                            .then(argument("filename", StringArgumentType.string())
                                .executes(context -> {
                                    var builder = TraderBuilder.getBuilderFor(context.getSource().getPlayer());
                                    String filename = context.getArgument("filename", String.class);
                                    if (filename.endsWith(".json")) filename = filename.substring(0, filename.length() - 5);
                                    builder.setFilename(filename);
                                    String finalFilename = filename;
                                    context.getSource().sendSuccess(() -> Component.literal("Filename set:").withStyle(ChatFormatting.DARK_GREEN).append(Component.literal(finalFilename).withStyle(ChatFormatting.BLUE)), false);
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
                                        context.getSource().sendSuccess(() -> Component.literal("Saved trader successfully!"), false);
                                        return 0;
                                    }
                                }
                                context.getSource().sendSuccess(() -> Component.literal("Cannot create trader due to the following errors:").withStyle(ChatFormatting.BOLD, ChatFormatting.RED), false);
                                errors.forEach(s -> context.getSource().sendSuccess(() -> Component.literal("- " + s).withStyle(ChatFormatting.RED), false));
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

    public static final Map<Integer, Permission> LEGACY_PERMISSION_LOOKUP = Map.of(
            1, net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR,
            2, net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER,
            3, net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN,
            4, net.minecraft.server.permissions.Permissions.COMMANDS_OWNER
    );

    private static boolean validatePermissionFallback(CommandSourceStack source) {
        if (ServerTraders.Config.enablePermissionFallback) {
            return source.permissions().hasPermission(CommandManager.LEGACY_PERMISSION_LOOKUP.get(ServerTraders.Config.fallbackPermissionLevel));
        }
        return false;
    }

    private static String getItemName(ItemStack stack) {
        if (true) return stack.getOrDefault(DataComponents.CUSTOM_NAME, stack.getItemName()).getString();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return I18n.get(stack.getItem().getDescriptionId());
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }
}
