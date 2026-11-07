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
import org.lwjgl.input.Keyboard;

@Mod(modid = "liteshade", name = "LiteShade", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class LiteShade {
    public static final String[] NAMES = {"off", "vivid", "cinematic", "bloom", "warm", "cold", "noir", "crisp"};

    private static Configuration cfg;
    private static int preset = 0;
    private static KeyBinding key;
    private static int fails = 0;
    private static int timer = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        String saved = cfg.getString("preset", "general", "cinematic", "One of: off, vivid, cinematic, bloom, warm, cold, noir, crisp");
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
            @Override public String getCommandUsage(ICommandSender s) { return "/liteshade <off|vivid|cinematic|bloom|warm|cold|noir|crisp>"; }
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
        cfg.get("general", "preset", "cinematic").set(NAMES[i]);
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
}
