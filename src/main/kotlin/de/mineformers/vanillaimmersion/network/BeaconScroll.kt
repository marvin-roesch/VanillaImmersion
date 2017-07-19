package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Message and handler for notifying the server that the player has scrolled on a beacon.
 */
object BeaconScroll {
    /**
     * The message simply consists of the beacons's position and the scroll direction.
     */
    data class Message(var pos: BlockPos = BlockPos.ORIGIN, var direction: Int = 0) : IMessage {
        override fun toBytes(buf: ByteBuf) {
            buf.writeLong(pos.toLong())
            buf.writeInt(direction)
        }

        override fun fromBytes(buf: ByteBuf) {
            pos = BlockPos.fromLong(buf.readLong())
            direction = buf.readInt()
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            // We interact with the world, hence schedule our action
            player.serverWorld.addScheduledTask task@ {
                if (!player.world.isBlockLoaded(msg.pos))
                    return@task
                val tile = player.world.getTileEntity(msg.pos)
                // Ensure the player can interact and "show" them the text "GUI" to insert some text
                if (tile is BeaconLogic) {
                    tile.onScroll(msg.direction)
                }
            }
            return null
        }
    }
}
