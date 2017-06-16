package de.mineformers.vanillaimmersion.item

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * Implementation of ItemBlockSpecial that actually extends ItemBlock.
 */
class SpecialBlockItem(block: Block) : ItemBlock(block) {
    override fun onItemUse(player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand,
                           facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
        val stack = player.getHeldItem(hand)
        val state = world.getBlockState(pos)
        val block = state.block

        val placedPos =
            if (!block.isReplaceable(world, pos)) {
                pos.offset(facing)
            } else {
                pos
            }

        if (!stack.isEmpty && player.canPlayerEdit(placedPos, facing, stack)) {
            var placedState = this.block.getStateForPlacement(world, placedPos, facing, hitX, hitY, hitZ, 0, player, hand)

            if (!world.setBlockState(placedPos, placedState, 11)) {
                return EnumActionResult.FAIL
            } else {
                placedState = world.getBlockState(placedPos)

                if (placedState.block === this.block) {
                    ItemBlock.setTileEntityNBT(world, player, placedPos, stack)
                    placedState.block.onBlockPlacedBy(world, placedPos, placedState, player, stack)

                    if (player is EntityPlayerMP) {
                        CriteriaTriggers.PLACED_BLOCK.trigger(player, pos, stack)
                    }
                }

                val sound = placedState.block.getSoundType(placedState, world, pos, player)
                world.playSound(player, pos, sound.placeSound, SoundCategory.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f)
                stack.shrink(1)
                return EnumActionResult.SUCCESS
            }
        } else {
            return EnumActionResult.FAIL
        }
    }

    override fun getUnlocalizedName(stack: ItemStack): String? {
        return block.unlocalizedName.replace("tile.", "item.")
    }

    override fun getUnlocalizedName(): String? {
        return block.unlocalizedName.replace("tile.", "item.")
    }
}