package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.immersion.RepairHandler
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * ${JDOC}
 */
object AnvilText {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var text: String? = null) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeBoolean(text != null)
            if (text != null)
                ByteBufUtils.writeUTF8String(buf, text)
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            if (buf.readBoolean())
                text = ByteBufUtils.readUTF8String(buf)
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            player.serverWorld.addScheduledTask {
                val tile = player.worldObj.getTileEntity(msg.pos)
                if (tile is AnvilLogic && tile.canInteract(player)) {
                    tile.playerLock = null
                    tile.currentName = msg.text
                    RepairHandler.tryRepair(player.worldObj, msg.pos, player)
                }
            }
            return null
        }
    }
}