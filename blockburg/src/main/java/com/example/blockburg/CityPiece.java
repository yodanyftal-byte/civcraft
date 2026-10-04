package com.example.blockburg;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A whole 80x80 city (5x5 grid of 16x16 cells), drawn in code. */
public class CityPiece extends StructurePiece {
    public static final int SIZE = 80, CELL = 16, MAX_H = 40;

    // {walls, corner pillars, roof}
    private static final Block[][] PALETTES = {
            {Blocks.BRICKS, Blocks.STONE_BRICKS, Blocks.COBBLESTONE},
            {Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE},
            {Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE},
            {Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.BLUE_TERRACOTTA},
            {Blocks.SPRUCE_PLANKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DARK_OAK_PLANKS},
    };
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY};

    private final int baseY;
    private final long seed;

    public CityPiece(int x, int y, int z, long seed) {
        super(BlockburgMod.CITY_PIECE.get(), 0, new BoundingBox(x, y - 8, z, x + SIZE - 1, y + MAX_H, z + SIZE - 1));
        this.baseY = y;
        this.seed = seed;
    }

    public CityPiece(CompoundTag tag) {
        super(BlockburgMod.CITY_PIECE.get(), tag);
        this.baseY = tag.getInt("BaseY");
        this.seed = tag.getLong("Seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("BaseY", baseY);
        tag.putLong("Seed", seed);
    }

    /** {type (0 plaza, 1 park, 2 building), floors, palette, treeX, treeZ} */
    private int[] plot(int cx, int cz) {
        RandomSource r = RandomSource.create(seed ^ (cx * 341873128712L + cz * 132897987541L));
        int d = Math.max(Math.abs(cx - 2), Math.abs(cz - 2));
        int type = (cx == 2 && cz == 2) ? 0 : (r.nextInt(100) < 18 ? 1 : 2);
        int floors = 2 + r.nextInt(3) + (d <= 1 ? 2 + r.nextInt(4) : 0); // taller near the center
        return new int[]{type, floors, r.nextInt(PALETTES.length), 8 + r.nextInt(4), 8 + r.nextInt(4)};
    }

    private void put(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, Block block) {
        put(level, pos, x, y, z, block.defaultBlockState());
    }

    private void put(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState state) {
        level.setBlock(pos.set(x, y, z), state, 2);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[][][] plots = new int[5][5][];
        for (int i = 0; i < 5; i++) for (int j = 0; j < 5; j++) plots[i][j] = plot(i, j);

        int x0 = Math.max(chunkBox.minX(), boundingBox.minX()), x1 = Math.min(chunkBox.maxX(), boundingBox.maxX());
        int z0 = Math.max(chunkBox.minZ(), boundingBox.minZ()), z1 = Math.min(chunkBox.maxZ(), boundingBox.maxZ());
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        BlockState hangingLantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int lx = x - boundingBox.minX(), lz = z - boundingBox.minZ();
                int cx = lx / CELL, cz = lz / CELL, ix = lx % CELL, iz = lz % CELL;
                int[] p = plots[cx][cz];
                boolean building = p[0] == 2;
                int H = p[1] * 4;
                int top = building ? Math.max(16, H + 3) : 16;

                // Foundation + clear the air above
                for (int y = baseY - 8; y < baseY - 3; y++) put(level, pos, x, y, z, Blocks.STONE);
                for (int y = baseY - 3; y < baseY; y++) put(level, pos, x, y, z, Blocks.DIRT);
                for (int y = baseY + 1; y <= baseY + top; y++) put(level, pos, x, y, z, Blocks.AIR);

                // Roads
                if (ix < 4 || iz < 4) {
                    boolean line = (ix == 1 && iz >= 4 && iz % 4 < 2) || (iz == 1 && ix >= 4 && ix % 4 < 2);
                    put(level, pos, x, baseY, z, line ? Blocks.WHITE_CONCRETE : Blocks.GRAY_CONCRETE);
                    continue;
                }

                boolean lamp = (ix == 4 && iz == 4) || (ix == 15 && iz == 15);

                if (p[0] == 0) { // PLAZA with fountain
                    put(level, pos, x, baseY, z, Blocks.STONE_BRICKS);
                    if (ix >= 7 && ix <= 12 && iz >= 7 && iz <= 12) {
                        if (ix == 7 || ix == 12 || iz == 7 || iz == 12) {
                            put(level, pos, x, baseY + 1, z, Blocks.STONE_BRICKS);
                        } else {
                            put(level, pos, x, baseY, z, Blocks.WATER);
                            if (ix >= 9 && ix <= 10 && iz >= 9 && iz <= 10) {
                                for (int y = 1; y <= 3; y++) put(level, pos, x, baseY + y, z, Blocks.CHISELED_STONE_BRICKS);
                                put(level, pos, x, baseY + 4, z, Blocks.SEA_LANTERN);
                            }
                        }
                    }
                } else if (p[0] == 1) { // PARK
                    put(level, pos, x, baseY, z, Blocks.GRASS_BLOCK);
                    int dx = ix - p[3], dz = iz - p[4];
                    boolean trunk = dx == 0 && dz == 0;
                    if (trunk) for (int y = 1; y <= 4; y++) put(level, pos, x, baseY + y, z, Blocks.OAK_LOG);
                    for (int y = 3; y <= 5; y++) {
                        int lim = (y == 5) ? 1 : 3;
                        if (Math.abs(dx) + Math.abs(dz) <= lim && !(trunk && y <= 4)) put(level, pos, x, baseY + y, z, leaves);
                    }
                    long h = (x * 341873128712L ^ z * 132897987541L ^ seed) * 0x9E3779B97F4A7C15L;
                    if (!trunk && Math.abs(dx) + Math.abs(dz) > 3 && (h >>> 60) == 0) {
                        put(level, pos, x, baseY + 1, z, FLOWERS[(int) ((h >>> 50) & 3)]);
                    }
                    lamp = false;
                } else { // BUILDING
                    Block[] pal = PALETTES[p[2]];
                    int bx = ix - 6, bz = iz - 6;
                    boolean inside = bx >= 0 && bx <= 7 && bz >= 0 && bz <= 7;
                    put(level, pos, x, baseY, z, inside ? Blocks.OAK_PLANKS : Blocks.STONE_BRICKS);
                    if (inside) {
                        boolean edge = bx == 0 || bx == 7 || bz == 0 || bz == 7;
                        boolean corner = (bx == 0 || bx == 7) && (bz == 0 || bz == 7);
                        for (int dy = 1; dy <= H + 1; dy++) {
                            int y = baseY + dy;
                            if (dy == H + 1) { // parapet
                                if (edge) put(level, pos, x, y, z, pal[1]);
                            } else if (dy == H) { // roof (with a hatch above the ladder)
                                if (bx == 1 && bz == 1) put(level, pos, x, y, z, ladder);
                                else put(level, pos, x, y, z, pal[2]);
                            } else if (edge) {
                                if (corner) put(level, pos, x, y, z, pal[1]);
                                else if (bz == 0 && (bx == 3 || bx == 4) && dy <= 2) { /* door opening */ }
                                else {
                                    int t = (bx == 0 || bx == 7) ? bz : bx;
                                    boolean win = (dy % 4 == 2 || dy % 4 == 3) && t >= 2 && t <= 5;
                                    put(level, pos, x, y, z, win ? Blocks.GLASS : pal[0]);
                                }
                            } else if (bx == 1 && bz == 1) {
                                put(level, pos, x, y, z, ladder);
                            } else if (dy % 4 == 0) {
                                put(level, pos, x, y, z, Blocks.SPRUCE_PLANKS);
                            } else if (dy % 4 == 3 && bx == 4 && bz == 4) {
                                put(level, pos, x, y, z, hangingLantern);
                            }
                        }
                    }
                }

                if (lamp) {
                    for (int y = 1; y <= 3; y++) put(level, pos, x, baseY + y, z, Blocks.COBBLESTONE_WALL);
                    put(level, pos, x, baseY + 4, z, Blocks.LANTERN);
                }
            }
        }

        // A few villagers in the central plaza (spawned once, by the chunk that contains the spot)
        int px = boundingBox.minX() + 2 * CELL + 5, pz = boundingBox.minZ() + 2 * CELL + 5;
        BlockPos spawn = new BlockPos(px, baseY + 1, pz);
        if (chunkBox.isInside(spawn)) {
            for (int i = 0; i < 3; i++) {
                Villager v = EntityType.VILLAGER.create(level.getLevel());
                if (v == null) continue;
                v.moveTo(px + 0.5 + i, baseY + 1, pz + 0.5 + i * 2, random.nextFloat() * 360f, 0f);
                v.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.STRUCTURE, null);
                v.setPersistenceRequired();
                level.addFreshEntityWithPassengers(v);
            }
        }
    }
}
