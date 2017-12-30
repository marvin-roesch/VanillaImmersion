package de.mineformers.vanillaimmersion.util

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper

fun SimpleNetworkWrapper.sendToAllWatching(world: World, pos: BlockPos, msg: IMessage) {
    val manager = (world as WorldServer).playerChunkMap
    // Send the packet to all player's watching the TE's chunk
    for (player in world.playerEntities)
        if (manager.isPlayerWatchingChunk(player as EntityPlayerMP, pos.x shr 4, pos.z shr 4))
            this.sendTo(msg, player)
}