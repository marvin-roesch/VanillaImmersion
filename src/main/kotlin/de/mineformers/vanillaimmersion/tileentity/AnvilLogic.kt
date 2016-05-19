package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
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
    var itemName: String? = null
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

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
        return compound
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
    }

    override fun getUpdateTag(): NBTTagCompound? {
        val compound = writeToNBT(NBTTagCompound())
        if (itemName != null)
            compound.setString("ItemName", itemName)
        if (playerLock != null)
            compound.setUniqueId("Lock", playerLock)
        return compound
    }

    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        Inventories.clear(inventory)
        val compound = pkt.nbtCompound
        readFromNBT(compound)
        itemName = compound.getString("ItemName")
        playerLock =
            if (compound.hasUniqueId("Lock"))
                compound.getUniqueId("Lock")
            else
                null
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
}