package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.util.*
import net.minecraft.block.BlockFurnace
import net.minecraft.block.BlockFurnace.FACING
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.FurnaceRecipes
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.stats.StatList
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.wrapper.InvWrapper

/**
 * Implements all logic and data storage for the furnace.
 */
open class FurnaceLogic : TileEntityFurnace(), SubSelections {
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

        val INPUT_SELECTION =
            selectionBox(AxisAlignedBB(3 * 0.0625, 9 * 0.0625, 0.0625,
                                       13 * 0.0625, 12 * 0.0625, 13 * 0.0625).shrink(0.004)) {
                slot(Slot.INPUT.ordinal) {
                    renderFilled = true
                }

                renderOptions()
            }
        val FUEL_SELECTION =
            selectionBox(AxisAlignedBB(3 * 0.0625, 0.0625, 0.0625,
                                       13 * 0.0625, 5 * 0.0625, 13 * 0.0625).shrink(0.004)) {
                slot(Slot.FUEL.ordinal) {
                    renderFilled = true
                }

                renderOptions()
            }
        val SELECTIONS = listOf(INPUT_SELECTION, FUEL_SELECTION)
    }

    /**
     * The furnace's block state.
     */
    val blockState: IBlockState
        get() = world.getBlockState(pos)
    /**
     * The furnace's orientation.
     */
    val facing: EnumFacing
        get() = blockState.getValue(BlockFurnace.FACING)
    /**
     * The furnace's rotation relative to a north facing.
     */
    val rotation: Rotation
        get() = when (facing) {
            EnumFacing.EAST -> Rotation.CLOCKWISE_90
            EnumFacing.WEST -> Rotation.COUNTERCLOCKWISE_90
            EnumFacing.SOUTH -> Rotation.CLOCKWISE_180
            else -> Rotation.NONE
        }
    /**
     * The furnace's inventory.
     */
    val inventory: IItemHandlerModifiable = InvWrapper(this)
    /**
     * Amount of "heat" left from the current fuel item.
     */
    protected var fuelLeft: Int
        get() = getField(0)
        set(value) = setField(0, value)
    /**
     * Amount of "heat" provided by the current fuel item.
     */
    protected var fuel: Int
        get() = getField(1)
        set(value) = setField(1, value)
    /**
     * Progress of the current smelting process.
     */
    protected var progress: Int
        get() = getField(2)
        set(value) = setField(2, value)
    /**
     * Time in ticks required to smelt this item.
     */
    protected var requiredTime: Int
        get() = getField(3)
        set(value) = setField(3, value)

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `furnace[slot]`
     */
    operator fun get(slot: Slot): ItemStack = getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `furnace[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack) = setInventorySlotContents(slot.ordinal, stack)

    override val boxes: List<SelectionBox>
        get() = SELECTIONS.map { it.withRotation(rotation) }

    override fun onRightClickBox(box: SelectionBox, player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                                 side: EnumFacing, hitVec: Vec3d): Boolean {
        // When clicking the front, insert or extract items from the furnace
        if (side == EnumFacing.NORTH && hand == EnumHand.MAIN_HAND) {
            if (world.isRemote)
                return true
            val slot = Slot.values()[box.slot!!.id]
            val existing = this[slot]
            if (stack.isEmpty && !existing.isEmpty) {
                // Extract item
                val extracted = inventory.extractItem(slot.ordinal, Int.MAX_VALUE, false)
                player.insertOrDrop(extracted)
                sync()
                player.addStat(StatList.FURNACE_INTERACTION)
                return true
            } else if (!stack.isEmpty) {
                // Insert item
                val remaining =
                    if (player.isSneaking) {
                        // Insert all when sneaking
                        inventory.insertItem(slot.ordinal, stack, false)
                    } else {
                        val single = stack.copy()
                        single.count = 1
                        // Only insert one by default
                        val consumed = inventory.insertItem(slot.ordinal, single, false).isEmpty
                        if (consumed)
                            stack.shrink(1)
                        stack
                    }
                player.setHeldItem(hand, remaining)
                sync()
                player.addStat(StatList.FURNACE_INTERACTION)
                return true
            }
        }
        return false
    }

    /**
     * Checks whether the current input item can be smelted, i.e. the result fits into the output slot.
     */
    open protected fun canSmelt(): Boolean {
        val input = this[Slot.INPUT]
        val output = this[Slot.OUTPUT]
        // If there is no input, there is nothing to smelt
        if (input.isEmpty) {
            return false
        } else {
            val result = FurnaceRecipes.instance().getSmeltingResult(input) ?: return false
            // If there currently is no output, the item can definitely be smelted
            if (output.isEmpty) {
                return true
            }
            // If the current output is different from the new result, the input can't be smelted
            if (!output.isItemEqual(result)) {
                return false
            }
            val amountSum = output.count + result.count
            return amountSum <= inventoryStackLimit && amountSum <= output.maxStackSize //Forge BugFix: Make it respect stack sizes properly.
        }
    }

    /**
     * Update the furnace's smelting process.
     */
    override fun update() {
        // We may only do logic on the server
        if (this.world.isRemote)
            return

        val wasBurning = this.isBurning
        var markDirty = false
        var sync = false

        // Reduce the amount of fuel whenever the furnace is burning
        if (this.isBurning) {
            reduceFuel()
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
            if (!fuelStack.isEmpty)
                if (fuelLeft > 0 && --fuelStack.count == 0) {
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
        if (!this[Slot.OUTPUT].isEmpty) {
            markDirty = true
            world.spawn(pos, world.getBlockState(pos).getValue(FACING), this[Slot.OUTPUT])
            this[Slot.OUTPUT] = ItemStack.EMPTY
        }

        if (markDirty) {
            this.markDirty()
        }
        if (sync || markDirty) {
            sync()
        }
    }

    /**
     * Changes the current fuel level according to the implementation's rules.
     */
    open protected fun reduceFuel() {
        --this.fuelLeft
    }

    /**
     * Updates the furnace block according to its burning state.
     */
    private fun updateState() {
        BlockFurnace.setState(isBurning, world, pos)
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag() = writeToNBT(NBTTagCompound())

    /**
     * Creates a packet for updates of the tile entity at runtime.
     */
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        this.clear()
        readFromNBT(pkt.nbtCompound)
    }

    /**
     * Reads data from the update packet.
     */
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)
}
