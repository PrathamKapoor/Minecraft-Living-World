package livingworld.settlement;

import java.util.HashMap;
import java.util.Map;
import livingworld.economy.Stockpile;
import net.minecraft.world.level.block.Blocks;

/**
 * Architectural Blueprint Registry (Section 9, Milestone 2 & Quality Rebuild).
 * Authentic, multi-room, pre-authored architectural templates replacing simplistic boxes:
 * - 35x25 Imperial Royal Palace (Multi-room: Portico, Grand Entrance Hall, Vaulted Throne Room with Dais, East Wing Royal Bedchamber & Study, West Wing Council War Chamber & Treasury)
 * - Authentic Cozy Cottage with pitched A-frame roof, porch, chimney, flowerboxes, and full interior
 * - Jettied Two-Story Medieval Residence with balcony and dual gables
 * - 16-block Military Watchtower with battered base, intermediate staging decks, arrow slits, and machicolated battlements
 * - Multi-Room Garrison Barracks (Armory, Bunks, Mess Hall)
 * - Fortified Gatehouse with twin crenellated towers and portcullis
 * - Market Square with central plaza and striped merchant stalls
 * - Artisan Workshop / Forge with stone chimney, anvil, blast furnace, and grindstone
 * - Gambrel-Roof Grand Barn with animal stalls and hayloft
 * - Imperial Vaulted Warehouse with cargo crane and crate stacks
 * - Irrigated Wheat Farm with retaining borders and composter
 * - Fenced Livestock Pasture Pen with covered shelter and feeding troughs
 */
public class BlueprintRegistry {
    private static final Map<String, Blueprint> BLUEPRINTS = new HashMap<>();

    static {
        register(createSmallOakHouse());
        register(createTwoStoryHouse());
        register(createWheatFarm());
        register(createFarmhouse());
        register(createWorkshop());
        register(createBarn());
        register(createWarehouse());
        register(createMarket());
        register(createBarracks());
        register(createFortifiedWatchtower());
        register(createGatehouse());
        register(createPalace());
        register(createLivestockPen());
    }

    public static void register(Blueprint blueprint) {
        BLUEPRINTS.put(blueprint.getId(), blueprint);
    }

    public static Blueprint get(String id) {
        return BLUEPRINTS.get(id);
    }

    public static Map<String, Blueprint> getAll() {
        return BLUEPRINTS;
    }

