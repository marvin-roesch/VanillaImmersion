package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.model.ModelBook
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIXED
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntityEnchantmentTableRenderer
import net.minecraft.client.resources.I18n
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemBlock
import net.minecraft.tileentity.TileEntityEnchantmentTable
import net.minecraft.util.EnchantmentNameParts
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.relauncher.ReflectionHelper
import java.util.*

/**
 * Renders the items on top of a crafting table as well as the book "GUI".
 */
class EnchantingTableRenderer : TileEntityEnchantmentTableRenderer() {
    companion object {
        /**
         * The field holding the enchantment clues.
         */
        private final val NAME_PARTS_FIELD by lazy {
            ReflectionHelper.findField(EnchantmentNameParts::class.java, "field_148337_c", "namePartsArray")
        }
        /**
         * The field holding the enchantment RNG, seeded with the player's XP.
         */
        private final val NAME_RAND_FIELD by lazy {
            ReflectionHelper.findField(EnchantmentNameParts::class.java, "field_148336_b", "rand")
        }

        /**
         * The enchantment clues.
         */
        private val nameParts: Array<String>
            @Suppress("UNCHECKED_CAST")
            get() = NAME_PARTS_FIELD.get(EnchantmentNameParts.getInstance()) as Array<String>

        /**
         * The enchantment RNG.
         */
        private val rand: Random
            get() = NAME_RAND_FIELD.get(EnchantmentNameParts.getInstance()) as Random

        /**
         * Generates a random piece of "lore" with a random [length] in a range.
         * Required since Vanilla's implementation seems to have issues and causes a StackOverflowException.
         */
        fun generateRandomLore(length: IntRange): String {
            // The string length is a random integer in the given range
            val l = rand.nextInt(length.endInclusive - length.start + 1) + length.start
            return (1..l).map { nameParts[rand.nextInt(nameParts.size)] }.joinToString(" ")
        }

        private fun init() {
            NAME_PARTS_FIELD.isAccessible = true
            NAME_RAND_FIELD.isAccessible = true
        }
    }

    private val BOOK_TEXTURE = ResourceLocation("vimmersion", "textures/entity/enchanting_table_book.png")
    private val BOOK_GUI_TEXTURE = ResourceLocation("textures/gui/book.png")
    private val LAPIS_TEXTURE = ResourceLocation("textures/items/dye_powder_blue.png")
    private val ENCHANTING_TABLE_TEXTURE = ResourceLocation("textures/gui/container/enchanting_table.png")
    private val ENCHANTING_LABEL_TEXTURE = ResourceLocation("vimmersion", "textures/gui/enchantment_label.png")
    private val book = ModelBook()

    init {
        init()
        // Make the pages of the book 1 "pixel" wider, otherwise text will hardly fit on them
        book.pagesRight = ModelRenderer(book).setTextureOffset(0, 10).addBox(0.0f, -4.0f, -0.99f, 6, 8, 1)
        book.pagesLeft = ModelRenderer(book).setTextureOffset(14, 10).addBox(0.0f, -4.0f, -0.01f, 6, 8, 1)
        book.flippingPageRight = ModelRenderer(book).setTextureOffset(28, 10).addBox(0.0f, -4.0f, 0.0f, 6, 8, 0)
        book.flippingPageLeft = ModelRenderer(book).setTextureOffset(28, 10).addBox(0.0f, -4.0f, 0.0f, 6, 8, 0)
    }

