package com.armorstudio.hud;

import com.armorstudio.compat.ClientCompat;
import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.ConfigManager;
import com.armorstudio.config.HudSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Warns once when an equipped armor piece drops to the low-durability threshold. The warning is local only (action bar
 * text plus a UI sound); nothing is sent to the server.
 */
public final class DurabilityWarnings {
    private static final boolean[] WARNED = new boolean[4];
    private static final Item[] ITEMS = new Item[4];

    private DurabilityWarnings() {}

    public static void tick(Minecraft mc) {
        HudSettings hud = ConfigManager.get().hud;
        if (mc.player == null || hud.lowThreshold <= 0 || (!hud.warnSound && !hud.warnMessage)) return;
        for (ArmorSlot slot : ArmorSlot.all()) {
            int i = slot.ordinal();
            ItemStack stack = mc.player.getItemBySlot(slot.equipmentSlot);
            Item item = stack.isEmpty() ? null : stack.getItem();
            if (item != ITEMS[i]) {
                // A different item was equipped: start fresh, but do not warn for a piece that was already low when equipped.
                ITEMS[i] = item;
                WARNED[i] = ArmorHud.isLow(stack, hud.lowThreshold);
                continue;
            }
            boolean low = ArmorHud.isLow(stack, hud.lowThreshold);
            if (low && !WARNED[i]) {
                WARNED[i] = true;
                warn(mc, hud, slot, stack);
            } else if (!low) {
                WARNED[i] = false;
            }
        }
    }

    private static void warn(Minecraft mc, HudSettings hud, ArmorSlot slot, ItemStack stack) {
        if (hud.warnMessage) {
            ClientCompat.actionBar(Component.translatable("armorstudio.warning.low_durability", stack.getHoverName(),
                ArmorHud.percentLeft(stack)).withStyle(ChatFormatting.RED));
        }
        if (hud.warnSound) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, 0.8f));
    }
}
