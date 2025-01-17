package rearth.oritech.block.entity.machines.generators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.block.BlockState;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockGeneratorBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;

public class BioGeneratorEntity extends MultiblockGeneratorBlockEntity {
    public BioGeneratorEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BIO_GENERATOR_ENTITY, pos, state, Oritech.CONFIG.generators.bioGeneratorData.energyPerTick());
    }
    
    @Override
    protected Multimap<Direction, BlockApiCache<EnergyStorage, Direction>> getNeighborCaches(BlockPos pos, World world) {
        
        var res = ArrayListMultimap.<Direction, BlockApiCache<EnergyStorage, Direction>>create();
        
        var topCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.up(2));
        res.put(Direction.UP, topCache);
        var botCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.down());
        res.put(Direction.DOWN, botCache);
        var northCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north());
        res.put(Direction.NORTH, northCache);
        var eastCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.east());
        res.put(Direction.EAST, eastCache);
        var southCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south());
        res.put(Direction.SOUTH, southCache);
        var westCache = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.west());
        res.put(Direction.WEST, westCache);
        var northCacheUp = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.north().up());
        res.put(Direction.NORTH, northCacheUp);
        var eastCacheUp = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.east().up());
        res.put(Direction.EAST, eastCacheUp);
        var southCacheUp = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.south().up());
        res.put(Direction.SOUTH, southCacheUp);
        var westCacheUp = BlockApiCache.create(EnergyStorage.SIDED, (ServerWorld) world, pos.west().up());
        res.put(Direction.WEST, westCacheUp);
        
        return res;
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.BIO_GENERATOR;
    }
    
    @Override
    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 1, 1, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 75, 21),
          new GuiSlot(1, 75, 59));
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.BIO_GENERATOR_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 2;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        
        return List.of(
          new Vec3i(1, 0, 0),
          new Vec3i(1, 1, 0)
        );
    }
    
    @Override
    public long getDefaultExtractionRate() {
        return Oritech.CONFIG.generators.bioGeneratorData.maxEnergyExtraction();
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.generators.bioGeneratorData.energyCapacity();
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 1, 0)
        );
    }
}
