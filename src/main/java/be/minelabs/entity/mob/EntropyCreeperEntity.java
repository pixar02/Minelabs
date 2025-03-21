package be.minelabs.entity.mob;

import be.minelabs.Minelabs;
import be.minelabs.mixin.ExplosionAccessor;
import be.minelabs.sound.SoundEvents;
import be.minelabs.util.Tags;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.*;
import java.util.function.Predicate;

public class EntropyCreeperEntity extends CreeperEntity {

    // Total amount of ticks the animation runs for.
    private static final int ANIMATION_TICKS = 10;

    // Portion of affected blocks to shuffle
    private static final double SHUFFLE_PERCENTAGE = 0.25;

    private static final int EXPLOSION_RADIUS = 3;

    private final List<BlockPos> blocksToShuffle = new ArrayList<>();
    private final List<LivingEntity> entitiesToShuffle = new ArrayList<>();

    // After it exploded, how many ticks to shuffle for.
    private int ticksToGo = -1;

    public EntropyCreeperEntity(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENTROPY_CREEPER_HURT;
    }

    /**
     * Called due to mixin
     */
    protected void playPrimedSound() {
        this.playSound(SoundEvents.ENTITY_ENTROPY_CREEPER_PRIMED, 1.0f, 1.0f);
    }

