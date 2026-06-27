package me.senseiwells.kursive.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.senseiwells.kursive.client.gui.widget.ScaledStringWidget;
import net.minecraft.client.gui.components.StringWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StringWidget.class)
public class StringWidgetMixin {
    @ModifyExpressionValue(
        method = "visitLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"
        )
    )
    private int scaleTextWidth(int original) {
        if ((Object) this instanceof ScaledStringWidget scaled) {
            return (int) (original * scaled.getScale());
        }
        return original;
    }
}
