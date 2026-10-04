package com.example.blockburg;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BlockburgMod.MODID)
public class BlockburgMod {
    public static final String MODID = "blockburg";

    public static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<CityStructure>> CITY_TYPE =
            STRUCTURES.register("city", () -> (StructureType<CityStructure>) () -> CityStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> CITY_PIECE =
            PIECES.register("city_piece", () -> (StructurePieceType.ContextlessType) CityPiece::new);

    public BlockburgMod(IEventBus bus) {
        STRUCTURES.register(bus);
        PIECES.register(bus);
    }
}
