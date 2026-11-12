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
    private final List<BlockPos> watchtowers = new ArrayList<>();
    private final List<RegisteredBuilding> buildings = new ArrayList<>();
    private ConstructionProject activeProject = null;

    public static class RegisteredBuilding {
        private final String blueprintId;
        private final BlockPos pos;
        private boolean completed;
        private int bedCount;

        public RegisteredBuilding(String blueprintId, BlockPos pos, boolean completed, int bedCount) {
            this.blueprintId = blueprintId;
            this.pos = pos != null ? pos.immutable() : BlockPos.ZERO;
            this.completed = completed;
            this.bedCount = bedCount;
        }

        public String getBlueprintId() { return blueprintId; }
        public BlockPos getPos() { return pos; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public int getBedCount() { return bedCount; }
        public void setBedCount(int bedCount) { this.bedCount = bedCount; }
    }

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
        addBuilding(project.getBlueprintId(), project.getOrigin(), false, 0);
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
        Random rand = new Random(id.getMostSignificantBits() ^ level.getGameTime());

        // 1. Expand housing if needed (diverse residential blueprints)
        if (population >= housingCapacity - 2) {
            String[] residentialOptions = {"small_oak_house", "two_story_house", "farmhouse", "inn_guesthouse", "bakery"};
            String chosen = residentialOptions[rand.nextInt(residentialOptions.length)];
            Blueprint bp = BlueprintRegistry.get(chosen);
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject(chosen, targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued residential " + chosen + " at " + targetPos);
                    return;
                }
            }
        }
        // 2. Expand agricultural food production
        else if (stockpile.get(Stockpile.ResourceType.FOOD) <= 60) {
            String[] foodOptions = {"wheat_farm", "livestock_pen", "barn", "bakery"};
            String chosen = foodOptions[rand.nextInt(foodOptions.length)];
            Blueprint bp = BlueprintRegistry.get(chosen);
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject(chosen, targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued food infrastructure " + chosen + " at " + targetPos);
                    return;
                }
            }
        }
        // 3. Expand defensive military infrastructure
        else if (guardCount < 4) {
            String[] militaryOptions = {"archer_watchtower", "armory", "barracks", "training_yard"};
            String chosen = militaryOptions[rand.nextInt(militaryOptions.length)];
            Blueprint bp = BlueprintRegistry.get(chosen);
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject(chosen, targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued defense structure " + chosen + " at " + targetPos);
                    return;
                }
            }
        }
        // 4. Civic & Industrial Workshops
        else {
            String[] civicOptions = {"workshop", "mason_workshop", "windmill", "library", "communal_hall", "cleric_clinic"};
            String chosen = civicOptions[rand.nextInt(civicOptions.length)];
            Blueprint bp = BlueprintRegistry.get(chosen);
            if (bp != null && stockpile.hasAll(bp.getResourceCost())) {
                BlockPos targetPos = findClearPlot(level, bp.getWidth(), bp.getDepth());
                if (targetPos != null) {
                    stockpile.consumeAll(bp.getResourceCost());
                    queueProject(new ConstructionProject(chosen, targetPos));
                    livingworld.LivingWorldMod.LOGGER.info("[Living World] Autonomous expansion: Village " + name + " queued civic workshop " + chosen + " at " + targetPos);
                }
            }
        }
    }

    public boolean hasStructureOverlap(BlockPos origin, int width, int height, int depth) {
        if (origin == null) return false;
        int minX = origin.getX() - 2;
        int maxX = origin.getX() + width + 2;
        int minY = origin.getY() - 1;
        int maxY = origin.getY() + height + 2;
        int minZ = origin.getZ() - 2;
        int maxZ = origin.getZ() + depth + 2;

        // Check against existing registered buildings
        for (RegisteredBuilding b : buildings) {
            Blueprint bp = BlueprintRegistry.get(b.getBlueprintId());
            int bWidth = bp != null ? bp.getWidth() : 8;
            int bHeight = bp != null ? bp.getHeight() : 10;
            int bDepth = bp != null ? bp.getDepth() : 8;
            BlockPos bPos = b.getPos();

            int bMinX = bPos.getX();
            int bMaxX = bPos.getX() + bWidth;
            int bMinY = bPos.getY();
            int bMaxY = bPos.getY() + bHeight;
            int bMinZ = bPos.getZ();
            int bMaxZ = bPos.getZ() + bDepth;

            boolean overlapX = minX <= bMaxX && maxX >= bMinX;
            boolean overlapY = minY <= bMaxY && maxY >= bMinY;
            boolean overlapZ = minZ <= bMaxZ && maxZ >= bMinZ;

            if (overlapX && overlapY && overlapZ) {
                return true; // Overlaps an existing building!
            }
        }

        // Check against active construction project
        if (activeProject != null) {
            Blueprint bp = BlueprintRegistry.get(activeProject.getBlueprintId());
            int pWidth = bp != null ? bp.getWidth() : 8;
            int pHeight = bp != null ? bp.getHeight() : 10;
            int pDepth = bp != null ? bp.getDepth() : 8;
            BlockPos pPos = activeProject.getOrigin();

            if (minX <= pPos.getX() + pWidth && maxX >= pPos.getX() &&
                minY <= pPos.getY() + pHeight && maxY >= pPos.getY() &&
                minZ <= pPos.getZ() + pDepth && maxZ >= pPos.getZ()) {
                return true;
            }
        }

        // Check against queued construction projects
        for (ConstructionProject q : constructionQueue) {
            Blueprint bp = BlueprintRegistry.get(q.getBlueprintId());
            int pWidth = bp != null ? bp.getWidth() : 8;
            int pHeight = bp != null ? bp.getHeight() : 10;
            int pDepth = bp != null ? bp.getDepth() : 8;
            BlockPos pPos = q.getOrigin();

            if (minX <= pPos.getX() + pWidth && maxX >= pPos.getX() &&
                minY <= pPos.getY() + pHeight && maxY >= pPos.getY() &&
                minZ <= pPos.getZ() + pDepth && maxZ >= pPos.getZ()) {
                return true;
            }
        }

        // Check against watchtowers
        for (BlockPos wt : watchtowers) {
            if (minX <= wt.getX() + 8 && maxX >= wt.getX() &&
                minY <= wt.getY() + 16 && maxY >= wt.getY() &&
                minZ <= wt.getZ() + 8 && maxZ >= wt.getZ()) {
                return true;
            }
        }

        return false;
    }

    public BlockPos findClearPlot(ServerLevel level, int width, int depth) {
        // Search in a spiral around settlement center with 3D reservation check and water/roof rejection
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

                    // Ground must be solid, dry, and natural terrain (NEVER an existing building's roof!)
                    if (!groundState.isSolid() || !groundFluid.isEmpty() || !airFluid.isEmpty()) {
                        plotValid = false;
                        break;
                    }

                    // Anti-Stacking Invariant: Reject if ground block is building material (planks, bricks, slabs, stairs)
                    if (groundState.is(net.minecraft.world.level.block.Blocks.OAK_PLANKS) ||
                        groundState.is(net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS) ||
                        groundState.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS) ||
                        groundState.is(net.minecraft.world.level.block.Blocks.COBBLESTONE) ||
                        groundState.is(net.minecraft.world.level.block.Blocks.OAK_SLAB) ||
                        groundState.is(net.minecraft.world.level.block.Blocks.BRICKS)) {
                        plotValid = false; // Roof of an existing building!
                        break;
                    }

                    // Must have clear sky above ground before construction
                    if (!level.canSeeSky(groundPos.above())) {
                        plotValid = false;
                        break;
                    }

                    if (colY < minElevation) minElevation = colY;
                    if (colY > maxElevation) maxElevation = colY;
                }
            }

            // Reject plots with extreme slope (> 4 block variance across footprint) or isolated deep ravines (> 6 blocks below center)
            if (plotValid && (maxElevation - minElevation <= 4)) {
                if (maxElevation < center.getY() - 6) {
                    continue; // Plot is down in a surface ravine or chasm below the settlement plateau!
                }
                BlockPos plotOrigin = new BlockPos(testX, maxElevation, testZ);
                // 3D Bounding Box Collision Check: reject if overlapping any existing building or watchtower
                if (hasStructureOverlap(plotOrigin, width, 12, depth)) {
                    continue; // Plot overlaps an existing registered building reservation!
                }
                livingworld.worldgen.SettlementTerrainStabilizer.get().stabilizeExpansionPlot(level, plotOrigin, width, depth);
                livingworld.command.LivingWorldCommand.fillFoundationColumns(level, plotOrigin, width, depth);
                return plotOrigin;
            }
        }
        return null;
    }

    public void tacticalTick(ServerLevel level, int maxBlocksPerTick) {
        // Progress active construction project (prepaid upon queue registration)
        if (activeProject != null) {
            activeProject.step(level, maxBlocksPerTick);
            if (activeProject.isComplete()) {
                onProjectFinished(level, activeProject);
                activeProject = null;
            }
        } else if (!constructionQueue.isEmpty()) {
            activeProject = constructionQueue.poll();
        }
    }

    private void onProjectFinished(ServerLevel level, ConstructionProject project) {
        String bpId = project.getBlueprintId();
        Blueprint bp = BlueprintRegistry.get(bpId);
        int beds = bp != null ? bp.getBedCount() : 0;
        if (bp != null) {
            if (beds > 0) {
                housingCapacity += beds * 2; // Each valid bed accommodates demographic expansion
            }
            if ("wheat_farm".equals(bpId) || "bakery".equals(bpId) || "barn".equals(bpId)) {
                stockpile.add(Stockpile.ResourceType.FOOD, 25);
            }
            if ("livestock_pen".equals(bpId) || "barn".equals(bpId)) {
                livingworld.worldgen.EnclosurePopulationManager.get().checkAndPopulateLivestockPen(level, project.getOrigin().offset(5, 1, 5));
            } else if ("watchtower".equals(bpId) || "archer_watchtower".equals(bpId)) {
                guardCount += 2;
                addWatchtower(project.getOrigin());
            } else if ("armory".equals(bpId)) {
                stockpile.add(Stockpile.ResourceType.IRON, 15);
            }
        }
        addBuilding(bpId, project.getOrigin(), true, beds);
    }

    public List<RegisteredBuilding> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }

    public void addBuilding(String blueprintId, BlockPos pos, boolean completed, int bedCount) {
        for (RegisteredBuilding b : buildings) {
            if (b.getPos().equals(pos) && b.getBlueprintId().equals(blueprintId)) {
                b.setCompleted(completed);
                b.setBedCount(bedCount);
                return;
            }
        }
        buildings.add(new RegisteredBuilding(blueprintId, pos, completed, bedCount));
    }

    public List<BlockPos> getWatchtowers() {
        return Collections.unmodifiableList(watchtowers);
    }

    public void addWatchtower(BlockPos pos) {
        if (pos != null && !watchtowers.contains(pos)) {
            watchtowers.add(pos);
        }
    }

    public boolean isInSettlement(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }
}
