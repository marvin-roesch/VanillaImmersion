package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.*
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.wrapper.RangedWrapper

/**
 * Implements all logic and data storage for the anvil.
 */
class CraftingTableLogic : TileEntity() {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The crafting process's result.
             */
            OUTPUT,
            /**
             * The (0|0) slot in the crafting grid.
             */
            IN_TOP_LEFT,
            /**
             * The (1|0) slot in the crafting grid.
             */
            IN_TOP,
            /**
             * The (2|0) slot in the crafting grid.
             */
            IN_TOP_RIGHT,
            /**
             * The (0|1) slot in the crafting grid.
             */
            IN_LEFT,
            /**
             * The (1|1) slot in the crafting grid.
             */
            IN_MIDDLE,
            /**
             * The (2|1) slot in the crafting grid.
             */
            IN_RIGHT,
            /**
             * The (0|2) slot in the crafting grid.
             */
            IN_BOTTOM_LEFT,
            /**
             * The (1|2) slot in the crafting grid.
             */
            IN_BOTTOM,
            /**
             * The (2|2) slot in the crafting grid.
             */
            IN_BOTTOM_RIGHT
        }
    }

    /**
     * Extension of default item stack handler to control insertion and extraction from certain slots.
     */
    internal inner class CraftingTableInventory : ItemStackHandler(10) {
        override fun insertItem(slot: Int, stack: ItemStack?, simulate: Boolean): ItemStack? {
            // No stacks may be inserted into the output slot
            if (slot == Slot.OUTPUT.ordinal)
                return stack
            return super.insertItem(slot, stack, simulate)
        }

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack? {
            if (slot == 0) {
                val extracted = super.extractItem(slot, amount, simulate)
                if (extracted != null && !simulate)
                    CraftingHandler.takeCraftingResult(world, pos, null, extracted)
                return extracted
            }
            return super.extractItem(slot, amount, simulate)
        }

        override fun onContentsChanged(slot: Int) {
            // Sync any inventories to the client
            markDirty()
            sync()
        }

        /**
         * Easy access to the inventory's stacks, for easier iteration.
         */
        val contents: Array<ItemStack?>
            get() = stacks
    }

    /**
     * The crafting table's block state.
     */
    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)
    /**
     * The crafting table's orientation.
     */
    val facing: EnumFacing
        get() = blockState.getValue(CraftingTable.FACING)
    /**
     * The crafting table's inventory.
     */
    internal val inventory = CraftingTableInventory()
    /**
     * A wrapper around the inventory to access the [top ingredient][Slot.IN_TOP] slot.
     */
    private val topInventory by lazy {
        RangedWrapper(inventory, 2, 3)
    }
    /**
     * A wrapper around the inventory to access the [bottom ingredient][Slot.IN_BOTTOM] slot.
     */
    private val bottomInventory by lazy {
        RangedWrapper(inventory, 8, 9)
    }
    /**
     * A wrapper around the inventory to access the [left ingredient][Slot.IN_LEFT] slot.
     */
    private val leftInventory by lazy {
        RangedWrapper(inventory, 4, 5)
    }
    /**
     * A wrapper around the inventory to access the [right ingredient][Slot.IN_RIGHT] slot.
     */
    private val rightInventory by lazy {
        RangedWrapper(inventory, 6, 7)
    }
    /**
     * A wrapper around the inventory to access the [middle ingredient][Slot.IN_MIDDLE] slot.
     */
    private val middleInventory by lazy {
        RangedWrapper(inventory, 5, 6)
    }
    /**
     * A wrapper around the inventory to access the [output][Slot.OUTPUT] slot.
     */
    private val outputInventory by lazy {
        RangedWrapper(inventory, 0, 1)
    }

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `table[slot]`
     */
    operator fun get(slot: Slot): ItemStack? = inventory.getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `table[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack?) = inventory.setStackInSlot(slot.ordinal, stack)

    /**
     * Serializes the crafting table's data to NBT.
     */
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
        return compound
    }

    /**
     * Reads the crafting table's data from NBT.
     */
    override fun readFromNBT(compound: NBTTagCompound?) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound!!.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag() = writeToNBT(NBTTagCompound())

    /**
     * Creates a packet for updates of the tile entity at runtime.
     */
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)

    /**
     * Reads data from the update packet.
     */
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        Inventories.clear(inventory)
        readFromNBT(pkt.nbtCompound)
    }

    /**
     * Checks whether the crafting table has a capability attached to the given side.
     * Will definitely return `true` for the item handler capability
     */
    override fun hasCapability(capability: Capability<*>?, side: EnumFacing?): Boolean {
        return capability == ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side)
    }

    /**
     * Gets the instance of a capability for the given side, here specifically the item handler.
     */
    override fun <T : Any?> getCapability(capability: Capability<T>?, side: EnumFacing?): T {
        @Suppress("UNCHECKED_CAST")
        if (capability == ITEM_HANDLER_CAPABILITY) {
            // Return the appropriate inventory for the vertical sides
            if (side == UP)
                return middleInventory as T
            else if (side == DOWN)
                return outputInventory as T

            // Transform the passed side into one that's relative to the crafting table's orientation
            val relativeSide =
                if (side != null)
                    EnumFacing.getHorizontal((facing.horizontalIndex + 2) % 4 + side.horizontalIndex)
                else
                    null
            // Return the appropriate inventory
            when (relativeSide) {
                NORTH -> return bottomInventory as T
                SOUTH -> return topInventory as T
                WEST -> return rightInventory as T
                EAST -> return leftInventory as T
                null -> return inventory as T
                else -> Unit
            }
        }
        return super.getCapability(capability, side)
    }
}