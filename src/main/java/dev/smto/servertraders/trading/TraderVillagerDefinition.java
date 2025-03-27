package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

public record TraderVillagerDefinition(VillagerType type, VillagerProfession profession) {
    public static TraderVillagerDefinition DEFAULT = new TraderVillagerDefinition(
            Registries.VILLAGER_TYPE.get(VillagerType.PLAINS), Registries.VILLAGER_PROFESSION.get(VillagerProfession.NONE));
    public static Codec<TraderVillagerDefinition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codecs.IDENTIFIER_PATH.fieldOf("type").xmap((s) -> Registries.VILLAGER_TYPE.get(Identifier.ofVanilla(s)), (t) -> Registries.VILLAGER_TYPE.getId(t).getPath()).forGetter(TraderVillagerDefinition::type),
                    Codecs.IDENTIFIER_PATH.fieldOf("profession").xmap((s) -> Registries.VILLAGER_PROFESSION.get(Identifier.ofVanilla(s)), (p) -> Registries.VILLAGER_PROFESSION.getId(p).getPath()).forGetter(TraderVillagerDefinition::profession)
            ).apply(instance, TraderVillagerDefinition::new));
    public VillagerData toVanilla() {
        return new VillagerData(
                // Look at this silliness, I wonder what the mojang-approved way to do this is...
                Registries.VILLAGER_TYPE.getOrThrow(RegistryKey.of(RegistryKeys.VILLAGER_TYPE, Registries.VILLAGER_TYPE.getId(this.type))),
                Registries.VILLAGER_PROFESSION.getOrThrow(RegistryKey.of(RegistryKeys.VILLAGER_PROFESSION, Registries.VILLAGER_PROFESSION.getId(this.profession))),
                5
        );
    }
}
