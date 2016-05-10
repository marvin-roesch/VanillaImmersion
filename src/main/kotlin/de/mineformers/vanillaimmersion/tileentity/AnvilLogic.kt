package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.network.AnvilUpdate
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import java.util.*

/**
 * ${JDOC}
 */
class AnvilLogic : TileEntity(), ITickable {
    companion object {
        enum class Slot {
            INPUT_OBJECT, INPUT_MATERIAL, OUTPUT
        }
    }

    internal inner class CraftingTableInventory : ItemStackHandler(10) {
        override fun insertItem(slot: Int, stack: ItemStack?, simulate: Boolean): ItemStack? {
            if (slot == Slot.OUTPUT.ordinal)
                return stack
            if (getStackInSlot(Slot.OUTPUT.ordinal) != null)
                return stack
            return super.insertItem(slot, stack, simulate)
        }

        override fun onContentsChanged(slot: Int) {
            markDirty()
            sync()
        }

        val contents: Array<ItemStack?>
            get() = stacks
    }

    internal val inventory = CraftingTableInventory()
    var playerLock: UUID? = null
        set(value) {
            field = value
            sync()
        }
    var currentName: String? = null
        set(value) {
            field = value
            sync()
        }

    operator fun get(slot: Slot): ItemStack? = inventory.getStackInSlot(slot.ordinal)

    operator fun set(slot: Slot, stack: ItemStack?) = inventory.setStackInSlot(slot.ordinal, stack)

    fun canInteract(player: EntityPlayer) =
        if (playerLock == null)
            true
        else
            player.uniqueID == playerLock

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
        if (capability == ITEM_HANDLER_CAPABILITY && side == null) {
            return inventory as T
        }
        return super.getCapability(capability, side)
    }

    val facing: EnumFacing
        get() = blockState.getValue(CraftingTable.FACING)

    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)

    override fun getDescriptionPacket() = VanillaImmersion.NETWORK.getPacketFrom(AnvilUpdate.Message(this))
}