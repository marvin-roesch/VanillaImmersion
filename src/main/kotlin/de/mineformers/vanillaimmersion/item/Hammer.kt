package de.mineformers.vanillaimmersion.item

import com.google.common.collect.Multimap
import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

/**
 * The Hammer serves as tool for editing objects on the anvil.
 */
class Hammer : Item() {
    init {
        creativeTab = VanillaImmersion.CREATIVE_TAB
        unlocalizedName = "$MODID.hammer"
        registryName = ResourceLocation(MODID, "hammer")
        maxStackSize = 1
    }

    @SideOnly(Side.CLIENT)
    override fun isFull3D(): Boolean {
        return true
    }

    override fun getAttributeModifiers(equipmentSlot: EntityEquipmentSlot,
                                       stack: ItemStack): Multimap<String, AttributeModifier> {
        val multimap = super.getAttributeModifiers(equipmentSlot, stack)

        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.attributeUnlocalizedName,
                         AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.4, 0))
        }

        return multimap
    }
}