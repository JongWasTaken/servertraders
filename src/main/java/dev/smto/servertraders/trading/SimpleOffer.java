package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.Optional;

public record SimpleOffer(ItemStack buy, ItemStack buy2, ItemStack sell) {
    public static Codec<SimpleOffer> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                            ItemStack.CODEC.fieldOf("buy").forGetter(SimpleOffer::buy),
                            ItemStack.OPTIONAL_CODEC.fieldOf("buy2").forGetter(SimpleOffer::buy2),
                            ItemStack.CODEC.fieldOf("sell").forGetter(SimpleOffer::sell)
                    ).apply(instance, SimpleOffer::new));
    public static SimpleOffer simple(ItemStack buy, ItemStack sell) {
        return new SimpleOffer(buy, ItemStack.EMPTY, sell);
    }
    public TradeOffer toVanilla() {
        var vBuy = new TradedItem(buy.getItem(), buy.getCount());
        Optional<TradedItem> vBuy2 = Optional.empty();
        if (!buy2.isEmpty()) {
            vBuy2 = Optional.of(new TradedItem(buy2.getItem(), buy2.getCount()));
        }
        return new TradeOffer(
                vBuy, vBuy2, this.sell, 0, Integer.MAX_VALUE, 0, 1.0f, 0
        );
    }
    public boolean isSimple() {
        return buy2.isEmpty();
    }
}
