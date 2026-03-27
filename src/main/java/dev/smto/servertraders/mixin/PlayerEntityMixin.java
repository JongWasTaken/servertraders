package dev.smto.servertraders.mixin;


import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.smto.servertraders.trading.TraderManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    public void interact(Entity entity, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (!entity.level().isClientSide()) {
            var player = ((Player)(Object)this);
            if (!player.isSpectator() ) {
                if (entity instanceof Villager v) {
                    if (TraderManager.checkInteractedVillager(player, v)) {
                        cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
                        cir.cancel();
                    }
                }
            }
        }
    }
}
