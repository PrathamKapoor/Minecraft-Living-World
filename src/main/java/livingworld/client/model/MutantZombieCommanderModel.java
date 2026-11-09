package livingworld.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;

/**
 * Custom 3-Headed Mutant Zombie Commander Model (Fix 4, F10).
 * Distinctive multi-headed mutated boss model featuring:
 * - Dominant center head tracking look pitch & yaw
 * - Angled left and right mutated secondary heads with independent sway
 * - Broad hulking torso and broadened shoulders (12x14x6)
 * - Heavy muscular arms reaching forward menacingly
 * - Thick mutated legs
 */
public class MutantZombieCommanderModel extends EntityModel<ZombieRenderState> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart headCenter;
    private final ModelPart headLeft;
    private final ModelPart headRight;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MutantZombieCommanderModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.headCenter = root.getChild("head_center");
        this.headLeft = root.getChild("head_left");
        this.headRight = root.getChild("head_right");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Center Dominant Head
        root.addOrReplaceChild("head_center",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.offset(0.0F, -4.0F, 0.0F));

        // Angled Left Mutated Head
        root.addOrReplaceChild("head_left",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
            PartPose.offsetAndRotation(5.5F, -2.5F, 0.5F, 0.10F, 0.40F, 0.15F));

        // Angled Right Mutated Head
        root.addOrReplaceChild("head_right",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .mirror()
                .addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
            PartPose.offsetAndRotation(-5.5F, -2.5F, 0.5F, 0.10F, -0.40F, -0.15F));

        // Broad, Heavily Mutated Torso (14 wide, 15 tall, 7 deep) with hunched dorsal ridge
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-7.0F, 0.0F, -3.5F, 14.0F, 15.0F, 7.0F),
            PartPose.offset(0.0F, -4.0F, 0.0F));

        // Asymmetrical Left Arm: Mutated clawed arm (4.5 wide, 13 tall, 4.5 deep)
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(40, 16)
                .mirror()
                .addBox(-1.0F, -2.0F, -2.25F, 4.5F, 13.0F, 4.5F),
            PartPose.offset(7.5F, -2.0F, 0.0F));

        // Asymmetrical Right Arm: Oversized mutated smasher club arm (6.0 wide, 16 tall, 6.0 deep)
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-5.0F, -2.0F, -3.0F, 6.0F, 16.0F, 6.0F),
            PartPose.offset(-7.5F, -3.0F, 0.0F));

        // Massive Left Leg
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .mirror()
                .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 14.0F, 5.0F),
            PartPose.offset(3.5F, 11.0F, 0.0F));

        // Massive Right Leg
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 14.0F, 5.0F),
            PartPose.offset(-3.5F, 11.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ZombieRenderState state) {
        super.setupAnim(state);

        // Center head look tracking
        this.headCenter.yRot = state.yRot * ((float) Math.PI / 180F);
        this.headCenter.xRot = state.xRot * ((float) Math.PI / 180F);

        // Side heads look around independently
        float animPos = state.walkAnimationPos;
        this.headLeft.yRot = 0.40F + (float) Math.sin(animPos * 0.3F) * 0.15F;
        this.headRight.yRot = -0.40F + (float) Math.cos(animPos * 0.3F) * 0.15F;

        // Forward-reaching menacing arms
        this.rightArm.xRot = -1.45F + (float) Math.sin(animPos * 0.4F) * 0.1F;
        this.leftArm.xRot = -1.45F - (float) Math.sin(animPos * 0.4F) * 0.1F;
        this.rightArm.yRot = 0.15F;
        this.leftArm.yRot = -0.15F;

        // Walking leg swing
        this.rightLeg.xRot = (float) Math.cos(animPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = (float) Math.cos(animPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed;
    }
}
