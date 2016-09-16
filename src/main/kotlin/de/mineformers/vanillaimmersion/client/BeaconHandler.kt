package de.mineformers.vanillaimmersion.client

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.network.BeaconScroll
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Handles interaction (scrolling in particular) with the beacon.
 */
object BeaconHandler {
    @SubscribeEvent
    fun onMouseInput(event: MouseEvent) {
        val hovered = Minecraft.getMinecraft().objectMouseOver ?: return
        if (hovered.typeOfHit != RayTraceResult.Type.BLOCK || event.dwheel == 0)
            return
        val tile = Minecraft.getMinecraft().theWorld.getTileEntity(hovered.blockPos)
        if (tile is BeaconLogic && tile.state != null && tile.state!!.stage <= 2 &&
            hovered.sideHit !in setOf(EnumFacing.UP, EnumFacing.DOWN)) {
            VanillaImmersion.NETWORK.sendToServer(BeaconScroll.Message(hovered.blockPos, event.dwheel))
            event.isCanceled = true
        }
    }
}