    /**
     * Based on Explosion::collectBlocksAndDamageEntities
     * Get all the affected blocks in the explosion radius
     *
     * @param explosion ExplosionAccessor
     * @return Set of {@link BlockPos}
     */
    protected Set<BlockPos> getAffectedBlocks(ExplosionAccessor explosion) {
        Set<BlockPos> blocks = Sets.newHashSet();
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) continue;
                    double d = (float) j / 15.0f * 2.0f - 1.0f;
                    double e = (float) k / 15.0f * 2.0f - 1.0f;
                    double f = (float) l / 15.0f * 2.0f - 1.0f;
                    double g = Math.sqrt(d * d + e * e + f * f);
                    d /= g;
                    e /= g;
                    f /= g;
                    double m = explosion.getX();
                    double n = explosion.getY();
                    double o = explosion.getZ();
                    for (float h = explosion.getPower() * (0.7f + this.world.random.nextFloat() * 0.6f); h > 0.0f; h -= 0.22500001f) {
                        BlockPos blockPos = BlockPos.ofFloored(m, n, o);
                        BlockState blockState = this.world.getBlockState(blockPos);
                        FluidState fluidState = this.world.getFluidState(blockPos);
                        if (!this.world.isInBuildLimit(blockPos)) continue;
                        Optional<Float> optional = explosion.getBehavior().getBlastResistance((Explosion) explosion, this.world, blockPos, blockState, fluidState);
                        if (optional.isPresent()) {
                            h -= (optional.get() + 0.3f) * 0.3f;
                        }
                        if (h > 0.0f && explosion.getBehavior().canDestroyBlock((Explosion) explosion, this.world, blockPos, blockState, h) && isShuffleable(world.getBlockState(blockPos))) {
                            blocks.add(blockPos);
                        }
                        m += d * (double) 0.3f;
                        n += e * (double) 0.3f;
                        o += f * (double) 0.3f;
                    }
                }
            }
        }
        return blocks;
    }

    /**
     * Based on Explosion::collectBlocksAndDamageEntities
     * Get all the affected entities in the explosion radius
     *
     * @param explosion ExplosionAccessor
     * @return List of {@link LivingEntity}
     */
    protected List<LivingEntity> getAffectedEntities(ExplosionAccessor explosion) {
        List<LivingEntity> entities = new ArrayList<>();
        float j = explosion.getPower() * 2.0f;
        int k = MathHelper.floor(explosion.getX() - (double) j - 1.0);
        int l = MathHelper.floor(explosion.getX() + (double) j + 1.0);
        int d = MathHelper.floor(explosion.getY() - (double) j - 1.0);
        int q = MathHelper.floor(explosion.getY() + (double) j + 1.0);
        int e = MathHelper.floor(explosion.getZ() - (double) j - 1.0);
        int r = MathHelper.floor(explosion.getZ() + (double) j + 1.0);
        List<Entity> f = world.getOtherEntities(this, new Box(k, d, e, l, q, r));
        Vec3d vec3d = getPos();

        Predicate<Entity> predicate = EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR;
        for (Entity entity : f) {
            double u;
            double t;
            double s;
            if (entity.isImmuneToExplosion() || !((Math.sqrt(entity.squaredDistanceTo(vec3d)) / (double) j) <= 1.0) || (Math.sqrt((s = entity.getX() - explosion.getX()) * s + (t = (entity instanceof TntEntity ? entity.getY() : entity.getEyeY()) - explosion.getY()) * t + (u = entity.getZ() - explosion.getZ()) * u)) == 0.0)
                continue;
            if (entity instanceof LivingEntity && predicate.test(entity)) {
                entities.add((LivingEntity) entity);
            }
        }
        return entities;
    }

    /**
     * Override Tick()
     * What to do while exploding
     */
    @Override
    public void tick() {
        if (ticksToGo >= 0) {
            if (ticksToGo % 5 == 0) {
                shuffle();
            }
            if (ticksToGo == 0) {
                discard();
            }
            ticksToGo--;
        } else {
            super.tick();
        }
    }

    /**
     * Mixin Injected explosion method
     * Make sure to "kill" the entity
     * <p>
     * Shuffle blocks in the explosion radius around the entity.
     *
     * @return boolean: cancel default explosion or not
     */
    public boolean preExplode() {
        if (!this.world.isClient) {
            dead = true;
            this.setInvulnerable(true);
            setInvisible(true);     // so we can't see it (discard happens after animation)
            // TODO: entity can still be attacked in this state.
            //  Either prevent this or have the entropy creeper spawn an entropy bomb upon explode?

            this.playSound(SoundEvents.ENTITY_ENTROPY_CREEPER_EXPLODE, 1.0f, 1.0f);

            // Use explosion code to determine affected blocks
            Explosion.DestructionType destructionType = this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) ? Explosion.DestructionType.DESTROY : Explosion.DestructionType.KEEP;
            // if creeper is charged
            float multiplier = this.shouldRenderOverlay() ? 2.0f : 1.0f;
            Explosion explosion = new Explosion(this.world, this, null, null, getX(), getY(), getZ(), EXPLOSION_RADIUS * multiplier, false, destructionType);

            // Adapted from the Explosion class
            Set<BlockPos> blockposSet = getAffectedBlocks((ExplosionAccessor) explosion);
            blocksToShuffle.addAll(blockposSet);

            Collection<LivingEntity> entities = getAffectedEntities((ExplosionAccessor) explosion);
            entitiesToShuffle.addAll(entities);

            shuffle();
            ticksToGo = ANIMATION_TICKS - 1;
        }
        return false;  // make sure the original 'explode' function doesn't run.
    }

    /**
     * Actual shuffle of the Entropy Creeper
     * <p>
     * Teleports the Entities (+ Colors sheep in random color)
     * Shuffles the blocks
     */
    private void shuffle() {
        if (this.world.isClient) return;

        // Teleport affected entities
        for (LivingEntity entity : entitiesToShuffle) {
            if (entity != null) {
                if (entity instanceof SheepEntity sheep) {
                    DyeColor old = sheep.getColor();
                    sheep.setColor(DyeColor.byId(random.nextInt(15)));
                    Minelabs.LOGGER.debug(old + " -> " + sheep.getColor());
                }
                BlockPos teleportpos = blocksToShuffle.get(random.nextInt(blocksToShuffle.size()));
                Minelabs.LOGGER.debug("teleport to: " + teleportpos);
                entity.teleport(teleportpos.getX(), teleportpos.getY(), teleportpos.getZ());
            }
        }

        // Shuffle blocks
        Minelabs.LOGGER.debug("size: " + blocksToShuffle.size());
        if (world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            for (int i = 0; i < blocksToShuffle.size() * SHUFFLE_PERCENTAGE; i++) {
                BlockPos pos1 = blocksToShuffle.get(random.nextInt(blocksToShuffle.size()));
                BlockPos pos2 = blocksToShuffle.get(random.nextInt(blocksToShuffle.size()));

                BlockState b1 = world.getBlockState(pos1);
                BlockState b2 = world.getBlockState(pos2);
                if (b1.getPistonBehavior() == PistonBehavior.DESTROY) {
                    world.breakBlock(pos1, true, this);
                    b1 = Blocks.AIR.getDefaultState();
                }
                if (b2.getPistonBehavior() == PistonBehavior.DESTROY) {
                    world.breakBlock(pos2, true, this);
                    b2 = Blocks.AIR.getDefaultState();
                }
                Minelabs.LOGGER.debug(b1 + " <-> " + b2);

                // prepare blockstates for their placement position
                b1 = Block.postProcessState(b1, world, pos2);
                b2 = Block.postProcessState(b2, world, pos1);
                world.setBlockState(pos1, b2, Block.NOTIFY_LISTENERS | Block.MOVED);
                world.setBlockState(pos2, b1, Block.NOTIFY_LISTENERS | Block.MOVED);
            }
            blocksToShuffle.forEach(pos -> world.updateNeighbors(pos, world.getBlockState(pos).getBlock()));
        }
    }

    /**
     * Check if the block is a shuffleable block
     *
     * @param blockState: block
     * @return whether the block should be movable.
     */
    private boolean isShuffleable(BlockState blockState) {
        return blockState.getBlock().getHardness() > -1 &&
                !blockState.isIn(BlockTags.DRAGON_IMMUNE) &&
                !blockState.isIn(Tags.Blocks.ENTROPY_IMMUNE);
    }
}