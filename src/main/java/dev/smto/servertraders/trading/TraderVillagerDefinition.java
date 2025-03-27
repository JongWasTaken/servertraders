package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

public record TraderVillagerDefinition(VillagerType type, VillagerProfession profession) {
    public static TraderVillagerDefinition DEFAULT = new TraderVillagerDefinition(VillagerType.PLAINS, VillagerProfession.NONE);
    public static Codec<TraderVillagerDefinition> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codecs.IDENTIFIER_PATH.fieldOf("type").xmap((s) -> Registries.VILLAGER_TYPE.get(Identifier.ofVanilla(s)), (t) -> Registries.VILLAGER_TYPE.getId(t).getPath()).forGetter(TraderVillagerDefinition::type),
                    Codecs.IDENTIFIER_PATH.fieldOf("profession").xmap((s) -> Registries.VILLAGER_PROFESSION.get(Identifier.ofVanilla(s)), (p) -> Registries.VILLAGER_PROFESSION.getId(p).getPath()).forGetter(TraderVillagerDefinition::profession)
            ).apply(instance, TraderVillagerDefinition::new));
}
