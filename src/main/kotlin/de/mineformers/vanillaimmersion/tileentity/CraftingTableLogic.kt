package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.network.CraftingTableUpdate
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
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
 * ${JDOC}
 */
class CraftingTableLogic : TileEntity(), ITickable {
    companion object {
        enum class Slot {
            OUTPUT,
            IN_TOP_LEFT, IN_TOP, IN_TOP_RIGHT,
            IN_LEFT, IN_MIDDLE, IN_RIGHT,
            IN_BOTTOM_LEFT, IN_BOTTOM, IN_BOTTOM_RIGHT
        }
    }

    internal inner class CraftingTableInventory : ItemStackHandler(10) {
        override fun insertItem(slot: Int, stack: ItemStack?, simulate: Boolean): ItemStack? {
            if (slot == 0)
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
            markDirty()
            sync()
        }

        val contents: Array<ItemStack?>
            get() = stacks
    }

    internal val inventory = CraftingTableInventory()
    private val topInventory by lazy {
        RangedWrapper(inventory, 2, 3)
    }
    private val bottomInventory by lazy {
        RangedWrapper(inventory, 8, 9)
    }
    private val leftInventory by lazy {
        RangedWrapper(inventory, 4, 5)
    }
    private val rightInventory by lazy {
        RangedWrapper(inventory, 6, 7)
    }
    private val middleInventory by lazy {
        RangedWrapper(inventory, 5, 6)
    }
    private val outputInventory by lazy {
        RangedWrapper(inventory, 0, 1)
    }

    operator fun get(slot: Slot): ItemStack? = inventory.getStackInSlot(slot.ordinal)

    operator fun set(slot: Slot, stack: ItemStack?) = inventory.setStackInSlot(slot.ordinal, stack)

    override fun update() {
        var markDirty = false
        for (i in inventory.contents.indices) {
            if (inventory.getStackInSlot(i)?.stackSize == 0) {
                inventory.setStackInSlot(i, null)
                markDirty = true
            }
        }
        if (markDirty) {
            markDirty()
            sync()
        }
    }

    override fun writeToNBT(compound: NBTTagCompound?) {
        super.writeToNBT(compound)
        compound!!.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
    }

    override fun readFromNBT(compound: NBTTagCompound?) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound!!.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
    }

    override fun hasCapability(capability: Capability<*>?, side: EnumFacing?): Boolean {
        return capability == ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>?, side: EnumFacing?): T {
        @Suppress("UNCHECKED_CAST")
        if (capability == ITEM_HANDLER_CAPABILITY) {
            if (side == UP)
                return middleInventory as T
            else if (side == DOWN)
                return outputInventory as T

            val relativeSide =
                if (side != null)
                    EnumFacing.getHorizontal((facing.horizontalIndex + 2) % 4 + side.horizontalIndex)
                else
                    null
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

    val facing: EnumFacing
        get() = blockState.getValue(CraftingTable.FACING)

    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)

    override fun getDescriptionPacket() = VanillaImmersion.NETWORK.getPacketFrom(CraftingTableUpdate.Message(this))
}