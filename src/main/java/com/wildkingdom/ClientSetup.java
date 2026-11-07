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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

@EventBusSubscriber(modid = WildKingdom.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        for (Species.Spec s : Species.ALL) {
            EntityType type = WildKingdom.TYPES.get(s.id()).get();
            final String id = s.id();
            EntityRendererProvider provider = s.ride()
                    ? ctx -> new SaddleRenderer(ctx, new SaddleModel(Identifier.fromNamespaceAndPath(WildKingdom.MODID, id)))
                    : ctx -> new GeoEntityRenderer(ctx, type);
            e.registerEntityRenderer(type, provider);
        }
    }

    static final DataTicket<Boolean> SADDLED = DataTicket.create("wk_saddled", Boolean.class);

    static final class SaddleModel extends DefaultedEntityGeoModel<WildMob> {
        private final Identifier saddled;

        SaddleModel(Identifier base) {
            super(base);
            this.saddled = Identifier.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_saddled");
        }

        @Override
        public Identifier getModelResource(GeoRenderState s) {
            return s.getOrDefaultGeckolibData(SADDLED, false) ? buildFormattedModelPath(saddled) : super.getModelResource(s);
        }

        @Override
        public Identifier getTextureResource(GeoRenderState s) {
            return s.getOrDefaultGeckolibData(SADDLED, false) ? buildFormattedTexturePath(saddled) : super.getTextureResource(s);
        }
    }

    static final class SaddleRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<WildMob, R> {
        SaddleRenderer(EntityRendererProvider.Context ctx, GeoModel<WildMob> model) { super(ctx, model); }

        @Override
        public void captureDefaultRenderState(WildMob a, Void rel, R state, float pt) {
            super.captureDefaultRenderState(a, rel, state, pt);
            state.addGeckolibData(SADDLED, a.isSaddled());
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
