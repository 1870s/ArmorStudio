package com.armorstudio.gui.widget;

import com.armorstudio.config.ArmorSlot;
import com.armorstudio.config.Options.DurabilityText;
import com.armorstudio.gui.Text;
import com.armorstudio.gui.Theme;
import com.armorstudio.hud.ArmorHud;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Detail view of one equipped armor piece (real equipment): durability and, on hover, the full item tooltip. */
public class ArmorDetailRow extends Row {
    private final ArmorSlot slot;

    public ArmorDetailRow(ArmorSlot slot) {
        this.slot = slot;
    }

    private ItemStack stack() {
        var player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : player.getItemBySlot(slot.equipmentSlot);
    }

    @Override
    public int height() {
        return 26;
    }

    @Override
    protected void extractRow(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY, float dt) {
        card(g, x, y, width, height() - 2, Theme.lerp(Theme.CARD, Theme.CARD_HOVER, hover));
        ItemStack stack = stack();
        if (stack.isEmpty()) {
            g.text(font, slot.displayName().copy().append(": ").append(Component.translatable("armorstudio.hud.empty_slot")),
                x + 28, y + 8, Theme.TEXT_MUTED, false);
            return;
        }
        g.item(stack, x + 6, y + 4);
        String durability = ArmorHud.durabilityText(stack, DurabilityText.BOTH);
        int dw = font.width(durability);
        g.text(font, Text.trim(font, stack.getHoverName(), width - 44 - dw), x + 28, y + 4, Theme.TEXT, false);
        int enchants = stack.getEnchantments().size();
        Component sub = enchants > 0 ? Component.translatable("armorstudio.hud.enchant_count", enchants) : Component.translatable("armorstudio.hud.no_enchants");
        g.text(font, sub, x + 28, y + 14, enchants > 0 ? Theme.ACCENT_LIGHT : Theme.TEXT_MUTED, false);
        int color = stack.isDamageableItem() ? 0xFF000000 | stack.getBarColor() : Theme.TEXT_DIM;
        g.text(font, durability, x + width - 8 - dw, y + 9, color, false);
    }

    @Override
    public List<FormattedCharSequence> tooltip(Font font, int mouseX, int mouseY) {
        ItemStack stack = stack();
        if (!contains(mouseX, mouseY) || stack.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL)) {
            lines.addAll(font.split(line, 260));
        }
        return lines;
    }
}
