@file:JvmName("InventoryExtensions")

package de.mineformers.vanillaimmersion.util

import com.google.common.base.Objects
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable

fun IItemHandler.extract(vararg slots: Pair<Int, Int>): ItemStack {
    for ((slot, amount) in slots) {
        val extracted = this.extractItem(slot, amount, false)
        if (!extracted.isEmpty)
            return extracted
    }
    return ItemStack.EMPTY
}

/**
 * Spills the contents of the [IItemHandler] onto the ground.
 *
 * @param slots the slots to spill, may be null if all slots are to be spilled
 */
fun IItemHandler.spill(world: World, pos: BlockPos, slots: Iterable<Int>? = null) {
    this.spill(world, Vec3d(pos), slots)
}

/**
 * Spills the contents of the [IItemHandler] onto the ground at an entity's feet.
 *
 * @param slots the slots to spill, may be null if all slots are to be spilled
 */
fun IItemHandler.spill(entity: Entity, slots: Iterable<Int>? = null) {
    this.spill(entity.world, entity.position, slots)
}

/**
 * Spills the contents of the [IItemHandler] onto the ground.
 *
 * @param slots the slots to spill, may be null if all slots are to be spilled
 */
fun IItemHandler.spill(world: World, pos: Vec3d, slots: Iterable<Int>? = null) {
    val indices = slots ?: 0..this.slots - 1
    for (i in indices) {
        val stack = this.getStackInSlot(i)

        if (!stack.isEmpty) {
            InventoryHelper.spawnItemStack(world, pos.x, pos.y, pos.z, stack)
        }
    }
}

/**
 * Drop an item stack in a certain direction from a block.
 * The entity will spawn in the middle of the specified face and will move away from it.
 */
fun World.spawn(pos: BlockPos, side: EnumFacing, stack: ItemStack) {
    if (stack.isEmpty || this.isRemote)
        return
    val offset = Vec3d(0.5, 0.5, 0.5) + 0.5 * Vec3d(side.directionVec) + pos
    val entity = EntityItem(this, offset.x, offset.y, offset.z, stack)
    // Add some random motion away from the block's face
    entity.motionX = this.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetX
    entity.motionY = this.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetY
    entity.motionZ = this.rand.nextGaussian() * 0.05 + 0.05 * side.frontOffsetZ
    this.spawnEntity(entity)
    // Play the drop sound
    this.playSound(null, offset.x, offset.y, offset.z, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.2f,
        ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7f + 1.0f) * 2.0f)
}

/**
 * Completely clears out all slots of an [inventory], i.e. setting them to `null`.
 */
fun IItemHandlerModifiable.clear() {
    for (i in 0..(this.slots - 1))
        this.setStackInSlot(i, ItemStack.EMPTY)
}

/**
 * Insert an item stack into a player's inventory or drop it to the ground if there is no space left.
 */
fun EntityPlayer.insertOrDrop(stack: ItemStack) {
    if (stack.isEmpty)
        return
    if (!this.inventory.addItemStackToInventory(stack))
        InventoryHelper.spawnItemStack(this.world, this.posX, this.posY, this.posZ, stack)
    else
        this.world.playSound(null, this.posX, this.posY, this.posZ,
            SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS,
            .2f, ((this.rng.nextFloat() - this.rng.nextFloat()) * .7f + 1f) * 2f)
}
/**
 * Checks whether to item stacks are to be considered equal in regards to their
 * <ul>
 *     <li>Item</li>
 *     <li>Metadata</li>
 *     <li>NBT Tag Compound</li>
 * </ul>
 */
fun ItemStack.equal(b: ItemStack) = this == b || this != null && b != null && equalsImpl(this, b)

private fun equalsImpl(a: ItemStack, b: ItemStack) = a.item === b.item && a.metadata == b.metadata
    && Objects.equal(a.tagCompound, b.tagCompound)

/**
 * Tries to merge an item stack into another one if they are equal.
 * Note that this function will mutate the passed stacks,
 * meaning that modifying the stacks' sizes afterwards is *not* necessary.
 *
 * @return the result of merging both item stacks
 */
fun ItemStack.merge(into: ItemStack) = this.merge(into, false)

/**
 * Tries to merge an item stack into another one.
 * Note that this function will mutate the passed stacks,
 * meaning that modifying the stacks' sizes afterwards is *not* necessary.
 *
 * @param force if true, the merging will be performed even if the stacks are not equal
 * @return the result of merging both item stacks
 */
fun ItemStack.merge(into: ItemStack, force: Boolean): ItemStack {
    if (this.isEmpty) {
        return into
    }

    if (into.isEmpty) {
        val result = this.copy()
        this.count = 0
        return result
    }

    if (force || equalsImpl(this, into)) {
        val transferCount = Math.min(into.maxStackSize - into.count, this.count)
        this.count -= transferCount
        into.count += transferCount
    }
    return into
}

/**
 * Various utilities for dealing with inventories and item stacks.
 */
object Inventories {

}