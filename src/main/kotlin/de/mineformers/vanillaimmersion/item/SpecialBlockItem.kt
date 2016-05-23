package de.mineformers.vanillaimmersion.item

import net.minecraft.block.Block
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
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
    override fun onItemUse(stack: ItemStack, player: EntityPlayer, world: World, pos: BlockPos, hand: EnumHand,
                           facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
        val state = world.getBlockState(pos)
        val block = state.block

        val placedPos =
            if (!block.isReplaceable(world, pos)) {
                pos.offset(facing)
            } else {
                pos
            }

        if (player.canPlayerEdit(placedPos, facing, stack) && stack.stackSize != 0 &&
            world.canBlockBePlaced(this.block, placedPos, false, facing, null, stack)) {
            var placedState = this.block.onBlockPlaced(world, placedPos, facing, hitX, hitY, hitZ, 0, player)

            if (!world.setBlockState(placedPos, placedState, 11)) {
                return EnumActionResult.FAIL
            } else {
                placedState = world.getBlockState(placedPos)

                if (placedState.block === this.block) {
                    ItemBlock.setTileEntityNBT(world, player, placedPos, stack)
                    placedState.block.onBlockPlacedBy(world, placedPos, placedState, player, stack)
                }

                val sound = this.block.soundType
                world.playSound(player, placedPos, sound.placeSound, SoundCategory.BLOCKS,
                                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f)
                --stack.stackSize
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