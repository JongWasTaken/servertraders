package dev.smto.servertraders.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

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
    public MerchantOffer toVanilla() {
        var vBuy = new ItemCost(this.buy.getItem(), this.buy.getCount());
        Optional<ItemCost> vBuy2 = Optional.empty();
        if (!this.buy2.isEmpty()) {
            vBuy2 = Optional.of(new ItemCost(this.buy2.getItem(), this.buy2.getCount()));
        }
        return new MerchantOffer(
                vBuy, vBuy2, this.sell, 0, Integer.MAX_VALUE, 0, 1.0f, 0
        );
    }
    public boolean isSimple() {
        return this.buy2.isEmpty();
    }
}
