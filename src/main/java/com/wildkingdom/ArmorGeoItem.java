package com.wildkingdom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArmorGeoItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String set;

    public ArmorGeoItem(Item.Properties props, String set) {
        super(props);
        this.set = set;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        final String s = this.set;
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public @Nullable GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) this.renderer = new SetRenderer<>(s);
                return this.renderer;
            }
        });
    }

    static final class SetModel extends GeoModel<ArmorGeoItem> {
        private final Identifier model, tex, anim;

        SetModel(String set) {
            this.model = Identifier.fromNamespaceAndPath(WildKingdom.MODID, "armor/" + set);
            this.tex = Identifier.fromNamespaceAndPath(WildKingdom.MODID, "textures/armor/" + set + ".png");
            this.anim = Identifier.fromNamespaceAndPath(WildKingdom.MODID, "armor/" + set);
        }

        @Override public Identifier getModelResource(GeoRenderState s) { return model; }
        @Override public Identifier getTextureResource(GeoRenderState s) { return tex; }
        @Override public Identifier getAnimationResource(ArmorGeoItem a) { return anim; }
    }

    static final class SetRenderer<R extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<ArmorGeoItem, R> {
        SetRenderer(String set) { super(new SetModel(set)); }
    }
}
