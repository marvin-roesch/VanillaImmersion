@file:JvmName("TileEntityExtensions")

package de.mineformers.vanillaimmersion.tileentity

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.WorldServer

/**
 * ${JDOC}
 */
fun TileEntity.sync() {
    if (world !is WorldServer)
        return
    val packet = descriptionPacket
    val manager = (world as WorldServer).playerChunkMap
    for (player in world.playerEntities)
        if (manager.isPlayerWatchingChunk(player as EntityPlayerMP, pos.x shr 4, pos.z shr 4))
            player.playerNetServerHandler.sendPacket(packet)
}