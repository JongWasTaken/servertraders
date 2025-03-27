package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;

public record TraderDefinition(String name, String identifier, Boolean hidden, TraderVillagerDefinition villager, List<SimpleOffer> trades) {
    public static Codec<TraderDefinition> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
                Codecs.NON_EMPTY_STRING.fieldOf("name").forGetter(TraderDefinition::name),
                Codecs.NON_EMPTY_STRING.fieldOf("identifier").forGetter(TraderDefinition::identifier),
                Codec.BOOL.fieldOf("hidden").forGetter(TraderDefinition::hidden),
                TraderVillagerDefinition.CODEC.fieldOf("villager").forGetter(TraderDefinition::villager),
                SimpleOffer.CODEC.listOf().fieldOf("trades").forGetter(TraderDefinition::trades)
        ).apply(instance, TraderDefinition::new));
}
