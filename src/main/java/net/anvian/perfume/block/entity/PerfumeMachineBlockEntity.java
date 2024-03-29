package net.anvian.perfume.block.entity;

import net.anvian.perfume.recipe.PerfumeMachineRecipe;
import net.anvian.perfume.screen.PerfumeMachineScreenHandler;
import net.anvian.perfume.sound.ModSounds;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PerfumeMachineBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(4, ItemStack.EMPTY);

    private static final int FUEL_SLOT = 0;
    private static final int BOTTLE_SLOT = 1;
    private static final int ITEM_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;

    private final PropertyDelegate propertyDelegate;
    private int progress = 0;
    private int maxProgress = 3000;
    private int fuelTime = 0;
    private int maxFuelTime = 0;

    public PerfumeMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PERFUME_MACHINE_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> PerfumeMachineBlockEntity.this.progress;
                    case 1 -> PerfumeMachineBlockEntity.this.maxProgress;
                    case 2 -> PerfumeMachineBlockEntity.this.fuelTime;
                    case 3 -> PerfumeMachineBlockEntity.this.maxFuelTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index){
                    case 0 -> PerfumeMachineBlockEntity.this.progress = value;
                    case 1 -> PerfumeMachineBlockEntity.this.maxProgress = value;
                    case 2 -> PerfumeMachineBlockEntity.this.fuelTime = value;
                    case 3 -> PerfumeMachineBlockEntity.this.maxFuelTime = value;
                }
            }

            @Override
            public int size() {
                return 4;
            }
        };
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.perfume.perfume_machine");
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("perfume_machine.progress", progress);
        nbt.putInt("perfume_machine.fuelTime", fuelTime);
        nbt.putInt("perfume_machine.maxFuelTime", maxFuelTime);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt("perfume_machine.progress");
        fuelTime = nbt.getInt("perfume_machine.fuelTime");
        maxFuelTime = nbt.getInt("perfume_machine.maxFuelTime");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PerfumeMachineScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if(world.isClient()) {
            return;
        }

        if(isConsumingFuel()) {
            this.fuelTime--;
        }

        if(isOutputSlotEmptyOrReceivable()) {
            if(this.hasRecipe()) {
                if(hasFuelInFuelSlot() && !isConsumingFuel()) {
                    this.consumeFuel();
                }
                if (isConsumingFuel()){
                    this.increaseCraftProgress();
                    markDirty(world, pos, state);

                    if(hasCraftingFinished()) {
                        world.playSound(null, pos, ModSounds.PIP_SOUND, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        this.craftItem();
                        this.resetProgress();
                    }
                }
            } else {
                this.resetProgress();
            }
        } else {
            this.resetProgress();
            markDirty(world, pos, state);
        }
    }
    private boolean hasFuelInFuelSlot() {
        return !this.getStack(FUEL_SLOT).isEmpty();
    }

    private boolean isConsumingFuel() {
        return this.fuelTime > 0;
    }

    private void consumeFuel() {
        if(!getStack(FUEL_SLOT).isEmpty()) {
            //time in ticks that the fuel will burn
            this.fuelTime = 300;
            this.maxFuelTime = this.fuelTime;
            this.removeStack(FUEL_SLOT,1);
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private void craftItem() {
        Optional<RecipeEntry<PerfumeMachineRecipe>> recipe = getCurrentRecipe();

        this.removeStack(BOTTLE_SLOT, 1);
        this.removeStack(ITEM_SLOT, 1);

        this.setStack(OUTPUT_SLOT, new ItemStack(recipe.get().value().getResult(null).getItem(),
                getStack(OUTPUT_SLOT).getCount() + recipe.get().value().getResult(null).getCount()));
    }

    private boolean hasCraftingFinished() {
        return progress >= maxProgress;
    }

    private void increaseCraftProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        Optional<RecipeEntry<PerfumeMachineRecipe>> recipe = getCurrentRecipe();
        
        return recipe.isPresent() && canInsertAmountIntoOutputSlot(recipe.get().value().getResult(null))
                && canInsertItemIntoOutputSlot(recipe.get().value().getResult(null).getItem());
    }

    private Optional<RecipeEntry<PerfumeMachineRecipe>> getCurrentRecipe() {
        SimpleInventory inv = new SimpleInventory(this.size());
        for(int i = 0; i < this.size(); i++) {
            inv.setStack(i, this.getStack(i));
        }

        return getWorld().getRecipeManager().getFirstMatch(PerfumeMachineRecipe.Type.INSTANCE, inv, getWorld());
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.getStack(OUTPUT_SLOT).getItem() == item || this.getStack(OUTPUT_SLOT).isEmpty();
    }

    private boolean canInsertAmountIntoOutputSlot(ItemStack result) {
        return this.getStack(OUTPUT_SLOT).getCount() + result.getCount() <= getStack(OUTPUT_SLOT).getMaxCount();
    }

    private boolean isOutputSlotEmptyOrReceivable() {
        return this.getStack(OUTPUT_SLOT).isEmpty() || this.getStack(OUTPUT_SLOT).getCount() < this.getStack(OUTPUT_SLOT).getMaxCount();
    }
}
