package livingworld.client.model;

import livingworld.client.VillageGuardRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Dedicated 3D Model for Village Guard and Village Archer (v0.2.4).
 * Combines authentic Minecraft Villager head & 3D nose features with full bipedal
 * humanoid limbs for wielding weapons, raising shields, aiming bows, and wearing 3D armor.
 */
public class VillageGuardModel extends HumanoidModel<VillageGuardRenderState> {
    public VillageGuardModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // Authentic Villager Head with prominent 3D nose
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F)
                .texOffs(24, 0).addBox(-1.0F, -3.0F, -6.0F, 2.0F, 4.0F, 2.0F),
            PartPose.ZERO
        );

        // Hat / Helmet Brow Layer
        root.addOrReplaceChild("hat",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.ZERO
        );

        // Torso / Gambeson
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.ZERO
        );

        // Right Arm (Sword / Bow wielding arm)
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(-5.0F, 2.0F, 0.0F)
        );

        // Left Arm (Shield / Offhand arm)
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(5.0F, 2.0F, 0.0F)
        );

        // Right Leg
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(-1.9F, 12.0F, 0.0F)
        );

        // Left Leg
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F)
                .texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(1.9F, 12.0F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }
}
