package de.mineformers.vanillaimmersion.tileentity

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.Blocks.ENCHANTING_TABLE
import de.mineformers.vanillaimmersion.client.EnchantingUIHandler
import de.mineformers.vanillaimmersion.client.particle.EnchantingParticle
import de.mineformers.vanillaimmersion.network.EnchantingTableUpdate
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ContainerEnchantment
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityEnchantmentTable
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldServer
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.Constants
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.oredict.OreDictionary

/**
 * ${JDOC}
 */
class EnchantingTableLogic : TileEntityEnchantmentTable() {
    companion object {
        enum class Slot {
            OBJECT, MODIFIERS
        }
    }

    internal inner class CraftingTableInventory : ItemStackHandler(2) {
        val lapis = OreDictionary.getOres("gemLapis")

        override fun getStackLimit(slot: Int, stack: ItemStack?) =
            if (slot == 0)
                1
            else if (lapis.any { OreDictionary.itemMatches(it, stack, false) })
                3
            else
                0

        override fun onContentsChanged(slot: Int) {
            if (worldObj is WorldServer)
                updateEnchantment(FakePlayerFactory.getMinecraft(worldObj as WorldServer))
            markDirty()
            sync()
        }

        val contents: Array<ItemStack?>
            get() = stacks
    }

    internal val inventory = CraftingTableInventory()
    internal val enchantabilities = intArrayOf(0, 0, 0)
    internal val enchantmentIds = intArrayOf(-1, -1, -1)
    internal val enchantmentLevels = intArrayOf(-1, -1, -1)
    internal var nameSeed = 0L
    internal var page = -1
    internal var enchantmentProgress = 0
    internal var consumedModifiers = 0
    internal var enchantmentResult: ItemStack? = null

    internal var bobStop = 0f

    operator fun get(slot: Slot): ItemStack? = inventory.getStackInSlot(slot.ordinal)

    operator fun set(slot: Slot, stack: ItemStack?) = inventory.setStackInSlot(slot.ordinal, stack)

    override fun update() {
        var markDirty = false
        bookSpreadPrev = bookSpread
        bookRotationPrev = bookRotation
        val closetPlayer = worldObj.getClosestPlayer(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 3.0, false)

        if (closetPlayer != null) {
            val d0 = closetPlayer.posX - (this.pos.x.toFloat() + 0.5f).toDouble()
            val d1 = closetPlayer.posZ - (this.pos.z.toFloat() + 0.5f).toDouble()
            tRot = MathHelper.atan2(d1, d0).toFloat()
            bookSpread += 0.1f
        } else {
            tRot += 0.02f
            bookSpread -= 0.1f
        }

        while (this.bookRotation >= Math.PI.toFloat()) {
            bookRotation -= Math.PI.toFloat() * 2f
        }

        while (this.bookRotation < -Math.PI.toFloat()) {
            bookRotation += Math.PI.toFloat() * 2f
        }

        while (this.tRot >= Math.PI.toFloat()) {
            tRot -= Math.PI.toFloat() * 2f
        }

        while (this.tRot < -Math.PI.toFloat()) {
            tRot += Math.PI.toFloat() * 2f
        }

        var dRot = tRot - bookRotation
        while (dRot >= Math.PI.toFloat()) {
            dRot -= Math.PI.toFloat() * 2f
        }

        while (dRot < -Math.PI.toFloat()) {
            dRot += Math.PI.toFloat() * 2f
        }

        bookRotation += dRot * 0.4f
        bookSpread = MathHelper.clamp_float(this.bookSpread, 0.0f, 1.0f)
        ++tickCount

        if (enchantmentResult != null) {
            if (enchantmentProgress == 0 && worldObj.isRemote) {
                // Super client-safe code, believe me ;)
                bobStop = tickCount + EnchantingUIHandler.partialTicks
            }
            enchantmentProgress++
            if ((40..80).contains(enchantmentProgress) && worldObj.isRemote) {
                // Same here
                val p = Vec3d(Math.random(), 0.75, Math.random()) + pos
                val d = Vec3d(0.5, 1.75, 0.5) + pos
                val entity = EnchantingParticle(worldObj, p.x, p.y, p.z, d.x, d.y, d.z)
                Minecraft.getMinecraft().effectRenderer.addEffect(entity)
            }
            if (enchantmentProgress > 120 && !worldObj.isRemote) {
                Inventories.spawn(worldObj, pos, EnumFacing.UP, enchantmentResult)
                this[Slot.OBJECT] = null
                if (this[Slot.MODIFIERS] != null)
                    this[Slot.MODIFIERS]!!.stackSize -= consumedModifiers
                consumedModifiers = 0
                enchantmentProgress = 0
                bobStop = 0f
                enchantmentResult = null
            }
            markDirty = true
        }

        for (i in inventory.contents.indices) {
            val stack = inventory.getStackInSlot(i)
            if (stack?.stackSize == 0) {
                inventory.setStackInSlot(i, null)
                markDirty = true
            } else if (enchantmentResult == null && stack != null && bookSpread <= 0.0 && !worldObj.isRemote) {
                Inventories.spawn(worldObj, pos, EnumFacing.UP, inventory.extractItem(i, Int.MAX_VALUE, false))
                markDirty = true
            }
        }

        if (markDirty) {
            markDirty()
            sync()
        }
    }

