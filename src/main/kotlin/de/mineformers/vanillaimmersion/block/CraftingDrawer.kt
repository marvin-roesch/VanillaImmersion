package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.util.Rendering
import de.mineformers.vanillaimmersion.util.rotateY
import mezz.jei.plugins.vanilla.crafting.CraftingRecipeCategory.width
import net.minecraft.block.Block
import net.minecraft.block.BlockHorizontal
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.particle.ParticleDigging
import net.minecraft.client.particle.ParticleManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.client.model.animation.Animation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class CraftingDrawer : Block(Material.WOOD) {
    companion object {
        /**
         * Facing property indicating the front of the table associated with this drawer.
         */
        val FACING = BlockHorizontal.FACING
        val OPEN_SAMPLES = listOf(0f, 0.11875f, 0.225f, 0.31875f, 0.4f, 0.46875f, 0.525f, 0.56875f, 0.6f, 0.61875f, 0.625f)
        val CLOSE_SAMPLES = listOf(0.625f, 0.61875f, 0.6f, 0.56875f, 0.525f, 0.46875f, 0.4f, 0.31875f, 0.225f, 0.11875f, 0f, 0f)

        fun sampleWidth(table: CraftingTableLogic?): Double {
            val clickTime = table?.clickTime?.apply(0f) ?: Float.NEGATIVE_INFINITY
            val samples = if (table?.drawerOpened == true && table.drawerChanging) CLOSE_SAMPLES else OPEN_SAMPLES
            val progress = table?.world?.let { (Animation.getWorldTime(it, Animation.getPartialTickTime()) - clickTime) * 0.9f / 0.8f } ?: 1f
            val timeScaled = (samples.size - 1) * progress.coerceIn(0f, 1f)
            val s1 = timeScaled.toInt().coerceIn(0, samples.size - 2)
            val sampleProgress = timeScaled - s1
            val s2 = s1 + 1
            return samples[s1] * (1.0 - sampleProgress) + samples[s2] * sampleProgress
        }
    }

    init {
        setHardness(2.5f)
        soundType = SoundType.WOOD
        unlocalizedName = "${VanillaImmersion.MODID}.drawer"
        defaultState = blockState.baseState.withProperty(FACING, EnumFacing.NORTH)
        registryName = ResourceLocation(VanillaImmersion.MODID, "drawer")
    }

    private fun getCraftingTable(world: IBlockAccess, pos: BlockPos, state: IBlockState? = null): CraftingTableLogic? {
        val facing = (state ?: world.getBlockState(pos)).getValue(FACING)
        val tablePos = pos.offset(facing.rotateYCCW())
        return world.getTileEntity(tablePos) as? CraftingTableLogic
    }

    private fun getRotation(facing: EnumFacing) =
        when (facing) {
            EnumFacing.EAST -> Rotation.CLOCKWISE_90
            EnumFacing.WEST -> Rotation.COUNTERCLOCKWISE_90
            EnumFacing.SOUTH -> Rotation.CLOCKWISE_180
            else -> Rotation.NONE
        }

    override fun onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if (!world.isRemote) {
            getCraftingTable(world, pos)?.let { if (it.drawerOpened && !it.drawerChanging) it.changeDrawer() }
        }
        return true
    }

    @Deprecated("Vanilla")
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB {
        val tile = getCraftingTable(source, pos, state)
        val width = sampleWidth(tile)
        return AxisAlignedBB(.0, .625, 2 * .0625, width, .8125, 14 * .0625).rotateY(getRotation(state.getValue(FACING)))
    }

    @Deprecated("Vanilla")
    override fun isFullCube(state: IBlockState) = false

    @Deprecated("Vanilla")
    override fun isOpaqueCube(state: IBlockState) = false

    override fun onBlockAdded(world: World, pos: BlockPos, state: IBlockState) {
        super.onBlockAdded(world, pos, state)
        if (getCraftingTable(world, pos) == null) {
            world.destroyBlock(pos, false)
        }
    }

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        getCraftingTable(world, pos, state)?.let { if (it.drawerOpened && !it.drawerChanging) world.destroyBlock(it.pos, true) }
        super.breakBlock(world, pos, state)
    }

    override fun getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer): ItemStack {
        val tile = getCraftingTable(world, pos) ?: return ItemStack.EMPTY
        return Blocks.CRAFTING_TABLE.getPickBlock(tile.blockState, RayTraceResult(target.hitVec, target.sideHit, tile.pos), world, tile.pos, player)
    }

    @SideOnly(Side.CLIENT)
    override fun addDestroyEffects(world: World, pos: BlockPos, manager: ParticleManager): Boolean {
        val tile = getCraftingTable(world, pos)
        if (tile != null) {
            val state = tile.blockState
            if (!state.block.isAir(state, world, pos) && !state.block.addDestroyEffects(world, pos, manager)) {
                val factory = ParticleDigging.Factory()

                for (i in 0..3) {
                    for (j in 0..3) {
                        for (k in 0..3) {
                            val dX = (i.toDouble() + 0.5) / 4.0
                            val dY = (j.toDouble() + 0.5) / 4.0
                            val dZ = (k.toDouble() + 0.5) / 4.0
                            val particle = factory.createParticle(
                                0, world, pos.x.toDouble() + dX, pos.y.toDouble() + dY, pos.z.toDouble() + dZ,
                                dX - 0.5, dY - 0.5, dZ - 0.5,
                                Block.getStateId(state)
                            ) as ParticleDigging
                            manager.addEffect(particle.setBlockPos(pos))
                        }
                    }
                }
            }
        }
        return true
    }

    override fun getRenderType(state: IBlockState) = EnumBlockRenderType.INVISIBLE

    override fun getMetaFromState(state: IBlockState) = state.getValue(FACING).horizontalIndex

    override fun getStateFromMeta(meta: Int) = defaultState.withProperty(FACING, EnumFacing.getHorizontal(meta))

    override fun createBlockState() = BlockStateContainer(this, FACING)
}