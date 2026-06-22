package net.hfstack.rallyguard.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class HireGuardScreen extends AbstractContainerScreen<HireGuardMenu> {

    private Button hireButton;

    public HireGuardScreen(HireGuardMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 320;
        this.imageHeight = 110;
    }

    @Override
    protected void init() {
        super.init();

        // escondendo labels padrão
        this.inventoryLabelX = Integer.MAX_VALUE / 2;
        this.inventoryLabelY = -1000;
        this.titleLabelX = Integer.MAX_VALUE / 2;

        int y = (this.height - this.imageHeight) / 2;

        this.hireButton = Button.builder(
                Component.translatable("gui.rallyguard.hire.button"),
                b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }
        ).bounds(this.width / 2 - 50, y + 66, 100, 20).build();

        this.addRenderableWidget(this.hireButton);
    }

    /**
     * NÃO escurecer o mundo.
     */
    @Override
    public void renderBackground(GuiGraphics g) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        this.renderTooltip(g, mouseX, mouseY);
    }

    /**
     * Painel simples + texto centralizado com wrap.
     */
    @Override
    protected void renderBg(GuiGraphics g, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int bg = 0xF0101010;
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, bg);

        int border = 0xFFFFFFFF;
        g.fill(x, y, x + this.imageWidth, y + 1, border);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, border);
        g.fill(x, y, x + 1, y + this.imageHeight, border);
        g.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, border);

        // Título
        String title = Component.translatable("gui.rallyguard.hire.title").getString();
        int tw = this.font.width(title);
        g.drawString(this.font, title, (this.width - tw) / 2, y + 10, 0xFFFFFF, true);

        // Body com wrap (respeita \n)
        Component body = Component.translatable("gui.rallyguard.hire.body");
        int maxTextWidth = this.imageWidth - 24;
        List<FormattedCharSequence> lines = this.font.split(body, maxTextWidth);

        int lineY = y + 36;
        for (FormattedCharSequence seq : lines) {
            int w = this.font.width(seq);
            int lineX = this.width / 2 - (w / 2);
            g.drawString(this.font, seq, lineX, lineY, 0xFFFFFF, true);
            lineY += this.font.lineHeight + 2;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // vazio de propósito
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
