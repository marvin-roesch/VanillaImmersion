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
        set(value) {
            field = value
            sync()
        }

    /**
     * Determines all available effects for the current levels of the beacon.
     */
    fun availableEffects(secondary: Boolean): List<Potion> {
        if (!secondary) {
            return EFFECTS_LIST.take(Math.min(levels, 3)).flatMap { it.toList() }
        } else {
            return if (state == null || state!!.primary == null)
                emptyList()
            else
                EFFECTS_LIST.drop(3).flatMap { it.toList() } + state!!.primary!!
        }
    }

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

    fun onScroll(direction: Int) {
        if (state == null || direction == 0)
            return
        val state = this.state!!
        val secondary = state.stage == 2
        val current = if (secondary) state.secondary else state.primary
        val available = availableEffects(secondary)
        val currentIndex = available.indexOf(current)
        val signum = if(direction > 0) 1 else -1
        when {
            currentIndex == 0 && direction < 0 ->
                if (secondary)
                    this.state = state.copy(secondary = null)
                else
                    this.state = state.copy(primary = null, secondary = null)
            currentIndex >= 0 || signum == 1 -> {
                val new = available[Math.max(0, Math.min(currentIndex + signum, available.size - 1))]
                if (secondary)
                    this.state = state.copy(secondary = new)
                else
                    this.state = state.copy(primary = new, secondary = null)
            }
        }
    }

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
        } else {
            this.state = null
        }
        if (worldObj != null && worldObj.isRemote) {
            worldObj.markBlockRangeForRenderUpdate(pos, pos)
        }
    }

    override fun setField(id: Int, value: Int) {
        super.setField(id, value)
        sync()
    }

    data class BeaconState(val primary: Potion? = null, val secondary: Potion? = null, val stage: Int = 1)
}