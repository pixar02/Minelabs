package be.uantwerpen.minelabs.block.entity;

import be.uantwerpen.minelabs.inventory.ImplementedInventory;
import be.uantwerpen.minelabs.item.Items;
import be.uantwerpen.minelabs.util.MinelabsProperties;
import be.uantwerpen.minelabs.util.NucleusState;
import be.uantwerpen.minelabs.util.NuclidesTable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BohrBlockEntity extends BlockEntity implements ImplementedInventory {

    //    Color for rendering the text
    public static final int WHITE_COLOR = ColorHelper.Argb.getArgb(255, 255, 255, 255);
    public static final int GREEN_COLOR = ColorHelper.Argb.getArgb(255, 0, 255, 0);
    public static final int RED_COLOR = ColorHelper.Argb.getArgb(255, 255, 0, 0);

    private static final int MAX_TIMER = 99 * 20;
    private int timer = MAX_TIMER;

    //the inventory stack [0,3[ = protons, [3,6[ = neutrons, [6,9[ = electrons
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(9, ItemStack.EMPTY);

    public BohrBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.BOHR_BLOCK_ENTITY, pos, state);
    }

    /**
     * counts all the protons in the block
     *
     * @return nr of protons in the block
     */
    public int getProtonCount() {
        int count = 0;
//        slot 0 to 2
        for (int slot = 0; slot < 3; slot++)
//            if there is protons
            if (getItems().get(slot).getItem() == Items.PROTON) {
//                add to sum
                count += getItems().get(slot).getCount();
            }
        return count;
    }

    /**
     * counts all the neutrons in the block
     *
     * @return nr of neutrons in the block
     */
    public int getNeutronCount() {
        int count = 0;
//        slot 3 to 5
        for (int slot = 3; slot < 6; slot++)
//            if neutrons in the stack
            if (getItems().get(slot).getItem() == Items.NEUTRON) {
//                add them in the count
                count += getItems().get(slot).getCount();
            }
        return count;
    }

    /**
     * counts all the electrons in the block
     *
     * @return nr of electrons in the block
     */
    public int getElectronCount() {
        int count = 0;
//        slot 6 to 8
        for (int slot = 6; slot < 9; slot++)
//            if electron in slot
            if (getItems().get(slot).getItem() == Items.ELECTRON) {
                count += getItems().get(slot).getCount();
            }
        return count;
    }


    @Nullable
  /*  public static BlockPos getMasterPos(BlockState state, BlockPos blockPos) {
        BohrPart part = state.get(MinelabsProperties.BOHR_PART);
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        switch (part) {
            case BASE -> {
                return blockPos;
            }
            case BACK -> {
                return blockPos.offset(direction);
            }
            case RIGHT -> {
                return blockPos.offset(direction.rotateYCounterclockwise());
            }
            case CORNER -> {
                return blockPos.offset(direction).offset(direction.rotateYCounterclockwise());
            }
            default -> {
                return null;
            }
        }
    }

   */

    /**
     * takes a starting block and find the positions of the other 3 bohr-blocks
     *
     * @param state    the state of the starting block
     * @param blockPos the position of the starting block
     * @param world    the world of the starting block
     * @return a list of other bohr block belonging to the same bohr plate
     */
    /*
    public static List<BlockPos> getBohrParts(BlockState state, BlockPos blockPos, World world) {
        BohrPart part = state.get(MinelabsProperties.BOHR_PART);
        Direction direction = state.get(Properties.HORIZONTAL_FACING);
        switch (part) {
            case BASE -> {
                return List.of(
                        blockPos.offset(direction.rotateYClockwise()), // RIGHT
                        blockPos.offset(direction.rotateYClockwise()).offset(direction.getOpposite()), //CORNER
                        blockPos.offset(direction.getOpposite())); // BACK
            }
            case BACK -> {
                return List.of(
                        blockPos.offset(direction), // BASE
                        blockPos.offset(direction.rotateYClockwise()).offset(direction), // RIGHT
                        blockPos.offset(direction.rotateYClockwise())); //CORNER
            }
            case RIGHT -> {
                return List.of(
                        blockPos.offset(direction.getOpposite()), // CORNER
                        blockPos.offset(direction.rotateYCounterclockwise()).offset(direction.getOpposite()), // BACK
                        blockPos.offset(direction.rotateYCounterclockwise())); //BASE
            }
            case CORNER -> {
                return List.of(
                        blockPos.offset(direction), // RIGHT
                        blockPos.offset(direction.rotateYCounterclockwise()).offset(direction), // BASE
                        blockPos.offset(direction.rotateYCounterclockwise())); // BACK
            }
            default -> {
                return new ArrayList<>();
            }
        }
    }


     */
    public static void tick(World world, BlockPos pos, BlockState state, BohrBlockEntity entity) {
        if (world.isClient) {
            return;
        }

        // normal texture
        int status = 0;

        int nrOfProtons = entity.getProtonCount();
        int nrOfNeutrons = entity.getNeutronCount();
        int nrOfElectrons = entity.getElectronCount();

        if (NuclidesTable.isStable(nrOfProtons, nrOfNeutrons, nrOfElectrons)) {
            entity.timer = MAX_TIMER; // max timer value
            if (entity.isCollectable()) {// collectible -> green texture
                status = 1;
            }
        } else { // unstable => red texture
            status = 2;
            entity.timer = Math.max(0, entity.timer - 1);
        }
        if (entity.timer == 0) {
            NbtCompound nbtCompound = entity.createNbt();
            entity.writeNbt(nbtCompound);
            entity.scatterParticles(3);
            entity.timer = MAX_TIMER;
            markDirty(world, pos, state);
        }

        world.setBlockState(pos, state.with(MinelabsProperties.STATUS, status));
        world.updateListeners(pos, state, state.with(MinelabsProperties.STATUS, status), Block.NOTIFY_LISTENERS);
    }
