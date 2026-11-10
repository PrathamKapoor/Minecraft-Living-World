package livingworld.client;

import livingworld.entity.IllagerShieldbearerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

public class IllagerShieldbearerRenderer extends MobRenderer<IllagerShieldbearerEntity, IllagerRenderState, IllagerModel<IllagerRenderState>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/illager/pillager.png");

    public IllagerShieldbearerRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.55f);
    }

    @Override
    public Identifier getTextureLocation(IllagerRenderState state) {
        return TEXTURE;
    }

    @Override
    public IllagerRenderState createRenderState() {
        return new IllagerRenderState();
    }
}
