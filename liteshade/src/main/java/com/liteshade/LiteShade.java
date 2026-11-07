package com.liteshade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

@Mod(modid = "liteshade", name = "LiteShade", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class LiteShade {
    public static final String[] NAMES = {"off", "vivid", "cinematic", "bloom", "warm", "cold", "noir", "crisp", "sunny", "sunnylite"};

    private static Configuration cfg;
    private static int preset = 0;
    private static KeyBinding key;
    private static int fails = 0;
    private static int timer = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        String saved = cfg.getString("preset", "general", "sunny", "One of: off, vivid, cinematic, bloom, warm, cold, noir, crisp, sunny, sunnylite");
        preset = indexOf(saved);
        if (cfg.hasChanged()) cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        key = new KeyBinding("Cycle shader", Keyboard.KEY_O, "LiteShade");
        ClientRegistry.registerKeyBinding(key);
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override public String getCommandName() { return "liteshade"; }
            @Override public String getCommandUsage(ICommandSender s) { return "/liteshade <off|vivid|cinematic|bloom|warm|cold|noir|crisp|sunny|sunnylite>"; }
            @Override public int getRequiredPermissionLevel() { return 0; }
            @Override public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
            @Override public void processCommand(ICommandSender s, String[] a) {
                if (a.length == 0) { say("Preset: " + NAMES[preset] + ". " + getCommandUsage(s)); return; }
                int i = indexOf(a[0]);
                if (i < 0 || !NAMES[i].equalsIgnoreCase(a[0])) { say(getCommandUsage(s)); return; }
                set(i);
            }
        });
    }

    private static int indexOf(String n) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equalsIgnoreCase(n)) return i;
        return 0;
    }

    private static void say(String m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText("§b[LiteShade]§r " + m));
    }

    private static void set(int i) {
        preset = i;
        fails = 0;
        cfg.get("general", "preset", "sunny").set(NAMES[i]);
        cfg.save();
        apply();
        say("Shader: " + NAMES[i]);
    }

    private static void apply() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (preset == 0) {
                if (mc.entityRenderer.isShaderActive()) mc.entityRenderer.stopUseShader();
                return;
            }
            mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/liteshade_" + NAMES[preset] + ".json"));
            if (!mc.entityRenderer.isShaderActive()) fails++;
        } catch (Throwable t) {
            fails++;
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (key.isPressed()) set((preset + 1) % NAMES.length);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || preset == 0 || fails >= 3) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.entityRenderer == null) return;
        if (++timer < 40) return;
        timer = 0;
        if (!mc.entityRenderer.isShaderActive()) apply();
    }

    private static Field shadersField;
    private static boolean fieldFailed = false;

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || preset == 0 || fieldFailed) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.entityRenderer == null || !mc.entityRenderer.isShaderActive()) return;
        Entity v = mc.getRenderViewEntity();
        if (v == null) return;
        try {
            if (shadersField == null) shadersField = ReflectionHelper.findField(ShaderGroup.class, "listShaders", "field_148031_d");
            ShaderGroup g = mc.entityRenderer.getShaderGroup();
            List<Shader> list = (List<Shader>) shadersField.get(g);
            float pt = e.renderTickTime;
            double th = mc.theWorld.getCelestialAngle(pt) * Math.PI * 2.0;
            double dx = -Math.sin(th), dy = Math.cos(th), dz = 0.0;
            double yaw = Math.toRadians(v.prevRotationYaw + (v.rotationYaw - v.prevRotationYaw) * pt);
            double pitch = Math.toRadians(v.prevRotationPitch + (v.rotationPitch - v.prevRotationPitch) * pt);
            double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
            double rx = -Math.cos(yaw), ry = 0.0, rz = -Math.sin(yaw);
            double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
            double cx = dx * rx + dy * ry + dz * rz;
            double cy = dx * ux + dy * uy + dz * uz;
            double cz = dx * fx + dy * fy + dz * fz;
            double fov = Math.toRadians(mc.gameSettings.fovSetting);
            double aspect = (double) mc.displayWidth / (double) mc.displayHeight;
            double t = Math.tan(fov / 2.0);
            float sx = 0.5f, sy = 0.5f, inten = 0f;
            if (cz > 0.05) {
                double nx = cx / (cz * t * aspect), ny = cy / (cz * t);
                sx = (float) (nx * 0.5 + 0.5);
                sy = (float) (ny * 0.5 + 0.5);
                double off = Math.max(Math.abs(nx), Math.abs(ny));
                float fade = (float) Math.max(0.0, Math.min(1.0, 1.8 - off));
                float day = (float) Math.max(0.0, Math.min(1.0, dy * 3.0 + 0.2));
                float rain = 1.0f - mc.theWorld.getRainStrength(pt) * 0.85f;
                inten = fade * day * rain;
            }
            float warm = (float) Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(dy) * 2.2));
            for (Shader sh : list) {
                ShaderUniform u = sh.getShaderManager().getShaderUniform("Sun");
                if (u != null) u.set(sx, sy, inten, warm);
            }
        } catch (Throwable ex) {
            fieldFailed = true;
        }
    }
}
