package com.example.blockburg;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class CityStructure extends Structure {
    public static final MapCodec<CityStructure> CODEC = simpleCodec(CityStructure::new);

    public CityStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        ChunkGenerator gen = ctx.chunkGenerator();
        int cx = cp.getMiddleBlockX(), cz = cp.getMiddleBlockZ();
        int half = CityPiece.SIZE / 2;
        int[][] pts = {{0, 0}, {-half, -half}, {half, -half}, {-half, half}, {half, half}};
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, centerH = 0;
        for (int[] o : pts) {
            int h = gen.getFirstOccupiedHeight(cx + o[0], cz + o[1], Heightmap.Types.WORLD_SURFACE_WG,
                    ctx.heightAccessor(), ctx.randomState());
            if (o[0] == 0 && o[1] == 0) centerH = h;
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        // Avoid oceans/rivers and very hilly terrain
        if (min <= gen.getSeaLevel() + 1 || max - min > 8) return Optional.empty();

        int baseY = centerH - 1;
        BlockPos origin = new BlockPos(cx - half, baseY, cz - half);
        long seed = ctx.random().nextLong();
        return Optional.of(new GenerationStub(origin,
                builder -> builder.addPiece(new CityPiece(origin.getX(), baseY, origin.getZ(), seed))));
    }

    @Override
    public StructureType<?> type() {
        return BlockburgMod.CITY_TYPE.get();
    }
}
