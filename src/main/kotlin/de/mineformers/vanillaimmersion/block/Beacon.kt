package de.mineformers.vanillaimmersion.block

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import net.minecraft.block.BlockBeacon
import net.minecraft.block.BlockFurnace
import net.minecraft.block.SoundType
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.InventoryHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

/**
 * Immersive Beacon implementation.
 * Derives from the Vanilla beacon to allow substitution later on.
 */
class Beacon : BlockBeacon() {
    init {
        setHardness(3F)
        unlocalizedName = "$MODID.beacon"
        setCreativeTab(VanillaImmersion.CREATIVE_TAB)
        registryName = ResourceLocation(MODID, "beacon")
        setLightLevel(1F)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int) = BeaconLogic() // Return our own implementation
}