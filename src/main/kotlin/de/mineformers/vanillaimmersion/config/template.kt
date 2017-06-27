package de.mineformers.vanillaimmersion.config

import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigOrigin
import com.typesafe.config.ConfigUtil
import com.typesafe.config.ConfigValue
import com.typesafe.config.impl.ConfigImpl
import net.minecraftforge.fml.client.config.ConfigGuiType
import net.minecraftforge.fml.client.config.GuiConfigEntries
import net.minecraftforge.fml.client.config.GuiEditArrayEntries
import net.minecraftforge.fml.relauncher.ReflectionHelper
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.*
import java.util.regex.Pattern

/**
 * A simple configuration entry. May be a category or a property.
 */
sealed class ConfigEntry(val name: String, val type: ConfigGuiType, val languageKey: String?, val comment: String?) {
    /**
     * A configuration category holding onto other entries. Children may be a mix of categories and properties.
     */
    class Category(name: String, type: ConfigGuiType, languageKey: String?, comment: String?,
                   val elements: List<ConfigEntry>) :
        ConfigEntry(name, type, languageKey, comment) {
        /**
         * Mad reflection hax because TypeSafe Config is a little too strict with its immutability.
         */
        companion object {
            /**
             * Reference to the "config value from object" method.
             */
            private val CONFIG_FROM_ANY_REF: Method
            /**
             * Reference to the "set comment for config value" method.
             */
            private val SET_ORIGIN_COMMENT: Method
            /**
             * Reference to the [com.typesafe.config.impl.FromMapMode.KEYS_ARE_KEYS] field.
             */
            private val KEYS_ARE_KEYS: Any
            /**
             * Reference to the [ConfigImpl.defaultValueOrigin] field.
             */
            private val DEFAULT_ORIGIN: ConfigOrigin
            /**
             * Reference to the [com.typesafe.config.impl.SimpleConfigList] constructor.
             */
            private val SIMPLE_CONFIG_LIST_CONSTRUCTOR: Constructor<*>

            init {
                val fromMapModeClass = Class.forName("com.typesafe.config.impl.FromMapMode")
                val originClass = Class.forName("com.typesafe.config.impl.SimpleConfigOrigin")
                val listClass = Class.forName("com.typesafe.config.impl.SimpleConfigList")
                CONFIG_FROM_ANY_REF = ReflectionHelper.findMethod(ConfigImpl::class.java as Class<Any?>,
                                                                  "fromAnyRef",
                                                                  "fromAnyRef",
                                                                  Any::class.java,
                                                                  ConfigOrigin::class.java,
                                                                  fromMapModeClass)
                CONFIG_FROM_ANY_REF.isAccessible = true
                SET_ORIGIN_COMMENT = ReflectionHelper.findMethod(originClass as Class<Any?>,
                                                                 "setComments",
                                                                 "setComments",
                                                                 List::class.java)
                SET_ORIGIN_COMMENT.isAccessible = true
                KEYS_ARE_KEYS = ReflectionHelper.getPrivateValue(fromMapModeClass as Class<Any?>, null,
                                                                 "KEYS_ARE_KEYS")
                DEFAULT_ORIGIN = ReflectionHelper.getPrivateValue(ConfigImpl::class.java as Class<Any?>, null,
                                                                  "defaultValueOrigin")
                SIMPLE_CONFIG_LIST_CONSTRUCTOR = listClass.getDeclaredConstructor(ConfigOrigin::class.java, java.util.List::class.java)
                SIMPLE_CONFIG_LIST_CONSTRUCTOR.isAccessible = true
            }

            /**
             * Creates an origin instance with the given comment.
             */
            private fun origin(comment: String?): ConfigOrigin {
                return SET_ORIGIN_COMMENT.invoke(DEFAULT_ORIGIN, comment?.lines()) as ConfigOrigin
            }

            /**
             * Creates a config value from any object.
             */
            fun fromAnyRef(obj: Any?, comment: String?): ConfigValue {
                if (obj is Collection<*> && obj.isNotEmpty()) {
                    val i = obj.iterator()
                    if (!i.hasNext())
                        return fromAnyRef(emptyList<Any>(), comment)

                    val values = ArrayList<ConfigValue>()
                    while (i.hasNext()) {
                        val v = fromAnyRef(i.next(), null)
                        values.add(v)
                    }

                    return SIMPLE_CONFIG_LIST_CONSTRUCTOR.newInstance(origin(comment), values) as ConfigValue
                }
                return CONFIG_FROM_ANY_REF.invoke(null, obj, origin(comment), KEYS_ARE_KEYS) as ConfigValue
            }
        }

        /**
         * Gets the config entry at a given path or null if it does not exist.
         */
        operator fun get(path: String): ConfigEntry? {
            fun get(keys: List<String>, cat: Category): ConfigEntry? {
                val elem = cat.elements.find { it.name == keys.first() }
                if (keys.size == 1)
                    return elem
                else
                    return when (elem) {
                        is Category -> get(keys.drop(1), elem)
                        else -> null
                    }
            }

            return get(ConfigUtil.splitPath(path), this)
        }

        /**
         * Converts this category into a mapping from its children names to their respective default value.
         */
        fun toMap(): Map<String, Any?> =
            elements.associateBy({ it.name }, {
                when (it) {
                    is Category -> it.toMap()
                    is Property<*> -> it.default
                }
            })

        /**
         * Converts this category into a TypeSafe Config config value.
         */
        fun toConfig(): ConfigValue =
            elements.fold(fromAnyRef(emptyMap<String, Any>(), comment) as ConfigObject, {
                acc, entry ->
                acc.withValue(entry.name, when (entry) {
                    is Category -> entry.toConfig()
                    is Property<*> -> fromAnyRef(entry.default, entry.comment)
                })
            })
    }

