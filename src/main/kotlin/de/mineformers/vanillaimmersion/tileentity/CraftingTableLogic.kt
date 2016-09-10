package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.util.Inventories
import de.mineformers.vanillaimmersion.util.SelectionBox
import de.mineformers.vanillaimmersion.util.SubSelections
import de.mineformers.vanillaimmersion.util.selectionBox
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.*
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.*
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.wrapper.RangedWrapper

/**
 * Implements all logic and data storage for the anvil.
 */
class CraftingTableLogic : TileEntity(), SubSelections {
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

        val SELECTIONS by lazy {
            val builder = mutableListOf<SelectionBox>()
            for (x in 0..3)
                for (y in 0..2) {
                    if (x == 3 && y != 1)
                        continue
                    builder.add(
                        selectionBox(AxisAlignedBB((13 - x * 3) * .0625, .8751, (12 - y * 3) * .0625,
                                                   (13 - x * 3 - 2) * .0625, .89, (12 - y * 3 - 2) * .0625)
                                         .contract(0.004)) {
                            rightClicks = false
                            leftClicks = false

                            slot(if(x == 3) 0 else 1 + x + y * 3)

                            renderOptions {
                                hoverColor = Vec3d(.1, .1, .1)
                            }
                        })
                }
            builder.toList()
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
            // Items may only be extracted from the output slot if the recipe does not require any player data etc.
            if (slot == Slot.OUTPUT.ordinal) {
                // Simulate the extraction first to get the likely result
                val extracted = super.extractItem(slot, amount, true)
                // Check the "craftability" again and indicate failure if it was unsuccessful
                if (!CraftingHandler.takeCraftingResult(world, pos, null, extracted, simulate))
                    return null
            }
            return super.extractItem(slot, amount, simulate)
        }

        override fun onContentsChanged(slot: Int) {
            // Update the crafting result if the output slot was affected (i.e. the output was extracted)
            if (slot == Slot.OUTPUT.ordinal) {
                CraftingHandler.craft(worldObj, pos, null)
            }
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
     * The crafting table's rotation relative to a north facing.
     */
    val rotation: Rotation
        get() = when (facing) {
            EnumFacing.EAST -> Rotation.CLOCKWISE_90
            EnumFacing.WEST -> Rotation.COUNTERCLOCKWISE_90
            EnumFacing.SOUTH -> Rotation.CLOCKWISE_180
            else -> Rotation.NONE
        }
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

    inner class CraftingTableContainer(player: EntityPlayer, simulate: Boolean) :
        ContainerWorkbench(player.inventory, worldObj, pos) {
        init {
            if (!simulate) {
                craftMatrix = object : InventoryCrafting(this, 3, 3) {
                    override fun getStackInSlot(index: Int) =
                        this@CraftingTableLogic.inventory.getStackInSlot(index + 1)

                    override fun setInventorySlotContents(index: Int, stack: ItemStack?) {
                        this@CraftingTableLogic.inventory.setStackInSlot(index + 1, stack)
                        onCraftMatrixChanged(this)
                    }

                    override fun removeStackFromSlot(index: Int): ItemStack? {
                        val result = this@CraftingTableLogic.inventory.extractItem(index + 1, Integer.MAX_VALUE, false)
                        onCraftMatrixChanged(this)
                        return result
                    }

                    override fun decrStackSize(index: Int, count: Int): ItemStack? {
                        val result = this@CraftingTableLogic.inventory.extractItem(index + 1, count, false)
                        onCraftMatrixChanged(this)
                        return result
                    }

                    override fun clear() {
                        for (i in 1..sizeInventory)
                            this@CraftingTableLogic.inventory.setStackInSlot(i, null)
                    }
                }
                craftResult = object : InventoryCraftResult() {
                    override fun getStackInSlot(index: Int) =
                        this@CraftingTableLogic.inventory.getStackInSlot(0)

                    override fun setInventorySlotContents(index: Int, stack: ItemStack?) {
                        this@CraftingTableLogic.inventory.contents[0] = stack
                        this@CraftingTableLogic.markDirty()
                        sync()
                    }

                    override fun removeStackFromSlot(index: Int): ItemStack? {
                        val result = this@CraftingTableLogic.inventory.getStackInSlot(0)
                        this@CraftingTableLogic.inventory.contents[0] = null
                        this@CraftingTableLogic.markDirty()
                        sync()
                        return result
                    }

                    override fun decrStackSize(index: Int, count: Int) =
                        removeStackFromSlot(0)

                    override fun clear() {
                        removeStackFromSlot(0)
                    }
                }
                inventorySlots.clear()
                inventoryItemStacks.clear()
                this.addSlotToContainer(SlotCrafting(player, this.craftMatrix, this.craftResult, 0, 124, 35))

                for (y in 0..2) {
                    for (x in 0..2) {
                        this.addSlotToContainer(Slot(this.craftMatrix, x + y * 3, 30 + x * 18, 17 + y * 18))
                    }
                }

                for (row in 0..2) {
                    for (col in 0..8) {
                        this.addSlotToContainer(Slot(player.inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
                    }
                }

                for (slot in 0..8) {
                    this.addSlotToContainer(Slot(player.inventory, slot, 8 + slot * 18, 142))
                }
                onCraftMatrixChanged(craftMatrix)
            }
        }

        override fun onContainerClosed(player: EntityPlayer) {
            val inventory = player.inventory

            if (inventory.itemStack != null) {
                player.dropItem(inventory.itemStack, false)
                inventory.itemStack = null
            }
        }

        override fun canInteractWith(playerIn: EntityPlayer) = true
    }

    fun createContainer(player: EntityPlayer, simulate: Boolean): ContainerWorkbench =
        CraftingTableContainer(player, simulate)

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

    override val boxes: List<SelectionBox>
        get() = SELECTIONS.map { it.withRotation(rotation) }

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