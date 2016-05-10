package de.mineformers.vanillaimmersion.immersion

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.network.AnvilLock
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.block.Block
import net.minecraft.block.BlockAnvil
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ContainerRepair
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.text.TextComponentString
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * ${JDOC}
 */
object RepairHandler {
    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        val state = event.world.getBlockState(event.pos)
        if (state.block != VanillaImmersion.Blocks.ANVIL)
            return
        if (!event.world.isRemote && event.face == EnumFacing.UP) {
            var stack: ItemStack? = event.itemStack
            if (stack != null && event.entityPlayer.isSneaking) {
                stack = stack.copy()
                stack.stackSize = 1
            }
            val hitVec = event.hitVec - event.pos
            if (handleClick(event.world, event.pos, event.entityPlayer, stack, hitVec)) {
                tryRepair(event.world, event.pos, event.entityPlayer)
                event.isCanceled = true
            }
            if (stack?.stackSize == 0 && event.itemStack != null) {
                if (--event.itemStack!!.stackSize <= 0)
                    event.entityPlayer.setHeldItem(event.hand, null)
            }
        } else if (event.world.isRemote &&
                   event.face == state.getValue(BlockAnvil.FACING).rotateY() &&
                   event.itemStack == null &&
                   event.hitVec.y >= event.pos.y + 0.625) {
            VanillaImmersion.NETWORK.sendToServer(AnvilLock.AcquireMessage(event.pos))
        }
    }

    fun tryRepair(world: World, pos: BlockPos, player: EntityPlayer) {
        val tile = world.getTileEntity(pos)
        if (tile !is AnvilLogic)
            return
        val container = ContainerRepair(player.inventory, world, pos, player)
        container.getSlot(0).putStack(tile[AnvilLogic.Companion.Slot.INPUT_OBJECT])
        container.getSlot(1).putStack(tile[AnvilLogic.Companion.Slot.INPUT_MATERIAL])
        container.updateItemName(tile.currentName)
        if (container.getSlot(2).hasStack) {
            val result = container.getSlot(2).stack
            (world as WorldServer).spawnParticle(EnumParticleTypes.BLOCK_DUST,
                                                 pos.x + 0.5, pos.y + 1.0, pos.z + 0.5,
                                                 150, 0.0, 0.0, 0.0, 0.15,
                                                 Block.getStateId(Blocks.ANVIL.defaultState))
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                            SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1f, 1f)
            tile[AnvilLogic.Companion.Slot.OUTPUT] = result
        } else {
            tile[AnvilLogic.Companion.Slot.OUTPUT] = null
            tile.currentName = null
        }
    }

    fun handleClick(world: World, pos: BlockPos, player: EntityPlayer, stack: ItemStack?, hitVec: Vec3d): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile !is AnvilLogic)
            return false
        val state = world.getBlockState(pos)
        val facing = state.getValue(CraftingTable.FACING)
        val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
        val rot = (-16 * ((hitVec - Vec3d(0.5, 0.0, 0.5)).rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))).toBlockPos()
        val slot = if (rot.x > 8) AnvilLogic.Companion.Slot.INPUT_MATERIAL else AnvilLogic.Companion.Slot.INPUT_OBJECT
        if (rot.z >= 8 && tile[AnvilLogic.Companion.Slot.OUTPUT] != null) {
            if (!tile.canInteract(player)) {
                player.addChatComponentMessage(TextComponentString("This anvil is currently in use by another player."))
                return false
            }
            val container = ContainerRepair(player.inventory, world, pos, player)
            container.getSlot(0).putStack(tile[AnvilLogic.Companion.Slot.INPUT_OBJECT])
            container.getSlot(1).putStack(tile[AnvilLogic.Companion.Slot.INPUT_MATERIAL])
            container.updateItemName(tile.currentName)
            if (!container.getSlot(2).canTakeStack(player))
                return false
            val result = container.getSlot(2).stack
            container.getSlot(2).onPickupFromSlot(player, result)
            tile[AnvilLogic.Companion.Slot.INPUT_OBJECT] = container.getSlot(0).stack
            tile[AnvilLogic.Companion.Slot.INPUT_MATERIAL] = container.getSlot(1).stack
            tile[AnvilLogic.Companion.Slot.OUTPUT] = null
            tile.currentName = null
            Inventories.insertOrDrop(player, result.copy())
            return true
        } else if (rot.z >= 8) {
            return false
        }
        val existing = tile[slot]
        if (stack == null && existing != null) {
            if (!tile.canInteract(player)) {
                player.addChatComponentMessage(TextComponentString("This anvil is currently in use by another player."))
                return false
            }
            Inventories.spawn(world, pos, EnumFacing.UP, existing.copy())
            tile[slot] = null
            return true
        } else if (stack != null) {
            if (!tile.canInteract(player)) {
                player.addChatComponentMessage(TextComponentString("This anvil is currently in use by another player."))
                return false
            }
            world.playSound(null, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                            SoundEvents.BLOCK_ANVIL_HIT, SoundCategory.BLOCKS, 1f, 1f)
            tile[slot] = Inventories.merge(stack, existing)
            return true
        }
        return false
    }
}