@file:JvmName("MiscClientExtensions")

package de.mineformers.vanillaimmersion.util

import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

val KeyBinding.isDown: Boolean
    get() = if (keyCode < 0) Mouse.isButtonDown(keyCode + 100) else Keyboard.isKeyDown(keyCode)