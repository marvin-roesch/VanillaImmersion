package de.mineformers.vanillaimmersion.config

import net.minecraftforge.fml.client.config.ConfigGuiType
import net.minecraftforge.fml.client.config.GuiConfigEntries
import net.minecraftforge.fml.client.config.GuiEditArrayEntries
import java.util.regex.Pattern

/**
 * Mutable builder for configuration entries.
 */
sealed class MutEntry(val name: String, val type: ConfigGuiType) {
    /**
     * A reference to a localization to be used for displaying the entry.
     * If unspecified, the entry's name will be used.
     */
    var languageKey: String? = null
    /**
     * A comment to display alongside the entry.
     * Used in the GUI only if there is no localization key of the format `languageKey.tooltip`
     */
    var comment: String? = null

    /**
     * Freezes this mutable representation and turns it immutable.
     */
    abstract fun freeze(): ConfigEntry

    /**
     * A builder for [configuration categories][ConfigEntry.Category].
     */
    class Category(name: String) : MutEntry(name, ConfigGuiType.CONFIG_CATEGORY) {
        private val elements = mutableListOf<MutEntry>()

        /**
         * Adds a string property to this category.
         */
        fun string(name: String, init: ValidatedProperty<String>.() -> Unit = {}) {
            val prop = ValidatedProperty<String>(name, ConfigGuiType.STRING)
            prop.init()
            elements += prop
        }

