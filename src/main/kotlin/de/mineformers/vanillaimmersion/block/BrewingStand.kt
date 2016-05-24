package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.sync
import de.mineformers.vanillaimmersion.util.Inventories
import de.mineformers.vanillaimmersion.util.Rays
import net.minecraft.block.BlockBrewingStand
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatList
import net.minecraft.tileentity.TileEntityBrewingStand
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

/**
 * Immersive Brewing Stand implementation.
 * Derives from the Vanilla brewing stand to allow substitution later on.
 */
class BrewingStand : BlockBrewingStand() {
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
        val boxes = listOf(
            AxisAlignedBB(pos.x + 10.0 * 0.0625, pos.y.toDouble(), pos.z + 6 * 0.0625,
                          pos.x + 14.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 10 * 0.0625),
            AxisAlignedBB(pos.x + 3.0 * 0.0625, pos.y.toDouble(), pos.z + 2 * 0.0625,
                          pos.x + 7.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 6 * 0.0625),
            AxisAlignedBB(pos.x + 3.0 * 0.0625, pos.y.toDouble(), pos.z + 10 * 0.0625,
                          pos.x + 7.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 14 * 0.0625)
        )
        val tile = world.getTileEntity(pos)
        val hit = Rays.rayTraceBoxes(player, boxes)
        if (hit != -1 && !world.isRemote && tile is TileEntityBrewingStand) {
            val existing = tile.getStackInSlot(hit)
            if (stack == null && existing != null) {
                // Extract item
                Inventories.insertOrDrop(player, existing)
                tile.setInventorySlotContents(hit, null)
                tile.sync()
                player.addStat(StatList.BREWINGSTAND_INTERACTION)
                return true
            } else if (stack != null && tile.isItemValidForSlot(hit, stack)) {
                // Insert item
                tile.setInventorySlotContents(hit, Inventories.merge(stack, existing))
                tile.sync()
                player.addStat(StatList.BREWINGSTAND_INTERACTION)
                return true
            }
        }
        return true
    }

    @Deprecated("Vanilla")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = FULL_BLOCK_AABB

    override fun createNewTileEntity(world: World, meta: Int) = BrewingStandLogic() // Return our own implementation
}