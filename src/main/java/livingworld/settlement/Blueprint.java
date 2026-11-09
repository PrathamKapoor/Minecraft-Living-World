package livingworld.settlement;

import java.util.*;
import livingworld.economy.Stockpile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Blueprint {
    private final String id;
    private final String displayName;
    private final int width;
    private final int height;
    private final int depth;
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

    public Blueprint addBlock(int dx, int dy, int dz, Block block) {
        blocks.put(new BlockPos(dx, dy, dz), block.defaultBlockState());
        return this;
    }

    public Blueprint addBlock(int dx, int dy, int dz, BlockState state) {
        blocks.put(new BlockPos(dx, dy, dz), state);
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
