package de.mineformers.vanillaimmersion.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigList
import com.typesafe.config.ConfigMergeable
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigOrigin
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigResolveOptions
import com.typesafe.config.ConfigValue
import de.mineformers.vanillaimmersion.config.gui.KeepVanillaEntry
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Delegate object for the mod's configruation.
 */
object Configuration : Config {
    private var impl: Config? = null
    private var file: File? = null

    /**
     * The mod's configuration template.
     */
    val TEMPLATE =
        config("vanilla-immersion") {
            category("blocks") {
                list<String>("keep-vanilla") {
                    default = emptyList()
                    comment = "Specifies which blocks should retain Vanilla behaviour, none by default.\n" +
                        "Allowed values: [anvil, beacon, brewing_stand, crafting_table, enchanting_table, furnace]"
                    if (FMLCommonHandler.instance().side == Side.CLIENT)
                        guiClass = KeepVanillaEntry::class.java
                    requiresGameRestart = true
                }
                category("enchantment-table") {
                    boolean("drop-items") {
                        default = true
                        comment = "Determines whether the table should drop its contents when a player is too far away."
                    }
                }
            }
        }

    fun shouldKeepVanilla(block: String) = getStringList("blocks.keep-vanilla").contains(block)

    /**
     * Load a configuration file for the mod.
     */
    internal fun load(configDirectory: File, name: String) {
        file = configDirectory.toPath().resolve("$name.conf").toFile()
        // Generate the fallback from the template
        val fallback = ConfigFactory.empty().withValue(TEMPLATE.name, TEMPLATE.toConfig())
        val cfg = ConfigFactory
            .parseFile(file).resolve()
            .withFallback(fallback)
            .getConfig("vanilla-immersion")
        if (!file!!.exists())
            Files.write(
                file!!.toPath(),
                fallback.root().render(
                    ConfigRenderOptions
                        .defaults()
                        .setOriginComments(false)
                        .setJson(false)
                ).lines()
            )
        impl = cfg
        save()
    }

    /**
     * Updates the underlying configuration with the provided values.
     */
    fun update(value: Config) {
        impl = value
    }

    /**
     * Saves the configuration to disk.
     */
    fun save() {
        Files.write(
            file!!.toPath(),
            ConfigFactory.empty().withValue(TEMPLATE.name, root()).root()
                .render(
                    ConfigRenderOptions
                        .defaults()
                        .setOriginComments(false)
                        .setJson(false)
                ).lines()
        )
    }

    // Auto-generated delegates

    override fun root(): ConfigObject {
        return impl!!.root()
    }

    override fun atPath(path: String): Config {
        return impl!!.atPath(path)
    }

    override fun resolve(options: ConfigResolveOptions): Config {
        return impl!!.resolve(options)
    }

    @Deprecated("")
    override fun getNanosecondsList(path: String): List<Long> {
        return impl!!.getNanosecondsList(path)
    }

    @Deprecated("")
    override fun getMilliseconds(path: String): Long? {
        return impl!!.getMilliseconds(path)
    }

    @Deprecated("")
    override fun getMillisecondsList(path: String): List<Long> {
        return impl!!.getMillisecondsList(path)
    }

    override fun getBooleanList(path: String): List<Boolean> {
        return impl!!.getBooleanList(path)
    }

    override fun withValue(path: String, value: ConfigValue): Config {
        return impl!!.withValue(path, value)
    }

    override fun getLong(path: String): Long {
        return impl!!.getLong(path)
    }

    override fun withoutPath(path: String): Config {
        return impl!!.withoutPath(path)
    }

    override fun getDouble(path: String): Double {
        return impl!!.getDouble(path)
    }

    override fun getIntList(path: String): List<Int> {
        return impl!!.getIntList(path)
    }

    override fun withFallback(other: ConfigMergeable): Config {
        return impl!!.withFallback(other)
    }

    override fun getLongList(path: String): List<Long> {
        return impl!!.getLongList(path)
    }

    override fun getAnyRefList(path: String): List<Any> {
        return impl!!.getAnyRefList(path)
    }

    override fun getInt(path: String): Int {
        return impl!!.getInt(path)
    }

    override fun getDuration(path: String, unit: TimeUnit): Long {
        return impl!!.getDuration(path, unit)
    }

    override fun hasPath(path: String): Boolean {
        return impl!!.hasPath(path)
    }

    override fun getObjectList(path: String): List<ConfigObject> {
        return impl!!.getObjectList(path)
    }

    override fun getString(path: String): String {
        return impl!!.getString(path)
    }

    @Deprecated("")
    override fun getNanoseconds(path: String): Long? {
        return impl!!.getNanoseconds(path)
    }

    override fun resolve(): Config {
        return impl!!.resolve()
    }

    override fun resolveWith(source: Config, options: ConfigResolveOptions): Config {
        return impl!!.resolveWith(source, options)
    }

    override fun isResolved(): Boolean {
        return impl!!.isResolved
    }

    override fun getValue(path: String): ConfigValue {
        return impl!!.getValue(path)
    }

    override fun origin(): ConfigOrigin {
        return impl!!.origin()
    }

    override fun checkValid(reference: Config, vararg restrictToPaths: String) {
        impl!!.checkValid(reference, *restrictToPaths)
    }

    override fun getNumber(path: String): Number {
        return impl!!.getNumber(path)
    }

    override fun atKey(key: String): Config {
        return impl!!.atKey(key)
    }

    override fun getBoolean(path: String): Boolean {
        return impl!!.getBoolean(path)
    }

    override fun getDurationList(path: String, unit: TimeUnit): List<Long> {
        return impl!!.getDurationList(path, unit)
    }

    override fun getBytes(path: String): Long? {
        return impl!!.getBytes(path)
    }

    override fun getConfig(path: String): Config {
        return impl!!.getConfig(path)
    }

    override fun getStringList(path: String): List<String> {
        return impl!!.getStringList(path)
    }

    override fun resolveWith(source: Config): Config {
        return impl!!.resolveWith(source)
    }

    override fun getNumberList(path: String): List<Number> {
        return impl!!.getNumberList(path)
    }

    override fun getConfigList(path: String): List<Config> {
        return impl!!.getConfigList(path)
    }

    override fun entrySet(): Set<MutableMap.MutableEntry<String, ConfigValue>> {
        return impl!!.entrySet()
    }

    override fun getAnyRef(path: String): Any {
        return impl!!.getAnyRef(path)
    }

    override fun getBytesList(path: String): List<Long> {
        return impl!!.getBytesList(path)
    }

    override fun isEmpty(): Boolean {
        return impl!!.isEmpty
    }

    override fun getObject(path: String): ConfigObject {
        return impl!!.getObject(path)
    }

    override fun getList(path: String): ConfigList {
        return impl!!.getList(path)
    }

    override fun withOnlyPath(path: String): Config {
        return impl!!.withOnlyPath(path)
    }

    override fun getDoubleList(path: String): List<Double> {
        return impl!!.getDoubleList(path)
    }
}