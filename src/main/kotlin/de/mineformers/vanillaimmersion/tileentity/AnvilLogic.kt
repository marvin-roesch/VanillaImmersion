package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.Items
import de.mineformers.vanillaimmersion.network.AnvilLock
import de.mineformers.vanillaimmersion.util.SelectionBox
import de.mineformers.vanillaimmersion.util.SubSelections
import de.mineformers.vanillaimmersion.util.clear
import de.mineformers.vanillaimmersion.util.insertOrDrop
import de.mineformers.vanillaimmersion.util.selectionBox
import net.minecraft.block.BlockAnvil
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ContainerRepair
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.NonNullList
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.wrapper.RangedWrapper
import java.util.Random
import java.util.UUID

/**
 * Implements all logic and data storage for the anvil.
 */
open class AnvilLogic : TileEntity(), SubSelections {
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

        val OBJECT_SELECTION =
            selectionBox(AxisAlignedBB(.35, 1.005, 0.68125, .65, 1.02, .98125)) {
                leftClicks = false
                slot(Slot.INPUT_OBJECT.ordinal)
                renderOptions {
                    icon = ResourceLocation("textures/items/iron_pickaxe.png")
                }
            }
        val MATERIAL_SELECTION =
            selectionBox(AxisAlignedBB(.35, 1.005, 0.32125, .65, 1.02, .62125)) {
                leftClicks = false
                slot(Slot.INPUT_MATERIAL.ordinal)
                renderOptions {
                    icon = ResourceLocation("textures/items/iron_ingot.png")
                }
            }
        val HAMMER_SELECTION =
            selectionBox(AxisAlignedBB(0.25, 1.005, .07, 0.75, 1.02, .25)) {
                leftClicks = false
                slot(Slot.HAMMER.ordinal)
                renderOptions {
                    icon = ResourceLocation(VanillaImmersion.MODID, "textures/icons/hammer.png")
                    uvs = listOf(
                        Vec3d(.0, .0, .0),
                        Vec3d(.0, .0, .6875),
                        Vec3d(.25, .0, 0.6875),
                        Vec3d(.25, .0, .0)
                    )
                }
            }
        val BLOCK_SELECTION =
            selectionBox(AxisAlignedBB(.125, .0, .0, .875, 1.0, 1.0))
        val SELECTIONS = listOf(OBJECT_SELECTION, MATERIAL_SELECTION, HAMMER_SELECTION, BLOCK_SELECTION)
    }

    /**
     * Extension of default item stack handler to control insertion and extraction from certain slots.
     */
    internal inner class AnvilInventory : ItemStackHandler(3) {
        override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
            if (slot == Slot.HAMMER.ordinal && stack.item != Items.HAMMER)
                return stack
            return super.insertItem(slot, stack, simulate)
        }

        override fun onContentsChanged(slot: Int) {
            // Update the output if the inputs were modified
            if (slot != Slot.HAMMER.ordinal && world?.isRemote == false) {
                hammerCount = 0
                if (slot == Slot.INPUT_OBJECT.ordinal)
                    itemName = null
            }
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
     * The anvil's block state.
     */
    val blockState: IBlockState
        get() = world.getBlockState(pos)
    /**
     * The anvil's orientation.
     */
    val facing: EnumFacing
        get() = blockState.getValue(BlockAnvil.FACING)
    /**
     * The anvil's rotation relative to a north facing.
     */
    val rotation: Rotation
        get() = when (facing) {
            EnumFacing.EAST -> Rotation.CLOCKWISE_90
            EnumFacing.WEST -> Rotation.COUNTERCLOCKWISE_90
            EnumFacing.SOUTH -> Rotation.CLOCKWISE_180
            else -> Rotation.NONE
        }
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
        set(value) {
            field = value
            sync()
        }

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `anvil[slot]`
     */
    operator fun get(slot: Slot): ItemStack = inventory.getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `anvil[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack) = inventory.setStackInSlot(slot.ordinal, stack)

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
        if (!player.world.isRemote && !canInteract(player)) {
            player.sendMessage(TextComponentTranslation("vimmersion.anvil.inUse"))
        }
    }

    override fun onRightClickBox(box: SelectionBox,
                                 player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                                 side: EnumFacing, hitVec: Vec3d): Boolean {
        if (world.isRemote || hand == EnumHand.OFF_HAND)
            return false
        if (box.slot == null) {
            if (side == EnumFacing.EAST &&
                stack.isEmpty &&
                hitVec.y >= 0.625 &&
                !this[Slot.INPUT_OBJECT].isEmpty) {
                // When you hit the front face, try to edit the text
                if (canInteract(player)) {
                    playerLock = player.uniqueID
                    VanillaImmersion.NETWORK.sendTo(AnvilLock.Message(pos), player as EntityPlayerMP)
                } else {
                    sendLockMessage(player)
                }
                return true
            }
            return false
        }
        var usedStack = stack
        // When the player is sneaking, only insert a single item from the stack
        if (!usedStack.isEmpty && player.isSneaking) {
            usedStack = usedStack.copy()
            usedStack.count = 1
        }
        val slot = Slot.values()[box.slot.id]
        val result = interactWithSlot(world, pos, this, player, slot, stack, slot != Slot.HAMMER)
        return result
    }

    /**
     * Handles interaction with a given slot.
     */
    open protected fun interactWithSlot(world: World, pos: BlockPos,
                                        tile: AnvilLogic,
                                        player: EntityPlayer,
                                        slot: Slot,
                                        stack: ItemStack,
                                        blockLocked: Boolean): Boolean {
        // One of the input slots was clicked
        val existing = tile[slot]
        // If there already was an item in there, drop it
        if (stack.isEmpty && !existing.isEmpty) {
            // If the anvil is locked, notify the player
            if (!tile.canInteract(player) && blockLocked) {
                tile.sendLockMessage(player)
                return false
            }
            val extracted = tile.inventory.extractItem(slot.ordinal, Int.MAX_VALUE, false)
            player.insertOrDrop(extracted)
            return true
        } else if (!stack.isEmpty) {
            // If the anvil is locked, notify the player
            if (!tile.canInteract(player) && blockLocked) {
                tile.sendLockMessage(player)
                return false
            }
            // Insert the item
            val remaining = tile.inventory.insertItem(slot.ordinal, stack, false)
            if (remaining != stack)
                world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, SoundEvents.BLOCK_ANVIL_HIT, SoundCategory.BLOCKS, 1f, 1f)
            player.setHeldItem(EnumHand.MAIN_HAND, remaining)
            return true
        }
        return false
    }

    /**
     * Tries to repair an item in an anvil. "Repair" may refer to applying a name or an enchantment as well.
     * Returns an object indicating the success of the operation, if not simulated, the player's experience will be
     * modified.
     */
    open fun tryRepair(world: World, pos: BlockPos, player: EntityPlayer, simulate: Boolean): RepairResult? {
        val tile = world.getTileEntity(pos)
        if (tile !is AnvilLogic || world.isRemote)
            return null
        // Use the Vanilla container to ensure maximum compatibility
        val container = ContainerRepair(player.inventory, world, pos, player)
        val objectSlot = container.getSlot(0)
        val materialSlot = container.getSlot(1)
        val outputSlot = container.getSlot(2)
        objectSlot.putStack(tile[Slot.INPUT_OBJECT])
        materialSlot.putStack(tile[Slot.INPUT_MATERIAL])
        container.updateItemName(tile.itemName)
        val result = outputSlot.stack
        if (outputSlot.stack.isEmpty)
            return null
        if (!simulate) {
            val oldLevel = player.experienceLevel
            outputSlot.onTake(player, result)
            player.experienceLevel = oldLevel
        }
        return RepairResult(objectSlot.stack, materialSlot.stack, result, container.maximumCost)
    }

    /**
     * A simple wrapper around the result of a repair operation.
     */
    data class RepairResult(val input: ItemStack, val material: ItemStack, val output: ItemStack, val requiredLevels: Int)

    override fun onLeftClickBox(box: SelectionBox,
                                player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                                side: EnumFacing, hitVec: Vec3d): Boolean {
        if (hand == EnumHand.OFF_HAND)
            return false
        if (stack.item != Items.HAMMER || side != EnumFacing.UP)
            return false
        hammer(player, stack)
        return true
    }

    /**
     * Tries to hammer the anvil's current input.
     */
    open fun hammer(player: EntityPlayer, stack: ItemStack) {
        if (world.isRemote)
            return
        val simulated = tryRepair(world, pos, player, true)
        // Block interaction when the player does not have the required experience
        if (simulated == null || (player.experienceLevel <= 0 && !player.capabilities.isCreativeMode)) {
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 0.5f, 0.2f)
            return
        }
        val world = this.world as WorldServer
        // If the input is invalid, abort
        hammerCount++
        val rand = Random()
        for (i in 0..5) {
            val dX = rand.nextGaussian() * 0.2
            val dY = rand.nextGaussian() * 0.2
            val dZ = rand.nextGaussian() * 0.2
            val mX = rand.nextGaussian() * 0.002
            val mY = rand.nextGaussian() * 0.002
            val mZ = rand.nextGaussian() * 0.002
            val material = this[Slot.INPUT_MATERIAL]
            val particleStack = if (!material.isEmpty && rand.nextBoolean()) material else this[Slot.INPUT_OBJECT]
            world.spawnParticle(
                EnumParticleTypes.ITEM_CRACK,
                pos.x + 0.5 + dX, pos.y + 1.2 + dY, pos.z + 0.5 + dZ, 1,
                mX, mY, mZ, .0,
                Item.getIdFromItem(particleStack.item), particleStack.metadata
            )
        }
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-1)
        }
        stack.damageItem(1, player)
        if (hammerCount == requiredHammerCount(player) || player.capabilities.isCreativeMode) {
            val output = tryRepair(world, pos, player, false)!!
            this[Slot.INPUT_OBJECT] = output.input
            this[Slot.INPUT_MATERIAL] = output.material
            itemName = null
            hammerCount = 0
            player.insertOrDrop(output.output.copy())
        } else {
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.5f, 1f)
        }
    }

    /**
     * The required amount of hits for the current input to be repaired.
     */
    open fun requiredHammerCount(player: EntityPlayer): Int {
        val result = tryRepair(world, pos, player, true) ?: return Int.MAX_VALUE
        return result.requiredLevels
    }

    override val boxes: List<SelectionBox>
        get() = SELECTIONS.map { it.withRotation(rotation) }

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
        inventory.clear()
        val compound = pkt.nbtCompound
        readFromNBT(compound)
        itemName = compound.getString("ItemName")
        playerLock = if (compound.hasUniqueId("Lock")) compound.getUniqueId("Lock") else null
    }

    /**
     * Checks whether the anvil has a capability attached to the given side.
     * Will definitely return `true` for the item handler capability
     */
    override fun hasCapability(capability: Capability<*>, side: EnumFacing?): Boolean {
        return capability == ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, side)
    }

    /**
     * Gets the instance of a capability for the given side, here specifically the item handler.
     */
    override fun <T> getCapability(capability: Capability<T>, side: EnumFacing?): T? {
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
