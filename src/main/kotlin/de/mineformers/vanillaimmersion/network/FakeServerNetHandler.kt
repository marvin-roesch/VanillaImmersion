package de.mineformers.vanillaimmersion.network

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.util.text.ITextComponent

class FakeServerNetHandler(player: EntityPlayerMP)
    : NetHandlerPlayServer(player.mcServer, NetworkManager(EnumPacketDirection.CLIENTBOUND), player) {
    override fun sendPacket(packet: Packet<*>) = Unit

    override fun update() = Unit

    override fun disconnect(textComponent: ITextComponent?) = Unit
}