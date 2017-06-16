package de.mineformers.vanillaimmersion.immersion

import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.client.CraftingDragHandler
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic.Companion.Slot
import de.mineformers.vanillaimmersion.util.blockPos
import de.mineformers.vanillaimmersion.util.equal
import de.mineformers.vanillaimmersion.util.insertOrDrop
import de.mineformers.vanillaimmersion.util.minus
import de.mineformers.vanillaimmersion.util.times
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatList
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Handles crafting operations.
 */
object CraftingHandler {
    /**
     * Handles right clicks on the crafting table's top surface.
     */
    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        if (event.face != EnumFacing.UP || event.hand != EnumHand.MAIN_HAND)
            return
        val tile = event.world.getTileEntity(event.pos) as? CraftingTableLogic ?: return
        val hitVec = event.hitVec - event.pos
        val (x, y) = getLocalPos(event.world, event.pos, hitVec)
        if (x in 0..7 && y in 0..7) {
            if (event.world.isRemote)
                CraftingDragHandler.onStartDragging()
            event.isCanceled = true
        } else if (x in 9..11 && y in 6..8 && Loader.isModLoaded("jei")) {
            if (event.world.isRemote)
                CraftingDragHandler.openRecipeGui()
            event.isCanceled = true
        }
    }

    /**
     * Handles left clicks on the crafting table's top surface.
     */
    @SubscribeEvent
    fun onLeftClick(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.face != EnumFacing.UP)
            return
        val tile = event.world.getTileEntity(event.pos) as? CraftingTableLogic ?: return
        val hitVec = event.hitVec - event.pos
        // Prevent Vanilla behaviour if ours was successful
        if (handleClick(event.world, event.pos, event.entityPlayer, hitVec)) {
            event.isCanceled = true
        }
    }

    fun getLocalPos(world: World, pos: BlockPos, hitVec: Vec3d): Pair<Int, Int> {
        val state = world.getBlockState(pos)

        // Rotate the hit vector of the game's ray tracing result to be able to ignore the block's rotation
        // Then, convert the vector to the "local" position on the table's face in the [0;15] (i.e. pixel)
        // coordinate space
        val facing = state.getValue(CraftingTable.FACING)
        val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
        val rot = (-16 * ((hitVec - Vec3d(0.5, 0.0, 0.5)).rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))).blockPos
        // The crafting grid starts at (3|4)
        val x = rot.x - 3
        val y = rot.z - 4
        return Pair(x, y)
    }

    /**
     * Handles a left click on the crafting table grid and drops the appropriate item.
     */
    fun handleClick(world: World, pos: BlockPos, player: EntityPlayer, hitVec: Vec3d): Boolean {
        val tile = world.getTileEntity(pos) as? CraftingTableLogic ?: return false

        // Get the pixel position on the grid that was clicked
        val (x, y) = getLocalPos(world, pos, hitVec)
        // The grid covers a 11x7 pixel area (including the output)
        if (x !in 0..11 || y !in 0..7)
            return false
        val (slotX, modX) = Pair(x / 3, x % 3)
        val (slotY, modY) = Pair(y / 3, y % 3)
        // Don't allow the 1 pixel gap between the individual crafting slots to be clicked
        if (modX == 2 || modY == 2)
            return true
        // The "slots" right above and below the output don't exist
        if (slotX == 3 && slotY % 2 == 0)
            return true
        // Special case for the output
        if (slotX == 3 && slotY == 1) {
            tile.takeCraftingResult(player, tile[Slot.OUTPUT], false)
            // If the player is sneaking, try to extract as many crafting results from the table as possible
            if (player.isSneaking && !world.isRemote) {
                val result = tile[Slot.OUTPUT]
                while (!result.isEmpty && result.equal(tile[Slot.OUTPUT])) {
                    tile.takeCraftingResult(player, tile[Slot.OUTPUT], false)
                }
            }
            return true
        }
        val slot = Slot.values()[slotX + slotY * 3 + 1]
        val existing = tile[slot]
        // Try to remove the item in the hovered slot
        if (!existing.isEmpty && !world.isRemote) {
            player.insertOrDrop(existing.copy())
            tile[slot] = ItemStack.EMPTY
            tile.craft(player)
            player.addStat(StatList.CRAFTING_TABLE_INTERACTION)
        }
        return true
    }

    /**
     * Performs the actual distribution of items across the crafting table once the dragging process has ended.
     */
    fun performDrag(table: CraftingTableLogic, player: EntityPlayer, slots: List<Int>) {
        val stack = player.getHeldItem(EnumHand.MAIN_HAND)
        if (stack.isEmpty)
            return
        // Calculate the amount of consumed items
        val consumed = slots.fold(0) {
            acc, slot ->
            // Gather the consumed amount for the current slot and add it to the crafting table
            val amount = splitDrag(table, player, stack, slots, slot)
            if (amount < 0) {
                acc
            } else {
                val copy = stack.copy()
                copy.count = amount
                val remaining = table.inventory.insertItem(slot + 1, copy, false).count
                acc + (amount - remaining)
            }
        }
        // Consume the items
        stack.shrink(consumed)
        // Try crafting with the added items
        table.craft(player)
    }

    /**
     * Splits the given stack across a set of slots and calculates the amount for the requested slot.
     */
    fun splitDrag(table: CraftingTableLogic, player: EntityPlayer,
                  dragStack: ItemStack, slots: List<Int>, slot: Int): Int {
        // First, seek out all slots that the stack may actually be inserted into
        val viableSlots = table.inventory.contents.drop(1).withIndex().filter {
            (it.value.isEmpty || dragStack.equal(it.value)) && slots.contains(it.index)
        }.map { it.index }
        // Calculate the amount available for dragging across the table
        val draggedSize = dragStack.count
        // If there are less items than slots to distribute across and the requested slot was only dragged across when
        // all items were consumed or if it isn't even a viable slot, mark it as "not receiving items"
        if ((draggedSize < viableSlots.size && slots.indexOf(slot) >= draggedSize) || !viableSlots.contains(slot))
            return -1
        val stack = table.inventory.getStackInSlot(slot + 1)
        val maxAmount =
            if (!player.isSneaking) {
                // Not Sneaking = Only insert one item
                1
            } else {
                // The maximum amount is the dragged size divided by the number of slots
                // If there are less items in the stack than slots, the maximum amount is 1
                draggedSize / Math.min(draggedSize, viableSlots.size)
            }
        // The amount to be inserted into the slot is either the maximum amount or
        // what remains to be filled into the slot
        return if (stack.isEmpty) maxAmount else Math.min(stack.maxStackSize - stack.count, maxAmount)
    }
}