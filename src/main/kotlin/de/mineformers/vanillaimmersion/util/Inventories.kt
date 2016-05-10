package de.mineformers.vanillaimmersion.util

import com.google.common.base.Objects
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandler

/**
 * Various utilities for dealing with inventories and item stacks.
 */
object Inventories {
    /**
     * Spills the contents of the [IItemHandler] onto the ground.
     *
     * @param slots the slots to spill, may be null if all slots are to be spilled
     */
    fun spill(world: World, pos: BlockPos, inventory: IItemHandler, slots: Iterable<Int>? = null) {
        spill(world, Vec3d(pos), inventory, slots)
    }

    /**
     * Spills the contents of the [IItemHandler] onto the ground at an entity's feet.
     *
     * @param slots the slots to spill, may be null if all slots are to be spilled
     */
    fun spill(world: World, entity: Entity, inventory: IItemHandler, slots: Iterable<Int>? = null) {
        spill(world, entity.position, inventory, slots)
    }

    /**
     * Spills the contents of the [IItemHandler] onto the ground.
     *
     * @param slots the slots to spill, may be null if all slots are to be spilled
     */
    private fun spill(world: World, pos: Vec3d, inventory: IItemHandler, slots: Iterable<Int>? = null) {
        val indices = slots ?: 0..inventory.slots - 1
        for (i in indices) {
            val stack = inventory.getStackInSlot(i)

            if (stack != null) {
                InventoryHelper.spawnItemStack(world, pos.x, pos.y, pos.z, stack)
            }
        }
    }

    /**
     * Insert an item stack into a player's inventory or drop it to the ground if there is no space left.
     */
    fun insertOrDrop(player: EntityPlayer, stack: ItemStack?) {
        if (stack == null)
            return
        if (!player.inventory.addItemStackToInventory(stack))
            InventoryHelper.spawnItemStack(player.worldObj, player.posX, player.posY, player.posZ, stack)
    }

    /**
     * Drop an item stack in a certain direction from a block.
     * The entity will spawn in the middle of the specified face and will move away from it.
     */
    fun spawn(world: World, pos: BlockPos, side: EnumFacing, stack: ItemStack?) {
        if (stack == null || world.isRemote)
            return
        val offset = Vec3d(0.5, 0.5, 0.5) + 0.5 * Vec3d(side.directionVec) + pos
        val entity = EntityItem(world, offset.x, offset.y, offset.z, stack)
        // Add some random motion away from the block's face
        entity.motionX = world.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetX
        entity.motionY = world.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetY
        entity.motionZ = world.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetZ
        world.spawnEntityInWorld(entity)
        // Play the drop sound
        world.playSound(null, offset.x, offset.y, offset.z, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.2f,
                        ((world.rand.nextFloat() - world.rand.nextFloat()) * 0.7f + 1.0f) * 2.0f)

    }

    /**
     * Checks whether to item stacks are to be considered equal in regards to their
     * <ul>
     *     <li>Item</li>
     *     <li>Metadata</li>
     *     <li>NBT Tag Compound</li>
     * </ul>
     */
    fun equal(a: ItemStack?, b: ItemStack?) = a == b || a != null && b != null && equalsImpl(a, b)

    private fun equalsImpl(a: ItemStack, b: ItemStack) = a.item === b.item && a.metadata == b.metadata
                                                         && Objects.equal(a.tagCompound, b.tagCompound)

    /**
     * Tries to merge an item stack into another one if they are equal.
     * Note that this function will mutate the passed stacks,
     * meaning that modifying the stacks' sizes afterwards is *not* necessary.
     *
     * @return the result of merging both item stacks
     */
    fun merge(from: ItemStack?, into: ItemStack?) = merge(from, into, false)

    /**
     * Tries to merge an item stack into another one.
     * Note that this function will mutate the passed stacks,
     * meaning that modifying the stacks' sizes afterwards is *not* necessary.
     *
     * @param force if true, the merging will be performed even if the stacks are not equal
     * @return the result of merging both item stacks
     */
    fun merge(from: ItemStack?, into: ItemStack?, force: Boolean): ItemStack? {
        if (from == null) {
            return into
        }

        if (into == null) {
            val result = from.copy()
            from.stackSize = 0
            return result
        }

        if (force || equalsImpl(from, into)) {
            val transferCount = Math.min(into.maxStackSize - into.stackSize, from.stackSize)
            from.stackSize -= transferCount
            into.stackSize += transferCount
        }
        return into
    }
}