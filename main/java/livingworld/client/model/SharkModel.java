package livingworld.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;

/**
 * Custom Great White Shark Model (Fix 1, F15).
 * Distinctive apex predator 3D model featuring:
 * - Pointed snout and sharp wedge apex
 * - Articulated lower jaw on underside
 * - Iconic tall triangular dorsal fin
 * - Swept-back pectoral fins
 * - Vertical heterocercal caudal tail fin with tail-whip oscillation
 */
public class SharkModel extends EntityModel<DolphinRenderState> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailFin;
    private final ModelPart dorsalFin;
    private final ModelPart pectoralFinLeft;
    private final ModelPart pectoralFinRight;

    public SharkModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tailFin = this.tail.getChild("tail_fin");
        this.dorsalFin = this.body.getChild("dorsal_fin");
        this.pectoralFinLeft = this.body.getChild("pectoral_fin_left");
        this.pectoralFinRight = this.body.getChild("pectoral_fin_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Main Fuselage / Torso
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 16.0F),
            PartPose.offset(0.0F, 18.0F, 0.0F));

        // Shark Head with Pointed Snout
        PartDefinition head = body.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 24)
                .addBox(-3.5F, -3.5F, -7.0F, 7.0F, 6.0F, 7.0F)
                .texOffs(0, 37)
                .addBox(-2.5F, -2.5F, -11.0F, 5.0F, 4.0F, 4.0F)
                .texOffs(18, 37)
                .addBox(-1.5F, -1.5F, -13.0F, 3.0F, 2.0F, 2.0F),
            PartPose.offset(0.0F, 0.0F, -8.0F));

        // Articulated Lower Jaw
        head.addOrReplaceChild("jaw",
            CubeListBuilder.create()
                .texOffs(28, 24)
                .addBox(-3.0F, 0.0F, -6.0F, 6.0F, 2.0F, 6.0F),
            PartPose.offset(0.0F, 1.5F, -1.0F));

        // Tall Triangular Dorsal Fin
        body.addOrReplaceChild("dorsal_fin",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-0.5F, -7.0F, -2.0F, 1.0F, 7.0F, 6.0F),
            PartPose.offset(0.0F, -4.0F, 0.0F));

        // Swept-back Pectoral Fins
        body.addOrReplaceChild("pectoral_fin_left",
            CubeListBuilder.create()
                .texOffs(40, 13)
                .addBox(0.0F, -0.5F, -2.0F, 7.0F, 1.0F, 5.0F),
            PartPose.offsetAndRotation(4.0F, 2.0F, -4.0F, 0.15F, -0.35F, 0.35F));

        body.addOrReplaceChild("pectoral_fin_right",
            CubeListBuilder.create()
                .texOffs(40, 13)
                .mirror()
                .addBox(-7.0F, -0.5F, -2.0F, 7.0F, 1.0F, 5.0F),
            PartPose.offsetAndRotation(-4.0F, 2.0F, -4.0F, 0.15F, 0.35F, -0.35F));

        // Rear Tail Peduncle
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create()
                .texOffs(0, 43)
                .addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 8.0F),
            PartPose.offset(0.0F, 0.0F, 8.0F));

        // Vertical Heterocercal Tail Fin (Upper lobe longer than lower lobe)
        tail.addOrReplaceChild("tail_fin",
            CubeListBuilder.create()
                .texOffs(26, 32)
                .addBox(-0.5F, -8.0F, 4.0F, 1.0F, 8.0F, 6.0F)
                .texOffs(40, 32)
                .addBox(-0.5F, 0.0F, 4.0F, 1.0F, 6.0F, 4.0F),
            PartPose.offset(0.0F, 0.0F, 4.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DolphinRenderState state) {
        super.setupAnim(state);

        float animPos = state.walkAnimationPos;

        // Tail oscillation
        this.tail.yRot = (float) Math.sin(animPos * 0.5F) * 0.45F;
        this.tailFin.yRot = (float) Math.sin(animPos * 0.5F - 0.5F) * 0.35F;

        // Body sway
        this.body.yRot = (float) Math.sin(animPos * 0.5F - 1.0F) * 0.10F;

        // Articulated jaw bite snap
        this.jaw.xRot = (float) Math.abs(Math.sin(animPos * 0.4F)) * 0.28F;
    }
}
