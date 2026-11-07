package com.comfykit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "comfykit", name = "ComfyKit", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class ComfyKit {
    // feature names (also used by /comfy and the config file)
    private static final String[] FEATURES = {"hud", "fps", "cps", "coords", "armor", "effects", "sprint", "autotool", "fullbright", "death", "goals"};
    private static final boolean[] ON = {true, true, true, true, true, true, false, true, false, true, true};

    private static Configuration cfg;
    private static final List<Long> LEFT = new ArrayList<Long>();
    private static final List<Long> RIGHT = new ArrayList<Long>();
    private static KeyBinding kZoom, kBright, kHud, kSprint;
    private static float savedGamma = 1.0f;
    private static boolean zoomSmooth = false;
    private static BlockPos deathPos = null;
    private static boolean wasDead = false;
    private static int lastSlot = -1;

    private static final String[] GOALS = {
        "Добыть дерево", "Каменная кирка", "Печка", "Железный слиток", "Железная кирка", "Алмаз",
        "Алмазная кирка", "Обсидиан", "Зайти в Незер", "Огненный стержень", "Жемчуг Эндера",
        "Глаз Эндера", "Зайти в Энд", "Яйцо дракона (победа!)"};
    private static final boolean[] DONE = new boolean[GOALS.length];
    private static String worldKey = null;
    private static int scanTimer = 0;

    private static boolean hasItem(EntityPlayer p, Item it) {
        if (it == null) return false;
        for (ItemStack s : p.inventory.mainInventory) if (s != null && s.getItem() == it) return true;
        for (ItemStack s : p.inventory.armorInventory) if (s != null && s.getItem() == it) return true;
        return false;
    }

    private static boolean[] check(EntityPlayer p) {
        boolean[] r = new boolean[GOALS.length];
        r[0] = hasItem(p, Item.getItemFromBlock(Blocks.log)) || hasItem(p, Item.getItemFromBlock(Blocks.log2)) || hasItem(p, Item.getItemFromBlock(Blocks.planks));
        r[1] = hasItem(p, Items.stone_pickaxe);
        r[2] = hasItem(p, Item.getItemFromBlock(Blocks.furnace));
        r[3] = hasItem(p, Items.iron_ingot);
        r[4] = hasItem(p, Items.iron_pickaxe);
        r[5] = hasItem(p, Items.diamond);
        r[6] = hasItem(p, Items.diamond_pickaxe);
        r[7] = hasItem(p, Item.getItemFromBlock(Blocks.obsidian));
        r[8] = p.dimension == -1;
        r[9] = hasItem(p, Items.blaze_rod);
        r[10] = hasItem(p, Items.ender_pearl);
        r[11] = hasItem(p, Items.ender_eye);
        r[12] = p.dimension == 1;
        r[13] = hasItem(p, Item.getItemFromBlock(Blocks.dragon_egg));
        return r;
    }

    private static String currentWorldKey(Minecraft mc) {
        if (mc.isSingleplayer() && mc.getIntegratedServer() != null) return "sp_" + mc.getIntegratedServer().getWorldName();
        if (mc.getCurrentServerData() != null) return "mp_" + mc.getCurrentServerData().serverIP;
        return null;
    }

    private static void loadGoals(String key) {
        worldKey = key;
        for (int i = 0; i < DONE.length; i++) DONE[i] = key != null && cfg.getBoolean("g" + i, "progress_" + key.replaceAll("[^A-Za-z0-9_.-]", "_"), false, "");
    }

    private static void saveGoal(int i) {
        if (worldKey == null) return;
        cfg.get("progress_" + worldKey.replaceAll("[^A-Za-z0-9_.-]", "_"), "g" + i, false).set(true);
        cfg.save();
    }

    private static int nextGoal() {
        for (int i = 0; i < DONE.length; i++) if (!DONE[i]) return i;
        return -1;
    }

    private static void tickGoals(Minecraft mc, EntityPlayer p) {
        String key = currentWorldKey(mc);
        if (key == null) return;
        if (!key.equals(worldKey)) loadGoals(key);
        if (++scanTimer < 20) return;
        scanTimer = 0;
        boolean[] now = check(p);
        for (int i = 0; i < now.length; i++) {
            if (now[i] && !DONE[i]) {
                DONE[i] = true;
                saveGoal(i);
                say("\u00a76Цель выполнена: \u00a7f" + GOALS[i] + " \u00a77(" + doneCount() + "/" + GOALS.length + ")");
                int n = nextGoal();
                if (n >= 0) say("Следующая: \u00a7e" + GOALS[n]);
                else say("\u00a6aВсе цели выполнены. Ты прошёл Майнкрафт!");
            }
        }
    }

    private static int doneCount() {
        int c = 0;
        for (boolean b : DONE) if (b) c++;
        return c;
    }

    private static int idx(String n) {
        for (int i = 0; i < FEATURES.length; i++) if (FEATURES[i].equalsIgnoreCase(n)) return i;
        return -1;
    }

    private static boolean on(String n) { return ON[idx(n)]; }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        for (int i = 0; i < FEATURES.length; i++) ON[i] = cfg.getBoolean(FEATURES[i], "features", ON[i], "Enable " + FEATURES[i]);
        if (cfg.hasChanged()) cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        kZoom = new KeyBinding("Zoom (hold)", Keyboard.KEY_C, "ComfyKit");
        kBright = new KeyBinding("Fullbright", Keyboard.KEY_B, "ComfyKit");
        kHud = new KeyBinding("Toggle HUD", Keyboard.KEY_H, "ComfyKit");
        kSprint = new KeyBinding("Toggle auto-sprint", Keyboard.KEY_V, "ComfyKit");
        ClientRegistry.registerKeyBinding(kZoom);
        ClientRegistry.registerKeyBinding(kBright);
        ClientRegistry.registerKeyBinding(kHud);
        ClientRegistry.registerKeyBinding(kSprint);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override public String getCommandName() { return "comfy"; }
            @Override public String getCommandUsage(ICommandSender s) { return "/comfy <hud|fps|cps|coords|armor|effects|sprint|autotool|fullbright|death|goals|goals list|cleardeath>"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
            @Override public void processCommand(ICommandSender s, String[] a) {
                if (a.length == 0) { say(getCommandUsage(s)); return; }
                if (a[0].equalsIgnoreCase("goals") && a.length > 1 && a[1].equalsIgnoreCase("list")) {
                    for (int g = 0; g < GOALS.length; g++) say((DONE[g] ? "\u00a7a[x] " : "\u00a77[ ] ") + GOALS[g]);
                    return;
                }
                if (a[0].equalsIgnoreCase("cleardeath")) { deathPos = null; say("Death marker cleared"); return; }
                int i = idx(a[0]);
                if (i < 0) { say(getCommandUsage(s)); return; }
                toggle(i);
            }
        });
    }

    private static void say(String m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("§a[ComfyKit]§r " + m));
    }

    private static void toggle(int i) {
        ON[i] = !ON[i];
        cfg.get("features", FEATURES[i], ON[i]).set(ON[i]);
        cfg.save();
        if (FEATURES[i].equals("fullbright")) applyBright();
        say(FEATURES[i] + ": " + (ON[i] ? "§aON" : "§cOFF"));
    }

    private static void applyBright() {
        Minecraft mc = Minecraft.getMinecraft();
        if (on("fullbright")) {
            if (mc.gameSettings.gammaSetting < 50f) savedGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100f;
        } else {
            mc.gameSettings.gammaSetting = savedGamma;
        }
    }

    // ---- input ----
    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) return;
        long now = System.currentTimeMillis();
        if (e.button == 0) LEFT.add(now);
        else if (e.button == 1) RIGHT.add(now);
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (kBright.isPressed()) toggle(idx("fullbright"));
        if (kHud.isPressed()) toggle(idx("hud"));
        if (kSprint.isPressed()) toggle(idx("sprint"));
    }

    @SubscribeEvent
    public void onFov(FOVUpdateEvent e) {
        if (kZoom != null && kZoom.isKeyDown()) e.newfov *= 0.25f;
    }

    // ---- logic ----
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null || mc.theWorld == null) return;

        // smooth camera while zooming
        boolean z = kZoom.isKeyDown() && mc.currentScreen == null;
        if (z != zoomSmooth) { zoomSmooth = z; mc.gameSettings.smoothCamera = z; }

        // auto sprint
        if (on("sprint") && mc.gameSettings.keyBindForward.isKeyDown() && !p.isSneaking() && !p.isUsingItem()
                && !p.isCollidedHorizontally && p.getFoodStats().getFoodLevel() > 6) {
            p.setSprinting(true);
        }

        // auto tool
        if (on("autotool") && mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown()
                && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            net.minecraft.block.Block b = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
            int cur = p.inventory.currentItem;
            ItemStack held = p.inventory.getStackInSlot(cur);
            float bestV = held == null ? 1.0f : held.getStrVsBlock(b);
            int best = cur;
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.inventory.getStackInSlot(i);
                if (s == null) continue;
                float v = s.getStrVsBlock(b);
                if (v > bestV + 0.5f) { bestV = v; best = i; }
            }
            if (best != cur) p.inventory.currentItem = best;
        }

        // death marker
        boolean dead = mc.currentScreen instanceof GuiGameOver || p.isDead || p.getHealth() <= 0f;
        if (dead && !wasDead && on("death")) {
            deathPos = new BlockPos(p.posX, p.posY, p.posZ);
            say("You died at §e" + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ());
        }
        wasDead = dead;
        if (deathPos != null && p.getDistanceSq(deathPos) < 9.0) { deathPos = null; say("Death point reached"); }

        if (on("goals")) tickGoals(mc, p);

        // fullbright persistence (server settings may reset)
        if (on("fullbright") && mc.gameSettings.gammaSetting < 50f) mc.gameSettings.gammaSetting = 100f;
    }

    // ---- HUD ----
    private static int cps(List<Long> l) {
        long now = System.currentTimeMillis();
        Iterator<Long> it = l.iterator();
        while (it.hasNext()) if (now - it.next() > 1000L) it.remove();
        return l.size();
    }

    @SubscribeEvent
    public void onHud(RenderGameOverlayEvent.Text e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!on("hud") || mc.thePlayer == null || mc.gameSettings.showDebugInfo) return;
        FontRenderer fr = mc.fontRendererObj;
        EntityPlayer p = mc.thePlayer;
        int y = 4;
        int x = 4;
        if (on("fps")) { fr.drawStringWithShadow("FPS: " + Minecraft.getDebugFPS(), x, y, 0xFFFFFF); y += 10; }
        if (on("cps")) { fr.drawStringWithShadow("CPS: " + cps(LEFT) + " | " + cps(RIGHT), x, y, 0xFFFFFF); y += 10; }
        if (on("coords")) {
            int fx = MathHelper.floor_double(p.posX), fy = MathHelper.floor_double(p.posY), fz = MathHelper.floor_double(p.posZ);
            EnumFacing f = EnumFacing.getHorizontal(MathHelper.floor_double((double) (p.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3);
            fr.drawStringWithShadow("XYZ: " + fx + " " + fy + " " + fz + " " + f.getName().substring(0, 1).toUpperCase(), x, y, 0xFFFFFF);
            y += 10;
            long t = mc.theWorld.getWorldTime() % 24000L;
            int hh = (int) ((t / 1000L + 6) % 24), mm = (int) ((t % 1000L) * 60 / 1000L);
            fr.drawStringWithShadow(String.format("%02d:%02d", hh, mm), x, y, 0xAAAAAA);
            y += 10;
        }
        if (on("death") && deathPos != null) {
            int dist = (int) Math.sqrt(p.getDistanceSq(deathPos));
            fr.drawStringWithShadow("☠ " + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ() + " (" + dist + "m)", x, y, 0xFF5555);
            y += 10;
        }
        if (on("goals")) {
            int n = nextGoal();
            String txt = n >= 0 ? "Цель: " + GOALS[n] + " (" + doneCount() + "/" + GOALS.length + ")" : "Все цели выполнены!";
            fr.drawStringWithShadow(txt, x, y, 0xFFD700);
            y += 10;
        }
        if (on("effects")) {
            Collection<PotionEffect> fx = p.getActivePotionEffects();
            for (PotionEffect pe : fx) {
                String name = I18n.format(pe.getEffectName()) + (pe.getAmplifier() > 0 ? " " + (pe.getAmplifier() + 1) : "");
                int col = pe.getDuration() < 200 ? 0xFF5555 : 0x55FFFF;
                fr.drawStringWithShadow(name + " " + Potion.getDurationString(pe), x, y, col);
                y += 10;
            }
        }
        if (on("armor")) drawArmor(mc, fr, p);
    }

    private void drawArmor(Minecraft mc, FontRenderer fr, EntityPlayer p) {
        ScaledResolution sr = new ScaledResolution(mc);
        int x = sr.getScaledWidth() - 52;
        int y = sr.getScaledHeight() / 2 - 40;
        ItemStack[] items = new ItemStack[]{p.inventory.armorInventory[3], p.inventory.armorInventory[2],
                p.inventory.armorInventory[1], p.inventory.armorInventory[0], p.getHeldItem()};
        for (ItemStack s : items) {
            if (s != null) {
                GlStateManager.enableRescaleNormal();
                RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemIntoGUI(s, x, y);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
                if (s.isItemStackDamageable()) {
                    int left = s.getMaxDamage() - s.getItemDamage();
                    float frac = left / (float) s.getMaxDamage();
                    int col = frac < 0.1f ? 0xFF5555 : (frac < 0.35f ? 0xFFFF55 : 0x55FF55);
                    fr.drawStringWithShadow(String.valueOf(left), x + 18, y + 4, col);
                } else if (s.stackSize > 1) {
                    fr.drawStringWithShadow("x" + s.stackSize, x + 18, y + 4, 0xFFFFFF);
                }
            }
            y += 18;
        }
    }
}
