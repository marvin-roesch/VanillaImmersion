package de.mineformers.vanillaimmersion.util

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.management.PlayerChunkMapEntry
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.ReflectionHelper

private val PLAYERS_FIELD by lazy {
    ReflectionHelper.findField(PlayerChunkMapEntry::class.java, "field_187283_c", "players")
}

val PlayerChunkMapEntry.players: List<EntityPlayerMP>
    @Suppress("UNCHECKED_CAST")
    get() = if (this.isSentToPlayers) PLAYERS_FIELD.get(this) as List<EntityPlayerMP> else emptyList()

fun SimpleNetworkWrapper.sendToAllWatching(world: World, pos: BlockPos, msg: IMessage) {
    val manager = (world as WorldServer).playerChunkMap
    val chunkEntry = manager.getEntry(pos.x shr 4, pos.z shr 4) ?: return
    // Send the packet to all player's watching the TE's chunk
    for (player in chunkEntry.players) {
        this.sendTo(msg, player)
    }
}
