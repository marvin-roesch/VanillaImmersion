package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
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
object EnchantingTableUpdate {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var stacks: Array<ItemStack?> = emptyArray(),
                       var enchantabilities: IntArray = intArrayOf(0, 0, 0),
                       var enchantmentIds: IntArray = intArrayOf(-1, -1, -1),
                       var enchantmentLevels: IntArray = intArrayOf(-1, -1, -1),
                       var nameSeed: Long = 0L,
                       var page: Int = -1,
                       var enchantmentProgress: Int = 0,
                       var enchantmentResult: ItemStack? = null,
                       var consumedModifiers: Int = 0) : IMessage {
        constructor(table: EnchantingTableLogic) : this(table.pos,
                                                        table.inventory.contents,
                                                        table.enchantabilities,
                                                        table.enchantmentIds,
                                                        table.enchantmentLevels,
                                                        table.nameSeed,
                                                        table.page,
                                                        table.enchantmentProgress,
                                                        table.enchantmentResult,
                                                        table.consumedModifiers)

        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            buf.writeInt(stacks.size)
            for (s in stacks) {
                ByteBufUtils.writeItemStack(buf, s)
            }
            buf.writeInt(enchantabilities.size)
            for (i in enchantabilities.indices) {
                buf.writeInt(enchantabilities[i])
                buf.writeInt(enchantmentIds[i])
                buf.writeInt(enchantmentLevels[i])
            }
            buf.writeLong(nameSeed)
            buf.writeInt(page)
            buf.writeInt(enchantmentProgress)
            ByteBufUtils.writeItemStack(buf, enchantmentResult)
            buf.writeInt(consumedModifiers)
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            stacks = arrayOfNulls(buf.readInt())
            for (i in stacks.indices) {
                stacks[i] = ByteBufUtils.readItemStack(buf)
            }
            enchantabilities = IntArray(buf.readInt())
            for (i in enchantabilities.indices) {
                enchantabilities[i] = buf.readInt()
                enchantmentIds[i] = buf.readInt()
                enchantmentLevels[i] = buf.readInt()
            }
            nameSeed = buf.readLong()
            page = buf.readInt()
            enchantmentProgress = buf.readInt()
            enchantmentResult = ByteBufUtils.readItemStack(buf)
            consumedModifiers = buf.readInt()
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().addScheduledTask {
                val tile = Minecraft.getMinecraft().theWorld.getTileEntity(msg.pos)
                if (tile is EnchantingTableLogic) {
                    for (i in msg.stacks.indices) {
                        tile.inventory.setStackInSlot(i, msg.stacks[i])
                    }
                    for (i in msg.enchantabilities.indices) {
                        tile.enchantabilities[i] = msg.enchantabilities[i]
                        tile.enchantmentIds[i] = msg.enchantmentIds[i]
                        tile.enchantmentLevels[i] = msg.enchantmentLevels[i]
                    }
                    tile.nameSeed = msg.nameSeed
                    tile.page = msg.page
                    tile.enchantmentProgress = msg.enchantmentProgress
                    tile.enchantmentResult = msg.enchantmentResult
                    tile.consumedModifiers = msg.consumedModifiers
                }
            }
            return null
        }
    }
}