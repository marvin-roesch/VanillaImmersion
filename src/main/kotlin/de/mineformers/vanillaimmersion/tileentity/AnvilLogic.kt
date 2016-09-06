package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.immersion.RepairHandler
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.BlockAnvil
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraft.util.text.TextComponentTranslation
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.wrapper.RangedWrapper
import java.util.*

/**
 * Implements all logic and data storage for the anvil.
 */
class AnvilLogic : TileEntity() {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The object to be repaired.
             */
            INPUT_OBJECT,
            /**
             * Whatever material is used for the repairing. May also be an enchanted book etc.
             */
            INPUT_MATERIAL,
            /**
             * The hammer stored by the anvil.
             */
            HAMMER
        }
    }

    /**
     * Extension of default item stack handler to control insertion and extraction from certain slots.
     */
    internal inner class AnvilInventory : ItemStackHandler(3) {
        override fun onContentsChanged(slot: Int) {
            // Update the output if the inputs were modified
            if (slot != Slot.HAMMER.ordinal && world?.isRemote == false) {
                hammerCount = 0
            }
            // Sync any inventory changes to the client
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
     * The anvil's block state.
     */
    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)
    /**
     * The anvil's orientation.
     */
    val facing: EnumFacing
        get() = blockState.getValue(BlockAnvil.FACING)
    /**
     * The anvil's inventory.
     */
    internal val inventory = AnvilInventory()
    /**
     * A wrapper around the inventory to access the [object][Slot.INPUT_OBJECT] slot.
     */
    private val objectInventory by lazy {
        RangedWrapper(inventory, 0, 1)
    }
    /**
     * A wrapper around the inventory to access the [material][Slot.INPUT_MATERIAL] slot.
     */
    private val materialInventory by lazy {
        RangedWrapper(inventory, 1, 2)
    }
    /**
     * The UUID of the player who has acquired a lock on this anvil.
     * `null` if there is no such player.
     */
    var playerLock: UUID? = null
        set(value) {
            field = value
            sync()
        }
    /**
     * The name for the input object.
     * `null` or empty string if there was no name entered.
     */
    var itemName: String? = null
        set(value) {
            field = value
            sync()
        }
    /**
     * The amount of times the current input was hammered already.
     */
    var hammerCount = 0

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `anvil[slot]`
     */
    operator fun get(slot: Slot): ItemStack? = inventory.getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `anvil[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack?) = inventory.setStackInSlot(slot.ordinal, stack)

    /**
     * Checks whether a player is allowed to interact with this anvil,
     * i.e. if there is no lock or if they have acquired it.
     */
    fun canInteract(player: EntityPlayer) =
        if (playerLock == null)
            true
        else
            player.uniqueID == playerLock

    /**
     * Notifies the player if they may not interact with this anvil.
     */
    fun sendLockMessage(player: EntityPlayer) {
        // Only do this on the server since this might be called from common code
        if (!player.worldObj.isRemote && !canInteract(player)) {
            player.addChatMessage(TextComponentTranslation("vimmersion.anvil.inUse"))
        }
    }

    /**
     * Tries to hammer the anvil's current input.
     */
    fun hammer(player: EntityPlayer) {
        // If the input is invalid, abort
        RepairHandler.tryRepair(world, pos, player, true) ?: return
        hammerCount++
        if (hammerCount == 3 || player.capabilities.isCreativeMode) {
            val output = RepairHandler.tryRepair(world, pos, player, false)!!
            this[Slot.INPUT_OBJECT] = output.input
            this[Slot.INPUT_MATERIAL] = output.material
            itemName = null
            hammerCount = 0
            Inventories.insertOrDrop(player, output.output?.copy())
        } else {
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 1f)
        }
    }

    /**
     * Determines the amount of times inputs must be hit with a hammer for a successful repair.
     */
    fun requiredHammerCount(input: ItemStack): Int {
        // 3rd degree polynomial fitted to Vanilla tool materials
        // Produces the following mapping based on tool material:
        // Gold -> 2 hits
        // Wood -> 3 hits
        // Stone -> 4 hits
        // Iron -> 6 hits
        // Diamond -> 8 hits
        fun polynomial(x: Double) = Math.round(6.046774959e-9 * x * x * x
                                               - 2.157463514e-5 * x * x
                                               + 2.314246515e-2 * x
                                               + 1.445764646).toInt()

        val item = input.item
        return when (item) {
            is ItemTool -> polynomial(item.toolMaterial.maxUses.toDouble())
            is ItemSword -> polynomial(Item.ToolMaterial.valueOf(item.toolMaterialName).maxUses.toDouble())
            else -> 3
        }
    }

    /**
     * Serializes the anvil's data to NBT.
     */
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
        compound.setInteger("HammerCount", hammerCount)
        return compound
    }

    /**
     * Reads the anvil's data from NBT.
     */
    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
        hammerCount = compound.getInteger("HammerCount")
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag(): NBTTagCompound? {
        val compound = writeToNBT(NBTTagCompound())
        if (itemName != null)
            compound.setString("ItemName", itemName)
        if (playerLock != null)
            compound.setUniqueId("Lock", playerLock)
        return compound
    }

    /**
     * Creates a packet for updates of the tile entity at runtime.
     */
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)

    /**
     * Reads data from the update packet.
     */
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

    /**
     * Checks whether the anvil has a capability attached to the given side.
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
            // Objects may be directly inserted into the left side of the anvil (when looking at its front)
            if (side == facing.opposite)
                return objectInventory as T
            // Materials may be directly inserted into the backside of the anvil (when looking at its front)
            if (side == facing.rotateYCCW())
                return materialInventory as T
            // Otherwise, allow insertion/extraction from all slots
            return inventory as T
        }
        return super.getCapability(capability, side)
    }
}