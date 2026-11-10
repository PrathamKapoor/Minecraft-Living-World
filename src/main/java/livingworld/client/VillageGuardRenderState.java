package livingworld.client;

import livingworld.entity.VillageGuardEntity;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Dedicated RenderState for Village Guards and Village Archers (v0.2.4).
 * Extends HumanoidRenderState to capture all 4 armor slots, held items in both hands,
 * bow pulling ticks, shield blocking state, and military rank progression.
 */
public class VillageGuardRenderState extends HumanoidRenderState {
    public VillageGuardEntity.GuardRank rank = VillageGuardEntity.GuardRank.SOLDIER;
    public boolean isArcher = false;
    public boolean isShieldBlocking = false;
}
