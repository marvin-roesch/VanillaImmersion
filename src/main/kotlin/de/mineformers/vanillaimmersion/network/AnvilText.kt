package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Message and handler for notifying the server about a text change in an anvil.
 */
object AnvilText {
    /**
     * The message simply holds the anvil's position and the text to be set.
     */
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var text: String? = null) : IMessage {
        override fun toBytes(buf: ByteBuf) {
            buf.writeLong(pos.toLong())
            buf.writeBoolean(text != null)
            if (text != null)
                ByteBufUtils.writeUTF8String(buf, text)
        }

        override fun fromBytes(buf: ByteBuf) {
            pos = BlockPos.fromLong(buf.readLong())
            if (buf.readBoolean())
                text = ByteBufUtils.readUTF8String(buf)
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            // We interact with the world, hence schedule our action
            player.serverWorld.addScheduledTask {
                val tile = player.world.getTileEntity(msg.pos)
                // Ensure the player has acquired the lock on the anvil
                if (tile is AnvilLogic && tile.canInteract(player)) {
                    // Release the lock, change the name and try to "repair"
                    tile.playerLock = null
                    tile.itemName = msg.text
                }
            }
            return null
        }
    }
}