    // TODO: Maybe switch to FastTESR?
    override fun renderTileEntityAt(te: TileEntityEnchantmentTable, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        // Since we derive from the vanilla renderer, we can't change the type parameters
        if (te is EnchantingTableLogic) {
            pushMatrix()

            // Use the lighting of the block directly above the table
            val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
            val bX = light % 65536
            val bY = light / 65536
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

            // We need to know how much lapis lazuli modifiers the table holds to tell whether its sufficient
            val modifiers = te[Slot.MODIFIERS]
            val lapis = modifiers?.stackSize ?: 0

            pushMatrix()
            // Translate to the table's center
            translate(x + 0.5, y + 0.75, z + 0.5)
            enableLighting()

            // What follows is a straight copy from Vanilla, with meaningful names
            // We don't call the super method because we need these values later on anyway
            val hover = te.tickCount + partialTicks
            translate(0.0f, 0.1f + MathHelper.sin(hover * 0.1f) * 0.01f, 0.0f)

            var dYaw = te.bookRotation - te.bookRotationPrev
            while (dYaw >= Math.PI) {
                dYaw -= Math.PI.toFloat() * 2f
            }
            while (dYaw < -Math.PI) {
                dYaw += Math.PI.toFloat() * 2f
            }

            val yaw = te.bookRotationPrev + dYaw * partialTicks
            rotate(-yaw * (180f / Math.PI.toFloat()), 0.0f, 1.0f, 0.0f)
            rotate(80.0f, 0.0f, 0.0f, 1.0f)

            var flipLeft = te.pageFlipPrev + (te.pageFlip - te.pageFlipPrev) * partialTicks + 0.25f
            var flipRight = te.pageFlipPrev + (te.pageFlip - te.pageFlipPrev) * partialTicks + 0.75f
            flipLeft = (flipLeft - MathHelper.truncateDoubleToInt(flipLeft.toDouble()).toFloat()) * 1.6f - 0.3f
            flipRight = (flipRight - MathHelper.truncateDoubleToInt(flipRight.toDouble()).toFloat()) * 1.6f - 0.3f
            if (flipLeft < 0.0f) {
                flipLeft = 0.0f
            }
            if (flipRight < 0.0f) {
                flipRight = 0.0f
            }
            if (flipLeft > 1.0f) {
                flipLeft = 1.0f
            }
            if (flipRight > 1.0f) {
                flipRight = 1.0f
            }

            val spread = te.bookSpreadPrev + (te.bookSpread - te.bookSpreadPrev) * partialTicks

            this.bindTexture(BOOK_TEXTURE)
            enableCull()
            this.book.render(null, hover, flipLeft, flipRight, spread, 0.0f, 0.0625f)
            // End of Vanilla part

            popMatrix()

            // We only draw the page content if its actually visible (i.e. book is not closed)
            if (spread > 0) {
                renderPages(te, x, y, z, lapis, yaw, hover, flipLeft, spread)
            }

            // Renders the items
            color(1f, 1f, 1f, 1f)
            translate(x + 0.5, y + 1.4, z + 0.5)

            Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
            enableRescaleNormal()
            RenderHelper.enableStandardItemLighting()

            // Seed the animation randomizer with the TE's position, this will make the animations consistent
            val rand = Random(te.pos.toLong())
            val bobRand = rand.nextInt(100)
            // The bobbing should stop momentarily when there is a result
            val bobTicks = if (te.result == null) te.tickCount + partialTicks else te.bobStop
            val bob = MathHelper.sin((bobTicks + bobRand) / 10.0f) * 0.1 + 0.1
            // Only perform the upwards animation if there is a result
            val t =
                if (te.result == null)
                    0.0
                else
                    Math.min(te.progress + partialTicks, 40f) / 40.0
            // Only perform the upwards animation if there is a result
            val inputAnim =
                if (te.result != null)
                    bob + (0.3 - bob) * t
                else
                    0.0
            renderItem(te, Slot.OBJECT, 0.0, bob + inputAnim, 0.0)

            // Get the offsets for each modifier's animation before they are used
            // Otherwise there might be inconsistency for the positions
            val lapisOffsets = listOf(rand.nextInt(720), rand.nextInt(720), rand.nextInt(720))
            val rotationOffset = rand.nextInt(50)

            // Render the modifiers in a circle around the object to enchant
            for (i in 1..lapis) {
                val offset = te.tickCount + rotationOffset + partialTicks
                val bobAngle = Math.toRadians(offset * 4.0 + lapisOffsets[i - 1])
                val angle = Math.toRadians((360.0 / modifiers!!.stackSize) * i + offset).toFloat()
                val currentX = MathHelper.sin(angle) * 0.5
                val currentZ = MathHelper.cos(angle) * 0.5
                renderItem(te, Slot.MODIFIERS, currentX, Math.sin(bobAngle) * 0.1, currentZ)
            }

            RenderHelper.disableStandardItemLighting()
            disableRescaleNormal()

            popMatrix()
        }
    }

