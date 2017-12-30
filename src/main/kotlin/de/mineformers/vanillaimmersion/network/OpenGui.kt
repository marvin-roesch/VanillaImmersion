package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.VanillaImmersion
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Messages and handlers to deal with GUIs for JEI integration.
 */
object OpenGui {
    /**
     * The page hit message just holds a reference to the enchantment table's position, the affected page and
     * the position that was clicked.
     */
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var id: Int = 0) : IMessage {
        override fun toBytes(buf: ByteBuf) {
            buf.writeLong(pos.toLong())
            buf.writeInt(id)
        }

        override fun fromBytes(buf: ByteBuf) {
            pos = BlockPos.fromLong(buf.readLong())
            id = buf.readInt()
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            // We interact with the world, hence schedule our action
            player.serverWorld.addScheduledTask  {
                if (!player.world.isBlockLoaded(msg.pos))
                    return@addScheduledTask
                player.openGui(VanillaImmersion, msg.id, player.world, msg.pos.x, msg.pos.y, msg.pos.z)
            }
            return null
        }
    }
}
