package com.comfykit;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectUtils;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

@Mod("comfykit")
public class ComfyKit {
    private static final String[] FEATURES = {"hud", "fps", "cps", "coords", "armor", "effects", "sprint", "autotool", "fullbright", "death"};
    private static final boolean[] ON = {true, true, true, true, true, true, false, true, false, true};

    private static final Properties PROPS = new Properties();
    private static File cfgFile;
    private static KeyBinding kZoom, kBright, kHud, kSprint, kTool;
    private static final List<Long> LEFT = new ArrayList<Long>();
    private static final List<Long> RIGHT = new ArrayList<Long>();
    private static double savedGamma = 1.0;
    private static boolean zoomSmooth = false;
    private static BlockPos deathPos = null;
    private static boolean wasDead = false;
    private static int fpsCount = 0, fpsShown = 0;
    private static long fpsT = 0;

    public ComfyKit() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static boolean on(String n) {
        for (int i = 0; i < FEATURES.length; i++) if (FEATURES[i].equals(n)) return ON[i];
        return false;
    }

    private static void load() {
        cfgFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "comfykit.properties");
        try {
            if (cfgFile.exists()) {
                FileInputStream in = new FileInputStream(cfgFile);
                PROPS.load(in);
                in.close();
            }
        } catch (Exception ignored) { }
        for (int i = 0; i < FEATURES.length; i++) {
            String v = PROPS.getProperty(FEATURES[i]);
            if (v != null) ON[i] = Boolean.parseBoolean(v);
        }
    }

    private static void save() {
        for (int i = 0; i < FEATURES.length; i++) PROPS.setProperty(FEATURES[i], String.valueOf(ON[i]));
        try {
            FileOutputStream out = new FileOutputStream(cfgFile);
            PROPS.store(out, "ComfyKit");
            out.close();
        } catch (Exception ignored) { }
    }

    private void clientSetup(FMLClientSetupEvent e) {
        load();
        kZoom = new KeyBinding("Zoom (hold)", GLFW.GLFW_KEY_C, "ComfyKit");
        kBright = new KeyBinding("Fullbright", GLFW.GLFW_KEY_B, "ComfyKit");
        kHud = new KeyBinding("Toggle HUD", GLFW.GLFW_KEY_H, "ComfyKit");
        kSprint = new KeyBinding("Toggle auto-sprint", GLFW.GLFW_KEY_V, "ComfyKit");
        kTool = new KeyBinding("Toggle auto-tool", GLFW.GLFW_KEY_N, "ComfyKit");
        ClientRegistry.registerKeyBinding(kZoom);
        ClientRegistry.registerKeyBinding(kBright);
        ClientRegistry.registerKeyBinding(kHud);
        ClientRegistry.registerKeyBinding(kSprint);
        ClientRegistry.registerKeyBinding(kTool);
    }

    private static void say(String m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendMessage(new StringTextComponent("§a[ComfyKit]§r " + m), Util.NIL_UUID);
    }

    private static void toggle(String n) {
        for (int i = 0; i < FEATURES.length; i++) {
            if (FEATURES[i].equals(n)) {
                ON[i] = !ON[i];
                save();
                if (n.equals("fullbright")) applyBright();
                say(n + ": " + (ON[i] ? "§aON" : "§cOFF"));
            }
        }
    }

    private static void applyBright() {
        GameSettings o = Minecraft.getInstance().options;
        if (on("fullbright")) {
            if (o.gamma < 50.0) savedGamma = o.gamma;
            o.gamma = 100.0;
        } else {
            o.gamma = savedGamma;
        }
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseInputEvent e) {
        if (e.getAction() != GLFW.GLFW_PRESS) return;
        long now = System.currentTimeMillis();
        if (e.getButton() == 0) LEFT.add(now);
        else if (e.getButton() == 1) RIGHT.add(now);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (kBright == null) return;
        if (kBright.consumeClick()) toggle("fullbright");
        if (kHud.consumeClick()) toggle("hud");
        if (kSprint.consumeClick()) toggle("sprint");
        if (kTool.consumeClick()) toggle("autotool");
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        if (kZoom != null && kZoom.isDown()) e.setNewfov(e.getNewfov() * 0.25f);
    }

    @SubscribeEvent
    public void onTick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || kZoom == null) return;
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.level == null) return;

        boolean z = kZoom.isDown() && mc.screen == null;
        if (z != zoomSmooth) { zoomSmooth = z; mc.options.smoothCamera = z; }

        if (on("sprint") && mc.options.keyUp.isDown() && !p.isShiftKeyDown() && !p.isUsingItem()
                && !p.horizontalCollision && p.getFoodData().getFoodLevel() > 6) {
            p.setSprinting(true);
        }

        if (on("autotool") && mc.screen == null && mc.options.keyAttack.isDown()
                && mc.hitResult != null && mc.hitResult.getType() == RayTraceResult.Type.BLOCK) {
            BlockPos bp = ((BlockRayTraceResult) mc.hitResult).getBlockPos();
            BlockState st = mc.level.getBlockState(bp);
            int cur = p.inventory.selected;
            float bestV = p.inventory.getItem(cur).getDestroySpeed(st);
            int best = cur;
            for (int i = 0; i < 9; i++) {
                float v = p.inventory.getItem(i).getDestroySpeed(st);
                if (v > bestV + 0.5f) { bestV = v; best = i; }
            }
            if (best != cur) p.inventory.selected = best;
        }

        boolean dead = mc.screen instanceof DeathScreen || p.isDeadOrDying();
        if (dead && !wasDead && on("death")) {
            deathPos = p.blockPosition();
            say("You died at §e" + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ());
        }
        wasDead = dead;
        if (deathPos != null && p.blockPosition().distSqr(deathPos) < 9.0) { deathPos = null; say("Death point reached"); }

        if (on("fullbright") && mc.options.gamma < 50.0) mc.options.gamma = 100.0;
    }

    private static int cps(List<Long> l) {
        long now = System.currentTimeMillis();
        Iterator<Long> it = l.iterator();
        while (it.hasNext()) if (now - it.next() > 1000L) it.remove();
        return l.size();
    }

    @SubscribeEvent
    public void onRenderTick(net.minecraftforge.event.TickEvent.RenderTickEvent e) {
        if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        fpsCount++;
        long now = System.currentTimeMillis();
        if (now - fpsT >= 1000L) { fpsShown = fpsCount; fpsCount = 0; fpsT = now; }
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (!on("hud") || mc.player == null || mc.options.renderDebug) return;
        MatrixStack ms = e.getMatrixStack();
        FontRenderer fr = mc.font;
        ClientPlayerEntity p = mc.player;
        int x = 4, y = 4;
        if (on("fps")) { fr.drawShadow(ms, "FPS: " + fpsShown, x, y, 0xFFFFFF); y += 10; }
        if (on("cps")) { fr.drawShadow(ms, "CPS: " + cps(LEFT) + " | " + cps(RIGHT), x, y, 0xFFFFFF); y += 10; }
        if (on("coords")) {
            String f = p.getDirection().getName().substring(0, 1).toUpperCase();
            fr.drawShadow(ms, "XYZ: " + (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ()) + " " + f, x, y, 0xFFFFFF);
            y += 10;
            long t = mc.level.getDayTime() % 24000L;
            int hh = (int) ((t / 1000L + 6) % 24), mm = (int) ((t % 1000L) * 60 / 1000L);
            fr.drawShadow(ms, String.format("%02d:%02d", hh, mm), x, y, 0xAAAAAA);
            y += 10;
        }
        if (on("death") && deathPos != null) {
            int dist = (int) Math.sqrt(p.blockPosition().distSqr(deathPos));
            fr.drawShadow(ms, "☠ " + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ() + " (" + dist + "m)", x, y, 0xFF5555);
            y += 10;
        }
        if (on("effects")) {
            for (EffectInstance pe : p.getActiveEffects()) {
                String name = I18n.get(pe.getDescriptionId()) + (pe.getAmplifier() > 0 ? " " + (pe.getAmplifier() + 1) : "");
                int col = pe.getDuration() < 200 ? 0xFF5555 : 0x55FFFF;
                fr.drawShadow(ms, name + " " + EffectUtils.formatDuration(pe, 1.0F), x, y, col);
                y += 10;
            }
        }
        if (on("armor")) drawArmor(mc, ms, fr, p);
    }

    private void drawArmor(Minecraft mc, MatrixStack ms, FontRenderer fr, ClientPlayerEntity p) {
        int x = mc.getWindow().getGuiScaledWidth() - 52;
        int y = mc.getWindow().getGuiScaledHeight() / 2 - 40;
        ItemStack[] items = new ItemStack[]{p.inventory.armor.get(3), p.inventory.armor.get(2),
                p.inventory.armor.get(1), p.inventory.armor.get(0), p.getMainHandItem()};
        for (ItemStack s : items) {
            if (!s.isEmpty()) {
                mc.getItemRenderer().renderGuiItem(s, x, y);
                if (s.isDamageableItem()) {
                    int left = s.getMaxDamage() - s.getDamageValue();
                    float frac = left / (float) s.getMaxDamage();
                    int col = frac < 0.1f ? 0xFF5555 : (frac < 0.35f ? 0xFFFF55 : 0x55FF55);
                    fr.drawShadow(ms, String.valueOf(left), x + 18, y + 4, col);
                } else if (s.getCount() > 1) {
                    fr.drawShadow(ms, "x" + s.getCount(), x + 18, y + 4, 0xFFFFFF);
                }
            }
            y += 18;
        }
    }
}
