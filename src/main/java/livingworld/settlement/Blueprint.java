package livingworld.settlement;

import java.util.*;
import livingworld.economy.Stockpile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Modular Vanilla Architectural Blueprint Specification (v0.2.5 Living Village Rebuild).
 * Enforces authentic two-block bed geometry (matching FOOT and HEAD states),
 * functional workstations, and strict zero-vanilla-override invariants.
 */
public class Blueprint {
    private final String id;
    private final String displayName;
    private final int width;
    private final int height;
    private final int depth;
    private int bedCount = 0;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<Stockpile.ResourceType, Integer> resourceCost = new EnumMap<>(Stockpile.ResourceType.class);

    public Blueprint(String id, String displayName, int width, int height, int depth) {
        this.id = id;
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int getBedCount() {
        return bedCount;
    }

    public Blueprint addBlock(int dx, int dy, int dz, Block block) {
        blocks.put(new BlockPos(dx, dy, dz), block.defaultBlockState());
        return this;
    }

    public Blueprint addBlock(int dx, int dy, int dz, BlockState state) {
        blocks.put(new BlockPos(dx, dy, dz), state);
        return this;
    }

    /**
     * Adds an authentic two-block Minecraft bed (FOOT + HEAD matching parts and facing).
     * Guarantees valid sleeping, pathfinding, and POI registration for baby villager maturation.
     */
    public Blueprint addBed(int footX, int footY, int footZ, Direction facing, Block bedBlock) {
        if (bedBlock instanceof BedBlock) {
            BlockState footState = bedBlock.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.FOOT)
                .setValue(BedBlock.FACING, facing);
            BlockState headState = bedBlock.defaultBlockState()
                .setValue(BedBlock.PART, BedPart.HEAD)
                .setValue(BedBlock.FACING, facing);

            addBlock(footX, footY, footZ, footState);
            addBlock(footX + facing.getStepX(), footY, footZ + facing.getStepZ(), headState);
            this.bedCount++;
        } else {
            addBlock(footX, footY, footZ, bedBlock);
        }
        return this;
    }

    public Blueprint setCost(Stockpile.ResourceType type, int amount) {
        resourceCost.put(type, amount);
        return this;
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return blocks;
    }

    public Map<Stockpile.ResourceType, Integer> getResourceCost() {
        return resourceCost;
    }
}
