package livingworld.client;

import livingworld.entity.ZombieSheepEntity;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;

public class ZombieSheepRenderer extends MobRenderer<ZombieSheepEntity, SheepRenderState, SheepModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/zombie/zombie_sheep.png");

    public ZombieSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel(context.bakeLayer(ModelLayers.SHEEP)), 0.7f);
    }

    @Override
    public Identifier getTextureLocation(SheepRenderState state) {
        return TEXTURE;
    }

    @Override
    public SheepRenderState createRenderState() {
        return new SheepRenderState();
    }
}
