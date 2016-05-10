package de.mineformers.vanillaimmersion.network

import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic.Companion.Slot
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * ${JDOC}
 */
object FurnaceUpdate {
    data class Message(var pos: BlockPos = BlockPos.ORIGIN,
                       var inputStack: ItemStack? = ItemStack(Blocks.AIR),
                       var fuelStack: ItemStack? = ItemStack(Blocks.AIR),
                       var fuelLeft: Int = 0,
                       var fuel: Int = 0,
                       var progress: Int = 0,
                       var requiredTime: Int = 0) : IMessage {
        constructor(furnace: FurnaceLogic) : this(furnace.pos,
                                                  furnace[Slot.INPUT],
                                                  furnace[Slot.FUEL],
                                                  furnace.getField(0),
                                                  furnace.getField(1),
                                                  furnace.getField(2),
                                                  furnace.getField(3))

        override fun toBytes(buf: ByteBuf?) {
            buf!!.writeLong(pos.toLong())
            ByteBufUtils.writeItemStack(buf, inputStack)
            ByteBufUtils.writeItemStack(buf, fuelStack)
            buf.writeInt(fuelLeft)
            buf.writeInt(fuel)
            buf.writeInt(progress)
            buf.writeInt(requiredTime)
        }

        override fun fromBytes(buf: ByteBuf?) {
            pos = BlockPos.fromLong(buf!!.readLong())
            inputStack = ByteBufUtils.readItemStack(buf)
            fuelStack = ByteBufUtils.readItemStack(buf)
            fuelLeft = buf.readInt()
            fuel = buf.readInt()
            progress = buf.readInt()
            requiredTime = buf.readInt()
        }
    }

    object Handler : IMessageHandler<Message, IMessage> {
        override fun onMessage(msg: Message, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().addScheduledTask {
                val tile = Minecraft.getMinecraft().theWorld.getTileEntity(msg.pos)
                if (tile is FurnaceLogic) {
                    tile[Slot.INPUT] = msg.inputStack
                    tile[Slot.FUEL] = msg.fuelStack
                    tile.setField(0, msg.fuelLeft)
                    tile.setField(1, msg.fuel)
                    tile.setField(2, msg.progress)
                    tile.setField(3, msg.requiredTime)
                }
            }
            return null
        }
    }
}