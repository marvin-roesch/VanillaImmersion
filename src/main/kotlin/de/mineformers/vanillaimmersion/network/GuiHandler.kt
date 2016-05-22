package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.client.gui.CraftingTableGui
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler

/**
 * GUI Handler for Vanilla Immersion, currently only used for JEI integration.
 */
class GuiHandler : IGuiHandler {
    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        return CraftingTableGui(getServerGuiElement(ID, player, world, x, y, z) as Container)
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        val tile = world.getTileEntity(BlockPos(x, y, z))
        if (tile is CraftingTableLogic) {
            return tile.createContainer(player, false)
        }
        return null
    }
}