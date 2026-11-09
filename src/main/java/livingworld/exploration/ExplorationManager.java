package livingworld.exploration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import livingworld.config.FeatureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Exploration, Mineshaft Overhaul, and Trial Chamber Clues (F16).
 */
public class ExplorationManager {
    private static final ExplorationManager INSTANCE = new ExplorationManager();
    private final Set<Long> generatedDepots = new HashSet<>();
    private long lastMineshaftCheck = 0;

    public static ExplorationManager get() {
        return INSTANCE;
    }

    public ItemStack createTrialGuideBook() {
        try {
            ItemStack book = new ItemStack(Items.BOOK);
            book.set(DataComponents.CUSTOM_NAME, Component.literal("§6Explorer's Field Journal: Trial Vaults"));
            List<Component> lore = List.of(
                Component.literal("§7A well-worn field journal containing vital intelligence:"),
                Component.literal("§e1. Drink an Ominous Bottle near Trial Spawners to awaken Ominous Trials."),
                Component.literal("§e2. Vanquish the hostile waves to secure an Ominous Trial Key."),
                Component.literal("§e3. Unlock the heavy Ominous Vaults to claim the legendary Mace Core!")
            );
            book.set(DataComponents.LORE, new ItemLore(lore));
            return book;
        } catch (Throwable t) {
            return null;
        }
    }

    public void enhanceWanderingTrader(WanderingTrader trader) {
        if (!FeatureConfig.get().enableExplorationClues) {
            return;
        }

        MerchantOffers offers = trader.getOffers();
        // 1. Physical Trial Guide Journal
        ItemStack guideBook = createTrialGuideBook();
        if (guideBook != null) {
            offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                guideBook,
                4, 2, 0.05f
            ));
        }

        // 2. Exploration Keys & Ominous Draughts
        offers.add(new MerchantOffer(
            new ItemCost(Items.EMERALD, 4),
            new ItemStack(Items.TRIAL_KEY, 1),
            3, 5, 0.05f
        ));

        offers.add(new MerchantOffer(
            new ItemCost(Items.EMERALD, 6),
            new ItemStack(Items.OMINOUS_BOTTLE, 1),
            2, 8, 0.05f
        ));

        offers.add(new MerchantOffer(
            new ItemCost(Items.EMERALD, 3),
            new ItemStack(Items.POWERED_RAIL, 8),
            5, 4, 0.05f
        ));
    }

    public void checkMineshaftEncounters(ServerLevel level, ServerPlayer player, long currentTicks) {
        if (!FeatureConfig.get().enableExplorationClues) {
            return;
        }

        if (currentTicks - lastMineshaftCheck < 400) { // Check every 20 seconds
            return;
        }
        lastMineshaftCheck = currentTicks;

        BlockPos pos = player.blockPosition();
        if (pos.getY() > 40) return; // Mineshafts are underground

        // Detect abandoned mineshaft infrastructure
        boolean inMineshaft = level.getBlockState(pos).is(Blocks.RAIL)
            || level.getBlockState(pos.below()).is(Blocks.RAIL)
            || level.getBlockState(pos).is(Blocks.COBWEB)
            || level.getBlockState(pos.north()).is(Blocks.OAK_FENCE);

        long chunkCoord = player.chunkPosition().pack();
        if (inMineshaft && !generatedDepots.contains(chunkCoord) && level.getRandom().nextInt(2) == 0) {
            generatedDepots.add(chunkCoord);
            generateExcavationDepot(level, pos, player);
        }
    }

    public void generateExcavationDepot(ServerLevel level, BlockPos origin, ServerPlayer player) {
        BlockPos depotPos = origin.offset(2, 0, 2);

        // 1. Carve 4x3x4 alcove and place wooden track platform
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                BlockPos floor = depotPos.offset(dx, -1, dz);
                level.setBlock(floor, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos air = depotPos.offset(dx, dy, dz);
                    level.setBlock(air, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // 2. Place Powered Rail track and Lantern
        level.setBlock(depotPos, Blocks.POWERED_RAIL.defaultBlockState(), 3);
        level.setBlock(depotPos.above(2), Blocks.LANTERN.defaultBlockState(), 3);

        // 3. Place Reinforced Miner Chest
        BlockPos chestPos = depotPos.offset(1, 0, 0);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            try {
                chest.setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
                chest.setItem(1, new ItemStack(Items.POWERED_RAIL, 16));
                chest.setItem(2, new ItemStack(Items.REDSTONE_BLOCK, 2));
                chest.setItem(3, new ItemStack(Items.TRIAL_KEY, 1));
                chest.setItem(4, new ItemStack(Items.GOLDEN_APPLE, 1));
            } catch (Exception ignored) {
                // Headless test fallback
            }
        }

        // 4. Spawn Cave Miner Skeleton Guardian
        Skeleton minerSkeleton = EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
        if (minerSkeleton != null) {
            minerSkeleton.setPos(depotPos.getX() + 0.5, depotPos.getY(), depotPos.getZ() + 0.5);
            minerSkeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            minerSkeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            minerSkeleton.setCustomName(Component.literal("§7Forgotten Miner"));
            minerSkeleton.setCustomNameVisible(true);
            level.addFreshEntity(minerSkeleton);
        }

        if (player != null) {
            player.sendSystemMessage(Component.literal("§6[Living World] §eYou uncovered an Abandoned Mineshaft Excavation Depot with rare supplies!"));
        }
        livingworld.LivingWorldMod.LOGGER.info("[Living World] Mineshaft: Excavation Depot generated at " + depotPos + " with reinforced chest and Forgotten Miner guardian!");
    }
}
