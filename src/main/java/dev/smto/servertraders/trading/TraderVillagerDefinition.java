package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

public record TraderVillagerDefinition(VillagerType type, VillagerProfession profession) {
    public static TraderVillagerDefinition DEFAULT = new TraderVillagerDefinition(
            BuiltInRegistries.VILLAGER_TYPE.getValue(VillagerType.PLAINS), BuiltInRegistries.VILLAGER_PROFESSION.getValue(VillagerProfession.NONE));
    public static Codec<TraderVillagerDefinition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ExtraCodecs.RESOURCE_PATH_CODEC.fieldOf("type").xmap((s) -> BuiltInRegistries.VILLAGER_TYPE.getValue(Identifier.withDefaultNamespace(s)), (t) -> BuiltInRegistries.VILLAGER_TYPE.getKey(t).getPath()).forGetter(TraderVillagerDefinition::type),
                    ExtraCodecs.RESOURCE_PATH_CODEC.fieldOf("profession").xmap((s) -> BuiltInRegistries.VILLAGER_PROFESSION.getValue(Identifier.withDefaultNamespace(s)), (p) -> BuiltInRegistries.VILLAGER_PROFESSION.getKey(p).getPath()).forGetter(TraderVillagerDefinition::profession)
            ).apply(instance, TraderVillagerDefinition::new));
    public VillagerData toVanilla() {
        return new VillagerData(
                // Look at this silliness, I wonder what the mojang-approved way to do this is...
                BuiltInRegistries.VILLAGER_TYPE.getOrThrow(ResourceKey.create(Registries.VILLAGER_TYPE, BuiltInRegistries.VILLAGER_TYPE.getKey(this.type))),
                BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(ResourceKey.create(Registries.VILLAGER_PROFESSION, BuiltInRegistries.VILLAGER_PROFESSION.getKey(this.profession))),
                5
        );
    }
}
