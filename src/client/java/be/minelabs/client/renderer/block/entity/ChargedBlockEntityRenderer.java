package be.minelabs.client.renderer.block.entity;

import be.minelabs.block.entity.AnimatedChargedBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class ChargedBlockEntityRenderer<T extends AnimatedChargedBlockEntity> implements BlockEntityRenderer<T> {
	
    private final Context context;

	public ChargedBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    	this.context = ctx;
    }

	@Override
	public void render(T blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
		World world = blockEntity.getWorld();
		matrices.push();
		double offset = 0;
		if (blockEntity.time != 0) {
			double time_fraction = Math.max(0, Math.min(1, (blockEntity.getWorld().getTime() + tickDelta - blockEntity.time) / AnimatedChargedBlockEntity.time_move_ticks));
			if (blockEntity.annihilation) {
				offset = .5 * time_fraction * time_fraction;
			} else {
				offset = time_fraction < 0.5 ? 2 * time_fraction * time_fraction : 2 * time_fraction * (-time_fraction + 2) - 1;
			}
		}
		//System.out.println(blockEntity.annihilation);
		//System.out.println(offset);
		if (!(blockEntity.annihilation && offset ==.5)) {
			matrices.translate(blockEntity.movement_direction.getVector().getX() * offset, blockEntity.movement_direction.getVector().getY() * offset, blockEntity.movement_direction.getVector().getZ() * offset);
			BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
			blockRenderManager.getModelRenderer().render(
					world,
					blockRenderManager.getModel(blockEntity.render_state),
					blockEntity.render_state,
					blockEntity.getPos(),
					matrices,
					vertexConsumerProvider.getBuffer(RenderLayers.getMovingBlockLayer(blockEntity.render_state)),
					false, net.minecraft.util.math.random.Random.create(),
					blockEntity.render_state.getRenderingSeed(blockEntity.getPos()),
					OverlayTexture.DEFAULT_UV);
		}
    	matrices.pop();
	}
}
