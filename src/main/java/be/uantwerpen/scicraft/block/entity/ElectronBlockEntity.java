package be.uantwerpen.scicraft.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


public class ElectronBlockEntity extends BlockEntity {
    private double charge = -1.0;
    private double field_x = 0.0;
    private double field_y = 0.0;
    private double field_z = 0.0;
    private boolean update_next_tick = false;
    public long time = 0;
    public boolean is_moving = false;
    public int mark_delete;

    public ElectronBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.ELECTRON_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound tag) {
        // Save the current value of the number to the tag
        tag.putDouble("ex", field_x);
        tag.putDouble("ey", field_y);
        tag.putDouble("ez", field_z);
        tag.putDouble("q", charge);
        tag.putBoolean("ut", update_next_tick);
        tag.putBoolean("is", is_moving);
        tag.putLong("time", time);
        tag.putInt("md", mark_delete);
        super.writeNbt(tag);
    }

    // Deserialize the BlockEntity
    @Override
    public void readNbt(NbtCompound tag) {
        super.readNbt(tag);
        field_x = tag.getDouble("ex");
        field_y = tag.getDouble("ey");
        field_z = tag.getDouble("ez");
        charge = tag.getDouble("q");
        update_next_tick = tag.getBoolean("ut");
        is_moving = tag.getBoolean("is");
        time = tag.getLong("time");
        mark_delete = tag.getInt("md");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public static void tick(World world, BlockPos pos, BlockState state, ElectronBlockEntity be) {
        if (be.update_next_tick && !be.is_moving) {
            NbtCompound my_nbt = be.createNbt();
            my_nbt.putBoolean("ut", false);
            my_nbt.putBoolean("is", true);
            my_nbt.putLong("time", world.getTime());
            be.readNbt(my_nbt);
        } else if (be.mark_delete != 0) {
            int x_new = Math.abs(be.mark_delete) == 1 ?  Math.abs(be.mark_delete) / be.mark_delete : 0;
            int y_new = Math.abs(be.mark_delete) == 2 ?  Math.abs(be.mark_delete) / be.mark_delete : 0;
            int z_new = Math.abs(be.mark_delete) == 3 ?  Math.abs(be.mark_delete) / be.mark_delete : 0;
            be.world.removeBlockEntity(be.pos);
            be.markRemoved();
            BlockState blockState = Block.postProcessState(be.getCachedState().getBlock().getDefaultState(), be.world, be.pos);
            be.world.setBlockState(be.pos.add(x_new, y_new, z_new), blockState, Block.NOTIFY_ALL);
            be.world.updateNeighbor(be.pos, blockState.getBlock(), be.pos);
        }
    }
}
