package livingworld.client;

import livingworld.entity.IllagerSwordsmanEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

public class IllagerSwordsmanRenderer extends MobRenderer<IllagerSwordsmanEntity, IllagerRenderState, IllagerModel<IllagerRenderState>> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/illager/vindicator.png");

    public IllagerSwordsmanRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.VINDICATOR)), 0.5f);
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
