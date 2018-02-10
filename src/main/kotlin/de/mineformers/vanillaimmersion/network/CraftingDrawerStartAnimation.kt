package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Message and handler for notifying the client about the crafting drawer being opened/closed.
 */
object CraftingDrawerStartAnimation {
    /**
     * The message just holds the table's position, the time of the click and whether the drawer is opening now.
     */
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var time: Float = 0f,
                       var opening: Boolean = false) : IMessage {
        override fun toBytes(buf: ByteBuf) {
            buf.writeLong(pos.toLong())
            buf.writeFloat(time)
            buf.writeBoolean(opening)
        }

        override fun fromBytes(buf: ByteBuf) {
            pos = BlockPos.fromLong(buf.readLong())
            time = buf.readFloat()
            opening = buf.readBoolean()
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            // We interact with the world, hence schedule our action
            Minecraft.getMinecraft().addScheduledTask {
                val player = Minecraft.getMinecraft().player
                val tile = player.world.getTileEntity(msg.pos) as? CraftingTableLogic ?: return@addScheduledTask
                tile.startAnimation(msg.time, msg.opening)
            }
            return null
        }
    }
}
