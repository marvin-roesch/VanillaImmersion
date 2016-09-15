package de.mineformers.vanillaimmersion.tileentity

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
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
     * The height of the pyramid beneath the beacon, -1 if there is none.
     */
    val levels: Int
        get() = getField(0)
    /**
     * The beacon's primary effect.
     */
    var primaryEffect: Potion?
        set(value) {
            setField(1, Potion.getIdFromPotion(value))
            sync()
        }
        get() = Potion.getPotionById(getField(1))
    /**
     * The beacon's secondary effect.
     */
    var secondaryEffect: Potion?
        set(value) {
            setField(2, Potion.getIdFromPotion(value))
            sync()
        }
        get() = Potion.getPotionById(getField(2))
    /**
     * Determines the beacon's current editing state.
     */
    var state: BeaconState? = null

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

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        val result = super.writeToNBT(compound)
        if (state != null) {
            val state = NBTTagCompound()
            state.setInteger("Stage", this.state!!.stage)
            state.setInteger("Primary", Potion.getIdFromPotion(this.state!!.primary))
            state.setInteger("Secondary", Potion.getIdFromPotion(this.state!!.secondary))
            result.setTag("EditingState", state)
        }
        return result
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        if (compound.hasKey("EditingState")) {
            val state = compound.getCompoundTag("EditingState")
            this.state = BeaconState(Potion.getPotionById(state.getInteger("Primary")),
                                     Potion.getPotionById(state.getInteger("Secondary")),
                                     state.getInteger("Stage"))
        }
    }

    override fun setField(id: Int, value: Int) {
        super.setField(id, value)
        sync()
    }

    data class BeaconState(val primary: Potion? = null, val secondary: Potion? = null, val stage: Int = 1)
}