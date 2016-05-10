package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.Blocks.FURNACE
import de.mineformers.vanillaimmersion.VanillaImmersion.Blocks.LIT_FURNACE
import de.mineformers.vanillaimmersion.network.FurnaceUpdate
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.BlockFurnace.FACING
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityFurnace

/**
 * ${JDOC}
 */
class FurnaceLogic : TileEntityFurnace() {
    companion object {
        enum class Slot {
            INPUT, FUEL, OUTPUT
        }

        internal var KEEP_INVENTORY = false
    }

    private var fuelLeft = 0
    private var fuel = 0
    private var progress = 0
    private var requiredTime = 0

    override fun isBurning() = fuelLeft > 0

    override fun getField(id: Int): Int =
        when (id) {
            0 -> fuelLeft
            1 -> fuel
            2 -> progress
            3 -> requiredTime
            else -> 0
        }

    override fun setField(id: Int, value: Int) {
        when (id) {
            0 -> fuelLeft = value
            1 -> fuel = value
            2 -> progress = value
            3 -> requiredTime = value
        }
    }

    operator fun get(slot: Slot): ItemStack? = getStackInSlot(slot.ordinal)

    operator fun set(slot: Slot, stack: ItemStack?) = setInventorySlotContents(slot.ordinal, stack)

    private fun canSmelt(): Boolean {
        val input = this[Slot.INPUT]
        val output = this[Slot.OUTPUT]
        if (input == null) {
            return false
        } else {
            val result = FurnaceRecipes.instance().getSmeltingResult(input) ?: return false
            if (output == null) return true
            if (!output.isItemEqual(result)) return false
            val amountSum = output.stackSize + result.stackSize
            return amountSum <= inventoryStackLimit && amountSum <= output.maxStackSize //Forge BugFix: Make it respect stack sizes properly.
        }
    }

    override fun update() {
        if (this.worldObj.isRemote)
            return

        val wasBurning = this.isBurning
        var markDirty = false
        var sync = false

        if (this.isBurning) {
            --this.fuelLeft
            sync = true
        }

        val fuelStack = this[Slot.FUEL]
        val input = this[Slot.INPUT]
        if (!wasBurning && canSmelt()) {
            fuel = getItemBurnTime(fuelStack)
            fuelLeft = fuel
            sync = true

            if (fuelStack != null)
                if (fuelLeft > 0 && --fuelStack.stackSize == 0) {
                    this[Slot.FUEL] = fuelStack.item.getContainerItem(fuelStack)
                    markDirty = true
                }
        }
        if (this.isBurning && canSmelt()) {
            ++this.progress

            if (this.progress == this.requiredTime) {
                this.progress = 0
                this.requiredTime = this.getCookTime(input)
                this.smeltItem()
                markDirty = true
            }
        } else {
            progress = (progress - 2).coerceIn(0, requiredTime)
            sync = true
        }

        if (wasBurning != this.isBurning) {
            updateState()
            markDirty = true
        }

        if (this[Slot.INPUT]?.stackSize == 0) {
            this[Slot.INPUT] = null
            markDirty = true
        }
        if (this[Slot.OUTPUT] != null) {
            markDirty = true
            Inventories.spawn(world, pos, worldObj.getBlockState(pos).getValue(FACING), this[Slot.OUTPUT])
            this[Slot.OUTPUT] = null
        }

        if (markDirty) {
            this.markDirty()
        }
        if (sync || markDirty) {
            sync()
        }
    }

    private fun updateState() {
        val state = worldObj.getBlockState(pos)
        val tile = worldObj.getTileEntity(pos)
        KEEP_INVENTORY = true

        if (isBurning) {
            worldObj.setBlockState(pos, LIT_FURNACE.defaultState.withProperty(FACING, state.getValue(FACING)), 3)
        } else {
            worldObj.setBlockState(pos, FURNACE.defaultState.withProperty(FACING, state.getValue(FACING)), 3)
        }

        KEEP_INVENTORY = false

        if (tile != null) {
            tile.validate()
            worldObj.setTileEntity(pos, tile)
        }
    }

    override fun setInventorySlotContents(index: Int, stack: ItemStack?) {
        val old = getStackInSlot(index)
        val unchanged = stack != null && stack.isItemEqual(old) && ItemStack.areItemStackTagsEqual(stack, old)

        if (index == 0 && !unchanged) {
            this.requiredTime = this.getCookTime(stack)
            this.progress = 0
        }

        super.setInventorySlotContents(index, stack)
        this.markDirty()
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)

        this.fuelLeft = compound.getInteger("BurnTime")
        this.progress = compound.getInteger("CookTime")
        this.requiredTime = compound.getInteger("CookTimeTotal")
        this.fuel = getItemBurnTime(this[Slot.FUEL])
    }

    override fun writeToNBT(compound: NBTTagCompound) {
        super.writeToNBT(compound)

        compound.setInteger("BurnTime", fuelLeft)
        compound.setInteger("CookTime", progress)
        compound.setInteger("CookTimeTotal", requiredTime)
    }

    override fun getDescriptionPacket() = VanillaImmersion.NETWORK.getPacketFrom(FurnaceUpdate.Message(this))
}