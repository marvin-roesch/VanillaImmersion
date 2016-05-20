package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import io.netty.buffer.ByteBuf
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Messages and handlers to deal with actions on the enchantment table. Currently only deals with page hits.
 */
object EnchantingAction {
    /**
     * The page hit message just holds a reference to the enchantment table's position, the affected page and
     * the position that was clicked.
     */
    data class PageHitMessage(var pos: BlockPos = BlockPos.ORIGIN,
                              var right: Boolean = false,
                              var x: Double = 0.0,
                              var y: Double = 0.0) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeBoolean(right)
            buf.writeDouble(x)
            buf.writeDouble(y)
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            right = buf.readBoolean()
            x = buf.readDouble()
            y = buf.readDouble()
        }
    }

    object PageHitHandler : IMessageHandler<PageHitMessage, IMessage> {
        override fun onMessage(msg: PageHitMessage, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            // We interact with the world, hence schedule our action
            player.serverWorld.addScheduledTask {
                val tile = player.worldObj.getTileEntity(msg.pos)
                if (tile is EnchantingTableLogic) {
                    // Delegate the actual logic to the tile entity
                    tile.performPageAction(player, tile.page + if (msg.right) 1 else 0, msg.x, msg.y)
                }
            }
            return null
        }
    }
}