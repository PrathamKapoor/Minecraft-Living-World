package livingworld.client;

import livingworld.client.model.VillageGuardModel;
import livingworld.entity.VillageArcherEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dedicated Client Renderer for Village Archer (v0.2.4).
 * Extends HumanoidMobRenderer with HumanoidArmorLayer and ItemInHandLayer,
 * visibly displaying armor, held bow, aiming stances, and drawing animations.
 */
public class VillageArcherRenderer extends HumanoidMobRenderer<VillageArcherEntity, VillageGuardRenderState, VillageGuardModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/guard/village_archer.png");

    public VillageArcherRenderer(EntityRendererProvider.Context context) {
        super(context, new VillageGuardModel(VillageGuardModel.createBodyLayer().bakeRoot()), 0.5f);
        this.addLayer(new HumanoidArmorLayer<>(this,
            ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new),
            context.getEquipmentRenderer()));
    }

    @Override
    public Identifier getTextureLocation(VillageGuardRenderState state) {
        return TEXTURE;
    }

    @Override
    public VillageGuardRenderState createRenderState() {
        return new VillageGuardRenderState();
    }

    @Override
    public void extractRenderState(VillageArcherEntity entity, VillageGuardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rank = entity.getRank();
        state.isArcher = true;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(VillageArcherEntity entity, HumanoidArm arm) {
        ItemStack item = entity.getItemHeldByArm(arm);
        if (item.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (entity.getMainArm() == arm && item.is(Items.BOW)) {
            if (entity.isUsingItem() || entity.getTarget() != null) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        }
        return HumanoidModel.ArmPose.ITEM;
    }
}
