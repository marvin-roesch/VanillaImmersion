package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.block.BrewingStand
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.stats.StatList
import net.minecraft.tileentity.TileEntityBrewingStand
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.wrapper.InvWrapper

/**
 * Implements all logic and data storage for the brewing stand.
 */
open class BrewingStandLogic : TileEntityBrewingStand(), SubSelections {
    companion object {
        /**
         * Helper enum for meaningful interaction with the inventory.
         */
        enum class Slot {
            /**
             * The first bottle to be modified.
             */
            BOTTLE1,
            /**
             * The second bottle to be modified.
             */
            BOTTLE2,
            /**
             * The third bottle to be modified.
             */
            BOTTLE3,
            /**
             * The ingredient to be infused into each bottle.
             */
            INPUT_INGREDIENT,
            /**
             * Blaze powder used to power the brewing.
             */
            INPUT_POWDER
        }

        val BOTTLE1_SELECTION =
            selectionBox(AxisAlignedBB(10.0 * 0.0625, .0, 6 * 0.0625,
                                       14.0 * 0.0625, 12 * 0.0625, 10 * 0.0625)) {
                slot(Slot.BOTTLE1.ordinal) {
                    renderFilled = true
                }

                renderOptions {
                    hoveredOnly = true
                }
            }
        val BOTTLE2_SELECTION =
            selectionBox(AxisAlignedBB(3.0 * 0.0625, .0, 2 * 0.0625,
                                       7.0 * 0.0625, 12 * 0.0625, 6 * 0.0625)) {
                slot(Slot.BOTTLE2.ordinal) {
                    renderFilled = true
                }

                renderOptions {
                    hoveredOnly = true
                }
            }
        val BOTTLE3_SELECTION =
            selectionBox(AxisAlignedBB(3.0 * 0.0625, .0, 10 * 0.0625,
                                       7.0 * 0.0625, 12 * 0.0625, 14 * 0.0625)) {
                slot(Slot.BOTTLE3.ordinal) {
                    renderFilled = true
                }

                renderOptions {
                    hoveredOnly = true
                }
            }
        val BOWL_SELECTION =
            selectionBox(AxisAlignedBB(5.0 * 0.0625, 13.5 * 0.0625, 5 * 0.0625,
                                       11.0 * 0.0625, 15.5 * 0.0625, 11 * 0.0625)) {
                slot(Slot.INPUT_INGREDIENT.ordinal) {
                    renderFilled = true
                }

                renderOptions {
                    hoveredOnly = true
                }
            }
        val ROD_SELECTION =
            selectionBox(AxisAlignedBB(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625)) {
                rightClicks = false
            }
        val SELECTIONS = listOf(BOTTLE1_SELECTION, BOTTLE2_SELECTION, BOTTLE3_SELECTION, BOWL_SELECTION, ROD_SELECTION)
    }

    /**
     * The brewing stand's block state.
     */
    val blockState: IBlockState
        get() = worldObj.getBlockState(pos)
    /**
     * The brewing stand's inventory.
     */
    val inventory: IItemHandlerModifiable = InvWrapper(this)

    /**
     * Gets the ItemStack in a given slot.
     * Marked as operator to allow this: `stand[slot]`
     */
    operator fun get(slot: Slot): ItemStack? = getStackInSlot(slot.ordinal)

    /**
     * Sets the ItemStack in a given slot.
     * Marked as operator to allow this: `stand[slot] = stack`
     */
    operator fun set(slot: Slot, stack: ItemStack?) = setInventorySlotContents(slot.ordinal, stack)

    override val boxes = SELECTIONS

    override fun cancelsVanillaSelectionRendering() = true

    override fun onRightClickBox(box: SelectionBox, player: EntityPlayer, hand: EnumHand, stack: ItemStack?,
                                 side: EnumFacing, hitVec: Vec3d): Boolean {
        if (hand == EnumHand.OFF_HAND || box == ROD_SELECTION)
            return false
        if (world.isRemote)
            return true
        // If we can insert into the fuel slot, do it
        val slot =
            if (box == BOWL_SELECTION && canInsertFuel(stack))
                4
            else
                box.slot!!.id
        val existing = getStackInSlot(slot)
        if (stack == null && existing != null) {
            // Extract item
            val extracted = inventory.extractItem(slot, Int.MAX_VALUE, false)
            Inventories.insertOrDrop(player, extracted)
            sync()
            player.addStat(StatList.BREWINGSTAND_INTERACTION)
            return true
        } else if (stack != null) {
            // Insert item
            val remaining =
                if (player.isSneaking) {
                    // Insert all when sneaking
                    inventory.insertItem(slot, stack, false)
                } else {
                    val single = stack.copy()
                    single.stackSize = 1
                    // Only insert one by default
                    val consumed = inventory.insertItem(slot, single, false) == null
                    if (consumed)
                        stack.stackSize--
                    stack
                }
            player.setHeldItem(hand, remaining)
            sync()
            player.addStat(StatList.BREWINGSTAND_INTERACTION)
            return true
        }
        return true
    }

    /**
     * Checks whether a given item stack may be inserted as fuel into this brewing stand.
     */
    fun canInsertFuel(stack: ItemStack?): Boolean {
        // Only actual fuel can be inserted, obviously
        if (stack == null || !isItemValidForSlot(4, stack))
            return false
        val existingIngredient = get(Slot.INPUT_INGREDIENT)
        val existingFuel = get(Slot.INPUT_POWDER)
        // Prefer the ingredient slot, if the stack is a valid ingredient
        if (existingIngredient == null && isItemValidForSlot(3, stack))
            return false
        // Prefer the ingredient slot, if it still has space for the stack
        if (existingIngredient != null && existingIngredient.item === stack.item)
            return existingIngredient.stackSize == existingIngredient.maxStackSize
        // Only allow insertion if there is no fuel already or there is more space
        return existingFuel == null ||
               (existingFuel.item === stack.item && existingFuel.stackSize != existingFuel.maxStackSize)
    }

    /**
     * Checks the collision with an item and inserts it into the ingredient stack, if suitable.
     */
    fun onItemCollision(item: EntityItem) {
        if (worldObj.isRemote)
            return
        // Cast a ray straight down onto the "bowl" to check if the item is on top of it
        val hit = Rays.rayTraceBox(item.positionVector, Vec3d(.0, -1.0, .0), BrewingStand.BOWL_AABB.offset(pos))
        if (hit != null) {
            // Try to insert the item
            val remaining = inventory.insertItem(3, item.entityItem, false)
            if (remaining == null) {
                item.setDead()
            } else {
                item.setEntityItemStack(remaining)
            }
            sync()
        }
    }

    /**
     * Composes a tag for updates of the TE (both initial chunk data and later updates).
     */
    override fun getUpdateTag(): NBTTagCompound? {
        val compound = writeToNBT(NBTTagCompound())
        return compound
    }

    /**
     * Creates a packet for updates of the tile entity at runtime.
     */
    override fun getUpdatePacket() = SPacketUpdateTileEntity(this.pos, 0, this.updateTag)

    override fun shouldRefresh(world: World, pos: BlockPos, oldState: IBlockState, newSate: IBlockState) =
        oldState.block !== newSate.block

    /**
     * Reads data from the update packet.
     */
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        Inventories.clear(this)
        val compound = pkt.nbtCompound
        readFromNBT(compound)
    }
}