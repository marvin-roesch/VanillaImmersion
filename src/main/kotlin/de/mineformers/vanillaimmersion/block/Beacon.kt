package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import net.minecraft.block.BlockBeacon
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

/**
 * Immersive Beacon implementation.
 * Derives from the Vanilla beacon to allow substitution later on.
 */
class Beacon : BlockBeacon() {
    companion object {
        val EDITING_STAGE = PropertyInteger.create("editing_stage", 0, 2)!!
    }

    init {
        setHardness(3F)
        unlocalizedName = "$MODID.beacon"
        setCreativeTab(VanillaImmersion.CREATIVE_TAB)
        registryName = ResourceLocation(MODID, "beacon")
        setLightLevel(1F)
        defaultState = blockState.baseState.withProperty(EDITING_STAGE, 0)
    }

    @Deprecated("Vanilla")
    override fun getStateFromMeta(meta: Int) = defaultState!!

    @Deprecated("Vanilla")
    override fun getMetaFromState(state: IBlockState) = 0

    override fun createNewTileEntity(worldIn: World, meta: Int) = BeaconLogic() // Return our own implementation

    override fun createBlockState() = BlockStateContainer(this, EDITING_STAGE)

    @Deprecated("Vanilla")
    override fun getActualState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState {
        val tile = world.getTileEntity(pos)
        if (tile is BeaconLogic) {
            return state.withProperty(EDITING_STAGE, tile.state?.stage ?: 0)
        }
        return state
    }
}