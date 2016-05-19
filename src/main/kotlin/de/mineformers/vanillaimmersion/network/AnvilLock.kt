package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.client.renderer.AnvilTextGui
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * ${JDOC}
 */
object AnvilLock {
    data class AcquireMessage(var pos: BlockPos = BlockPos.ORIGIN) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
        }
    }

    object AcquireHandler : IMessageHandler<AcquireMessage, IMessage> {
        override fun onMessage(msg: AcquireMessage, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            player.serverWorld.addScheduledTask {
                val tile = player.worldObj.getTileEntity(msg.pos)
                if (tile is AnvilLogic && tile.canInteract(player)) {
                    tile.playerLock = player.uniqueID
                    VanillaImmersion.NETWORK.sendTo(AcquiredMessage(msg.pos), player)
                } else if (tile is AnvilLogic) {
                    tile.sendLockMessage(player)
                }
            }
            return null
        }
    }

    data class AcquiredMessage(var pos: BlockPos = BlockPos.ORIGIN) : IMessage {
        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
        }
    }

    object AcquiredHandler : IMessageHandler<AcquiredMessage, IMessage> {
        override fun onMessage(msg: AcquiredMessage, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().addScheduledTask {
                val player = Minecraft.getMinecraft().thePlayer
                val tile = player.worldObj.getTileEntity(msg.pos)
                if (tile is AnvilLogic && tile.canInteract(player)) {
                    Minecraft.getMinecraft().displayGuiScreen(AnvilTextGui(tile))
                }
            }
            return null
        }
    }
}