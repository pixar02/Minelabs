package be.uantwerpen.minelabs.block.entity;

import be.uantwerpen.minelabs.Minelabs;
import be.uantwerpen.minelabs.gui.lab_chest_gui.LabChestScreenHandler;
import be.uantwerpen.minelabs.inventory.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class LabChestBlockEntity extends BlockEntity implements ImplementedInventory, NamedScreenHandlerFactory, IAnimatable {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(21, ItemStack.EMPTY);
    protected final PropertyDelegate propertyDelegate = new ArrayPropertyDelegate(0);

    private LabChestBlockEntity.AnimationStage animationStage;
    private float animationProgress;
    private float prevAnimationProgress;
    private int viewerCount;
    private AnimationFactory factory = new AnimationFactory(this);

    public LabChestBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.LAB_CHEST_BLOCK_ENTITY, pos, state);
        this.animationStage = LabChestBlockEntity.AnimationStage.CLOSED;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, inventory);
        super.readNbt(nbt);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LabChestScreenHandler(syncId, inv, this, propertyDelegate);
    }

    public static void tick(World world, BlockPos pos, BlockState state, LabChestBlockEntity blockEntity) {
        blockEntity.updateAnimation(world, pos, state);
    }
    private void updateAnimation(World world, BlockPos pos, BlockState state) {
        this.prevAnimationProgress = this.animationProgress;
        switch (this.animationStage) {
            case CLOSED -> this.animationProgress = 0.0F;
            case OPENING -> {
                this.animationProgress += 0.1F;
                if (this.animationProgress >= 2.0F) {
                    this.animationStage = AnimationStage.OPENED;
                    this.animationProgress = 20.0F;
                    //updateNeighborStates(world, pos, state);
                }
            }

            //this.pushEntities(world, pos, state);
            case CLOSING -> {
                this.animationProgress -= 0.1F;
                if (this.animationProgress <= 0.0F) {
                    this.animationStage = AnimationStage.CLOSED;
                    this.animationProgress = 0.0F;
                    //updateNeighborStates(world, pos, state);
                }
            }
            case OPENED -> this.animationProgress = 2.0F;
        }

    }

    public LabChestBlockEntity.AnimationStage getAnimationStage() {
        return this.animationStage;
    }

    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        //Minelabs.LOGGER.info("type: " + type +" data:"+ data);
        if (type == 1) {
            this.viewerCount = data;
            if (data == 0) {
                this.animationStage = LabChestBlockEntity.AnimationStage.CLOSING;
                //updateNeighborStates(this.getWorld(), this.pos, this.getCachedState());
            }

            if (data == 1) {
                this.animationStage = LabChestBlockEntity.AnimationStage.OPENING;
                //updateNeighborStates(this.getWorld(), this.pos, this.getCachedState());
            }
            return true;
        } else {
            return super.onSyncedBlockEvent(type, data);
        }
    }

    @Override
    public void onOpen(PlayerEntity player) {
        //Minelabs.LOGGER.info("Open Viewers:" + viewerCount);
        if (!player.isSpectator()) {
            if (this.viewerCount < 0) {
                this.viewerCount = 0;
            }

            ++this.viewerCount;
            this.world.addSyncedBlockEvent(this.pos, this.getCachedState().getBlock(), 1, this.viewerCount);
            if (this.viewerCount == 1) {
                this.world.emitGameEvent(player, GameEvent.CONTAINER_OPEN, this.pos);
                //PLAY SOUND ???
            }
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        //TODO WHY IS IT NOT CALLED
        Minelabs.LOGGER.info("Close Viewers:" + viewerCount);
        if (!player.isSpectator()) {
            --this.viewerCount;
            this.world.addSyncedBlockEvent(this.pos, this.getCachedState().getBlock(), 1, this.viewerCount);
            if (this.viewerCount <= 0) {
                this.world.emitGameEvent(player, GameEvent.CONTAINER_CLOSE, this.pos);
                // PLAY SOUND
            }
        }
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<LabChestBlockEntity>
                (this, "controller", 0, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("open", false));
        //return PlayState.CONTINUE;
        //event.getController().setAnimation(new AnimationBuilder().addAnimation("open"));
        //TODO TRIGGERS/EVENTS
        Minelabs.LOGGER.info(this.animationStage);
        switch (this.animationStage) {
            case CLOSED -> {
                event.getController().clearAnimationCache();
                return PlayState.STOP;
            }
            case OPENED -> {
                return PlayState.STOP;

            }
            case OPENING -> {
                event.getController().clearAnimationCache();
                event.getController().setAnimation(new AnimationBuilder().addAnimation("open", false));
                return PlayState.CONTINUE;
            }
            case CLOSING -> {
                // event.getController().setAnimation(new AnimationBuilder().addAnimation("close", false));
                // event.getController().setAnimation(new AnimationBuilder().addAnimation("open", false));
                return PlayState.CONTINUE;
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }


    public enum AnimationStage {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING;

        AnimationStage() {
        }
    }
}