    /**
     * A configuration property entry. May be a primitive, a string or a list.
     */
    open class Property<T>(name: String, type: ConfigGuiType, languageKey: String?, comment: String?,
                           val default: T?, val requiresWorldRestart: Boolean, val requiresGameRestart: Boolean,
                           val validValues: List<String>?, val guiClass: Class<out GuiConfigEntries.IConfigEntry>? = null) :
        ConfigEntry(name, type, languageKey, comment)

    /**
     * A configuration property for lists.
     */
    class ListProperty<T>(name: String, type: ConfigGuiType, languageKey: String?, comment: String?,
                          default: List<T>?, requiresWorldRestart: Boolean, requiresGameRestart: Boolean,
                          validValues: List<String>?, guiClass: Class<out GuiConfigEntries.IConfigEntry>? = null,
                          val maxLength: Int, val fixedLength: Boolean, val entryGuiClass: Class<out GuiEditArrayEntries.IArrayEntry>?) :
        Property<List<T>>(name, type, languageKey, comment,
                          default, requiresWorldRestart, requiresGameRestart, validValues, guiClass)

    /**
     * A configuration property to be validated with a [Pattern].
     */
    class ValidatedProperty<T>(name: String, type: ConfigGuiType, languageKey: String?, comment: String?,
                               default: T?, requiresWorldRestart: Boolean, requiresGameRestart: Boolean,
                               validValues: List<String>?, guiClass: Class<out GuiConfigEntries.IConfigEntry>? = null,
                               val pattern: Pattern?) :
        Property<T>(name, type, languageKey, comment,
                    default, requiresWorldRestart, requiresGameRestart, validValues, guiClass)

    /**
     * A configuration property with a minimum and maximum value.
     */
    class RangedProperty<T>(name: String, type: ConfigGuiType, languageKey: String?, comment: String?,
                            default: T?, requiresWorldRestart: Boolean, requiresGameRestart: Boolean,
                            validValues: List<String>?, guiClass: Class<out GuiConfigEntries.IConfigEntry>? = null,
                            val minimum: T?, val maximum: T?) :
        Property<T>(name, type, languageKey, comment,
                    default, requiresWorldRestart, requiresGameRestart, validValues, guiClass)
}