    fun updateEnchantment(player: EntityPlayer) {
        val container = ContainerEnchantment(player.inventory, worldObj, pos)
        container.tableInventory.setInventorySlotContents(0, this[Slot.OBJECT]?.copy())
        container.tableInventory.setInventorySlotContents(1, this[Slot.MODIFIERS]?.copy())
        for (i in enchantabilities.indices) {
            enchantabilities[i] = container.enchantLevels[i]
            enchantmentIds[i] = container.enchantClue[i]
            enchantmentLevels[i] = container.worldClue[i]
        }
        nameSeed = container.xpSeed.toLong()
        if (enchantabilities.any { it > 0 })
            page = 0
        else
            page = -1
        markDirty()
        sync()
    }

    private fun tryEnchantment(player: EntityPlayer, enchantment: Int) {
        val container = ContainerEnchantment(player.inventory, worldObj, pos)
        container.tableInventory.setInventorySlotContents(0, this[Slot.OBJECT]?.copy())
        container.tableInventory.setInventorySlotContents(1, this[Slot.MODIFIERS]?.copy())
        if (container.enchantItem(player, enchantment)) {
            enchantmentResult = container.tableInventory.getStackInSlot(0)
            enchantmentProgress = 0
            val oldModifiers = this[Slot.MODIFIERS]?.stackSize ?: 0
            val modifiers = container.tableInventory.getStackInSlot(1)?.stackSize ?: 0
            consumedModifiers = oldModifiers - modifiers
            markDirty()
            sync()
        }
    }

    fun performPageAction(player: EntityPlayer, page: Int, x: Double, y: Double) {
        if (enchantmentResult != null)
            return
        var sync = false
        if ((page % 2 == 0 && x >= 17 && y >= 98 && x <= 87 && y <= 118)
            || (page % 2 == 1 && x >= 7 && y >= 98 && x <= 77 && y <= 118)) {
            if (page == 0) {
                for (i in 0..(inventory.slots - 1)) {
                    val extracted = inventory.extractItem(i, Int.MAX_VALUE, false)
                    Inventories.insertOrDrop(player, extracted)
                }
                updateEnchantment(player)
            } else {
                tryEnchantment(player, page - 1)
            }
        } else if ((x <= 47 || x <= 16 && y >= 98 && y <= 118) && page == 2) {
            this.page = Math.max(this.page - 2, 0)
            worldObj.playSound(null, pos, VanillaImmersion.Sounds.ENCHANTING_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1f)
            sync = true
        } else if ((x >= 47 || x >= 78 && y >= 98 && y <= 118) && page == 1) {
            this.page = Math.min(this.page + 2, 2)
            worldObj.playSound(null, pos, VanillaImmersion.Sounds.ENCHANTING_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1f)
            sync = true
        } else {
            ENCHANTING_TABLE.onBlockActivated(world, pos, world.getBlockState(pos),
                                              player, EnumHand.MAIN_HAND, player.getHeldItem(EnumHand.MAIN_HAND),
                                              EnumFacing.UP, 0f, 0f, 0f)
        }
        if (sync)
            sync()
    }

    override fun writeToNBT(compound: NBTTagCompound?) {
        super.writeToNBT(compound)
        compound!!.setTag("Inventory", ITEM_HANDLER_CAPABILITY.writeNBT(inventory, null))
    }

    override fun readFromNBT(compound: NBTTagCompound?) {
        super.readFromNBT(compound)
        ITEM_HANDLER_CAPABILITY.readNBT(inventory, null, compound!!.getTagList("Inventory", Constants.NBT.TAG_COMPOUND))
    }

    override fun hasCapability(capability: Capability<*>?, side: EnumFacing?): Boolean {
        return (enchantmentResult == null && capability == ITEM_HANDLER_CAPABILITY) || super.hasCapability(capability,
                                                                                                           side)
    }

    override fun <T : Any?> getCapability(capability: Capability<T>?, side: EnumFacing?): T {
        @Suppress("UNCHECKED_CAST")
        if (capability == ITEM_HANDLER_CAPABILITY) {
            return inventory as T
        }
        return super.getCapability(capability, side)
    }


    override fun getDescriptionPacket() = VanillaImmersion.NETWORK.getPacketFrom(EnchantingTableUpdate.Message(this))
}