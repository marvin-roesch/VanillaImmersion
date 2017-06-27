package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.util.spill
import net.minecraft.block.BlockAnvil
import net.minecraft.block.SoundType
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * Immersive Anvil implementation.
 * Derives from the Vanilla anvil to allow substitution later on.
 * Adds a tile entity to the anvil, but this does not break Vanilla compatibility.
 */
open class Anvil : BlockAnvil() {
    init {
        setHardness(5.0F)
        soundType = SoundType.ANVIL
        setResistance(2000.0F)
        unlocalizedName = "anvil"
        registryName = ResourceLocation("minecraft", "anvil")
    }

    /**
     * Handles right clicks for the anvil.
     * Does not do anything since interaction is handled through
     * [RepairHandler][de.mineformers.vanillaimmersion.immersion.RepairHandler].
     */
    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand,
                                  side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = true

    /**
     * Drops the anvil's contents when it's broken.
     */
    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        val tile = world.getTileEntity(pos)
        if (tile is AnvilLogic) {
            tile.inventory.spill(world, pos)
            world.updateComparatorOutputLevel(pos, this)
        }
        super.breakBlock(world, pos, state)
    }

    /**
     * We need to override this since the Vanilla anvil does not have a TileEntity.
     */
    override fun hasTileEntity(state: IBlockState?) = true

    /**
     * Create the anvil's TileEntity.
     */
    override fun createTileEntity(world: World?, state: IBlockState?) = AnvilLogic()
}