    /**
     * Renders the currently visible pages of the enchantment table
     */
    private fun renderPages(te: EnchantingTableLogic, x: Double, y: Double, z: Double, lapis: Int,
                            yaw: Float, hover: Float, flipLeft: Float, spread: Float) {
        // Reseed the RNG, just like Vanilla
        EnchantmentNameParts.getInstance().reseedRandomGenerator(te.xpSeed)

        pushMatrix()

        // Disable lighting and depth since they are not required for the "GUI"
        disableLighting()
        depthMask(false)

        // Values copied from ModelBook
        // "breath" refers to the slight closing and reopening of the book
        val breath = (MathHelper.sin(hover * 0.02f) * 0.1f + 1.25f) * spread
        val rotation = breath - breath * 2.0f * flipLeft
        // Apply transformations such that we can work relatively to the center fold of the book
        translate(x + 0.5, y + 0.75 + 1.6 * 0.0625f + 0.0001, z + 0.5)
        translate(0f, MathHelper.sin(hover * 0.1f) * 0.01f, 0f)
        rotate(-yaw * (180f / Math.PI.toFloat()), 0f, 1f, 0f)
        rotate(80f, 0f, 0f, 1f)
        translate(MathHelper.sin(breath) / 16, 0f, 0f)

        // Render the left page
        pushMatrix()
        // Again, values from ModelBook
        rotate(-rotation * (180f / Math.PI.toFloat()), 0f, 1f, 0f)
        translate(0.0, 0.25, 0.0)
        translate(0.375f, 0f, 0f)
        // Transform to GUI scale
        scale(-0.004, -0.004, 0.004)

        // We may use GUI methods from here since we have applied transformations that let us move in its local space
        if (te.page == -1) {
            // No actual page visible: Draw a bunch of nonsense
            drawWrappedText("~fa" + generateRandomLore(30..40), 4, 4, 86, 0x685E4A, 125 / 9 - 1)
        } else if (te.page == 2) {
            // Page 2 shows the second available enchantment
            renderEnchantmentPage(te, 1, lapis)
        } else if (te.page == 0) {
            // Page 0 shows the "Cancel" button
            drawWrappedText("~fa" + generateRandomLore(30..40), 4, 4, 86, 0x685E4A, 98 / 9 - 1)
            // Only allow cancelling as long as there is no result
            if (te.result == null) {
                Minecraft.getMinecraft().textureManager.bindTexture(ENCHANTING_LABEL_TEXTURE)
                color(1f, 1f, 1f, 1f)
                Gui.drawModalRectWithCustomSizedTexture((94 - 70) / 2 + 5, 98, 0f, 0f, 70, 20, 70f, 20f)
                val font = Minecraft.getMinecraft().fontRendererObj
                val text = I18n.format("gui.cancel")
                val width = font.getStringWidth(text)
                font.drawString(text, (94 - width) / 2 + 5, 98 + (20 - font.FONT_HEIGHT) / 2 + 1, 0x685E4A)
            }
        }
        popMatrix()

        // Render the right page
        pushMatrix()
        // Again, values from ModelBook
        rotate(rotation * (180f / Math.PI.toFloat()), 0f, 1f, 0f)
        translate(0.0, 0.25, 0.0)
        // Transform to GUI scale
        scale(0.004, -0.004, 0.004)

        // We may use GUI methods from here since we have applied transformations that let us move in its local space
        Gui.drawRect(0, 0, 94, 125, 0xFFF3F3F3.toInt())
        if (te.page == -1) {
            // No actual page visible: Draw a bunch of nonsense
            drawWrappedText("~fa" + generateRandomLore(30..40), 4, 4, 86, 0x685E4A, 125 / 9 - 1)
        } else if (te.page == 0) {
            // The TE only holds the reference to the left page
            // Left page 0 means right page 1
            // Page 1 shows the first available enchantment
            renderEnchantmentPage(te, 0, lapis)
        } else if (te.page == 2) {
            // See above why page 3
            // Page 3 shows the third available enchantment
            renderEnchantmentPage(te, 2, lapis)
        }
        popMatrix()

        depthMask(true)
        enableLighting()

        popMatrix()
    }

