package de.mineformers.vanillaimmersion.client.gui

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.network.AnvilText
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic.Companion.Slot
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

/**
 * "GUI" for easy handling of text input for the anvil.
 */
class AnvilTextGui(val anvil: AnvilLogic) : GuiScreen() {
    /**
     * The text field making up the main part of the "GUI"
     */
    val nameField by lazy {
        val field = GuiTextField(0, this.fontRenderer, 0, 0, 102, 20)
        field.setTextColor(0xFFFFFFFF.toInt())
        field.text = anvil[Slot.INPUT_OBJECT].displayName
        field.enableBackgroundDrawing = false
        field.maxStringLength = 30
        field.isFocused = true
        field.setCanLoseFocus(false)
        field
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // We don't draw anything in this GUI
    }

    override fun doesGuiPauseGame() = false

    override fun initGui() {
        super.initGui()
        // Required since we want to allow spamming keys
        Keyboard.enableRepeatEvents(true)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Enter and ESC work for closing the GUI
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null)
            if (mc.currentScreen == null) {
                mc.setIngameFocus()
            }
            // Inform the server about the changed text
            VanillaImmersion.NETWORK.sendToServer(AnvilText.Message(anvil.pos, nameField.text))
            return
        }
        if (nameField.textboxKeyTyped(typedChar, keyCode)) {
            anvil.itemName = nameField.text
        }
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        Keyboard.enableRepeatEvents(false)
    }
}