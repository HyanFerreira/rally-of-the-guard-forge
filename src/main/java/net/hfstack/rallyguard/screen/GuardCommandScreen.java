package net.hfstack.rallyguard.screen;

import net.hfstack.rallyguard.network.NetworkConstants;
import net.hfstack.rallyguard.network.NetworkBootstrap;
import net.hfstack.rallyguard.network.payload.GuardActionC2SPacket;
import net.hfstack.rallyguard.network.payload.GuardListS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GuardCommandScreen extends Screen {

    public static record Entry(int entityId, String name, boolean patrolling) {
    }

    private final List<Entry> all = new ArrayList<>();
    private int page = 0;

    // Layout
    private static final int PER_PAGE = 4;
    private static final int PANEL_W = 440;
    private static final int PANEL_H = 200;

    private static final int MARGIN_L = 16;
    private static final int MARGIN_R = 16;

    private static final int HEADER_Y = 24;
    private static final int HEADER_LINE_Y = 38;

    private static final int ROW_TOP = 38;
    private static final int ROW_HEIGHT = 28;

    private static final int COL_NAME_X = MARGIN_L;

    private static final int BTN_W1 = 80; // “Teletransportar”
    private static final int BTN_W2 = 80; // “Patrulhar/Parar”
    private static final int BTN_H = 18;
    private static final int BTN_GAP = 2;

    public GuardCommandScreen(List<Entry> entries) {
        super(Component.translatable("gui.rallyguard.command.title"));
        this.all.addAll(entries);
    }

    /**
     * Chamado pelo packet S2C (ver atualização do handle no passo 5).
     */
    public static void openFromPayload(GuardListS2CPacket payload) {
        List<Entry> list = new ArrayList<>(payload.entries().size());
        for (GuardListS2CPacket.Entry e : payload.entries()) {
            list.add(new Entry(e.entityId, e.name, e.patrolling));
        }
        Minecraft.getInstance().setScreen(new GuardCommandScreen(list));
    }

    @Override
    protected void init() {
        super.init();
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, all.size());

        int groupWidth = BTN_W1 + BTN_GAP + BTN_W2;
        int actionsRight = x + PANEL_W - MARGIN_R;
        int actionsLeft = actionsRight - groupWidth;

        for (int i = start; i < end; i++) {
            final int idx = i;
            Entry e = all.get(i);

            int rowTop = y + ROW_TOP + (i - start) * ROW_HEIGHT;
            int rowMidY = rowTop + (ROW_HEIGHT / 2);
            int btnY = rowMidY - (BTN_H / 2);

            Button summon = Button.builder(
                    Component.translatable("gui.rallyguard.command.summon"),
                    b -> sendAction(e.entityId(), NetworkConstants.ACTION_SUMMON)
            ).bounds(actionsLeft, btnY, BTN_W1, BTN_H).build();

            Component label = e.patrolling()
                    ? Component.translatable("gui.rallyguard.command.stop")
                    : Component.translatable("gui.rallyguard.command.patrol");

            Button patrol = Button.builder(label, b -> {
                sendAction(e.entityId(), NetworkConstants.ACTION_TOGGLE_PATROL);
                Entry curr = all.get(idx);
                all.set(idx, new Entry(curr.entityId(), curr.name(), !curr.patrolling()));
                rebuildButtons();
            }).bounds(actionsLeft + BTN_W1 + BTN_GAP, btnY, BTN_W2, BTN_H).build();

            this.addRenderableWidget(summon);
            this.addRenderableWidget(patrol);
        }

        Button prev = Button.builder(Component.literal("<"), b -> {
            if (page > 0) {
                page--;
                rebuildButtons();
            }
        }).bounds(x + 8, y + PANEL_H - 28, 22, 20).build();

        Button next = Button.builder(Component.literal(">"), b -> {
            if ((page + 1) * PER_PAGE < all.size()) {
                page++;
                rebuildButtons();
            }
        }).bounds(x + PANEL_W - 30, y + PANEL_H - 28, 22, 20).build();

        this.addRenderableWidget(prev);
        this.addRenderableWidget(next);
    }

    private void sendAction(int entityId, int action) {
        NetworkBootstrap.CHANNEL.sendToServer(new GuardActionC2SPacket(entityId, action));
    }

    /**
     * Não escurece o mundo atrás.
     */
    @Override
    public void renderBackground(GuiGraphics g) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        drawPanel(g);
        drawRows(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawPanel(GuiGraphics g) {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;

        int bg = 0xF0101010;
        g.fill(x, y, x + PANEL_W, y + PANEL_H, bg);

        int border = 0xFFFFFFFF;
        g.fill(x, y, x + PANEL_W, y + 1, border);
        g.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, border);
        g.fill(x, y, x + 1, y + PANEL_H, border);
        g.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, border);

        // título
        String title = Component.translatable("gui.rallyguard.command.title").getString();
        int titleW = this.font.width(title);
        g.drawString(this.font, title, (this.width - titleW) / 2, y + 8, 0xFFFFFF, true);

        // cabeçalhos
        g.drawString(this.font,
                Component.translatable("gui.rallyguard.command.name"),
                x + COL_NAME_X, y + HEADER_Y, 0xCCCCCC, true);

        // “Ações” centralizado sobre a área dos botões
        int groupWidth = BTN_W1 + BTN_GAP + BTN_W2;
        int actionsRight = x + PANEL_W - MARGIN_R;
        int actionsLeft = actionsRight - groupWidth;
        int actionsCenterX = actionsLeft + groupWidth / 2;

        String actions = Component.translatable("gui.rallyguard.command.actions").getString();
        int actionsW = this.font.width(actions);
        g.drawString(this.font, actions, actionsCenterX - (actionsW / 2), y + HEADER_Y, 0xCCCCCC, true);

        // linha do cabeçalho
        g.fill(x + 6, y + HEADER_LINE_Y, x + PANEL_W - 6, y + HEADER_LINE_Y + 1, 0x33FFFFFF);
    }

    private void drawRows(GuiGraphics g) {
        int x = (this.width - PANEL_W) / 2;
        int y = (this.height - PANEL_H) / 2;

        if (all.isEmpty()) {
            String msg = Component.translatable("gui.rallyguard.command.empty").getString();
            int w = this.font.width(msg);
            g.drawString(this.font, msg, (this.width - w) / 2, y + (PANEL_H / 2), 0xAAAAAA, true);
            drawPageIndicator(g, x, y);
            return;
        }

        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, all.size());

        int groupWidth = BTN_W1 + BTN_GAP + BTN_W2;
        int actionsRight = x + PANEL_W - MARGIN_R;
        int actionsLeft = actionsRight - groupWidth;

        for (int i = start; i < end; i++) {
            Entry e = all.get(i);

            int rowTop = y + ROW_TOP + (i - start) * ROW_HEIGHT;
            int rowMidY = rowTop + (ROW_HEIGHT / 2);
            int rowBot = rowTop + ROW_HEIGHT;

            // separador
            g.fill(x + 6, rowBot - 1, x + PANEL_W - 6, rowBot, 0x22FFFFFF);

            // nome com trim
            String name = e.name();
            int maxNameW = (actionsLeft - 12) - (x + COL_NAME_X);
            if (this.font.width(name) > maxNameW) {
                // trim simples com "..."
                String ell = "...";
                while (name.length() > 0 && this.font.width(name + ell) > maxNameW) {
                    name = name.substring(0, name.length() - 1);
                }
                name = name + ell;
            }
            int nameY = rowMidY - (this.font.lineHeight / 2);
            g.drawString(this.font, name, x + COL_NAME_X, nameY, 0xFFFFFF, true);
        }

        drawPageIndicator(g, x, y);
    }

    private void drawPageIndicator(GuiGraphics g, int x, int y) {
        int totalPages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        String pg = (page + 1) + " / " + totalPages;
        int w = this.font.width(pg);
        g.drawString(this.font, pg, x + (PANEL_W - w) / 2, y + PANEL_H - 24, 0xFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
