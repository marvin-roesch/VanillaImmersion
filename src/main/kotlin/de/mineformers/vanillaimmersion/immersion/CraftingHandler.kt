package de.mineformers.vanillaimmersion.immersion

import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic.Companion.Slot
import de.mineformers.vanillaimmersion.util.Inventories
import de.mineformers.vanillaimmersion.util.minus
import de.mineformers.vanillaimmersion.util.times
import de.mineformers.vanillaimmersion.util.toBlockPos
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerWorkbench
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.stats.StatList
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Handles crafting operations.
 */
object CraftingHandler {
    /**
     * Performs the actual distribution of items across the crafting table once the dragging process has ended.
     */
    fun performDrag(table: CraftingTableLogic, player: EntityPlayer, slots: List<Int>) {
        val stack = player.getHeldItem(EnumHand.MAIN_HAND) ?: return
        // Calculate the amount of consumed items
        val consumed = slots.fold(0) {
            acc, slot ->
            // Gather the consumed amount for the current slot and add it to the crafting table
            val amount = splitDrag(table, player, stack, slots, slot)
            if (amount < 0) {
                acc
            } else {
                val copy = stack.copy()
                copy.stackSize = amount
                val remaining = table.inventory.insertItem(slot + 1, copy, false)?.stackSize ?: 0
                acc + (amount - remaining)
            }
        }
        // Consume the items
        stack.stackSize -= consumed
        // Manually remove the stack from the player's hand if it was fully consumed
        if (stack.stackSize == 0)
            player.setHeldItem(EnumHand.MAIN_HAND, null)
        // Try crafting with the added items
        craft(table.world, table.pos, player)
    }

    /**
     * Splits the given stack across a set of slots and calculates the amount for the requested slot.
     */
    fun splitDrag(table: CraftingTableLogic, player: EntityPlayer,
                  dragStack: ItemStack?, slots: List<Int>, slot: Int): Int {
        // First, seek out all slots that the stack may actually be inserted into
        val viableSlots = table.inventory.contents.drop(1).withIndex().filter {
            (it.value == null || Inventories.equal(dragStack, it.value)) && slots.contains(it.index)
        }.map { it.index }
        // Calculate the amount available for dragging across the table
        val draggedSize = dragStack?.stackSize ?: 0
        // If there are less items than slots to distribute across and the requested slot was only dragged across when
        // all items were consumed or if it isn't even a viable slot, mark it as "not receiving items"
        if ((draggedSize < viableSlots.size && slots.indexOf(slot) >= draggedSize) || !viableSlots.contains(slot))
            return -1
        val stack = table.inventory.getStackInSlot(slot + 1)
        val maxAmount =
            if (player.isSneaking) {
                // Sneaking = Only insert one item
                1
            } else {
                // The maximum amount is the dragged size divided by the number of slots
                // If there are less items in the stack than slots, the maximum amount is 1
                draggedSize / Math.min(draggedSize, viableSlots.size)
            }
        // The amount to be inserted into the slot is either the maximum amount or
        // what remains to be filled into the slot
        return if (stack == null) maxAmount else Math.min(stack.maxStackSize - stack.stackSize, maxAmount)
    }

    /**
     * Tries to perform a crafting operation.
     */
    fun craft(world: World, pos: BlockPos, player: EntityPlayer?) {
        // Crafting only happens server-side
        if (world.isRemote)
            return
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return

        // Initialize the crafting matrix, either via a real crafting container if there is a player or
        // via a dummy inventory if there is none
        val matrix =
            if (player != null)
                ContainerWorkbench(player.inventory, world, pos).craftMatrix
            else {
                val container = object : Container() {
                    override fun canInteractWith(playerIn: EntityPlayer?) = true
                }
                InventoryCrafting(container, 3, 3)
            }
        // Fill the matrix with ingredients
        for (i in 1..(tile.inventory.slots - 1))
            matrix.setInventorySlotContents(i - 1, tile.inventory.getStackInSlot(i))
        val result = CraftingManager.getInstance().findMatchingRecipe(matrix, world)
        tile[Slot.OUTPUT] = result
    }

    /**
     * Takes the crafting result from a crafting table, optionally with a player.
     */
    fun takeCraftingResult(world: World, pos: BlockPos, player: EntityPlayer?, result: ItemStack?) {
        // Only take the result on the server and if it exists
        if (world.isRemote || result == null)
            return
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return

        // Use a fake player if the given one is null
        val craftingPlayer = player ?: FakePlayerFactory.getMinecraft(world as WorldServer)
        // Create a crafting container and fill it with ingredients
        val container = ContainerWorkbench(craftingPlayer.inventory, world, pos)
        val craftingSlot = container.getSlot(0)
        for (i in 1..(tile.inventory.slots - 1))
            container.craftMatrix.setInventorySlotContents(i - 1, tile.inventory.getStackInSlot(i)?.copy())
        // Imitate a player picking up an item from the output slot
        craftingSlot.onPickupFromSlot(craftingPlayer, result)
        // Change the crafting table's inventory according to the consumed items in the container
        for (i in 1..container.craftMatrix.sizeInventory) {
            tile.inventory.setStackInSlot(i, container.craftMatrix.getStackInSlot(i - 1))
        }
        if (player != null) {
            Inventories.insertOrDrop(player, result)
            tile[Slot.OUTPUT] = null
        }
        // Try to craft a new item right away
        craft(world, pos, player)
    }

    /**
     * Handles left clicks on the crafting table's top surface.
     */
    @SubscribeEvent
    fun onLeftClick(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.face != EnumFacing.UP)
            return
        val hitVec = event.hitVec - event.pos
        // Prevent Vanilla behaviour if ours was successful
        if (handleClick(event.world, event.pos, event.entityPlayer, hitVec)) {
            event.isCanceled = true
        }
    }

    /**
     * Handles a left click on the crafting table grid and drops the appropriate item.
     */
    fun handleClick(world: World, pos: BlockPos, player: EntityPlayer, hitVec: Vec3d): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return false
        val state = world.getBlockState(pos)

        // Rotate the hit vector of the game's ray tracing result to be able to ignore the block's rotation
        // Then, convert the vector to the "local" position on the table's face in the [0;15] (i.e. pixel)
        // coordinate space
        val facing = state.getValue(CraftingTable.FACING)
        val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
        val rot = (-16 * ((hitVec - Vec3d(0.5, 0.0, 0.5)).rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))).toBlockPos()
        // The crafting grid starts at (3|4) and covers a 11x7 pixel area (including the output)
        val x = rot.x - 3
        val y = rot.z - 4
        if (!(0..11).contains(x) || !(0..7).contains(y))
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
            takeCraftingResult(world, pos, player, tile[Slot.OUTPUT])
            // If the player is sneaking, try to extract as many crafting results from the table as possible
            if (player.isSneaking && !world.isRemote) {
                val result = tile[Slot.OUTPUT]
                while (result != null && Inventories.equal(result, tile[Slot.OUTPUT])) {
                    takeCraftingResult(world, pos, player, tile[Slot.OUTPUT])
                }
            }
            return true
        }
        val slot = Slot.values()[slotX + slotY * 3 + 1]
        val existing = tile[slot]
        // Try to remove the item in the hovered slot
        if (existing != null && !world.isRemote) {
            Inventories.spawn(world, pos, EnumFacing.UP, existing.copy())
            tile[slot] = null
            craft(world, pos, player)
            player.addStat(StatList.CRAFTING_TABLE_INTERACTION)
        }
        return true
    }
}