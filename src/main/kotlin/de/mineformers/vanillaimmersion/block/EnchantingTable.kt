package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic.Companion.Slot
import de.mineformers.vanillaimmersion.util.Inventories
import net.minecraft.block.BlockEnchantmentTable
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * Immersive Enchantment Table implementation.
 * Derives from the Vanilla enchanting table to allow substitution later on.
 */
class EnchantingTable : BlockEnchantmentTable() {
    init {
        setHardness(5.0F)
        setResistance(2000.0F)
        unlocalizedName = "enchantmentTable"
        registryName = ResourceLocation("vimmersion", "enchanting_table")
    }

    /**
     * Handles 'simple' right clicks for the enchantment table.
     * Adds or removes items from the table.
     * Interaction with the block's pages are handled elsewhere.
     *
     * @see de.mineformers.vanillaimmersion.client.EnchantingUIHandler
     */
    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand, stack: ItemStack?,
                                  side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (world.isRemote)
            return true
        val tile = world.getTileEntity(pos)
        if (tile !is EnchantingTableLogic)
            return false
        // Prevent interaction when enchantment is in progress
        if (tile.result != null)
            return false
        if (stack != null) {
            // Try to insert the stack, preferring the modifiers slot
            return tryInsertItem(tile, Slot.MODIFIERS, player, hand, stack) ||
                   tryInsertItem(tile, Slot.OBJECT, player, hand, stack)
        } else {
            // Extract the first stack from the table, preferring the object slot
            val extracted = tile.inventory.extractItem(0, 1, false) ?: tile.inventory.extractItem(1, 3, false)
            player.setHeldItem(hand, extracted)
        }
        return false
    }

    /**
     * Tries to insert an item stack into a given slot of the enchantment table.
     */
    private fun tryInsertItem(table: EnchantingTableLogic, slot: Slot,
                              player: EntityPlayer, hand: EnumHand, stack: ItemStack): Boolean {
        val result = table.inventory.insertItem(slot.ordinal, stack.copy(), false)
        if (result == null || result.stackSize != stack.stackSize) {
            // It seems like the insertion was successful, modify the player's held item.
            stack.stackSize = result?.stackSize ?: 0
            // The game should remove empty stacks automatically, it doesn't however
            if (stack.stackSize == 0)
                player.setHeldItem(hand, null)
            table.updateEnchantment(player)
            return true
        }
        return false
    }

    /**
     * Drops the enchantment table's contents when it's broken.
     */
    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        val tile = world.getTileEntity(pos)

        if (tile is EnchantingTableLogic) {
            Inventories.spill(world, pos, tile.inventory)
            world.updateComparatorOutputLevel(pos, this)
        }
        super.breakBlock(world, pos, state)
    }

    /**
     * Return our own TileEntity rather than the Vanilla one.
     */
    override fun createNewTileEntity(worldIn: World?, meta: Int) = EnchantingTableLogic()
}