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
 * ${JDOC}
 */
object CraftingHandler {
    fun performDrag(table: CraftingTableLogic, player: EntityPlayer, slots: List<Int>) {
        val stack = player.getHeldItem(EnumHand.MAIN_HAND)
        val consumed = slots.fold(0) {
            acc, slot ->
            val amount = splitDrag(table, player, stack, slots, slot)
            if (amount < 0)
                acc
            else {
                val copy = stack.copy()
                copy.stackSize = amount
                table.inventory.insertItem(slot + 1, copy, false)
                acc + amount
            }
        }
        stack.stackSize -= consumed
        if (stack.stackSize == 0)
            player.setHeldItem(EnumHand.MAIN_HAND, null)
        craft(table.world, table.pos, player)
    }

    fun splitDrag(table: CraftingTableLogic, player: EntityPlayer,
                  dragStack: ItemStack?, slots: List<Int>, slot: Int): Int {
        val viableSlots = table.inventory.contents.drop(1).withIndex().filter {
            (it.value == null || Inventories.equal(dragStack, it.value)) && slots.contains(it.index)
        }.map { it.index }
        val draggedSize = dragStack?.stackSize ?: 0
        if ((draggedSize < viableSlots.size && slots.indexOf(slot) >= draggedSize) || !viableSlots.contains(slot))
            return -1
        val stack = table.inventory.getStackInSlot(slot + 1)
        val maxAmount =
            if (player.isSneaking)
                1
            else
                draggedSize / Math.min(draggedSize, viableSlots.size)
        return if (stack == null) maxAmount else Math.min(stack.maxStackSize - stack.stackSize, maxAmount)
    }

    fun craft(world: World, pos: BlockPos, player: EntityPlayer?) {
        if (world.isRemote)
            return
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return

        val matrix =
            if (player != null)
                ContainerWorkbench(player.inventory, world, pos).craftMatrix
            else {
                val container = object : Container() {
                    override fun canInteractWith(playerIn: EntityPlayer?) = true
                }
                InventoryCrafting(container, 3, 3)
            }
        for (i in 1..(tile.inventory.slots - 1))
            matrix.setInventorySlotContents(i - 1, tile.inventory.getStackInSlot(i))
        val result = CraftingManager.getInstance().findMatchingRecipe(matrix, world)
        tile[Slot.OUTPUT] = result
    }

    fun takeCraftingResult(world: World, pos: BlockPos, player: EntityPlayer?, result: ItemStack?) {
        if (world.isRemote || result == null)
            return
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return

        val craftingPlayer = player ?: FakePlayerFactory.getMinecraft(world as WorldServer)
        val container = ContainerWorkbench(craftingPlayer.inventory, world, pos)
        val craftingSlot = container.getSlot(0)
        for (i in 1..(tile.inventory.slots - 1))
            container.craftMatrix.setInventorySlotContents(i - 1, tile.inventory.getStackInSlot(i)?.copy())
        craftingSlot.onPickupFromSlot(craftingPlayer, result)
        for (i in 1..container.craftMatrix.sizeInventory) {
            tile.inventory.setStackInSlot(i, container.craftMatrix.getStackInSlot(i - 1))
        }
        if (player != null) {
            Inventories.insertOrDrop(player, result)
            tile[Slot.OUTPUT] = null
        }
        craft(world, pos, player)
    }

    @SubscribeEvent
    fun onLeftClick(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.face != EnumFacing.UP)
            return
        val hitVec = event.hitVec - event.pos
        if (handleClick(event.world, event.pos, event.entityPlayer, null, hitVec)) {
            event.isCanceled = true
        }
    }

    fun handleClick(world: World, pos: BlockPos, player: EntityPlayer, stack: ItemStack?, hitVec: Vec3d): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile !is CraftingTableLogic)
            return false
        val state = world.getBlockState(pos)
        val facing = state.getValue(CraftingTable.FACING)
        val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
        val rot = (-16 * ((hitVec - Vec3d(0.5, 0.0, 0.5)).rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))).toBlockPos()
        val x = rot.x - 3
        val z = rot.z - 4
        if (!(0..11).contains(x) || !(0..7).contains(z))
            return false
        val (slotX, modX) = Pair(x / 3, x % 3)
        val (slotZ, modZ) = Pair(z / 3, z % 3)
        if (modX == 2 || modZ == 3)
            return true
        if (slotX == 3 && slotZ % 2 == 0)
            return true
        if (slotX == 3 && slotZ == 1) {
            takeCraftingResult(world, pos, player, tile[Slot.OUTPUT])
            if (player.isSneaking && !world.isRemote) {
                val result = tile[Slot.OUTPUT]
                while (result != null && Inventories.equal(result, tile[Slot.OUTPUT])) {
                    takeCraftingResult(world, pos, player, tile[Slot.OUTPUT])
                }
            }
            return true
        }
        val slot = Slot.values()[slotX + slotZ * 3 + 1]
        val existing = tile[slot]
        if (stack == null && existing != null) {
            if (!world.isRemote) {
                Inventories.spawn(world, pos, EnumFacing.UP, existing.copy())
                tile[slot] = null
                craft(world, pos, player)
                player.addStat(StatList.CRAFTING_TABLE_INTERACTION)
            }
            return true
        } else if (stack != null) {
            if (!world.isRemote) {
                tile[slot] = Inventories.merge(stack, existing)
                craft(world, pos, player)
                player.addStat(StatList.CRAFTING_TABLE_INTERACTION)
            }
            return true
        }
        return existing == null
    }
}