package be.minelabs.block.entity;

import be.minelabs.block.BlockEntities;
import be.minelabs.network.NetworkingConstants;
import be.minelabs.recipe.ionic.IonicInventory;
import be.minelabs.recipe.ionic.IonicRecipe;
import be.minelabs.screen.IonicBlockScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * IonicBlockEntity. We still implement {@link Inventory} for compat.
 */
public class IonicBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, Inventory {

    //The actual inventory
    private IonicInventory inventory = new IonicInventory(9, 9, 11);
    //PropertyDelegate that holds the progress, density and charge of both sides. synced to the client
    private final PropertyDelegate propertyDelegate;
    //List of needed input left ingredients; Synced
    private DefaultedList<Ingredient> leftIngredients = DefaultedList.of();
    //List of needed input right ingredients; Synced
    private DefaultedList<Ingredient> rightIngredients = DefaultedList.of();
    //Progress of the recipe; -1 means not started; Synced by propertyDelegate
    private int progress = 0;
    private int maxProgress = 23;
    //Density (amount) of items needed for the recipe;  Also used to tell the client a recipe is found;Synced by propertyDelegate
    private int leftdensity;
    //Density (amount) of items needed for the recipe;  Also used to tell the client a recipe is found;Synced by propertyDelegate
    private int rightdensity;
    //Charge of items needed for the recipe;Synced by propertyDelegate
    private int leftCharge;
    //Charge of items needed for the recipe;Synced by propertyDelegate
    private int rightCharge;
    //Current recipe selected; NOT synced
    private IonicRecipe currentrecipe;