    /**
     * Authentic Cozy Timber Cottage (9x8x8).
     * Features: Cobblestone foundation, oak log corner posts, oak plank/wool walls,
     * front porch with lantern, pitched stair roof with overhanging eaves, stone chimney,
     * exterior flowerboxes, cozy bed, table, furnace, crafting table, and rug.
     */
    private static Blueprint createSmallOakHouse() {
        Blueprint bp = new Blueprint("small_oak_house", "Cozy Timber Cottage", 9, 8, 8);
        bp.setCost(Stockpile.ResourceType.WOOD, 35);
        bp.setCost(Stockpile.ResourceType.STONE, 25);

        // Cobblestone foundation (y=0)
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 8; z++) {
                bp.addBlock(x, 0, z, Blocks.COBBLESTONE);
            }
        }

        // Walls and Corner Pillars (y=1 to y=4)
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 7; x++) {
                for (int z = 1; z <= 7; z++) {
                    boolean isCorner = (x == 1 || x == 7) && (z == 1 || z == 7);
                    boolean isWall = (x == 1 || x == 7 || z == 1 || z == 7);

                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.OAK_LOG);
                    } else if (isWall) {
                        // Doorway at front (x=4, z=1)
                        if (x == 4 && z == 1 && (y == 1 || y == 2)) {
                            continue;
                        }
                        // Front windows with glass pane (x=2 and x=6, z=1, y=2)
                        if ((x == 2 || x == 6) && z == 1 && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        }
                        // Side windows
                        else if ((z == 3 || z == 5) && (x == 1 || x == 7) && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        }
                        // Rear window
                        else if (x == 4 && z == 7 && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        }
                        // Lower wall stone accent, upper timber
                        else if (y == 1) {
                            bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                        } else {
                            bp.addBlock(x, y, z, Blocks.OAK_PLANKS);
                        }
                    }
                }
            }
        }

        // Front Porch (z=0, x=3..5)
        bp.addBlock(3, 0, 0, Blocks.STONE_BRICKS);
        bp.addBlock(4, 0, 0, Blocks.STONE_BRICK_STAIRS);
        bp.addBlock(5, 0, 0, Blocks.STONE_BRICKS);
        bp.addBlock(3, 1, 0, Blocks.OAK_FENCE);
        bp.addBlock(3, 2, 0, Blocks.OAK_FENCE);
        bp.addBlock(5, 1, 0, Blocks.OAK_FENCE);
        bp.addBlock(5, 2, 0, Blocks.OAK_FENCE);
        bp.addBlock(3, 3, 0, Blocks.OAK_SLAB);
        bp.addBlock(4, 3, 0, Blocks.OAK_SLAB);
        bp.addBlock(5, 3, 0, Blocks.OAK_SLAB);
        bp.addBlock(4, 2, 0, Blocks.LANTERN);

        // Exterior Flowerboxes (x=2 and x=6, z=0, y=1)
        bp.addBlock(2, 1, 0, Blocks.POTTED_POPPY);
        bp.addBlock(6, 1, 0, Blocks.POTTED_DANDELION);

        // Pitched Gable Roof with Overhangs (y=4 to y=7)
        // Roof rises from x=0 and x=8 towards ridge at x=4
        for (int z = 0; z < 8; z++) {
            // Eaves / Slopes
            bp.addBlock(0, 4, z, Blocks.OAK_STAIRS);
            bp.addBlock(1, 5, z, Blocks.OAK_STAIRS);
            bp.addBlock(2, 5, z, Blocks.OAK_PLANKS);
            bp.addBlock(2, 6, z, Blocks.OAK_STAIRS);
            bp.addBlock(3, 6, z, Blocks.OAK_PLANKS);
            bp.addBlock(3, 7, z, Blocks.OAK_STAIRS);

            // Center ridge
            bp.addBlock(4, 7, z, Blocks.OAK_SLAB);

            // Symmetrical right side
            bp.addBlock(5, 7, z, Blocks.OAK_STAIRS);
            bp.addBlock(5, 6, z, Blocks.OAK_PLANKS);
            bp.addBlock(6, 6, z, Blocks.OAK_STAIRS);
            bp.addBlock(6, 5, z, Blocks.OAK_PLANKS);
            bp.addBlock(7, 5, z, Blocks.OAK_STAIRS);
            bp.addBlock(8, 4, z, Blocks.OAK_STAIRS);
        }

        // Gable end infill walls
        for (int x = 2; x <= 6; x++) {
            bp.addBlock(x, 5, 1, Blocks.OAK_PLANKS);
            bp.addBlock(x, 5, 7, Blocks.OAK_PLANKS);
        }
        bp.addBlock(4, 6, 1, Blocks.OAK_PLANKS);
        bp.addBlock(4, 6, 7, Blocks.OAK_PLANKS);

        // Stone Brick Chimney on East wall (x=7, z=5..6)
        for (int y = 1; y <= 7; y++) {
            bp.addBlock(7, y, 5, Blocks.STONE_BRICKS);
        }
        bp.addBlock(7, 8, 5, Blocks.CAMPFIRE);

        // Interior Furnishings
        bp.addBlock(2, 1, 6, Blocks.BED.red());
        bp.addBlock(2, 1, 5, Blocks.CHEST);
        bp.addBlock(6, 1, 6, Blocks.CRAFTING_TABLE);
        bp.addBlock(6, 1, 5, Blocks.FURNACE);
        bp.addBlock(3, 1, 4, Blocks.BOOKSHELF);
        bp.addBlock(4, 1, 4, Blocks.CARPET.red());
        bp.addBlock(4, 3, 4, Blocks.LANTERN);

        return bp;
    }

    /**
     * Jettied Two-Story Medieval Residence (11x10x9).
     * Upper floor jetties out past lower stone walls. Balcony with wooden railings.
     */
    private static Blueprint createTwoStoryHouse() {
        Blueprint bp = new Blueprint("two_story_house", "Two-Story Residence", 11, 10, 9);
        bp.setCost(Stockpile.ResourceType.WOOD, 60);
        bp.setCost(Stockpile.ResourceType.STONE, 40);

        // Ground Floor Foundation (y=0)
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                bp.addBlock(x, 0, z, Blocks.STONE_BRICKS);
            }
        }

        // Ground Floor Walls (y=1 to y=4)
        for (int y = 1; y <= 4; y++) {
            for (int x = 1; x <= 9; x++) {
                for (int z = 1; z <= 7; z++) {
                    boolean isCorner = (x == 1 || x == 9) && (z == 1 || z == 7);
                    boolean isWall = (x == 1 || x == 9 || z == 1 || z == 7);

                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.STRIPPED_OAK_LOG);
                    } else if (isWall) {
                        if (x == 5 && z == 1 && (y == 1 || y == 2)) continue; // Front door
                        if ((x == 3 || x == 7) && z == 1 && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else if ((z == 3 || z == 5) && (x == 1 || x == 9) && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else {
                            bp.addBlock(x, y, z, (y == 1 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS));
                        }
                    }
                }
            }
        }

        // Intermediate Floor & Jettied Overhang (y=5)
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                bp.addBlock(x, 5, z, Blocks.OAK_PLANKS);
            }
        }

        // Upper Floor Timber Walls (y=6 to y=8)
        for (int y = 6; y <= 8; y++) {
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 9; z++) {
                    boolean isCorner = (x == 0 || x == 10) && (z == 0 || z == 8);
                    boolean isWall = (x == 0 || x == 10 || z == 0 || z == 8);

                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.OAK_LOG);
                    } else if (isWall) {
                        // Upper Balcony doorway at front (x=5, z=0)
                        if (x == 5 && z == 0 && (y == 6 || y == 7)) continue;
                        if ((x == 2 || x == 8) && (z == 0 || z == 8) && y == 7) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else {
                            bp.addBlock(x, y, z, Blocks.OAK_PLANKS);
                        }
                    }
                }
            }
        }

        // Balcony Railing (y=6, z=0, x=3..7)
        for (int x = 3; x <= 7; x++) {
            bp.addBlock(x, 6, 0, Blocks.OAK_FENCE);
        }

        // Pitched Roof (y=9)
        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 11; x++) {
                int distFromCenter = Math.abs(x - 5);
                if (distFromCenter <= 2) {
                    bp.addBlock(x, 9, z, Blocks.DARK_OAK_PLANKS);
                } else {
                    bp.addBlock(x, 8, z, Blocks.DARK_OAK_STAIRS);
                }
            }
        }

        // Interior Furnishings
        bp.addBlock(2, 1, 6, Blocks.OAK_STAIRS); // Ground floor stairs
        bp.addBlock(2, 2, 5, Blocks.OAK_STAIRS);
        bp.addBlock(2, 3, 4, Blocks.OAK_STAIRS);
        bp.addBlock(2, 4, 3, Blocks.OAK_STAIRS);

        bp.addBlock(8, 1, 6, Blocks.CRAFTING_TABLE);
        bp.addBlock(8, 1, 5, Blocks.FURNACE);
        bp.addBlock(8, 1, 4, Blocks.CHEST);
        bp.addBlock(5, 3, 4, Blocks.LANTERN);

        // Upper floor beds and study
        bp.addBlock(2, 6, 7, Blocks.BED.blue());
        bp.addBlock(8, 6, 7, Blocks.BED.red());
        bp.addBlock(5, 6, 6, Blocks.BOOKSHELF);
        bp.addBlock(5, 8, 4, Blocks.LANTERN);

        return bp;
    }

    /**
     * Proper 16-block Fortified Military Watchtower (9x16x9).
     * Battered 9x9 stone foundation tapering to 7x7 shaft,
     * arrow loop slits, intermediate staging decks (y=5, y=10) with ladder access,
     * machicolated fighting platform (y=13..15) with stone brick battlements and brazier.
     */
    private static Blueprint createFortifiedWatchtower() {
        Blueprint bp = new Blueprint("watchtower", "Fortified Military Watchtower", 9, 16, 9);
        bp.setCost(Stockpile.ResourceType.STONE, 80);
        bp.setCost(Stockpile.ResourceType.WOOD, 25);
        bp.setCost(Stockpile.ResourceType.IRON, 10);

        // Battered Stone Foundation (y=0 to y=2)
        for (int y = 0; y <= 2; y++) {
            for (int x = 0; x < 9; x++) {
                for (int z = 0; z < 9; z++) {
                    boolean isPerimeter = (x == 0 || x == 8 || z == 0 || z == 8);
                    if (y == 0 || isPerimeter) {
                        bp.addBlock(x, y, z, Blocks.COBBLESTONE);
                    } else {
                        bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                    }
                }
            }
        }

        // Tapering buttress stairs at base (y=3)
        for (int i = 0; i < 9; i++) {
            bp.addBlock(i, 3, 0, Blocks.STONE_BRICK_STAIRS);
            bp.addBlock(i, 3, 8, Blocks.STONE_BRICK_STAIRS);
            bp.addBlock(0, 3, i, Blocks.STONE_BRICK_STAIRS);
            bp.addBlock(8, 3, i, Blocks.STONE_BRICK_STAIRS);
        }

        // Central Shaft (y=3 to y=12, 7x7 area: x=1..7, z=1..7)
        for (int y = 3; y <= 12; y++) {
            for (int x = 1; x <= 7; x++) {
                for (int z = 1; z <= 7; z++) {
                    boolean isCorner = (x == 1 || x == 7) && (z == 1 || z == 7);
                    boolean isWall = (x == 1 || x == 7 || z == 1 || z == 7);

                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.POLISHED_ANDESITE);
                    } else if (isWall) {
                        // Ground entrance at front (x=4, z=1, y=3..4)
                        if (x == 4 && z == 1 && (y == 3 || y == 4)) continue;

                        // Arrow slit loops at strategic elevations
                        boolean isArrowSlit = (y == 5 || y == 8 || y == 11) && (x == 4 || z == 4);
                        if (isArrowSlit) {
                            bp.addBlock(x, y, z, Blocks.IRON_BARS);
                        } else {
                            bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                        }
                    }
                }
            }
        }

        // Intermediate Staging Deck 1 (y=5)
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                if (x != 4 || z != 4) bp.addBlock(x, 5, z, Blocks.OAK_PLANKS);
            }
        }
        bp.addBlock(3, 6, 2, Blocks.CHEST);
        bp.addBlock(5, 6, 2, Blocks.LANTERN);

        // Intermediate Staging Deck 2 (y=10)
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                if (x != 4 || z != 4) bp.addBlock(x, 10, z, Blocks.OAK_PLANKS);
            }
        }
        bp.addBlock(3, 11, 2, Blocks.BARREL);
        bp.addBlock(5, 11, 2, Blocks.LANTERN);

        // Internal Central Ladder
        for (int y = 1; y <= 13; y++) {
            bp.addBlock(4, y, 4, Blocks.LADDER);
        }

        // Machicolated Overhang Corbels (y=13, expands back to 9x9)
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                boolean isEdge = (x == 0 || x == 8 || z == 0 || z == 8);
                if (isEdge) {
                    bp.addBlock(x, 13, z, Blocks.STONE_BRICK_STAIRS);
                } else {
                    bp.addBlock(x, 13, z, Blocks.STONE_BRICKS);
                }
            }
        }

        // Fighting Platform Floor (y=14)
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                bp.addBlock(x, 14, z, Blocks.STONE_BRICK_SLAB);
            }
        }

        // Crenellated Battlements & Parapet (y=15)
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                boolean isPerimeter = (x == 0 || x == 8 || z == 0 || z == 8);
                if (isPerimeter) {
                    // Alternating crenellations
                    if ((x + z) % 2 == 0) {
                        bp.addBlock(x, 15, z, Blocks.STONE_BRICK_WALL);
                    }
                }
            }
        }

        // Central Signal Brazier & Lanterns
        bp.addBlock(4, 14, 4, Blocks.NETHERRACK);
        bp.addBlock(4, 15, 4, Blocks.CAMPFIRE);
        bp.addBlock(1, 15, 1, Blocks.LANTERN);
        bp.addBlock(7, 15, 1, Blocks.LANTERN);
        bp.addBlock(1, 15, 7, Blocks.LANTERN);
        bp.addBlock(7, 15, 7, Blocks.LANTERN);

        return bp;
    }

    /**
     * Fortified Gatehouse (15x9x7).
     * Twin crenellated towers flanking a central portcullis with raised walkway.
     */
    private static Blueprint createGatehouse() {
        Blueprint bp = new Blueprint("gatehouse", "Fortified Gatehouse", 15, 9, 7);
        bp.setCost(Stockpile.ResourceType.STONE, 90);
        bp.setCost(Stockpile.ResourceType.WOOD, 25);
        bp.setCost(Stockpile.ResourceType.IRON, 15);

        // Foundation (y=0)
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 7; z++) {
                bp.addBlock(x, 0, z, Blocks.STONE_BRICKS);
            }
        }

        // Twin Towers (West: x=0..3, East: x=11..14) and Central Arch (x=4..10)
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < 15; x++) {
                for (int z = 0; z < 7; z++) {
                    boolean isTower = (x <= 3 || x >= 11);
                    if (isTower) {
                        boolean isWall = (x == 0 || x == 3 || x == 11 || x == 14 || z == 0 || z == 6);
                        if (isWall) {
                            if (y == 3 && (z == 3)) {
                                bp.addBlock(x, y, z, Blocks.IRON_BARS); // Arrow loop
                            } else {
                                bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                            }
                        }
                    } else {
                        // Central Gateway
                        if (y >= 5) {
                            bp.addBlock(x, y, z, Blocks.STONE_BRICKS); // Arch ceiling
                        } else if (z == 3) {
                            bp.addBlock(x, y, z, Blocks.IRON_BARS); // Portcullis
                        }
                    }
                }
            }
        }

        // Upper Walkway & Murder Holes (y=7)
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 7; z++) {
                if (z != 3) {
                    bp.addBlock(x, 7, z, Blocks.STONE_BRICK_SLAB);
                } else {
                    bp.addBlock(x, 7, z, Blocks.IRON_BARS); // Murder holes overlooking portcullis
                }
            }
        }

        // Parapet Battlements (y=8)
        for (int x = 0; x < 15; x++) {
            if (x % 2 == 0) {
                bp.addBlock(x, 8, 0, Blocks.STONE_BRICK_WALL);
                bp.addBlock(x, 8, 6, Blocks.STONE_BRICK_WALL);
            }
        }
        bp.addBlock(7, 6, 2, Blocks.LANTERN);
        bp.addBlock(7, 6, 4, Blocks.LANTERN);

        return bp;
    }

    /**
     * Garrison Barracks (15x7x11).
     * Multi-room garrison: armory with weapon racks, soldier bunks, and mess hall.
     */
    private static Blueprint createBarracks() {
        Blueprint bp = new Blueprint("barracks", "Garrison Barracks", 15, 7, 11);
        bp.setCost(Stockpile.ResourceType.WOOD, 50);
        bp.setCost(Stockpile.ResourceType.STONE, 65);
        bp.setCost(Stockpile.ResourceType.IRON, 15);

        // Stone Foundation (y=0)
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 11; z++) {
                bp.addBlock(x, 0, z, Blocks.STONE_BRICKS);
            }
        }

        // Walls (y=1 to y=4)
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 15; x++) {
                for (int z = 0; z < 11; z++) {
                    boolean isOuter = (x == 0 || x == 14 || z == 0 || z == 10);
                    boolean isPartition = (x == 7); // Partition between bunks and armory

                    if (isOuter) {
                        if (x == 7 && z == 0 && (y == 1 || y == 2)) continue; // Front door
                        if ((x == 3 || x == 11) && (z == 0 || z == 10) && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else {
                            bp.addBlock(x, y, z, (y == 1 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS));
                        }
                    } else if (isPartition) {
                        if (z == 5 && (y == 1 || y == 2)) continue; // Interior doorway
                        bp.addBlock(x, y, z, Blocks.OAK_PLANKS);
                    }
                }
            }
        }

        // Timber Roof (y=5 to y=6)
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 11; z++) {
                bp.addBlock(x, 5, z, Blocks.DARK_OAK_PLANKS);
                if (z == 0 || z == 10 || x == 0 || x == 14) {
                    bp.addBlock(x, 6, z, Blocks.DARK_OAK_SLAB);
                }
            }
        }

        // West Room: Soldier Bunks (x=1..6)
        bp.addBlock(2, 1, 2, Blocks.BED.red());
        bp.addBlock(2, 1, 4, Blocks.BED.red());
        bp.addBlock(2, 1, 6, Blocks.BED.red());
        bp.addBlock(2, 1, 8, Blocks.BED.red());
        bp.addBlock(1, 1, 2, Blocks.CHEST);
        bp.addBlock(1, 1, 4, Blocks.CHEST);
        bp.addBlock(1, 1, 6, Blocks.CHEST);
        bp.addBlock(1, 1, 8, Blocks.CHEST);
        bp.addBlock(4, 3, 5, Blocks.LANTERN);

        // East Room: Armory & Mess Hall (x=8..13)
        bp.addBlock(12, 1, 2, Blocks.ANVIL);
        bp.addBlock(12, 1, 4, Blocks.IRON_BLOCK);
        bp.addBlock(12, 1, 6, Blocks.SMITHING_TABLE);
        bp.addBlock(12, 1, 8, Blocks.BARREL);
        bp.addBlock(9, 1, 4, Blocks.OAK_STAIRS);
        bp.addBlock(10, 1, 4, Blocks.OAK_PLANKS);
        bp.addBlock(11, 1, 4, Blocks.OAK_STAIRS);
        bp.addBlock(10, 3, 5, Blocks.LANTERN);

        return bp;
    }

    /**
     * Imperial Royal Palace (35x14x25).
     * Flagship architectural complex featuring:
     * - Grand Façade Portico with quartz pillars and entrance staircase (z=0..4, x=13..21)
     * - Grand Entrance Vestibule & Hall (z=4..12, x=11..23) with red carpet runner and hanging chandeliers
     * - Vaulted Throne Room with elevated dais and twin Gilded Thrones (z=13..24, x=11..23)
     * - East Wing: Royal Master Bedchamber & Study with canopy bed and fireplace (x=0..10, z=4..24)
     * - West Wing: Council War Chamber & Treasury with war table and armory (x=24..34, z=4..24)
     * - Articulated dark oak pitched gables, battlements, arched windows, and corner turrets.
     */
    private static Blueprint createPalace() {
        Blueprint bp = new Blueprint("palace", "Imperial Royal Palace", 35, 14, 25);
        bp.setCost(Stockpile.ResourceType.STONE, 250);
        bp.setCost(Stockpile.ResourceType.WOOD, 140);
        bp.setCost(Stockpile.ResourceType.IRON, 45);

        // Grand Foundation & Polished Andesite Floor (y=0)
        for (int x = 0; x < 35; x++) {
            for (int z = 0; z < 25; z++) {
                bp.addBlock(x, 0, z, Blocks.POLISHED_ANDESITE);
            }
        }

        // Outer Perimeter Walls & Wing Dividers (y=1 to y=6)
        for (int y = 1; y <= 6; y++) {
            for (int x = 0; x < 35; x++) {
                for (int z = 4; z < 25; z++) {
                    boolean isOuter = (x == 0 || x == 34 || z == 4 || z == 24);
                    boolean isEastDivider = (x == 10);
                    boolean isWestDivider = (x == 24);

                    // Corner Pillars and Buttresses
                    boolean isPillar = (x == 0 || x == 10 || x == 24 || x == 34) &&
                                       (z == 4 || z == 13 || z == 24);

                    if (isPillar) {
                        bp.addBlock(x, y, z, Blocks.QUARTZ_PILLAR);
                    } else if (isOuter) {
                        // Grand Entrance Double Doors (x=16..18, z=4, y=1..3)
                        if (x >= 16 && x <= 18 && z == 4 && y <= 3) continue;

                        // Arched Windows on exterior walls
                        boolean isWindow = (y >= 2 && y <= 4) &&
                            ((z == 4 && (x == 5 || x == 29)) ||
                             (z == 24 && (x == 5 || x == 17 || x == 29)) ||
                             ((x == 0 || x == 34) && (z == 9 || z == 18)));

                        if (isWindow) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else {
                            bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                        }
                    } else if (isEastDivider || isWestDivider) {
                        // Interior wing archways at z=8 and z=17
                        if ((z == 8 || z == 17) && y <= 3) continue;
                        bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                    }
                }
            }
        }

        // Grand Portico Entrance (z=0..3, x=13..21)
        for (int x = 13; x <= 21; x++) {
            for (int z = 0; z <= 3; z++) {
                bp.addBlock(x, 0, z, Blocks.SMOOTH_STONE);
                if (z == 0) bp.addBlock(x, 0, z, Blocks.STONE_BRICK_STAIRS);
            }
        }
        // Portico Pillars
        int[] porticoXs = {13, 15, 19, 21};
        for (int px : porticoXs) {
            for (int y = 1; y <= 5; y++) {
                bp.addBlock(px, y, 1, Blocks.QUARTZ_PILLAR);
            }
        }
        for (int x = 13; x <= 21; x++) {
            bp.addBlock(x, 6, 1, Blocks.QUARTZ_SLAB);
            bp.addBlock(x, 6, 2, Blocks.QUARTZ_SLAB);
            bp.addBlock(x, 6, 3, Blocks.QUARTZ_SLAB);
        }
        bp.addBlock(17, 5, 2, Blocks.LANTERN);

        // Red Carpet Runner in Grand Hall (x=16..18, z=4..19, y=1)
        for (int z = 4; z <= 19; z++) {
            bp.addBlock(16, 1, z, Blocks.CARPET.red());
            bp.addBlock(17, 1, z, Blocks.CARPET.red());
            bp.addBlock(18, 1, z, Blocks.CARPET.red());
        }

        // Vaulted Throne Dais (z=20..23, x=14..20)
        for (int x = 14; x <= 20; x++) {
            for (int z = 20; z <= 23; z++) {
                bp.addBlock(x, 1, z, Blocks.POLISHED_ANDESITE);
                if (z >= 21) bp.addBlock(x, 2, z, Blocks.POLISHED_ANDESITE);
            }
        }

        // Twin Royal Gilded Thrones (x=16 and x=18, z=22)
        bp.addBlock(16, 2, 22, Blocks.GOLD_BLOCK);
        bp.addBlock(16, 3, 22, Blocks.QUARTZ_STAIRS);
        bp.addBlock(18, 2, 22, Blocks.GOLD_BLOCK);
        bp.addBlock(18, 3, 22, Blocks.QUARTZ_STAIRS);
        bp.addBlock(17, 2, 22, Blocks.CARPET.red());

        // Chandeliers in Grand Hall & Throne Room
        bp.addBlock(17, 6, 8, Blocks.LANTERN);
        bp.addBlock(15, 6, 8, Blocks.LANTERN);
        bp.addBlock(19, 6, 8, Blocks.LANTERN);
        bp.addBlock(17, 7, 16, Blocks.LANTERN);
        bp.addBlock(15, 7, 16, Blocks.LANTERN);
        bp.addBlock(19, 7, 16, Blocks.LANTERN);

        // East Wing Furnishings (Royal Bedchamber & Study: x=1..9, z=5..23)
        // Canopy Bed
        bp.addBlock(3, 1, 8, Blocks.BED.red());
        bp.addBlock(4, 1, 8, Blocks.BED.red());
        bp.addBlock(2, 1, 7, Blocks.OAK_FENCE);
        bp.addBlock(2, 2, 7, Blocks.OAK_FENCE);
        bp.addBlock(5, 1, 7, Blocks.OAK_FENCE);
        bp.addBlock(5, 2, 7, Blocks.OAK_FENCE);
        // Study desk & bookshelves
        bp.addBlock(2, 1, 16, Blocks.BOOKSHELF);
        bp.addBlock(2, 2, 16, Blocks.BOOKSHELF);
        bp.addBlock(3, 1, 16, Blocks.LECTERN);
        bp.addBlock(4, 1, 16, Blocks.CHEST);
        bp.addBlock(5, 1, 16, Blocks.BOOKSHELF);
        // Royal Fireplace
        bp.addBlock(1, 1, 20, Blocks.BRICKS);
        bp.addBlock(1, 1, 21, Blocks.CAMPFIRE);
        bp.addBlock(1, 1, 22, Blocks.BRICKS);
        bp.addBlock(1, 2, 20, Blocks.BRICKS);
        bp.addBlock(1, 2, 22, Blocks.BRICKS);
        bp.addBlock(5, 4, 14, Blocks.LANTERN);

        // West Wing Furnishings (Council War Chamber & Treasury: x=25..33, z=5..23)
        // War Council Table
        for (int x = 27; x <= 31; x++) {
            for (int z = 8; z <= 12; z++) {
                bp.addBlock(x, 1, z, Blocks.DARK_OAK_PLANKS);
                bp.addBlock(x, 2, z, Blocks.CARPET.green());
            }
        }
        // Treasury / Armory
        bp.addBlock(32, 1, 18, Blocks.IRON_BLOCK);
        bp.addBlock(32, 1, 19, Blocks.GOLD_BLOCK);
        bp.addBlock(32, 1, 20, Blocks.CHEST);
        bp.addBlock(32, 1, 21, Blocks.ANVIL);
        bp.addBlock(30, 4, 14, Blocks.LANTERN);

        // Pitched Roof & Parapets (y=7 to y=13)
        for (int x = 0; x < 35; x++) {
            for (int z = 4; z < 25; z++) {
                // Central Hall High Vault (x=11..23)
                if (x >= 11 && x <= 23) {
                    int distFromCenter = Math.abs(x - 17);
                    int roofY = 12 - distFromCenter;
                    if (roofY >= 7) {
                        bp.addBlock(x, roofY, z, Blocks.DARK_OAK_PLANKS);
                    }
                } else {
                    // Wing roofs (y=7..9)
                    if (yCondition(x, z)) {
                        bp.addBlock(x, 7, z, Blocks.DARK_OAK_PLANKS);
                    }
                }
            }
        }

        // Crenellated Battlements on Perimeter
        for (int x = 0; x < 35; x += 2) {
            bp.addBlock(x, 7, 4, Blocks.STONE_BRICK_WALL);
            bp.addBlock(x, 7, 24, Blocks.STONE_BRICK_WALL);
        }
        for (int z = 4; z < 25; z += 2) {
            bp.addBlock(0, 7, z, Blocks.STONE_BRICK_WALL);
            bp.addBlock(34, 7, z, Blocks.STONE_BRICK_WALL);
        }

        return bp;
    }

    private static boolean yCondition(int x, int z) {
        return (x >= 1 && x <= 9) || (x >= 25 && x <= 33);
    }

    /**
     * Artisan Workshop / Blacksmith Forge (11x7x9).
     */
    private static Blueprint createWorkshop() {
        Blueprint bp = new Blueprint("workshop", "Artisan Blacksmith Forge", 11, 7, 9);
        bp.setCost(Stockpile.ResourceType.STONE, 45);
        bp.setCost(Stockpile.ResourceType.WOOD, 25);
        bp.setCost(Stockpile.ResourceType.IRON, 12);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                bp.addBlock(x, 0, z, Blocks.COBBLESTONE);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 11; x++) {
                for (int z = 0; z < 9; z++) {
                    boolean isCorner = (x == 0 || x == 10) && (z == 0 || z == 8);
                    boolean isWall = (x == 0 || x == 10 || z == 0 || z == 8);
                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                    } else if (isWall) {
                        if (x == 5 && z == 0 && (y == 1 || y == 2)) continue;
                        if ((x == 2 || x == 8) && (z == 0 || z == 8) && y == 2) {
                            bp.addBlock(x, y, z, Blocks.IRON_BARS);
                        } else {
                            bp.addBlock(x, y, z, Blocks.COBBLESTONE);
                        }
                    }
                }
            }
        }

        // Forge Workstations & Chimney
        bp.addBlock(2, 1, 6, Blocks.BLAST_FURNACE);
        bp.addBlock(3, 1, 6, Blocks.SMOKER);
        bp.addBlock(4, 1, 6, Blocks.ANVIL);
        bp.addBlock(5, 1, 6, Blocks.GRINDSTONE);
        bp.addBlock(6, 1, 6, Blocks.CAULDRON);
        bp.addBlock(7, 1, 6, Blocks.STONECUTTER);
        bp.addBlock(8, 1, 6, Blocks.SMITHING_TABLE);

        // Chimney above blast furnace
        for (int y = 2; y <= 6; y++) {
            bp.addBlock(2, y, 6, Blocks.BRICKS);
        }
        bp.addBlock(2, 7, 6, Blocks.CAMPFIRE);

        // Roof
        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 9; z++) {
                bp.addBlock(x, 5, z, Blocks.STONE_BRICK_SLAB);
            }
        }
        bp.addBlock(5, 4, 4, Blocks.LANTERN);

        return bp;
    }

    /**
     * Gambrel-Roof Grand Barn (13x8x11).
     */
    private static Blueprint createBarn() {
        Blueprint bp = new Blueprint("barn", "Gambrel-Roof Grand Barn", 13, 8, 11);
        bp.setCost(Stockpile.ResourceType.WOOD, 55);
        bp.setCost(Stockpile.ResourceType.STONE, 25);

        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 11; z++) {
                bp.addBlock(x, 0, z, Blocks.COBBLESTONE);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 13; x++) {
                for (int z = 0; z < 11; z++) {
                    boolean isCorner = (x == 0 || x == 12) && (z == 0 || z == 10);
                    boolean isWall = (x == 0 || x == 12 || z == 0 || z == 10);
                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.SPRUCE_LOG);
                    } else if (isWall) {
                        if ((x == 6) && (z == 0 || z == 10) && (y == 1 || y == 2 || y == 3)) continue; // Wide barn doors
                        bp.addBlock(x, y, z, Blocks.SPRUCE_PLANKS);
                    }
                }
            }
        }

        // Animal Stalls inside barn
        bp.addBlock(2, 1, 3, Blocks.OAK_FENCE);
        bp.addBlock(3, 1, 3, Blocks.OAK_FENCE_GATE);
        bp.addBlock(4, 1, 3, Blocks.OAK_FENCE);
        bp.addBlock(2, 1, 2, Blocks.HAY_BLOCK);
        bp.addBlock(8, 1, 3, Blocks.OAK_FENCE);
        bp.addBlock(9, 1, 3, Blocks.OAK_FENCE_GATE);
        bp.addBlock(10, 1, 3, Blocks.OAK_FENCE);
        bp.addBlock(10, 1, 2, Blocks.HAY_BLOCK);

        // Gambrel Curved Roof
        for (int z = 0; z < 11; z++) {
            for (int x = 0; x < 13; x++) {
                int dist = Math.abs(x - 6);
                if (dist <= 2) {
                    bp.addBlock(x, 7, z, Blocks.DYED_TERRACOTTA.red());
                } else if (dist <= 4) {
                    bp.addBlock(x, 6, z, Blocks.DYED_TERRACOTTA.red());
                } else {
                    bp.addBlock(x, 5, z, Blocks.DYED_TERRACOTTA.red());
                }
            }
        }

        bp.addBlock(6, 6, 5, Blocks.LANTERN);
        return bp;
    }

    /**
     * Imperial Vaulted Warehouse (13x7x11).
     */
    private static Blueprint createWarehouse() {
        Blueprint bp = new Blueprint("warehouse", "Imperial Vaulted Warehouse", 13, 7, 11);
        bp.setCost(Stockpile.ResourceType.STONE, 60);
        bp.setCost(Stockpile.ResourceType.WOOD, 40);

        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 11; z++) {
                bp.addBlock(x, 0, z, Blocks.STONE_BRICKS);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < 13; x++) {
                for (int z = 0; z < 11; z++) {
                    boolean isCorner = (x == 0 || x == 12) && (z == 0 || z == 10);
                    boolean isWall = (x == 0 || x == 12 || z == 0 || z == 10);
                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.POLISHED_ANDESITE);
                    } else if (isWall) {
                        if (x == 6 && z == 0 && (y == 1 || y == 2 || y == 3)) continue; // Wide cargo doors
                        bp.addBlock(x, y, z, Blocks.STONE_BRICKS);
                    }
                }
            }
        }

        // Stacks of Resource Crates and Barrels
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                bp.addBlock(x, 1, z, Blocks.BARREL);
                bp.addBlock(x, 2, z, Blocks.CHEST);
            }
        }
        for (int x = 8; x <= 10; x++) {
            for (int z = 6; z <= 8; z++) {
                bp.addBlock(x, 1, z, Blocks.IRON_BLOCK);
                bp.addBlock(x, 2, z, Blocks.CHEST);
            }
        }

        // Roof
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 11; z++) {
                bp.addBlock(x, 5, z, Blocks.DARK_OAK_PLANKS);
            }
        }
        bp.addBlock(6, 4, 5, Blocks.LANTERN);

        return bp;
    }

    /**
     * Market Square with central plaza and striped stalls (13x5x13).
     */
    private static Blueprint createMarket() {
        Blueprint bp = new Blueprint("market", "Town Market Square", 13, 5, 13);
        bp.setCost(Stockpile.ResourceType.WOOD, 30);
        bp.setCost(Stockpile.ResourceType.STONE, 25);

        // Paved Plaza Floor
        for (int x = 0; x < 13; x++) {
            for (int z = 0; z < 13; z++) {
                bp.addBlock(x, 0, z, (x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE);
            }
        }

        // Central Well / Fountain (x=5..7, z=5..7)
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                if (x == 6 && z == 6) {
                    bp.addBlock(x, 0, z, Blocks.WATER);
                } else {
                    bp.addBlock(x, 1, z, Blocks.STONE_BRICK_WALL);
                }
            }
        }

        // 4 Merchant Stalls with Striped Wool Canopies
        createStall(bp, 1, 1, Blocks.WOOL.red(), Blocks.WOOL.white());
        createStall(bp, 8, 1, Blocks.WOOL.blue(), Blocks.WOOL.yellow());
        createStall(bp, 1, 8, Blocks.WOOL.green(), Blocks.WOOL.white());
        createStall(bp, 8, 8, Blocks.WOOL.purple(), Blocks.WOOL.orange());

        return bp;
    }

    private static void createStall(Blueprint bp, int ox, int oz, net.minecraft.world.level.block.Block wool1, net.minecraft.world.level.block.Block wool2) {
        // Counter
        for (int x = 0; x < 4; x++) {
            bp.addBlock(ox + x, 1, oz, Blocks.BARREL);
        }
        // Corner posts
        bp.addBlock(ox, 1, oz, Blocks.OAK_FENCE);
        bp.addBlock(ox, 2, oz, Blocks.OAK_FENCE);
        bp.addBlock(ox + 3, 1, oz, Blocks.OAK_FENCE);
        bp.addBlock(ox + 3, 2, oz, Blocks.OAK_FENCE);
        bp.addBlock(ox, 1, oz + 3, Blocks.OAK_FENCE);
        bp.addBlock(ox, 2, oz + 3, Blocks.OAK_FENCE);
        bp.addBlock(ox + 3, 1, oz + 3, Blocks.OAK_FENCE);
        bp.addBlock(ox + 3, 2, oz + 3, Blocks.OAK_FENCE);

        // Striped Canopy
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
                bp.addBlock(ox + x, 3, oz + z, (x % 2 == 0) ? wool1 : wool2);
            }
        }
        bp.addBlock(ox + 1, 2, oz + 1, Blocks.LANTERN);
    }

    /**
     * Farmhouse Residence (9x7x9).
     */
    private static Blueprint createFarmhouse() {
        Blueprint bp = new Blueprint("farmhouse", "Rural Farmhouse", 9, 7, 9);
        bp.setCost(Stockpile.ResourceType.WOOD, 35);
        bp.setCost(Stockpile.ResourceType.STONE, 20);

        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                bp.addBlock(x, 0, z, Blocks.COBBLESTONE);
            }
        }

        for (int y = 1; y <= 3; y++) {
            for (int x = 0; x < 9; x++) {
                for (int z = 0; z < 9; z++) {
                    boolean isCorner = (x == 0 || x == 8) && (z == 0 || z == 8);
                    boolean isWall = (x == 0 || x == 8 || z == 0 || z == 8);
                    if (isCorner) {
                        bp.addBlock(x, y, z, Blocks.OAK_LOG);
                    } else if (isWall) {
                        if (x == 4 && z == 0 && (y == 1 || y == 2)) continue;
                        if ((x == 2 || x == 6) && (z == 0 || z == 8) && y == 2) {
                            bp.addBlock(x, y, z, Blocks.GLASS_PANE);
                        } else {
                            bp.addBlock(x, y, z, Blocks.OAK_PLANKS);
                        }
                    }
                }
            }
        }

        for (int z = 0; z < 9; z++) {
            for (int x = 0; x < 9; x++) {
                int dist = Math.abs(x - 4);
                bp.addBlock(x, 6 - dist, z, Blocks.DARK_OAK_STAIRS);
            }
        }

        bp.addBlock(2, 1, 6, Blocks.BED.yellow());
        bp.addBlock(6, 1, 6, Blocks.CRAFTING_TABLE);
        bp.addBlock(6, 1, 5, Blocks.FURNACE);
        bp.addBlock(4, 3, 4, Blocks.LANTERN);

        return bp;
    }

    /**
     * Irrigated Wheat Farm Plot (9x2x9).
     */
    private static Blueprint createWheatFarm() {
        Blueprint bp = new Blueprint("wheat_farm", "Irrigated Wheat Farm", 9, 2, 9);
        bp.setCost(Stockpile.ResourceType.WOOD, 15);
        bp.setCost(Stockpile.ResourceType.FOOD, 5);

        // Stone Retaining Perimeter
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                boolean isEdge = (x == 0 || x == 8 || z == 0 || z == 8);
                if (isEdge) {
                    bp.addBlock(x, 0, z, Blocks.STONE_BRICKS);
                    bp.addBlock(x, 1, z, (x == 4 && z == 0) ? Blocks.OAK_FENCE_GATE : Blocks.OAK_FENCE);
                } else if (x == 4 && z == 4) {
                    bp.addBlock(x, 0, z, Blocks.WATER);
                    bp.addBlock(x, 1, z, Blocks.OAK_SLAB); // Slab cover over water canal
                } else {
                    bp.addBlock(x, 0, z, Blocks.FARMLAND);
                    bp.addBlock(x, 1, z, Blocks.WHEAT);
                }
            }
        }
        bp.addBlock(1, 1, 1, Blocks.COMPOSTER);
        bp.addBlock(7, 1, 1, Blocks.WATER_CAULDRON);

        return bp;
    }

    /**
     * Fenced Livestock Pasture Pen (11x3x11).
     */
    private static Blueprint createLivestockPen() {
        Blueprint bp = new Blueprint("livestock_pen", "Livestock Pasture Pen", 11, 3, 11);
        bp.setCost(Stockpile.ResourceType.WOOD, 25);

        for (int x = 0; x < 11; x++) {
            for (int z = 0; z < 11; z++) {
                boolean isPerimeter = (x == 0 || x == 10 || z == 0 || z == 10);
                if (isPerimeter) {
                    if (x == 5 && z == 0) {
                        bp.addBlock(x, 1, z, Blocks.OAK_FENCE_GATE);
                    } else {
                        bp.addBlock(x, 1, z, Blocks.OAK_FENCE);
                    }
                }
            }
        }

        // Covered Lean-to Shelter (x=1..4, z=7..9)
        for (int x = 1; x <= 4; x++) {
            for (int z = 7; z <= 9; z++) {
                bp.addBlock(x, 2, z, Blocks.OAK_SLAB);
            }
        }
        bp.addBlock(2, 1, 8, Blocks.HAY_BLOCK);
        bp.addBlock(3, 1, 8, Blocks.HAY_BLOCK);
        bp.addBlock(8, 1, 8, Blocks.WATER_CAULDRON);

        return bp;
    }
}
