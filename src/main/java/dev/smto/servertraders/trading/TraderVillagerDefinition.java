package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

import java.util.Objects;

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
                TraderVillagerDefinition.getHolder(BuiltInRegistries.VILLAGER_TYPE, this.type),
                TraderVillagerDefinition.getHolder(BuiltInRegistries.VILLAGER_PROFESSION, this.profession),
                5
        );
    }

    private static <T> Holder.Reference<T> getHolder(Registry<T> reg, T value) {
        return reg.getOrThrow(ResourceKey.create(reg.key(), Objects.requireNonNull(reg.getKey(value))));
    }
}
