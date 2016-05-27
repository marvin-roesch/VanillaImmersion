package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion.Blocks.FURNACE
import de.mineformers.vanillaimmersion.VanillaImmersion.Blocks.LIT_FURNACE
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.BlockFurnace
import net.minecraft.block.BlockFurnace.FACING
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.EnumFacing

/**
 * Implements all logic and data storage for the furnace.
 */
class FurnaceLogic : TileEntityFurnace() {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The item to smelt.
             */
            INPUT,
            /**
             * The fuel for powering the smelting.
             */
            FUEL,
            /**
             * The result of the smelting.
             */
            OUTPUT
        }

        /**
         * Copied from Vanilla, notifies the block that it should keep its inventory when broken.
         * Required for swapping of lit and unlit furnace.
         */
        internal var KEEP_INVENTORY = false
    }

    /**
     * The furnace's block state.
     */
    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)
    /**
     * The furnace's orientation.
     */
    val facing: EnumFacing
        get() = blockState.getValue(BlockFurnace.FACING)
    /**
     * Amount of "heat" left from the current fuel item.
     */
    private var fuelLeft: Int
        get() = getField(0)
        set(value) = setField(0, value)
    /**
     * Amount of "heat" provided by the current fuel item.
     */
    private var fuel: Int
        get() = getField(1)
        set(value) = setField(1, value)
    /**
     * Progress of the current smelting process.
     */
    private var progress: Int
        get() = getField(2)
        set(value) = setField(2, value)
    /**
     * Time in ticks required to smelt this item.
     */
    private var requiredTime: Int
        get() = getField(3)
        set(value) = setField(3, value)

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `furnace[slot]`
     */
    operator fun get(slot: Slot): ItemStack? = getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `furnace[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack?) = setInventorySlotContents(slot.ordinal, stack)

    /**
     * Checks whether the current input item can be smelted, i.e. the result fits into the output slot.
     */
    private fun canSmelt(): Boolean {
        val input = this[Slot.INPUT]
        val output = this[Slot.OUTPUT]
        // If there is no input, there is nothing to smelt
        if (input == null) {
            return false
        } else {
            val result = FurnaceRecipes.instance().getSmeltingResult(input) ?: return false
            // If there currently is no output, the item can definitely be smelted
            if (output == null) {
                return true
            }
            // If the current output is different from the new result, the input can't be smelted
            if (!output.isItemEqual(result)) {
                return false
            }
            val amountSum = output.stackSize + result.stackSize
            return amountSum <= inventoryStackLimit && amountSum <= output.maxStackSize //Forge BugFix: Make it respect stack sizes properly.
        }
    }

    /**
     * Update the furnace's smelting process.
     */
    override fun update() {
        // We may only do logic on the server
        if (this.worldObj.isRemote)
            return

        val wasBurning = this.isBurning
        var markDirty = false
        var sync = false

        // Reduce the amount of fuel whenever the furnace is burning
        if (this.isBurning) {
            --this.fuelLeft
            sync = true
        }

        val fuelStack = this[Slot.FUEL]
        val input = this[Slot.INPUT]
        // If the furnace is not burning yet but there is a smeltable item, start burning
        if (!wasBurning && canSmelt()) {
            fuel = getItemBurnTime(fuelStack)
            fuelLeft = fuel
            sync = true

            // Reduce the fuel stack's size, replacing it with the container item if necessary
            if (fuelStack != null)
                if (fuelLeft > 0 && --fuelStack.stackSize == 0) {
                    this[Slot.FUEL] = fuelStack.item.getContainerItem(fuelStack)
                    markDirty = true
                }
        }
        // If we are already burning and the item can still be smelted
        if (this.isBurning && canSmelt()) {
            ++this.progress

            // Once the progress hits the required time, actually smelt the item and try to smelt the next one
            if (this.progress == this.requiredTime) {
                this.progress = 0
                this.requiredTime = this.getCookTime(input)
                this.smeltItem()
                markDirty = true
            }
        } else {
            // If we aren't burning or can't smelt the item, slowly decrease the progress until it hits 0
            val oldProgress = progress
            progress = (progress - 2).coerceIn(0, requiredTime)
            sync = sync || oldProgress != progress
        }

        // If we are burning now but weren't beforehand or vice-versa, change our state to (un)lit
        if (wasBurning != this.isBurning) {
            updateState()
            markDirty = true
        }

        // If there is an item in the output slot, directly drop it to the ground in front of the furnace
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

    /**
     * Updates the furnace block according to its burning state.
     */
    private fun updateState() {
        val state = worldObj.getBlockState(pos)
        val tile = worldObj.getTileEntity(pos)

        // Vanilla hacks
        KEEP_INVENTORY = true
        // Swap between lit and unlit variant
        if (isBurning) {
            worldObj.setBlockState(pos, LIT_FURNACE.defaultState.withProperty(FACING, state.getValue(FACING)), 3)
        } else {
            worldObj.setBlockState(pos, FURNACE.defaultState.withProperty(FACING, state.getValue(FACING)), 3)
        }
        KEEP_INVENTORY = false

        // More Vanilla hacks
        if (tile != null) {
            tile.validate()
            worldObj.setTileEntity(pos, tile)
        }
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag() = writeToNBT(NBTTagCompound())

    /**
     * Creates a packet for updates of the tile entity at runtime.
     */
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        Inventories.clear(this)
        readFromNBT(pkt.nbtCompound)
    }

    /**
     * Reads data from the update packet.
     */
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)
}