        /**
         * Adds a string property restricted to mod IDs to this category.
         */
        fun modid(name: String, init: ValidatedProperty<String>.() -> Unit = {}) {
            val prop = ValidatedProperty<String>(name, ConfigGuiType.MOD_ID)
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single character to this category.
         */
        fun char(name: String, init: RangedProperty<Char>.() -> Unit = {}) {
            val prop = RangedProperty<Char>(name, ConfigGuiType.STRING)
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single boolean value to this category.
         */
        fun boolean(name: String, init: Property<Boolean>.() -> Unit = {}) {
            val prop = Property<Boolean>(name, ConfigGuiType.BOOLEAN)
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single byte value to this category.
         */
        fun byte(name: String, init: RangedProperty<Byte>.() -> Unit = {}) {
            val prop = RangedProperty<Byte>(name, ConfigGuiType.INTEGER)
            prop.minimum = Byte.MIN_VALUE
            prop.maximum = Byte.MAX_VALUE
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single short value to this category.
         */
        fun short(name: String, init: RangedProperty<Short>.() -> Unit = {}) {
            val prop = RangedProperty<Short>(name, ConfigGuiType.INTEGER)
            prop.minimum = Short.MIN_VALUE
            prop.maximum = Short.MAX_VALUE
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single integer value to this category.
         */
        fun int(name: String, init: RangedProperty<Int>.() -> Unit = {}) {
            val prop = RangedProperty<Int>(name, ConfigGuiType.INTEGER)
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single long value to this category.
         */
        fun long(name: String, init: RangedProperty<Long>.() -> Unit = {}) {
            val prop = RangedProperty<Long>(name, ConfigGuiType.INTEGER)
            prop.minimum = Long.MIN_VALUE
            prop.maximum = Long.MAX_VALUE
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single float value to this category.
         */
        fun float(name: String, init: RangedProperty<Float>.() -> Unit = {}) {
            val prop = RangedProperty<Float>(name, ConfigGuiType.DOUBLE)
            prop.minimum = -Float.MAX_VALUE
            prop.maximum = Float.MAX_VALUE
            prop.init()
            elements += prop
        }

        /**
         * Adds a property for a single double value to this category.
         */
        fun double(name: String, init: RangedProperty<Double>.() -> Unit = {}) {
            val prop = RangedProperty<Double>(name, ConfigGuiType.DOUBLE)
            prop.init()
            elements += prop
        }

        /**
         * Adds a list property of a given type to this category.
         * The items of the list may be restricted.
         */
        fun <T> list(name: String, init: ListProperty<T>.() -> Unit = {}) {
            val prop = ListProperty<T>(name, ConfigGuiType.STRING)
            prop.init()
            elements += prop
        }

        /**
         * Adds a list property of booleans to this category.
         * The items of the list may be restricted.
         */
        fun booleanList(name: String, init: ListProperty<Boolean>.() -> Unit = {}) {
            val prop = ListProperty<Boolean>(name, ConfigGuiType.BOOLEAN)
            prop.init()
            elements += prop
        }

        /**
         * Adds a list property of integers to this category.
         * The items of the list may be restricted.
         */
        fun intList(name: String, init: ListProperty<Int>.() -> Unit = {}) {
            val prop = ListProperty<Int>(name, ConfigGuiType.INTEGER)
            prop.init()
            elements += prop
        }

        /**
         * Adds a list property of doubles to this category.
         * The items of the list may be restricted.
         */
        fun doubleList(name: String, init: ListProperty<Double>.() -> Unit = {}) {
            val prop = ListProperty<Double>(name, ConfigGuiType.DOUBLE)
            prop.init()
            elements += prop
        }

        /**
         * Adds another sub-category to this category.
         */
        fun category(name: String, init: Category.() -> Unit = {}) {
            val cfg = Category(name)
            cfg.init()
            elements += cfg
        }

        override fun freeze() =
            ConfigEntry.Category(name, type, languageKey, comment, elements.map { it.freeze() })
    }

    /**
     * A builder for [configuration properties][ConfigEntry.Property].
     */
    open class Property<T>(name: String, type: ConfigGuiType) : MutEntry(name, type) {
        /**
         * The default value for this property, will be stored to the default configuration file and
         * is used as a reference for the configuration GUI.
         */
        var default: T? = null
        /**
         * Specifies whether this property requires the current world has to be restarted for the changes to take effect.
         */
        var requiresWorldRestart: Boolean = false
        /**
         * Specifies whether this property requires the game has to be restarted for the changes to take effect.
         */
        var requiresGameRestart: Boolean = false
        /**
         * A list of strings potential values can be matched against.
         */
        var validValues: List<String>? = null
        /**
         * A custom class to be used for configuring this property, leave as `null` for default.
         */
        var guiClass: Class<out GuiConfigEntries.IConfigEntry>? = null

        override fun freeze() =
            ConfigEntry.Property(name, type, languageKey, comment,
                                 default, requiresWorldRestart, requiresGameRestart, validValues)
    }

    /**
     * A builder for [configuration list properties][ConfigEntry.ListProperty].
     */
    class ListProperty<T>(name: String, type: ConfigGuiType) : Property<List<T>>(name, type) {
        /**
         * The maximum number of elements this list may store.
         * Leave as `-1` for an unlimited amount.
         */
        var maxLength: Int = -1
        /**
         * Specifies whether this list is of a fixed length, i.e. remains as the length of the default value.
         */
        var fixedLength: Boolean = false
        /**
         * A custom class to be used for configuring this list's elements, leave as `null` for default.
         */
        var entryGuiClass: Class<out GuiEditArrayEntries.IArrayEntry>? = null

        override fun freeze() =
            ConfigEntry.ListProperty(name, type, languageKey, comment,
                                     default, requiresWorldRestart, requiresGameRestart, validValues, guiClass,
                                     maxLength, fixedLength, entryGuiClass)
    }

    /**
     * A builder for [validated configuration properties][ConfigEntry.ValidatedProperty].
     */
    class ValidatedProperty<T>(name: String, type: ConfigGuiType) : Property<T>(name, type) {
        /**
         * The pattern to match entered values against.
         */
        var pattern: Pattern? = null

        override fun freeze() =
            ConfigEntry.ValidatedProperty(name, type, languageKey, comment,
                                          default, requiresWorldRestart, requiresGameRestart, validValues, guiClass,
                                          pattern)
    }

    /**
     * A builder for [ranged configuration properties][ConfigEntry.RangedProperty].
     */
    class RangedProperty<T>(name: String, type: ConfigGuiType) : Property<T>(name, type) {
        /**
         * The minimum value this property may take, leave as `null` for an unlimited lower bound.
         */
        var minimum: T? = null
        /**
         * The maximum value this property may take, leave as `null` for an unlimited upper bound.
         */
        var maximum: T? = null

        override fun freeze() =
            ConfigEntry.RangedProperty(name, type, languageKey, comment,
                                       default, requiresWorldRestart, requiresGameRestart, validValues, guiClass,
                                       minimum, maximum)
    }
}

/**
 * Builds a new configuration category. Convenience method for "root" categories.
 */
fun config(name: String, init: MutEntry.Category.() -> Unit): ConfigEntry.Category {
    val cfg = MutEntry.Category(name)
    cfg.init()
    return cfg.freeze()
}