    /**
     * Renders the information about an enchantment.
     */
    private fun renderEnchantmentPage(te: EnchantingTableLogic, index: Int, lapis: Int) {
        // Gather the data about the enchantment
        val enchantment = Enchantment.getEnchantmentByID(te.enchantmentIds[index])
        val requiredLevel = te.requiredLevels[index]
        val level = te.enchantmentLevels[index]

        // Draw the left and right page flip buttons when appropriate
        Minecraft.getMinecraft().textureManager.bindTexture(BOOK_GUI_TEXTURE)
        if (index == 0) {
            val x = 75
            val y = 98 + (20 - 13) / 2 + 1 + if (requiredLevel == 0 || enchantment == null) 8 else 0
            Gui.drawScaledCustomSizeModalRect(x, y, 23f, 192f, 23, 13, 20, 10, 256f, 256f)
        } else if (index == 1) {
            val x = -2
            val y = 98 + (20 - 13) / 2 + 1 + if (requiredLevel == 0 || enchantment == null) 8 else 0
            Gui.drawScaledCustomSizeModalRect(x, y, 23f, 205f, 23, 13, 20, 10, 256f, 256f)
        }

        // If there is no enchantment to be achieved, just draw a bunch of nonsense
        if (requiredLevel == 0 || enchantment == null) {
            drawWrappedText("~fa" + generateRandomLore(30..40), 4, 4, 86, 0x685E4A, 125 / 9 - 1)
            return
        }

        // The clue is the name of an enchantment that is guaranteed to be included in this option
        val clue = I18n.format("container.enchant.clue", enchantment.getTranslatedName(level))
        // Hide the clue in a bunch of garbage
        val text = "~fa${generateRandomLore(5..8)} ~fn$clue ~fa${generateRandomLore(25..36)}"
        // Give some actually useful information at the end
        val information = "~faModifiers ~fn~m${lapis}_${index + 1}\n " +
                          "~faLevels ~fn~l${index}_$requiredLevel"
        drawWrappedText(text, 4, 4, 86, 0x685E4A, 98 / 9 - 3)
        drawWrappedText(information, 4, 76, 86, 0x685E4A, 2)

        // When there is a result, don't draw the "Enchant" button
        if (te.result != null)
            return
        // Draw the "Enchant" button
        val buttonOffset = if (index % 2 == 1) 5 else -5
        Minecraft.getMinecraft().textureManager.bindTexture(ENCHANTING_LABEL_TEXTURE)
        color(1f, 1f, 1f, 1f)
        Gui.drawModalRectWithCustomSizedTexture((94 - 70) / 2 + buttonOffset, 98, 0f, 0f, 70, 20, 70f, 20f)
        val enchantText = I18n.format("container.enchant")
        val font = Minecraft.getMinecraft().fontRendererObj
        val width = font.getStringWidth(enchantText)
        font.drawString(enchantText, (94 - width) / 2 + buttonOffset, 98 + (20 - font.FONT_HEIGHT) / 2 + 1, 0x685E4A)
    }