/*
    public boolean isMaster() {
        return this.getCachedState().get(MinelabsProperties.BOHR_PART) == BohrPart.BASE;
    }

 */

    /**
     * @return itemstacks of the plate
     */
    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    /**
     * renders the text of the bohrplate status
     */
    public void renderText() {

        assert world != null;

        int nrOfProtons = getProtonCount();
        int nrOfNeutrons = getNeutronCount();
        int nrOfElectrons = getElectronCount();

        String neutronHelp = "";
        String electronHelp = "";

        MatrixStack matrixStack = new MatrixStack();
        NucleusState nuclideStateInfo = NuclidesTable.getNuclide(nrOfProtons, nrOfNeutrons);
        String protonString = "#Protons: " + nrOfProtons;
        String electronString = "#Electrons: " + nrOfElectrons;
        String neutronString = "#Neutrons: " + nrOfNeutrons;
        String atomName = "None";
        String symbol = "None";
        String mainDecayMode = "Unstable";
        String ionicCharge = NuclidesTable.calculateIonicCharge(nrOfProtons, nrOfElectrons);
        String ion = "ion: " + ionicCharge;
        int color = RED_COLOR;
        boolean doesStableNuclideExist = true;

        if (nuclideStateInfo != null) {
            atomName = nuclideStateInfo.getAtomName();
            symbol = nuclideStateInfo.getSymbol();
//            mainDecayMode = nuclideStateInfo.getMainDecayMode();

            if (NuclidesTable.isStable(nrOfProtons, nrOfNeutrons, nrOfElectrons)) {
                color = GREEN_COLOR;
                mainDecayMode = "Stable";
            }
            else {
                doesStableNuclideExist = false;
            }
        }
        else {
            doesStableNuclideExist = false;
        }
        if (!doesStableNuclideExist) {
            ArrayList<String> buildHelp = getBuildHelp(nrOfProtons, nrOfNeutrons, nrOfElectrons);
            neutronHelp = buildHelp.get(0);
            electronHelp = buildHelp.get(1);
        }
        String atomInfo = mainDecayMode + "    " + atomName + "    " + symbol + "    " + ion + "    Timer: " + timer / 20;
        String helpInfo = neutronHelp + electronHelp + " to stabilise.";
        /*
         * Rendering of text:
         */
        MinecraftClient.getInstance().textRenderer.draw(matrixStack, atomInfo, 10, 10, color);
        if (!neutronHelp.isEmpty() || !electronHelp.isEmpty()) {
            MinecraftClient.getInstance().textRenderer.draw(matrixStack, helpInfo, 10, 20, RED_COLOR);
        }
        MinecraftClient.getInstance().textRenderer.draw(matrixStack, protonString, 10, 30, WHITE_COLOR);
        MinecraftClient.getInstance().textRenderer.draw(matrixStack, neutronString, 10, 40, WHITE_COLOR);
        MinecraftClient.getInstance().textRenderer.draw(matrixStack, electronString, 10, 50, WHITE_COLOR);
    }

    /**
     * Determines the help messages (for neutrons and electrons) to be shown to the player while building an atom.
     *
     * @param nrOfProtons   : amount of protons
     * @param nrOfNeutrons  : amount of neutrons
     * @param nrOfElectrons : amount of electrons
     * @return : array of two values (size=2, type=String) = the neutron help and the electron help message.
     */
    public ArrayList<String> getBuildHelp(int nrOfProtons, int nrOfNeutrons, int nrOfElectrons) {

        int neutronDiff;
        int electronDiff;
        String neutronHelp = "";
        String electronHelp = "";
        neutronDiff = NuclidesTable.findNextStableAtom(nrOfProtons, false); // here: amount of total neutrons we need
        if (neutronDiff != -1 && (neutronDiff != nrOfNeutrons)) {
            neutronDiff = nrOfNeutrons - neutronDiff; // here: the actual difference between what we need and what we have
            if (neutronDiff < 0) {
                neutronDiff = Math.abs(neutronDiff);
                neutronHelp = "Add ";
            } else {
                neutronHelp = "Remove ";
            }
            neutronHelp += neutronDiff + " neutron";
            if (neutronDiff > 1) {
                neutronHelp += "s";
            }
        }

        if (Math.abs(nrOfProtons - nrOfElectrons) > 5) {
            if (!neutronHelp.isEmpty()) {
                electronHelp += " and ";
            }
            if (nrOfProtons - nrOfElectrons > 0) {
                electronDiff = nrOfProtons - nrOfElectrons - 5;
                electronHelp += "add ";
            } else {
                electronDiff = nrOfElectrons - (nrOfProtons + 5);
                electronHelp += "remove ";
            }
            electronHelp += electronDiff + " electron";
            if (electronDiff > 1) {
                electronHelp += "s";
            }
        }
        return new ArrayList<>(Arrays.asList(neutronHelp, electronHelp));
    }

    /**
     * needs to be called on inventory change
     */
    @Override
    public void markDirty() {
        super.markDirty();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /**
     * @return true if an atom can be collected from the plate
     */
    public boolean isCollectable() {
        int protons = getProtonCount();
        int neutrons = getNeutronCount();
        int electrons = getElectronCount();
        NucleusState nucleusState = NuclidesTable.getNuclide(protons, neutrons);
        return protons == electrons && nucleusState != null && nucleusState.getAtomItem() != null && nucleusState.isStable() && protons != 0;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, this.items);
        timer = nbt.getInt("Timer");
    }

    /**
     * inserts one particle of item
     *
     * @param item the item to insert
     * @return success or failure
     */
    public ActionResult insertParticle(Item item) {

        int slot, times_looped = 0;

        if (item == Items.PROTON) {
            slot = 0;
        } else if (item == Items.NEUTRON) {
            slot = 3;
        } else if (item == Items.ELECTRON) {
            slot = 6;
        } else {
            return ActionResult.FAIL;
        }

        while (items.get(slot).getCount() == 64 && items.get(slot).getItem() == item) {
            slot += 1;
            times_looped += 1;
            if (times_looped == 3) return ActionResult.FAIL;
        }
        assert items.get(slot).getCount() < 64;
//            if the inventory is empty initialize the inventory with 0 items
        if (items.get(slot).isEmpty()) {
//                the item that the player was holding
            items.set(slot, new ItemStack(item));
//                set the counter for the item on 0
            items.get(slot).setCount(0);
        }
//            if the stack isn't full
        if (items.get(slot).getCount() < 64) {
//                add 1 to the inventory
            items.get(slot).setCount(items.get(slot).getCount() + 1);
        }
        markDirty();
        return ActionResult.SUCCESS;
    }

    public void scatterParticles() {
        this.scatterParticles(1);
    }

    /**
     * getter for the master/base block of the plate
     *
     * @param world the world to search the master in
     * @return the base block of the plate
     */
   /*
    @Nullable
    public BohrBlockEntity getMaster(World world) {
        if (this.getCachedState().getBlock() instanceof BohrBlock
                && this.getCachedState().get(MinelabsProperties.BOHR_PART) != BohrPart.BASE) {
            BlockPos blockPos = getMasterPos(this.getCachedState(), pos);
            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            BohrPart bohrPart = world.getBlockState(blockPos).get(MinelabsProperties.BOHR_PART);
            if (blockEntity instanceof BohrBlockEntity bohrBlockEntity && bohrPart == BohrPart.BASE) {
                return bohrBlockEntity;
            } else {
                world.removeBlock(pos, false);

//                System.out.println("No base bohr block found");
//                world.breakBlock(getPos(), false);
                return null;
            }
        }
        return this;
    }

    */

    /**
     * Removes a particle (proton, neutron or electron depending on passed param) from the bohrblock.
     *
     * @param item : item particle to be removed.
     */
    public ActionResult removeParticle(Item item) {

        int index;
        if (List.of(Items.ANTI_PROTON, Items.ANTI_NEUTRON, Items.POSITRON).contains(item)) {
            if (item == Items.ANTI_PROTON) {
                index = 0;
                item = Items.PROTON;
            } else if (item == Items.ANTI_NEUTRON) {
                index = 3;
                item = Items.NEUTRON;
            } else if (item == Items.POSITRON) {
                index = 6;
                item = Items.ELECTRON;
            } else return ActionResult.FAIL;
            for (int offset = 0; offset < 3; offset++) {

                if (items.get(index).getCount() > 0 && item == items.get(index + offset).getItem()) {
                    items.get(index + offset).setCount(items.get(index + offset).getCount() - 1);
                    markDirty();
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.FAIL;
    }

    /**
     * scatter the items above the bohrplate
     *
     * @param distance, nr of blocks up
     */
    public void scatterParticles(int distance) {

//        0.5 east and 0.5 south
        ItemScatterer.spawn(Objects.requireNonNull(getWorld()), getPos().up(distance).add(0.5f, 0, 0.5f), items);
        markDirty();
    }


    /**
     * creates and spawns the atom
     *
     * @param world the world of the bohrplate
     * @param pos   the position of the bohrblock
     */
    public void createAtom(World world, BlockPos pos) {

        int protons = getProtonCount();
        int neutrons = getNeutronCount();
        NucleusState nucleusState = NuclidesTable.getNuclide(protons, neutrons);
        if (isCollectable()) {
            assert nucleusState != null;
            ItemStack itemStack = new ItemStack(nucleusState.getAtomItem());
            itemStack.setCount(1);
            DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(1);
            defaultedList.add(itemStack);
            ItemScatterer.spawn(world, pos.up(1).add(0.5f, 0, 0.5f), defaultedList);
            clear();
            markDirty();
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, items);
        nbt.putInt("Timer", timer);
        super.writeNbt(nbt);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        //TODO
        // INSERT INTO CORRECT SLOTS
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        //TODO
        // EXTACTABLE WHEN COLLECTABLE
        return false;
    }
}