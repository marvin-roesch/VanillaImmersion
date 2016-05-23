package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic.Companion.Slot
import de.mineformers.vanillaimmersion.tileentity.sync
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.block.BlockFurnace
import net.minecraft.block.SoundType
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.stats.StatList
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

/**
 * Immersive Furnace implementation.
 * Derives from the Vanilla furnace to allow substitution later on.
 * Technically, the lit/unlit status could be handled through blockstates,
 * but this mod intends to keep full Vanilla compatibility, metadata can thus not be modified.
 */
class Furnace(val lit: Boolean) : BlockFurnace(lit) {
    init {
        setHardness(3.5F)
        soundType = SoundType.STONE
        unlocalizedName = "vimmersion.furnace"
        setCreativeTab(VanillaImmersion.CREATIVE_TAB)
        registryName = ResourceLocation(MODID, if (lit) "lit_furnace" else "furnace")
        if (lit) {
            setLightLevel(0.875F)
        }
    }

    @Deprecated("Vanilla")
    override fun isOpaqueCube(state: IBlockState) = false

    override fun createNewTileEntity(worldIn: World, meta: Int) = FurnaceLogic() // Return our own implementation

    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        val world = event.world
        val pos = event.pos
        val state = world.getBlockState(pos)
        val player = event.entityPlayer
        val hand = event.hand
        val stack = event.itemStack
        val side = event.face!!
        val hitVec = event.hitVec - pos
        if (state.block === this && onBlockActivated(world, pos, state, player, hand, stack, side,
                                                     hitVec.x.toFloat(), hitVec.y.toFloat(), hitVec.z.toFloat())) {
            event.isCanceled = true
        }
    }

    /**
     * Handles right clicks for the furnace.
     * Adds or removes items from the furnace.
     */
    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState,
                                  player: EntityPlayer, hand: EnumHand, stack: ItemStack?,
                                  side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        val tile = world.getTileEntity(pos)
        if (tile is FurnaceLogic && hand == EnumHand.MAIN_HAND) {
            // When clicking the front, insert or extract items from the furnace
            if (side == state.getValue(FACING)) {
                if (world.isRemote)
                    return true
                val slot = if (hitY >= 0.5) Slot.INPUT else Slot.FUEL
                val existing = tile[slot]
                if (stack == null && existing != null) {
                    // Extract item
                    Inventories.insertOrDrop(player, existing)
                    tile[slot] = null
                    tile.sync()
                    player.addStat(StatList.FURNACE_INTERACTION)
                    return true
                } else if (stack != null && tile.isItemValidForSlot(slot.ordinal, stack)) {
                    // Insert item
                    if (player.isSneaking) {
                        // Insert all when sneaking
                        tile[slot] = Inventories.merge(stack, existing)
                    } else {
                        val single = stack.copy()
                        single.stackSize = 1
                        // Only insert one by default
                        tile[slot] = Inventories.merge(single, existing)
                        if (single.stackSize == 0)
                            stack.stackSize--
                    }
                    tile.sync()
                    player.addStat(StatList.FURNACE_INTERACTION)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Spawns smoke and fire particles while the furnace is burning.
     * Also plays the crackling sound.
     * Mostly a straight copy from Vanilla, modified to always spawn in the center though.
     */
    @SideOnly(Side.CLIENT)
    override fun randomDisplayTick(stateIn: IBlockState?, world: World?, pos: BlockPos?, rand: Random?) {
        if (!lit) {
            return
        }
        val x = pos!!.x.toDouble() + 0.5
        val y = pos.y.toDouble() + rand!!.nextDouble() * 6.0 / 16.0
        val z = pos.z.toDouble() + 0.5
        val offset = rand.nextDouble() * 0.6 - 0.3

        world!!.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x - offset, y, z + offset, 0.0, 0.0, 0.0)
        world.spawnParticle(EnumParticleTypes.FLAME, x - offset, y, z + offset, 0.0, 0.0, 0.0)

        if (rand.nextDouble() < 0.1) {
            world.playSound(pos.x + 0.5,
                            pos.y.toDouble(),
                            pos.z + 0.5,
                            SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE,
                            SoundCategory.BLOCKS,
                            1.0f,
                            1.0f,
                            false)
        }
    }

    /**
     * Drops the furnace's contents when it's broken.
     */
    override fun breakBlock(worldIn: World?, pos: BlockPos?, state: IBlockState?) {
        // Mad hax: Keep the inventory when the furnace just changes state from unlit to lit
        // TODO: Evaluate whether this could be handled through TileEntity.shouldRefresh
        if (!FurnaceLogic.KEEP_INVENTORY) {
            val tile = worldIn?.getTileEntity(pos)
            if (tile is TileEntityFurnace) {
                InventoryHelper.dropInventoryItems(worldIn, pos, tile)
                worldIn!!.updateComparatorOutputLevel(pos, this)
            }
        }
        worldIn?.removeTileEntity(pos)
    }
}