package livingworld.client;

import livingworld.client.model.VillageGuardModel;
import livingworld.entity.VillageGuardEntity;
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
 * Dedicated Client Renderer for Village Guard (v0.2.4).
 * Extends HumanoidMobRenderer with HumanoidArmorLayer and ItemInHandLayer,
 * visibly displaying full iron/diamond armor, iron sword, and raised shield.
 */
public class VillageGuardRenderer extends HumanoidMobRenderer<VillageGuardEntity, VillageGuardRenderState, VillageGuardModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("livingworld", "textures/entity/guard/village_guard.png");

    public VillageGuardRenderer(EntityRendererProvider.Context context) {
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
    public void extractRenderState(VillageGuardEntity entity, VillageGuardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.rank = entity.getRank();
        state.isArcher = false;
        state.isShieldBlocking = entity.isBlocking();
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(VillageGuardEntity entity, HumanoidArm arm) {
        ItemStack item = entity.getItemHeldByArm(arm);
        if (item.isEmpty()) {
            return HumanoidModel.ArmPose.EMPTY;
        }
        if (item.is(Items.SHIELD) && entity.isBlocking()) {
            return HumanoidModel.ArmPose.BLOCK;
        }
        return HumanoidModel.ArmPose.ITEM;
    }
}