    /**
     * Draws text by automatically wrapping it when the maximum width is exceeded.
     * **Highly** specific to the usage, respects special characters for changing formatting etc.
     */
    private fun drawWrappedText(text: String, x: Int, y: Int, width: Int, color: Int, maxLines: Int = -1) {
        // TODO: Clean this mess up
        var font = Minecraft.getMinecraft().fontRendererObj
        var words = text.split(" ")
        var consumedWidth = 0
        var x1 = x
        var y1 = y
        var lines = 1
        while (words.isNotEmpty()) {
            // If we've exceeded the maximum amount of lines (i.e. we would interfere with other text), abort
            if (maxLines > -1 && lines > maxLines)
                return
            // Analyze the first word of the list
            var word = words[0]
            words = words.drop(1)

            // The ~f modifier changes the active font
            if (word.startsWith("~f")) {
                if (word[2].toLowerCase() == 'a')
                    font = Minecraft.getMinecraft().standardGalacticFontRenderer
                else
                    font = Minecraft.getMinecraft().fontRendererObj
                word = word.substring(3)
            }

            // The ~m modifier will draw a lapis lazuli icon with the required amount next to it
            var drawLapis = false
            if (word.startsWith("~m")) {
                val lapis = word[2].toString().toInt()
                val required = word.substring(4).trim('\n').toInt()
                val red = !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode && lapis < required
                word = "  : " + (if (red) TextFormatting.RED else TextFormatting.RESET) + word.substring(4)
                drawLapis = true
            }

            // The ~l modifier will draw the classifier icon for the enchantment and the required amount of levels
            var drawnXP = -1
            var xpV = 223f
            if (word.startsWith("~l")) {
                drawnXP = word[2].toString().toInt()
                val requiredLevel = word.substring(4).toInt()
                val insufficient = Minecraft.getMinecraft().thePlayer.experienceLevel < requiredLevel &&
                                   !Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode
                if (insufficient)
                    xpV = 239f
                word = "   : ${if (insufficient) TextFormatting.RED else TextFormatting.RESET}$requiredLevel"
            }

            // Draw the word
            val wordWidth = font.getStringWidth(word)
            var newWidth = consumedWidth + font.getStringWidth(word)
            if (consumedWidth != 0)
                newWidth += font.getCharWidth(' ')
            // If the word would exceed the width, move it to a new line
            if (newWidth > width) {
                x1 = x
                y1 += font.FONT_HEIGHT
                font.drawString(word.trim('\n'), x1, y1, color)
                // Handle our special cases
                if (drawLapis) {
                    color(1f, 1f, 1f, 1f)
                    Minecraft.getMinecraft().textureManager.bindTexture(LAPIS_TEXTURE)
                    Gui.drawModalRectWithCustomSizedTexture(x1, y1, 0f, 0f, 8, 8, 8f, 8f)
                }
                if (drawnXP > -1) {
                    color(1f, 1f, 1f, 1f)
                    Minecraft.getMinecraft().textureManager.bindTexture(ENCHANTING_TABLE_TEXTURE)
                    Gui.drawScaledCustomSizeModalRect(x1 - 1, y1 - 2, 16f * drawnXP, xpV, 16, 16, 12, 12, 256f, 256f)
                }
                x1 += wordWidth
                consumedWidth = wordWidth
                lines += 1
            } else {
                if (consumedWidth != 0 && !drawLapis && drawnXP == -1)
                    x1 += font.getCharWidth(' ')
                font.drawString(word.trim('\n'), x1, y1, color)
                // Handle our special cases
                if (drawLapis) {
                    color(1f, 1f, 1f, 1f)
                    Minecraft.getMinecraft().textureManager.bindTexture(LAPIS_TEXTURE)
                    Gui.drawModalRectWithCustomSizedTexture(x1, y1, 0f, 0f, 8, 8, 8f, 8f)
                }
                if (drawnXP > -1) {
                    color(1f, 1f, 1f, 1f)
                    Minecraft.getMinecraft().textureManager.bindTexture(ENCHANTING_TABLE_TEXTURE)
                    Gui.drawScaledCustomSizeModalRect(x1 - 1, y1 - 2, 16f * drawnXP, xpV, 16, 16, 12, 12, 256f, 256f)
                }
                x1 += wordWidth
                consumedWidth = x1 - x
            }
            // If there is an explicit line break, insert it
            if (word.endsWith("\n")) {
                x1 = x
                y1 += font.FONT_HEIGHT
                consumedWidth = 0
                lines += 1
            }
        }
    }

    /**
     * Renders an item on top of the enchanting table
     */
    private fun renderItem(te: EnchantingTableLogic, slot: Slot, x: Double, y: Double, z: Double) {
        pushMatrix()
        translate(x, y, z)
        scale(0.5, 0.5, 0.5)
        val stack = te[slot]
        // Most blocks use a block model which requires special treatment
        if (stack?.item is ItemBlock) {
            scale(1.5f, 1.5f, 1.5f)
        }
        // The modifiers shouldn't be quite as big
        if (slot == Slot.MODIFIERS)
            scale(0.5, 0.5, 0.5)
        rotate(45f, 0f, 1f, 0f)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }
}