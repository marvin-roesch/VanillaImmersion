package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import java.util.*

/**
 * ${JDOC}
 */
object AnvilUpdate {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var stacks: Array<ItemStack?> = emptyArray(),
                       var name: String? = null,
                       var lock: UUID? = null) : IMessage {
        constructor(anvil: AnvilLogic) : this(anvil.pos, anvil.inventory.contents, anvil.currentName, anvil.playerLock)

        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeInt(stacks.size)
            for (s in stacks) {
                ByteBufUtils.writeItemStack(buf, s)
            }
            buf.writeBoolean(name != null)
            if (name != null) {
                ByteBufUtils.writeUTF8String(buf, name)
            }
            buf.writeBoolean(lock != null)
            if (lock != null) {
                buf.writeLong(lock!!.mostSignificantBits)
                buf.writeLong(lock!!.leastSignificantBits)
            }
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            stacks = arrayOfNulls(buf.readInt())
            for (i in stacks.indices) {
                stacks[i] = ByteBufUtils.readItemStack(buf)
            }
            if (buf.readBoolean()) {
                name = ByteBufUtils.readUTF8String(buf)
            }
            if (buf.readBoolean()) {
                lock = UUID(buf.readLong(), buf.readLong())
            }
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().addScheduledTask {
                val tile = Minecraft.getMinecraft().theWorld.getTileEntity(msg.pos)
                if (tile is AnvilLogic) {
                    for (i in msg.stacks.indices) {
                        tile.inventory.setStackInSlot(i, msg.stacks[i])
                    }
                    tile.currentName = msg.name
                    tile.playerLock = msg.lock
                }
            }
            return null
        }
    }
}