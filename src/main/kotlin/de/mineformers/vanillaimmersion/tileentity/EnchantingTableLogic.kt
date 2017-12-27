package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.client.particle.EnchantingParticle
import de.mineformers.vanillaimmersion.config.Configuration
import de.mineformers.vanillaimmersion.util.Rendering
import de.mineformers.vanillaimmersion.util.clear
import de.mineformers.vanillaimmersion.util.insertOrDrop
import de.mineformers.vanillaimmersion.util.plus
import de.mineformers.vanillaimmersion.util.spawn
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ContainerEnchantment
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntityEnchantmentTable
import net.minecraft.util.EnumFacing
import net.minecraft.util.NonNullList
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldServer
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.oredict.OreDictionary

/**
 * Implements all logic and data storage for the enchantment table.
 */
open class EnchantingTableLogic : TileEntityEnchantmentTable() {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The object to be enchanted.
             */
            OBJECT,
            /**
             * The modifiers (i.e. lapis lazuli) for the enchantment process.
             */
            MODIFIERS
        }
    }

    open inner class EnchantingTableInventory : ItemStackHandler(2) {
        /**
         * Reference to all variants of lapis lazuli.
         */
        val lapis = OreDictionary.getOres("gemLapis")

        override fun getStackLimit(slot: Int, stack: ItemStack) =
            when {
                slot == Slot.OBJECT.ordinal -> 1 // The input slot may only take 1 item
                lapis.any { OreDictionary.itemMatches(it, stack, false) } -> 3 // The modifiers slot may only take 3 "lapis lazuli"-like items
                else -> 0
            }

        override fun onContentsChanged(slot: Int) {
            // Update the output if the inputs were modified
            if (world is WorldServer)
                updateEnchantment(FakePlayerFactory.getMinecraft(world as WorldServer))
            // Sync any inventory changes to the client
            markDirty()
            sync()
        }

        /**
         * Easy access to the inventory's stacks, for easier iteration.
         */
        val contents: NonNullList<ItemStack>
            get() = stacks
    }

    /**
     * The enchantment table's inventory.
     */
    open val inventory = EnchantingTableInventory()
    /**
     * An array of the required XP levels for the options available.
     */
    val requiredLevels = intArrayOf(0, 0, 0)
    /**
     * An array of guaranteed enchantments among the options available.
     */
    val enchantmentIds = intArrayOf(-1, -1, -1)
    /**
     * An array of levels for the guaranteed enchantments.
     */
    val enchantmentLevels = intArrayOf(-1, -1, -1)
    /**
     * The current XP seed, used for deciding the enchantments.
     */
    var xpSeed = 0L
    /**
     * The currently active (left) page.
     * `-1` if no enchantable item is stored.
     */
    var page = -1
    /**
     * The progress of an active enchantment process.
     */
    var progress = 0
    /**
     * The amount of modifiers consumed in an enchantment.
     */
    var consumedModifiers = 0
    /**
     * The result of enchanting an item.
     */
    var result: ItemStack = ItemStack.EMPTY
    /**
     * The last tick count (+ partial ticks) before the enchanting process was started.
     * Required for rendering.
     */
    var bobStop = 0f

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `table[slot]`
     */
    operator fun get(slot: Slot): ItemStack = inventory.getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `table[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack) = inventory.setStackInSlot(slot.ordinal, stack)

    /**
     * Updates the enchantment table's animation etc.
     */
    override fun update() {
        var markDirty = false
        // Let Vanilla updates it animation
        super.update()
        // Reset the page flipping since we want the book to stay perfectly open for our text to be visible.
        pageFlip = 0f
        pageFlipPrev = 0f
        flipA = 0f
        flipT = 0f

        // If there is a result, update the process
        if (!result.isEmpty) {
            // As soon as the client knows about the result, stop bobbing
            if (progress == 0 && world.isRemote) {
                // Super client-safe code, believe me ;)
                bobStop = tickCount + Rendering.partialTicks
            }
            progress++
            // Start spawning particles between second 2 and 4 of the process
            if ((40..80).contains(progress) && world.isRemote) {
                // Same here, very safe code
                val p = Vec3d(Math.random(), 0.75, Math.random()) + pos
                val d = Vec3d(0.5, 1.75, 0.5) + pos
                val entity = EnchantingParticle(world, p.x, p.y, p.z, d.x, d.y, d.z)
                Minecraft.getMinecraft().effectRenderer.addEffect(entity)
            }
            // Once the process is finished, drop the result and clean up all affected fields
            if (progress > 120 && !world.isRemote) {
                world.spawn(pos, EnumFacing.UP, result)
                this[Slot.OBJECT] = ItemStack.EMPTY
                inventory.extractItem(Slot.MODIFIERS.ordinal, consumedModifiers, false)
                consumedModifiers = 0
                progress = 0
                bobStop = 0f
                result = ItemStack.EMPTY
            }
            markDirty = true
        }

        // Drop all items in the table if there is no enchanting in progress and there's no player nearby (configurable)
        if (Configuration.getBoolean("blocks.enchantment-table.drop-items")
            && result.isEmpty && bookSpread <= 0.0 && !world.isRemote) {
            dropContents()
            markDirty = true
        }

        // Save data and synchronize with clients if required
        if (markDirty) {
            markDirty()
            sync()
        }
    }

    open fun dropContents() {
        for (i in inventory.contents.indices) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                world.spawn(pos, EnumFacing.UP, inventory.extractItem(i, Int.MAX_VALUE, false))
            }
        }
    }

    /**
     * Updates the enchantment data according to the stored items.
     */
    open fun updateEnchantment(player: EntityPlayer) {
        // Use Vanilla container for maximum compatibility
        val container = ContainerEnchantment(player.inventory, world, pos)
        container.tableInventory.setInventorySlotContents(0, this[Slot.OBJECT].copy())
        container.tableInventory.setInventorySlotContents(1, this[Slot.MODIFIERS].copy())
        for (i in requiredLevels.indices) {
            requiredLevels[i] = container.enchantLevels[i]
            enchantmentIds[i] = container.enchantClue[i]
            enchantmentLevels[i] = container.worldClue[i]
        }
        xpSeed = container.xpSeed.toLong()
        // If there are no enchantments which require levels, there is no need to enchant
        if (requiredLevels.any { it > 0 })
            page = 0
        else
            page = -1
        markDirty()
        sync()
    }

    /**
     * Tries to start an enchanting process.
     */
    open protected fun tryEnchantment(player: EntityPlayer, enchantment: Int) {
        val container = ContainerEnchantment(player.inventory, world, pos)
        container.tableInventory.setInventorySlotContents(0, this[Slot.OBJECT].copy())
        container.tableInventory.setInventorySlotContents(1, this[Slot.MODIFIERS].copy())
        // Only if Vanilla allows the enchantment, we do
        if (container.enchantItem(player, enchantment)) {
            // Set up all variables for process and initialize it
            result = container.tableInventory.getStackInSlot(0)
            progress = 0
            val oldModifiers = this[Slot.MODIFIERS].count
            val modifiers = container.tableInventory.getStackInSlot(1).count
            consumedModifiers = oldModifiers - modifiers
            // Notify clients about the change
            markDirty()
            sync()
        }
    }

    /**
     * Implements all interaction for the different pages.
     */
    fun performPageAction(player: EntityPlayer, page: Int, x: Double, y: Double) {
        // Block interaction while enchanting is in progress
        if (!result.isEmpty)
            return
        var sync = false
        // Magic numbers, yes, but this covers "button" clicks on all pages
        if ((page % 2 == 0 && x >= 17 && y >= 98 && x <= 87 && y <= 118)
            || (page % 2 == 1 && x >= 7 && y >= 98 && x <= 77 && y <= 118)) {
            // Page 0 contains the "Cancel" button
            if (page == 0) {
                // Drop all items and update the enchantment data
                for (i in 0 until inventory.slots) {
                    val extracted = inventory.extractItem(i, Int.MAX_VALUE, false)
                    player.insertOrDrop(extracted)
                }
                updateEnchantment(player)
            } else {
                // All other pages contain an "Enchant" button, hence enchant
                tryEnchantment(player, page - 1)
            }
        } else if ((x <= 47 || x <= 16 && y >= 98 && y <= 118) && page == 2) {
            // Handle the left page turn button
            this.page = Math.max(this.page - 2, 0)
            world.playSound(null, pos, VanillaImmersion.Sounds.ENCHANTING_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1f)
            sync = true
        } else if ((x >= 47 || x >= 78 && y >= 98 && y <= 118) && page == 1) {
            // Handle the right page turn button
            this.page = Math.min(this.page + 2, 2)
            world.playSound(null, pos, VanillaImmersion.Sounds.ENCHANTING_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1f)
            sync = true
        }
        if (sync)
            sync()
    }

    /**
     * Serializes the enchantment table's data to NBT.
     */
    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
        return compound
    }

    /**
     * Reads the enchantment table's data from NBT.
     */
    override fun readFromNBT(compound: NBTTagCompound) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag(): NBTTagCompound? {
        val compound = writeToNBT(NBTTagCompound())
        compound.setIntArray("RequiredLevels", requiredLevels)
        compound.setIntArray("Enchantments", enchantmentIds)
        compound.setIntArray("EnchantmentLevels", enchantmentLevels)
        compound.setLong("XPSeed", xpSeed)
        compound.setInteger("Page", page)
        compound.setInteger("Progress", progress)
        if (!result.isEmpty) {
            compound.setTag("Result", result.writeToNBT(NBTTagCompound()))
        }
        compound.setInteger("ConsumedModifiers", consumedModifiers)
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
        inventory.clear()
        val compound = pkt.nbtCompound
        readFromNBT(compound)
        val requiredLevels = compound.getIntArray("RequiredLevels")
        for (i in requiredLevels.indices) {
            this.requiredLevels[i] = requiredLevels[i]
        }
        val enchantmentIds = compound.getIntArray("Enchantments")
        for (i in enchantmentIds.indices) {
            this.enchantmentIds[i] = enchantmentIds[i]
        }
        val enchantmentLevels = compound.getIntArray("EnchantmentLevels")
        for (i in enchantmentLevels.indices) {
            this.enchantmentLevels[i] = enchantmentLevels[i]
        }
        xpSeed = compound.getLong("XPSeed")
        page = compound.getInteger("Page")
        progress = compound.getInteger("Progress")
        result =
            if (compound.hasKey("Result"))
                ItemStack(compound.getCompoundTag("Result"))
            else
                ItemStack.EMPTY
        consumedModifiers = compound.getInteger("ConsumedModifiers")
    }

    /**
     * Checks whether the enchantment table has a capability attached to the given side.
     * Will definitely return `true` for the item handler capability
     */
    override fun hasCapability(capability: Capability<*>, side: EnumFacing?): Boolean {
        return (result.isEmpty && capability == ITEM_HANDLER_CAPABILITY) || super.hasCapability(capability, side)
    }

    /**
     * Gets the instance of a capability for the given side, here specifically the item handler.
     */
    override fun <T : Any?> getCapability(capability: Capability<T>, side: EnumFacing?): T? {
        @Suppress("UNCHECKED_CAST")
        if (capability == ITEM_HANDLER_CAPABILITY) {
            return inventory as T
        }
        return super.getCapability(capability, side)
    }
}