package de.mineformers.vanillaimmersion.immersion

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.Items
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.network.AnvilLock
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic.Companion.Slot
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.block.BlockAnvil
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ContainerRepair
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Handles repairing operations with an anvil.
 */
object RepairHandler {
    /**
     * Handles right clicks on any anvil.
     */
    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        if (event.hand == EnumHand.OFF_HAND)
            return
        val state = event.world.getBlockState(event.pos)
        val tile = event.world.getTileEntity(event.pos)
        if (tile !is AnvilLogic || event.world.isRemote)
            return
        if (event.face == EnumFacing.UP) {
            var stack = event.itemStack
            // When the player is sneaking, only insert a single item from the stack
            if (stack != null && event.entityPlayer.isSneaking) {
                stack = stack.copy()
                stack.stackSize = 1
            }
            val hitVec = event.hitVec - event.pos
            if (handleClick(event.world, event.pos, event.entityPlayer, stack, hitVec)) {
                event.isCanceled = true
            }
            // If the item was consumed and it results in the item stack being empty, remove it from the player's hand
            if (stack?.stackSize == 0 && event.itemStack != null) {
                if (--event.itemStack!!.stackSize <= 0)
                    event.entityPlayer.setHeldItem(event.hand, null)
            }
        } else if (event.face == state.getValue(BlockAnvil.FACING).rotateY() &&
                   event.itemStack == null &&
                   event.hitVec.y >= event.pos.y + 0.625 &&
                   tile[Slot.INPUT_OBJECT] != null) {
            // When you hit the front face, try to edit the text
            if (tile.canInteract(event.entityPlayer)) {
                tile.playerLock = event.entityPlayer.uniqueID
                VanillaImmersion.NETWORK.sendTo(AnvilLock.Message(event.pos), event.entityPlayer as EntityPlayerMP)
            } else {
                tile.sendLockMessage(event.entityPlayer)
            }
        }
    }

    /**
     * Handles hammer smashes on any anvil.
     */
    @SubscribeEvent
    fun onLeftClick(event: PlayerInteractEvent.LeftClickBlock) {
        if (event.hand == EnumHand.OFF_HAND)
            return
        val tile = event.world.getTileEntity(event.pos)
        val stack = event.itemStack
        if (tile !is AnvilLogic || event.world.isRemote || stack?.item != Items.HAMMER || event.face != EnumFacing.UP)
            return
        event.isCanceled = true
        tile.hammer(event.entityPlayer)
    }

    /**
     * Handles a definite click on an anvil and decides whether to cancel any Vanilla behavior.
     */
    fun handleClick(world: World, pos: BlockPos, player: EntityPlayer, stack: ItemStack?, hitVec: Vec3d): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile !is AnvilLogic)
            return false
        // Rotate the hit vector of the game's ray tracing result to be able to ignore the block's rotation
        // Then, convert the vector to the "local" position on the anvil's face in the [0;15] (i.e. pixel)
        // coordinate space
        val state = world.getBlockState(pos)
        val facing = state.getValue(CraftingTable.FACING)
        val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
        val rot = -16 * ((hitVec - Vec3d(0.5, 0.0, 0.5)).rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))
        // The slot depends on the x position (due to the anvil's orientation this is more like "depth")
        val slot = when (rot.z) {
            in .0..5.1 -> Slot.INPUT_OBJECT
            in 6.0..11.0 -> Slot.INPUT_MATERIAL
            in 11.5..16.0 -> Slot.HAMMER
            else -> return false
        }
        return interactWithSlot(world, pos, tile, player, slot, stack, rot.z <= 11.0)
    }

    private fun interactWithSlot(world: World, pos: BlockPos,
                                 tile: AnvilLogic,
                                 player: EntityPlayer,
                                 slot: Slot,
                                 stack: ItemStack?,
                                 blockLocked: Boolean): Boolean {
        // One of the input slots was clicked
        val existing = tile[slot]
        // If there already was an item in there, drop it
        if (stack == null && existing != null) {
            // If the anvil is locked, notify the player
            if (!tile.canInteract(player) && blockLocked) {
                tile.sendLockMessage(player)
                return false
            }
            val extracted = tile.inventory.extractItem(slot.ordinal, Int.MAX_VALUE, false)
            Inventories.insertOrDrop(player, extracted)
            return true
        } else if (stack != null) {
            // If the anvil is locked, notify the player
            if (!tile.canInteract(player) && blockLocked) {
                tile.sendLockMessage(player)
                return false
            }
            // Insert the item
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                            SoundEvents.BLOCK_ANVIL_HIT, SoundCategory.BLOCKS, 1f, 1f)
            val remaining = tile.inventory.insertItem(slot.ordinal, stack, false)
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
    fun tryRepair(world: World, pos: BlockPos, player: EntityPlayer, simulate: Boolean): RepairResult? {
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
        val result = outputSlot.stack ?: return null
        if (!outputSlot.canTakeStack(player))
            return null
        if (!simulate) {
            outputSlot.onPickupFromSlot(player, result)
        }
        return RepairResult(objectSlot.stack, materialSlot.stack, result)
    }

    /**
     * A simple wrapper around the result of a repair operation.
     */
    data class RepairResult(val input: ItemStack?, val material: ItemStack?, val output: ItemStack?)
}