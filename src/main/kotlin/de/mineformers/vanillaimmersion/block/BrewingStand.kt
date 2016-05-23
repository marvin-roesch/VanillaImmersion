package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.util.Rays
import net.minecraft.block.BlockBrewingStand
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
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
                                  player: EntityPlayer, hand: EnumHand, heldItem: ItemStack?, side: EnumFacing,
                                  hitX: Float, hitY: Float, hitZ: Float): Boolean {
        val boxes = listOf(
            AxisAlignedBB(pos.x + 2.0 * 0.0625, pos.y.toDouble(), pos.z + 0.0625,
                          pos.x + 8.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 7 * 0.0625),
            AxisAlignedBB(pos.x + 2.0 * 0.0625, pos.y.toDouble(), pos.z + 9 * 0.0625,
                          pos.x + 8.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 15 * 0.0625),
            AxisAlignedBB(pos.x + 9.0 * 0.0625, pos.y.toDouble(), pos.z + 5 * 0.0625,
                          pos.x + 15.0 * 0.0625, pos.y + 12 * 0.0625, pos.z + 11 * 0.0625)
        )
        println(Rays.rayTraceBoxes(player, boxes))
        return true
    }

    @Deprecated("Override")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos) = FULL_BLOCK_AABB
}