    public IonicBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.IONIC_BLOCK_ENTITY, pos, state);
        //this.matchGetter = RecipeManager.createCachedMatchGetter(CraftingRecipes.IONIC_CRAFTING);

        this.inventory.addListener(sender -> IonicBlockEntity.this.markDirty());

        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> IonicBlockEntity.this.progress;
                    case 1 -> IonicBlockEntity.this.leftdensity;
                    case 2 -> IonicBlockEntity.this.rightdensity;
                    case 3 -> IonicBlockEntity.this.leftCharge;
                    case 4 -> IonicBlockEntity.this.rightCharge;
                    case 5 -> IonicBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> IonicBlockEntity.this.progress = value;
                    case 1 -> IonicBlockEntity.this.leftdensity = value;
                    case 2 -> IonicBlockEntity.this.rightdensity = value;
                    case 3 -> IonicBlockEntity.this.leftCharge = value;
                    case 4 -> IonicBlockEntity.this.rightCharge = value;
                    case 5 -> IonicBlockEntity.this.maxProgress = value;
                    default -> {
                    }
                }
            }

            @Override
            public int size() {
                return 6;
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new IonicBlockScreenHandler(syncId, inv, inventory, propertyDelegate, pos);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        NbtList nbtList = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
        this.inventory = new IonicInventory(9, 9, 11);
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            if (j >= 0 && j < inventory.size()) {
                inventory.setStack(j, ItemStack.fromNbt(nbtCompound));
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList nbtList = new NbtList();

        for (int i = 0; i < inventory.size(); ++i) {
            ItemStack itemStack = inventory.getStack(i);
            if (!itemStack.isEmpty()) {
                NbtCompound nbtCompound = new NbtCompound();
                nbtCompound.putByte("Slot", (byte) i);
                itemStack.writeNbt(nbtCompound);
                nbtList.add(nbtCompound);
            }
        }
        nbt.put("Items", nbtList);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return super.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        DynamicRegistryManager manager = world.getRegistryManager();
        if (hasRecipe() && canExport(manager) && containerCheck() && hasEnoughItems()) {
            progress++;
            markDirty();

            if (progress >= maxProgress) {
                // Craft
                craft(manager);
                resetProgress();
            }
        } else {
            resetProgress();
            advancementCheck();
        }
    }

    private boolean hasRecipe() {
        return currentrecipe != null;
    }

    /**
     * Callback used for advancements
     */
    private void advancementCheck() {
        // TODO advancements

    }

    private void resetProgress() {
        progress = 0;
    }

    private void craft(DynamicRegistryManager manager) {
        //Remove actual ingredients
        for (int i = 0; i < leftIngredients.size(); i++) {
            inventory.getStack(i).decrement(leftdensity);
        }

        //Remove actual ingredients
        for (int i = 0; i < rightIngredients.size(); i++) {
            inventory.getStack(i).decrement(rightdensity);
        }

        // Remove a container item if needed
        if (currentrecipe.needsContainer()) {
            inventory.getContainerStack().decrement(1);
        }
        // Update output stack
        inventory.setStack(28, new ItemStack(currentrecipe.getOutput(manager).getItem(),
                inventory.getStack(28).getCount() + currentrecipe.getOutput(manager).getCount()));
    }

    private boolean hasEnoughItems() {
        for (int i = 0; i < leftIngredients.size(); i++) {
            if (!leftIngredients.get(i).test(inventory.getLeftGrid().getStack(i)) || inventory.getLeftGrid().getStack(i).getCount() < leftdensity) {
                return false; // not enough items
            }
        }
        for (int i = 0; i < rightIngredients.size(); i++) {
            if (!rightIngredients.get(i).test(inventory.getRightGrid().getStack(i)) || inventory.getRightGrid().getStack(i).getCount() < rightdensity) {
                return false; // not enough items
            }
        }
        return true;
    }

    private boolean containerCheck() {
        return hasContainer() && currentrecipe.needsContainer() || !currentrecipe.needsContainer();
    }

    private boolean hasContainer() {
        return !inventory.getContainerStack().isEmpty() && inventory.getContainerStack().getCount() >= 1;
    }

    private boolean canExport(DynamicRegistryManager manager) {
        return inventory.getOutputStack().isEmpty()
                || inventory.getOutputStack().getCount() + currentrecipe.getOutput(manager).getCount() <= inventory.getOutputStack().getMaxCount()
                && inventory.getOutputStack().isOf(currentrecipe.getOutput(getWorld().getRegistryManager()).getItem());

    }

    public void resetRecipe() {
        this.currentrecipe = null;
        this.progress = -1;
        this.maxProgress = 23;
        this.leftdensity = 0;
        this.rightdensity = 0;
        this.leftCharge = 0;
        this.rightCharge = 0;
        this.leftIngredients = DefaultedList.of();
        this.rightIngredients = DefaultedList.of();
        this.markDirty();
    }

    public void updateRecipe() {
        getWorld().getRecipeManager().getFirstMatch(IonicRecipe.IonicRecipeType.INSTANCE, inventory, getWorld())
                .ifPresentOrElse(recipe -> {
                    if (recipe != currentrecipe) {
                        // Different recipe
                        this.currentrecipe = recipe;
                        this.progress = 0;
                        this.leftIngredients = recipe.getLeftingredients();
                        this.rightIngredients = recipe.getRightingredients();
                        this.leftdensity = recipe.getLeftdensity();
                        this.rightdensity = recipe.getRightdensity();
                        this.leftCharge = recipe.getLeftCharge();
                        this.rightCharge = recipe.getRightCharge();
                        this.maxProgress = recipe.getTime();
                        markDirty();
                        sendDataPacket();
                    }
                }, this::resetRecipe);
    }

    private void sendDataPacket() {
        if (world.isClient) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.leftIngredients.size());
        for (Ingredient ingredient : this.leftIngredients) {
            ingredient.write(buf);
        }
        buf.writeInt(this.rightIngredients.size());
        for (Ingredient ingredient : this.rightIngredients) {
            ingredient.write(buf);
        }

        for (ServerPlayerEntity player : PlayerLookup.tracking((ServerWorld) world, pos)) {
            ServerPlayNetworking.send(player, NetworkingConstants.IONICDATASYNC, buf);
        }
    }

    /**
     * Provides a buffer to {@link IonicBlockScreenHandler}, to get the position of the blockentity
     * The blockentity is used to get all needed values.
     *
     * @param player the player that is opening the screen
     * @param buf    the packet buffer
     */
    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public void setIngredients(DefaultedList<Ingredient> leftIngredients, DefaultedList<Ingredient> rightIngredients) {
        this.leftIngredients = leftIngredients;
        this.rightIngredients = rightIngredients;
    }

    public DefaultedList<Ingredient> getLeftIngredients() {
        return this.leftIngredients;
    }

    public DefaultedList<Ingredient> getRightIngredients() {
        return rightIngredients;
    }

    //Iventory overrides, for compatibility

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}
