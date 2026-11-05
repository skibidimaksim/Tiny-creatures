package com.wildkingdom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BestiaryClient {
    private BestiaryClient() {}

    public static void open() { Minecraft.getInstance().setScreen(new BestiaryScreen(0)); }

    static final class BestiaryScreen extends Screen {
        private static final int PER = 8;
        private final int page;

        BestiaryScreen(int page) {
            super(Component.translatable("gui.wildkingdom.bestiary"));
            this.page = page;
        }

        private int pages() { return (Species.ALL.size() + PER - 1) / PER; }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int y = this.height - 28;
            Button prev = Button.builder(Component.literal("<"), b -> Minecraft.getInstance().setScreen(new BestiaryScreen(page - 1)))
                    .bounds(cx - 70, y, 40, 20).build();
            prev.active = page > 0;
            Button next = Button.builder(Component.literal(">"), b -> Minecraft.getInstance().setScreen(new BestiaryScreen(page + 1)))
                    .bounds(cx + 30, y, 40, 20).build();
            next.active = page < pages() - 1;
            addRenderableWidget(prev);
            addRenderableWidget(next);
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float dt) {
            super.render(g, mx, my, dt);
            int cx = this.width / 2;
            g.drawCenteredString(this.font, this.title, cx, 10, 0xFFFFFFFF);
            g.drawCenteredString(this.font, Component.literal((page + 1) + " / " + pages()), cx, this.height - 42, 0xFFAAAAAA);
            int x = Math.max(10, cx - 170);
            int y = 28;
            for (int i = page * PER; i < Math.min(Species.ALL.size(), (page + 1) * PER); i++) {
                Species.Spec s = Species.ALL.get(i);
                g.renderItem(WildKingdom.EGGS.get(i).get().getDefaultInstance(), x, y);
                g.drawString(this.font, Component.translatable("entity.wildkingdom." + s.id()), x + 22, y, 0xFFFFFFFF);
                StringBuilder sb = new StringBuilder();
                sb.append("HP ").append((int) s.hp()).append("  ").append(s.dmg() > 0 ? "ATK " + (int) s.dmg() : "");
                if (s.ride()) sb.append("  ").append(Component.translatable("gui.wildkingdom.rideable").getString());
                if (s.boss()) sb.append("  ").append(Component.translatable("gui.wildkingdom.boss").getString());
                if (s.herd()) sb.append("  ").append(Component.translatable("gui.wildkingdom.herd").getString());
                g.drawString(this.font, Component.literal(sb.toString()), x + 22, y + 10, 0xFFAAAAAA);
                y += 24;
            }
        }

        @Override
        public boolean isPauseScreen() { return false; }
    }
}
