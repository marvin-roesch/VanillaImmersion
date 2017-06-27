package de.mineformers.vanillaimmersion.config.gui

import com.google.common.base.CaseFormat
import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.config.ConfigEntry
import de.mineformers.vanillaimmersion.config.ConfigStorage
import net.minecraftforge.fml.client.config.IConfigElement

/**
 * [IConfigElement] implementation for [configuration entries][ConfigEntry].
 */
class ConfigElement(val path: String, val entry: ConfigEntry, val storage: ConfigStorage) : IConfigElement {
    override fun getName() = entry.name

    override fun getQualifiedName() = name

    override fun getLanguageKey() =
        entry.languageKey ?:
            "config.${VanillaImmersion.MODID}.${CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, path)}"

    override fun getComment() = entry.comment

    override fun getType() = entry.type

    override fun isProperty() = entry !is ConfigEntry.Category

    override fun requiresMcRestart() =
        when (entry) {
            is ConfigEntry.Property<*> -> entry.requiresGameRestart
            else -> false
        }

    override fun requiresWorldRestart() =
        when (entry) {
            is ConfigEntry.Property<*> -> entry.requiresWorldRestart
            else -> false
        }

    override fun getChildElements() =
        when (entry) {
            is ConfigEntry.Category -> entry.elements.map { ConfigElement("$path.${it.name}", it, storage) }
            else -> null
        }

    override fun getConfigEntryClass() =
        when (entry) {
            is ConfigEntry.Property<*> -> entry.guiClass
            else -> null
        }

    override fun getDefault() =
        when (entry) {
            is ConfigEntry.Property<*> -> entry.default
            else -> null
        }

    override fun isDefault() = get() == default

    override fun getValidValues() =
        when (entry) {
            is ConfigEntry.Property<*> -> entry.validValues?.toTypedArray()
            else -> null
        }

    override fun get() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> (storage.get(path) as? List<*>?)?.toTypedArray()
            else -> storage.get(path)
        }

    override fun setToDefault() {
        set(default)
    }

    override fun set(value: Any?) {
        storage.set(path, value)
    }

    override fun set(value: Array<out Any>?) {
        storage.set(path, value)
    }

    override fun getValidationPattern() =
        when (entry) {
            is ConfigEntry.ValidatedProperty<*> -> entry.pattern
            else -> null
        }

    override fun getMinValue() =
        when (entry) {
            is ConfigEntry.RangedProperty<*> -> entry.minimum
            else -> null
        }

    override fun getMaxValue() =
        when (entry) {
            is ConfigEntry.RangedProperty<*> -> entry.maximum
            else -> null
        }

    override fun isList() = entry is ConfigEntry.ListProperty<*>

    override fun getArrayEntryClass() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> entry.entryGuiClass
            else -> null
        }

    override fun getMaxListLength() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> entry.maxLength
            else -> -1
        }

    override fun isListLengthFixed() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> entry.fixedLength
            else -> false
        }

    override fun getList() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> get() as? Array<Any>?
            else -> null
        }

    override fun getDefaults() =
        when (entry) {
            is ConfigEntry.ListProperty<*> -> entry.default?.toTypedArray()
            else -> null
        }

    override fun showInGui() = true
}