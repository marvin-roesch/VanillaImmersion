package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.client.particle.BubbleParticle
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import net.minecraft.block.BlockBrewingStand
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntityBrewingStand
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import java.util.*

/**
 * Immersive Brewing Stand implementation.
 * Derives from the Vanilla brewing stand to allow substitution later on.
 */
open class BrewingStand : BlockBrewingStand() {
    companion object {
        val BOWL_AABB = AxisAlignedBB(5.0 * 0.0625, 13.5 * 0.0625, 5 * 0.0625,
                                      11.0 * 0.0625, 15.5 * 0.0625, 11 * 0.0625)
    }

    init {
        setHardness(0.5F)
        setLightLevel(0.125F)
        unlocalizedName = "brewingStand"
        registryName = ResourceLocation("minecraft", "brewing_stand")
    }

    /**
     * Handles right clicks for the brewing stand.
     */
    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand,
                                  side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = false

    /**
     * Triggers whenever an entity collides with this block.
     */
    override fun onEntityCollidedWithBlock(world: World, pos: BlockPos, state: IBlockState, entity: Entity) {
        if (entity is EntityItem) {
            (world.getTileEntity(pos) as BrewingStandLogic).onItemCollision(entity)
        }
    }

    override fun randomDisplayTick(state: IBlockState, world: World, pos: BlockPos, rand: Random) {
        val tile = world.getTileEntity(pos)
        if (tile is TileEntityBrewingStand) {
            // If brewing is in progress, spawn bubbles out of all "tubes"
            if (tile.getField(0) > 0) {
                val x = pos.x + 0.5
                val y = pos.y + 1.0
                val z = pos.z + 0.5
                if (tile.getStackInSlot(0) != null)
                    Minecraft.getMinecraft().effectRenderer.addEffect(
                        BubbleParticle(world, x + 4.5 * 0.0625, y, z, .0, .0, .0))
                if (tile.getStackInSlot(1) != null)
                    Minecraft.getMinecraft().effectRenderer.addEffect(
                        BubbleParticle(world, x - 3.25 * 0.0625, y, z - 3.25 * 0.0625, .0, .0, .0))
                if (tile.getStackInSlot(2) != null)
                    Minecraft.getMinecraft().effectRenderer.addEffect(
                        BubbleParticle(world, x - 3.25 * 0.0625, y, z + 3.25 * 0.0625, .0, .0, .0))
            }
            // If there is fuel, smoke a little bit
            if (tile.getField(1) > 0) {
                val x = pos.x + 0.4 + rand.nextFloat() * 0.2
                val y = pos.y + 0.7 + rand.nextFloat() * 0.3
                val z = pos.z + 0.4 + rand.nextFloat() * 0.2
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x, y, z, 0.0, 0.0, 0.0)
            }
        }
    }

    @Deprecated("Vanilla")
    override fun addCollisionBoxToList(state: IBlockState, world: World, pos: BlockPos, mask: AxisAlignedBB,
                                       collidingBoxes: MutableList<AxisAlignedBB>, entity: Entity?, isActualState: Boolean) {
        super.addCollisionBoxToList(state, world, pos, mask, collidingBoxes, entity, isActualState)
        // Add the bowl for correct collisions
        addCollisionBoxToList(pos, mask, collidingBoxes,
                              BOWL_AABB.grow(.0, -0.0625 * 0.5, .0).offset(.0, -0.0625 * 0.5, .0))
    }

    @Deprecated("Vanilla")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = FULL_BLOCK_AABB

    override fun createNewTileEntity(world: World, meta: Int) = BrewingStandLogic() // Return our own implementation
}