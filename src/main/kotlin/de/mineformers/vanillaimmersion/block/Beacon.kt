package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import net.minecraft.block.BlockBeacon
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
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
        val EDITING_STAGE = PropertyInteger.create("editing_stage", 0, 3)!!
    }

    init {
        setHardness(3F)
        unlocalizedName = "$MODID.beacon"
        setCreativeTab(VanillaImmersion.CREATIVE_TAB)
        registryName = ResourceLocation(MODID, "beacon")
        setLightLevel(1F)
        defaultState = blockState.baseState.withProperty(EDITING_STAGE, 0)
    }

    /**
     * Handles beacon edit mode switching.
     */
    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand, stack: ItemStack?,
                                  side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile is BeaconLogic && !world.isRemote && hand == EnumHand.MAIN_HAND) {
            if (tile.state == null && stack == null) {
                // When right clicked with an empty hand, initiate editing
                val primary = tile.primaryEffect
                val secondary = tile.secondaryEffect
                val available = tile.availableEffects(false)
                tile.state = BeaconLogic.BeaconState(if (primary in available) primary else null, secondary)
            } else if (tile.state != null) {
                val beacon = tile.state!!
                if (beacon.stage != 3 && beacon.primary != null) {
                    // In the non-final stages, in- or decrease the stage accordingly
                    // Only allow the second stage to be displayed if the beacon has more than 3 levels
                    if (beacon.stage == 1 && tile.levels > 3)
                        tile.state = beacon.copy(secondary =
                                                 if (beacon.secondary == beacon.primary ||
                                                     beacon.secondary == MobEffects.REGENERATION)
                                                     beacon.secondary
                                                 else
                                                     null,
                                                 stage = 2)
                    else
                        tile.state = beacon.copy(stage = if(player.isSneaking && beacon.stage == 2) 1 else 3)
                } else {
                    // If the player is sneaking and we're in the last stage,
                    // change the stage rather than completing the editing
                    if (player.isSneaking && beacon.stage == 3) {
                        tile.state = beacon.copy(stage = if(tile.levels > 3) 2 else 1)
                        return true
                    }
                    // If there was an effect selected, check whether the player has the required payment or
                    // whether he is in creative mode
                    if (beacon.primary != null) {
                        if ((stack == null || !stack.item.isBeaconPayment(stack)) &&
                            !player.capabilities.isCreativeMode)
                            return true
                    }
                    // Update effects and stop editing
                    tile.primaryEffect = beacon.primary
                    tile.secondaryEffect = if (beacon.primary != null) beacon.secondary else null
                    tile.state = null
                    // Take away the payment item
                    if (stack != null && !player.capabilities.isCreativeMode) {
                        stack.stackSize--
                        if (stack.stackSize == 0)
                            player.setHeldItem(hand, null)
                    }
                }

            }
        }
        return true
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