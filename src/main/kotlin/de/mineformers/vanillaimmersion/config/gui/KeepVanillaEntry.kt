package de.mineformers.vanillaimmersion.config.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.client.config.GuiButtonExt
import net.minecraftforge.fml.client.config.GuiConfig
import net.minecraftforge.fml.client.config.GuiConfigEntries
import net.minecraftforge.fml.client.config.GuiEditArray
import net.minecraftforge.fml.client.config.GuiEditArrayEntries
import net.minecraftforge.fml.client.config.GuiUtils
import net.minecraftforge.fml.client.config.IConfigElement
import net.minecraftforge.fml.common.registry.ForgeRegistries

class KeepVanillaEntry(owningScreen: GuiConfig, owningEntryList: GuiConfigEntries, configElement: IConfigElement)
    : GuiConfigEntries.ArrayEntry(owningScreen, owningEntryList, configElement) {
    companion object {
        val allowedValues = listOf("anvil", "beacon", "brewing_stand", "crafting_table", "enchanting_table", "furnace")
    }

    private fun getLabel(value: String): String {
        val block = ForgeRegistries.BLOCKS.getValue(ResourceLocation("minecraft", value)) ?: return "Unknown"
        val stack = if (block == Blocks.BREWING_STAND) ItemStack(Items.BREWING_STAND) else ItemStack(block)
        return I18n.format("${stack.unlocalizedName}.name")
    }

    override fun updateValueButtonText() {
        if (currentValues.isEmpty()) {
            btnValue.displayString = I18n.format("config.vimmersion.labels.none")
        } else {
            btnValue.displayString = currentValues.joinToString(", ") { getLabel(it.toString()) }
        }
    }

    override fun valueButtonPressed(slotIndex: Int) {
        mc.displayGuiScreen(EditGui(this.owningScreen, configElement, slotIndex, currentValues, enabled()))
    }

    private inner class EditGui(parentScreen: GuiScreen, configElement: IConfigElement, slotIndex: Int, currentValues: Array<Any>, enabled: Boolean)
        : GuiEditArray(parentScreen, configElement, slotIndex, currentValues, enabled) {
        override fun initGui() {
            super.initGui()
            this.entryList = EditEntries(this, this.mc, this.configElement, this.beforeValues, this.currentValues)
        }

        override fun actionPerformed(button: GuiButton) {
            super.actionPerformed(button)
            if (button.id == 2001 || button.id == 2002)
                this.entryList = EditEntries(this, this.mc, this.configElement, this.beforeValues, this.currentValues)
        }

        private inner class EditEntries(parent: GuiEditArray, mc: Minecraft, configElement: IConfigElement, beforeValues: Array<Any>, currentValues: Array<Any>)
            : GuiEditArrayEntries(parent, mc, configElement, beforeValues, currentValues) {
            private val entries = mutableMapOf<String, Entry>()

            init {
                listEntries.clear()
                for (value in allowedValues) {
                    val entry = Entry(getLabel(value), currentValues.contains(value))
                    entries[value] = entry
                    listEntries.add(entry)
                }
            }

            override fun saveListChanges() {
                val trueValues = entries.filterValues { it.value }.map { it.key }.toTypedArray()
                if (slotIndex != -1 && parentScreen != null
                    && parentScreen is GuiConfig
                    && (parentScreen as GuiConfig).entryList.getListEntry(slotIndex) is GuiConfigEntries.ArrayEntry) {
                    val entry = (parentScreen as GuiConfig).entryList.getListEntry(slotIndex) as GuiConfigEntries.ArrayEntry
                    entry.setListFromChildScreen(trueValues)
                } else {
                    configElement.set(trueValues)
                }
            }

            override fun recalculateState() {
                isDefault = true
                isChanged = false

                val trueValues = entries.filterValues { it.value }.map { it.key }
                val listLength = trueValues.size

                if (listLength != configElement.defaults.size) {
                    isDefault = false
                }

                if (listLength != beforeValues.size) {
                    isChanged = true
                }

                if (!isChanged)
                    isChanged = trueValues.any { !beforeValues.contains(it) }
            }

            private inner class Entry(val label: String, val startValue: Boolean) : IArrayEntry {
                private val btnValue: GuiButtonExt
                private var value = false
                private var isChanged = false

                init {
                    this.value = startValue
                    this.btnValue = GuiButtonExt(0, 0, 0, this@EditEntries.controlWidth, 18, I18n.format(value.toString()))
                    this.btnValue.enabled = this@EditGui.enabled
                }

                override fun keyTyped(eventChar: Char, eventKey: Int) = Unit

                override fun updateCursorCounter() = Unit

                override fun mouseClicked(x: Int, y: Int, mouseEvent: Int) = Unit

                override fun mousePressed(slotIndex: Int, x: Int, y: Int, mouseEvent: Int, relativeX: Int, relativeY: Int): Boolean {
                    if (this.btnValue.mousePressed(mc, x, y)) {
                        btnValue.playPressSound(mc.soundHandler)
                        value = !value
                        isChanged = value != startValue
                        recalculateState()
                        return true
                    }
                    return false
                }

                override fun mouseReleased(slotIndex: Int, x: Int, y: Int, mouseEvent: Int, relativeX: Int, relativeY: Int) {
                    this.btnValue.mouseReleased(x, y)
                }

                override fun updatePosition(p_192633_1_: Int, p_192633_2_: Int, p_192633_3_: Int, p_192633_4_: Float) = Unit

                override fun drawEntry(slotIndex: Int, x: Int, y: Int, listWidth: Int, slotHeight: Int, mouseX: Int, mouseY: Int, isSelected: Boolean, partialTicks: Float) {
                    this.btnValue.x = listWidth / 4 + 48
                    this.btnValue.y = y

                    val trans = I18n.format(value.toString())
                    if (trans != value.toString())
                        this.btnValue.displayString = trans
                    else
                        this.btnValue.displayString = value.toString()
                    btnValue.packedFGColour = if (value) GuiUtils.getColorCode('2', true) else GuiUtils.getColorCode('4', true)
                    val label = (if (isChanged) TextFormatting.WHITE.toString() else TextFormatting.GRAY.toString()) +
                        (if (isChanged) TextFormatting.ITALIC.toString() else "") + label
                    mc.fontRenderer.drawString(
                        label,
                        listWidth / 4 - 48,
                        y + slotHeight / 2 - mc.fontRenderer.FONT_HEIGHT / 2,
                        16777215)
                    this.btnValue.drawButton(mc, mouseX, mouseY, partialTicks)
                }

                override fun drawToolTip(mouseX: Int, mouseY: Int) = Unit

                override fun getValue() = value

                override fun isValueSavable() = true
            }
        }
    }
}
