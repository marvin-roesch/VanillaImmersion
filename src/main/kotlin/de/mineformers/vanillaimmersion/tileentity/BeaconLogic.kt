package de.mineformers.vanillaimmersion.tileentity

import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.util.math.AxisAlignedBB

/**
 * Implements all logic and data storage for the beacon.
 */
open class BeaconLogic : TileEntityBeacon() {
    /**
     * The beacon's block state.
     */
    val blockState: IBlockState
        get() = world.getBlockState(pos)
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
    open fun availableEffects(secondary: Boolean): List<Potion> {
        return when {
            !secondary -> EFFECTS_LIST.take(Math.min(levels, 3)).flatMap { it.toList() }
            state == null || state!!.primary == null -> emptyList()
            else -> EFFECTS_LIST.drop(3).flatMap { it.toList() } + state!!.primary!!
        }
    }

    /**
     * Implements scrolling behaviour for the beacon while it is in edit mode.
     */
    open fun onScroll(direction: Int) {
        if (state == null || direction == 0)
            return
        val state = this.state!!
        val secondary = state.stage == 2
        val current = if (secondary) state.secondary else state.primary
        val available = availableEffects(secondary)
        if (available.isEmpty())
            return
        val currentIndex = available.indexOf(current)
        // We're only concerned with the sign of the scrolling, not its magnitude
        val signum = if (direction > 0) 1 else -1
        when {
            currentIndex == 0 && direction < 0 ->
                // When the active selection is the first non-null one and scrolling down, set the selection to null
                if (secondary)
                    this.state = state.copy(secondary = null)
                else
                    this.state = state.copy(primary = null, secondary = null)
            currentIndex >= 0 || signum == 1 -> {
                // Only allow scrolling in either direction if the active selection is not null
                val new = available[Math.max(0, Math.min(currentIndex + signum, available.size - 1))]
                if (secondary)
                    this.state = state.copy(secondary = new)
                else
                    this.state = state.copy(primary = new, secondary = null)
            }
        }
    }

    override fun updateBeacon() {
        val oldLevels = levels
        super.updateBeacon()
        // Cancel editing if the beacon's completeness state might have been affected
        if (oldLevels != levels)
            state = null
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        val result = super.writeToNBT(compound)
        // Write the editing state to NBT if in edit mode
        if (state != null) {
            val state = NBTTagCompound()
            state.setInteger("Stage", this.state!!.stage)
            state.setInteger("Primary", Potion.getIdFromPotion(this.state!!.primary!!))
            state.setInteger("Secondary", Potion.getIdFromPotion(this.state!!.secondary!!))
            result.setTag("EditingState", state)
        }
        return result
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        if (compound.hasKey("EditingState")) {
            val state = compound.getCompoundTag("EditingState")
            this.state = BeaconState(
                Potion.getPotionById(state.getInteger("Primary")),
                Potion.getPotionById(state.getInteger("Secondary")),
                state.getInteger("Stage")
            )
        } else {
            this.state = null
        }
        // Since we're inheriting from the Vanilla implementation, rendering updates must be issued here
        if (world != null && world.isRemote) {
            world.markBlockRangeForRenderUpdate(pos, pos)
        }
    }

    override fun setField(id: Int, value: Int) {
        super.setField(id, value)
        // Synchronise changes to all fields
        sync()
    }

    /**
     * Wrapper around the beacon's editing state.
     */
    data class BeaconState(val primary: Potion? = null, val secondary: Potion? = null, val stage: Int = 1)
}