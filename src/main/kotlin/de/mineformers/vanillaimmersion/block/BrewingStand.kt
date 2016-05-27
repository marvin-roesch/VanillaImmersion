package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.client.particle.BubbleParticle
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.sync
import de.mineformers.vanillaimmersion.util.Inventories
import de.mineformers.vanillaimmersion.util.Rays
import net.minecraft.block.BlockBrewingStand
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatList
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
class BrewingStand : BlockBrewingStand() {
    companion object {
        val BOTTLE1_AABB = AxisAlignedBB(10.0 * 0.0625, .0, 6 * 0.0625,
                                         14.0 * 0.0625, 12 * 0.0625, 10 * 0.0625)
        val BOTTLE2_AABB = AxisAlignedBB(3.0 * 0.0625, .0, 2 * 0.0625,
                                         7.0 * 0.0625, 12 * 0.0625, 6 * 0.0625)
        val BOTTLE3_AABB = AxisAlignedBB(3.0 * 0.0625, .0, 10 * 0.0625,
                                         7.0 * 0.0625, 12 * 0.0625, 14 * 0.0625)
        val BOWL_AABB = AxisAlignedBB(5.0 * 0.0625, 13.5 * 0.0625, 5 * 0.0625,
                                      11.0 * 0.0625, 15.5 * 0.0625, 11 * 0.0625)
    }

    init {
        setHardness(0.5F)
        setLightLevel(0.125F)
        setCreativeTab(VanillaImmersion.CREATIVE_TAB)
        unlocalizedName = "vimmersion.brewingStand"
        registryName = ResourceLocation(MODID, "brewing_stand")
    }

    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand, stack: ItemStack?, side: EnumFacing,
                                  hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (hand == EnumHand.OFF_HAND)
            return false
        // Check the various boxes of the brewing stand
        val boxes = listOf(
            BOTTLE1_AABB.offset(pos), BOTTLE2_AABB.offset(pos), BOTTLE3_AABB.offset(pos), BOWL_AABB.offset(pos)
        )
        val tile = world.getTileEntity(pos)
        val hit = Rays.rayTraceBoxes(player, boxes)
        if (hit != -1 && !world.isRemote && tile is BrewingStandLogic) {
            // If we can insert into the fuel slot, do it
            val slot =
                if (hit == 3 && tile.canInsertFuel(stack))
                    4
                else
                    hit
            val existing = tile.getStackInSlot(slot)
            if (stack == null && existing != null) {
                // Extract item
                val extracted = tile.inventory.extractItem(slot, Int.MAX_VALUE, false)
                Inventories.insertOrDrop(player, extracted)
                tile.sync()
                player.addStat(StatList.BREWINGSTAND_INTERACTION)
                return true
            } else if (stack != null) {
                // Insert item
                val remaining = tile.inventory.insertItem(slot, stack, false)
                player.setHeldItem(hand, remaining)
                tile.sync()
                player.addStat(StatList.BREWINGSTAND_INTERACTION)
                return true
            }
        }
        return true
    }

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
                                       collidingBoxes: MutableList<AxisAlignedBB>, entity: Entity?) {
        super.addCollisionBoxToList(state, world, pos, mask, collidingBoxes, entity)
        // Add the bowl for correct collisions
        addCollisionBoxToList(pos, mask, collidingBoxes,
                              BOWL_AABB.expand(.0, -0.0625 * 0.5, .0).offset(.0, -0.0625 * 0.5, .0))
    }

    @Deprecated("Vanilla")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = FULL_BLOCK_AABB

    override fun createNewTileEntity(world: World, meta: Int) = BrewingStandLogic() // Return our own implementation
}