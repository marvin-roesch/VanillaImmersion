package de.mineformers.vanillaimmersion.tileentity

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.wrapper.InvWrapper

/**
 * Implements all logic and data storage for the beacon.
 */
class BeaconLogic : TileEntityBeacon() {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The item used to pay for the effect.
             */
            PAYMENT
        }
    }

    /**
     * The beacon's block state.
     */
    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)
    /**
     * The beacon's inventory.
     */
    val inventory: IItemHandlerModifiable = InvWrapper(this)

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `beacon[slot]`
     */
    operator fun get(slot: Slot): ItemStack? = getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `beacon[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack?) = setInventorySlotContents(slot.ordinal, stack)

    override fun getRenderBoundingBox(): AxisAlignedBB = INFINITE_EXTENT_AABB
}