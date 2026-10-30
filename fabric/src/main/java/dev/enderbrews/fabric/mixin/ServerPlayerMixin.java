package dev.enderbrews.fabric.mixin;

import dev.enderbrews.fabric.EnderBrewsFabric;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla does not keep the language a client reports, and messages are translated on the server. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void enderbrews$rememberLanguage(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        EnderBrewsFabric.LANGUAGES.put(((ServerPlayer) (Object) this).getUUID(), packet.language());
    }
}
