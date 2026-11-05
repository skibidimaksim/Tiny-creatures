package com.wildkingdom;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@EventBusSubscriber(modid = WildKingdom.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (Species.Spec s : Species.ALL) {
            EntityType type = WildKingdom.TYPES.get(s.id()).get();
            EntityRendererProvider provider = ctx -> new GeoEntityRenderer(ctx, type);
            e.registerEntityRenderer(type, provider);
        }
    }

    @SubscribeEvent
    public static void fluidExt(RegisterClientExtensionsEvent e) {
        e.registerFluidType(new IClientFluidTypeExtensions() {
            @Override public Identifier getStillTexture() { return Identifier.fromNamespaceAndPath(WildKingdom.MODID, "block/resin_still"); }
            @Override public Identifier getFlowingTexture() { return Identifier.fromNamespaceAndPath(WildKingdom.MODID, "block/resin_flow"); }
        }, Resin.TYPE.get());
    }
}
