package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * ${JDOC}
 */
object CraftingTableUpdate {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var stacks: Array<ItemStack?> = emptyArray()) : IMessage {
        constructor(table: CraftingTableLogic) : this(table.pos, table.inventory.contents)

        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeInt(stacks.size)
            for (s in stacks) {
                ByteBufUtils.writeItemStack(buf, s)
            }
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            stacks = arrayOfNulls(buf.readInt())
            for (i in stacks.indices) {
                stacks[i] = ByteBufUtils.readItemStack(buf)
            }
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().addScheduledTask {
                val tile = Minecraft.getMinecraft().theWorld.getTileEntity(msg.pos)
                if (tile is CraftingTableLogic) {
                    for (i in msg.stacks.indices) {
                        tile.inventory.setStackInSlot(i, msg.stacks[i])
                    }
                }
            }
            return null
        }
    }
}