package livingworld.client;

import livingworld.entity.ZombieCowEntity;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class ZombieCowRenderer extends MobRenderer<ZombieCowEntity, LivingEntityRenderState, CowModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/zombie/zombie_cow.png");

    public ZombieCowRenderer(EntityRendererProvider.Context context) {
        super(context, new CowModel(context.bakeLayer(ModelLayers.COW)), 0.7f);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
