package livingworld.settlement;

import java.util.*;
import livingworld.economy.Stockpile;
import livingworld.faction.FactionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class Settlement {
    private final UUID id;
    private String name;
    private BlockPos center;
    private int radius;
    private FactionType factionType;
    private SettlementState state;
    private final Stockpile stockpile = new Stockpile();
    private int population = 6;
    private int housingCapacity = 10;
    private int guardCount = 2;
    private long lastStrategicUpdate = 0;

    private final Queue<ConstructionProject> constructionQueue = new LinkedList<>();
    private ConstructionProject activeProject = null;

    public Settlement(UUID id, String name, BlockPos center, int radius, FactionType factionType) {
        this.id = id;
        this.name = name;
        this.center = center.immutable();
        this.radius = radius;
        this.factionType = factionType;
        this.state = SettlementState.STABLE;

        // Initial settlement starter stocks
        stockpile.add(Stockpile.ResourceType.FOOD, 40);
        stockpile.add(Stockpile.ResourceType.WOOD, 50);
        stockpile.add(Stockpile.ResourceType.STONE, 40);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos getCenter() {
        return center;
    }

    public void setCenter(BlockPos center) {
        this.center = center.immutable();
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public FactionType getFactionType() {
        return factionType;
    }

    public void setFactionType(FactionType type) {
        this.factionType = type;
    }

    public SettlementState getState() {
        return state;
    }

    public void setState(SettlementState state) {
        this.state = state;
    }

    public Stockpile getStockpile() {
        return stockpile;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getHousingCapacity() {
        return housingCapacity;
    }

    public void setHousingCapacity(int housingCapacity) {
        this.housingCapacity = housingCapacity;
    }

    public int getGuardCount() {
        return guardCount;
    }

    public void setGuardCount(int count) {
        this.guardCount = count;
    }

    public ConstructionProject getActiveProject() {
        return activeProject;
    }

    public int getQueuedProjectsCount() {
        return constructionQueue.size();
    }

    public void queueProject(ConstructionProject project) {
        constructionQueue.add(project);
    }

    public void strategicTick(ServerLevel level, long currentTicks) {
        lastStrategicUpdate = currentTicks;

        // 1. Food Consumption & Civilian Sustenance
        int foodDemand = Math.max(1, population / 2);
        boolean wellFed = stockpile.consume(Stockpile.ResourceType.FOOD, foodDemand);

        if (wellFed) {
            // Food surplus allows natural growth if housing available
            if (population < housingCapacity && stockpile.get(Stockpile.ResourceType.FOOD) > 30) {
                population++;
                state = SettlementState.GROWING;
            } else {
                state = SettlementState.STABLE;
            }
        } else {
            // Famine state
            state = SettlementState.THREATENED;
        }

        // 2. Resource Production (Simulated basic gatherers / farmers)
        stockpile.add(Stockpile.ResourceType.FOOD, Math.max(2, population / 2));
        stockpile.add(Stockpile.ResourceType.WOOD, 2);
        stockpile.add(Stockpile.ResourceType.STONE, 1);

        // 3. Autonomous Construction Evaluation
        if (activeProject == null && constructionQueue.isEmpty()) {
            evaluateAutonomousExpansion(level);
        }
    }

    private void evaluateAutonomousExpansion(ServerLevel level) {
        // 1. Expand housing if needed
        if (population >= housingCapacity - 2) {
            Blueprint bp = BlueprintRegistry.get("small_oak_house");
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject("small_oak_house", targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued small_oak_house at " + targetPos);
                }
            }
        }
        // 2. Expand agricultural food production
        else if (stockpile.get(Stockpile.ResourceType.FOOD) <= 50) {
            Blueprint bp = BlueprintRegistry.get("wheat_farm");
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject("wheat_farm", targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued wheat_farm at " + targetPos);
                }
            }
        }
    }

    public BlockPos findClearPlot(ServerLevel level, int width, int depth) {
        // Search in a spiral around settlement center with full footprint and water rejection
        Random random = new Random(id.getMostSignificantBits() + center.asLong());
        for (int attempts = 0; attempts < 32; attempts++) {
            int angleDeg = random.nextInt(360);
            double dist = 12 + random.nextInt(Math.max(16, radius - 15));
            int offsetX = (int) (Math.cos(Math.toRadians(angleDeg)) * dist);
            int offsetZ = (int) (Math.sin(Math.toRadians(angleDeg)) * dist);

            int testX = center.getX() + offsetX;
            int testZ = center.getZ() + offsetZ;

            if (!level.isLoaded(new BlockPos(testX, 64, testZ)) ||
                !level.isLoaded(new BlockPos(testX + width, 64, testZ + depth))) {
                continue;
            }

            boolean plotValid = true;
            int minElevation = Integer.MAX_VALUE;
            int maxElevation = Integer.MIN_VALUE;

            for (int dx = 0; dx < width && plotValid; dx++) {
                for (int dz = 0; dz < depth && plotValid; dz++) {
                    int colX = testX + dx;
                    int colZ = testZ + dz;
                    BlockPos probe = new BlockPos(colX, 64, colZ);

                    // Biome check: strictly reject ocean and river foundations
                    var biome = level.getBiome(probe);
                    if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) ||
                        biome.is(net.minecraft.tags.BiomeTags.IS_DEEP_OCEAN) ||
                        biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
                        plotValid = false;
                        break;
                    }

                    int colY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, colX, colZ);
                    if (colY < 62) {
                        plotValid = false;
                        break;
                    }

                    BlockPos groundPos = new BlockPos(colX, colY - 1, colZ);
                    var groundState = level.getBlockState(groundPos);
                    var groundFluid = level.getFluidState(groundPos);
                    var airFluid = level.getFluidState(groundPos.above());

                    // Ground must be solid and dry (no water/lava on or above foundation)
                    if (!groundState.isSolid() || !groundFluid.isEmpty() || !airFluid.isEmpty()) {
                        plotValid = false;
                        break;
                    }

                    if (colY < minElevation) minElevation = colY;
                    if (colY > maxElevation) maxElevation = colY;
                }
            }

            // Reject plots with extreme slope (> 4 block variance across footprint)
            if (plotValid && (maxElevation - minElevation <= 4)) {
                BlockPos plotOrigin = new BlockPos(testX, maxElevation, testZ);
                livingworld.command.LivingWorldCommand.fillFoundationColumns(level, plotOrigin, width, depth);
                return plotOrigin;
            }
        }
        return null;
    }

    public void tacticalTick(ServerLevel level, int maxBlocksPerTick) {
        // Progress active construction project
        if (activeProject != null) {
            if (livingworld.economy.EconomyManager.get().hasConstructionMaterials(this)) {
                activeProject.step(level, maxBlocksPerTick);
                if (activeProject.isComplete()) {
                    onProjectFinished(activeProject);
                    activeProject = null;
                }
            }
        } else if (!constructionQueue.isEmpty()) {
            activeProject = constructionQueue.poll();
        }
    }

    private void onProjectFinished(ConstructionProject project) {
        String bpId = project.getBlueprintId();
        if ("small_oak_house".equals(bpId)) {
            housingCapacity += 4;
        } else if ("wheat_farm".equals(bpId)) {
            stockpile.add(Stockpile.ResourceType.FOOD, 20);
        } else if ("watchtower".equals(bpId)) {
            guardCount += 2;
        }
    }

    public boolean isInSettlement(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
