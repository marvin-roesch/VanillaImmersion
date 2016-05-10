package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * ${JDOC}
 */
object CraftingDrag {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var slots: MutableList<Int> = mutableListOf()) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeInt(slots.size)
            for (i in slots)
                buf.writeInt(i)
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            val count = buf.readInt()
            for (i in 1..count)
                slots.add(buf.readInt())
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            player.serverWorld.addScheduledTask {
                val tile = player.worldObj.getTileEntity(msg.pos)
                if (tile is CraftingTableLogic) {
                    CraftingHandler.performDrag(tile, player, msg.slots)
                }
            }
            return null
        }
    }
}