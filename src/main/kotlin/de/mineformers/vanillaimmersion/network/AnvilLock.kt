package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.client.renderer.AnvilTextGui
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Message and handler for notifying the client that is has acquired the lock on an anvil.
 */
object AnvilLock {
    /**
     * The message simply consists of the anvil's position.
     */
    data class Message(var pos: BlockPos = BlockPos.ORIGIN) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            // We interact with the world, hence schedule our action
            Minecraft.getMinecraft().addScheduledTask {
                val player = Minecraft.getMinecraft().thePlayer
                val tile = player.worldObj.getTileEntity(msg.pos)
                // Ensure the player can interact and "show" them the text "GUI" to insert some text
                if (tile is AnvilLogic && tile.canInteract(player)) {
                    Minecraft.getMinecraft().displayGuiScreen(AnvilTextGui(tile))
                }
            }
            return null
        }
    }
}