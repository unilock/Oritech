package rearth.oritech.block.entity.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.FluidPipeBlock;
import rearth.oritech.block.blocks.pipes.FluidPipeConnectionBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.FluidProvider;

import java.util.*;
import java.util.stream.Collectors;

public class FluidPipeInterfaceEntity extends GenericPipeInterfaceEntity implements FluidProvider {
    
    public static final int MAX_TRANSFER_RATE = (int) (FluidConstants.BUCKET * Oritech.CONFIG.fluidPipeExtractAmountBuckets());
    private static final int TRANSFER_PERIOD = Oritech.CONFIG.fluidPipeExtractIntervalDuration();
    
    private final HashMap<BlockPos, BlockApiCache<Storage<FluidVariant>, Direction>> lookupCache = new HashMap<>();
    
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }
        
        @Override
        protected long getCapacity(FluidVariant variant) {
            return (long) (MAX_TRANSFER_RATE * Oritech.CONFIG.fluidPipeInternalStorageBuckets());
        }
        
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            FluidPipeInterfaceEntity.this.markDirty();
        }
    };
    
    public FluidPipeInterfaceEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.FLUID_PIPE_ENTITY, pos, state);
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("fluidVariant", fluidStorage.variant.toNbt());
        nbt.putLong("amount", fluidStorage.amount);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        fluidStorage.variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariant"));
        fluidStorage.amount = nbt.getLong("amount");
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, GenericPipeInterfaceEntity blockEntity) {
        if (world.isClient || world.getTime() % TRANSFER_PERIOD != 0) return;
        
        var data = FluidPipeBlock.FLUID_PIPE_DATA.getOrDefault(world.getRegistryKey().getValue(), new PipeNetworkData());
        
        // try to fill internal storage from inputs (if extract true)
        // one transaction for each side
        if (state.get(FluidPipeConnectionBlock.EXTRACT) && fluidStorage.amount < fluidStorage.getCapacity()) {
            
            var sources = data.machineInterfaces.getOrDefault(pos, new HashSet<>());
            
            for (var sourcePos : sources) {
                var offset = pos.subtract(sourcePos);
                var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ());
                var sourceContainer = findFromCache(world, sourcePos, direction);
                if (sourceContainer == null || !sourceContainer.supportsExtraction()) continue;
                
                var availableInsert = fluidStorage.getCapacity() - fluidStorage.amount;
                var ownVariant = fluidStorage.variant;
                
                for (Iterator<StorageView<FluidVariant>> it = sourceContainer.nonEmptyIterator(); it.hasNext(); ) {
                    var fluid = it.next();
                    if (!ownVariant.isBlank() && !fluid.getResource().equals(ownVariant)) continue;
                    var targetVariant = fluid.getResource();
                    try (var tx = Transaction.openOuter()) {
                        var extracted = sourceContainer.extract(targetVariant, availableInsert, tx);
                        var inserted = fluidStorage.insert(targetVariant, extracted, tx);
                        if (inserted != extracted) {
                            // this should never happen
                            tx.abort();
                            Oritech.LOGGER.warn("Something weird has happened with fluid pipes. Working with them is just annoying. Caused at: " + pos);
                            continue;
                        } else {
                            tx.commit();
                        }
                        break;
                    }
                }
            }
        }
        
        // if one (or more) of connected blocks has fluid available (of first found type, only transfer one type per tick)
        // gather all connection targets supporting insertion
        // shuffle em
        // insert until no more fluid to output is available
        if (fluidStorage.amount <= 0) return;
        
        var targets = findNetworkTargets(pos, data);
        
        var fluidStorages = targets.stream()
                              .filter(targetPos -> targetPos.getLeft().getManhattanDistance(pos) > 1)   // ignore neighbors basically, as this pipe is set to extract
                              .map(target -> findFromCache(world, target.getLeft(), target.getRight()))
                              .filter(obj -> Objects.nonNull(obj) && obj.supportsInsertion())
                              .collect(Collectors.toList());
        
        Collections.shuffle(fluidStorages);
        
        var availableFluid = fluidStorage.getAmount();
        var ownType = fluidStorage.variant;
        
        try (var tx = Transaction.openOuter()) {
            for (var targetStorage : fluidStorages) {
                var transferred = targetStorage.insert(ownType, availableFluid, tx);
                fluidStorage.extract(ownType, transferred, tx);
                availableFluid -= transferred;
                
                if (availableFluid <= 0) break;
            }
            
            tx.commit();
        }
        
        markDirty();
        
    }
    
    private Storage<FluidVariant> findFromCache(World world, BlockPos pos, Direction direction) {
        var cacheRes = lookupCache.computeIfAbsent(pos, elem -> BlockApiCache.create(FluidStorage.SIDED, (ServerWorld) world, pos));
        return cacheRes.find(direction);
    }
    
    @Override
    public Storage<FluidVariant> getFluidStorage(Direction direction) {
        return fluidStorage;
    }
}
