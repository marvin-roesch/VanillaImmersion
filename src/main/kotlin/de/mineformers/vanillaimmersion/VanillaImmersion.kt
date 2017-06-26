package de.mineformers.vanillaimmersion

import de.mineformers.vanillaimmersion.block.Anvil
import de.mineformers.vanillaimmersion.block.Beacon
import de.mineformers.vanillaimmersion.block.BrewingStand
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.block.EnchantingTable
import de.mineformers.vanillaimmersion.block.Furnace
import de.mineformers.vanillaimmersion.client.BeaconHandler
import de.mineformers.vanillaimmersion.client.CraftingDragHandler
import de.mineformers.vanillaimmersion.client.renderer.AnvilRenderer
import de.mineformers.vanillaimmersion.client.renderer.BeaconRenderer
import de.mineformers.vanillaimmersion.client.renderer.BrewingStandRenderer
import de.mineformers.vanillaimmersion.client.renderer.CraftingTableRenderer
import de.mineformers.vanillaimmersion.client.renderer.EnchantingTableRenderer
import de.mineformers.vanillaimmersion.client.renderer.FurnaceRenderer
import de.mineformers.vanillaimmersion.client.renderer.Shaders
import de.mineformers.vanillaimmersion.config.Configuration
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.immersion.EnchantingHandler
import de.mineformers.vanillaimmersion.item.Hammer
import de.mineformers.vanillaimmersion.network.AnvilLock
import de.mineformers.vanillaimmersion.network.AnvilText
import de.mineformers.vanillaimmersion.network.BeaconScroll
import de.mineformers.vanillaimmersion.network.CraftingDrag
import de.mineformers.vanillaimmersion.network.GuiHandler
import de.mineformers.vanillaimmersion.network.OpenGui
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import de.mineformers.vanillaimmersion.util.SubSelectionHandler
import de.mineformers.vanillaimmersion.util.SubSelectionRenderer
import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.obj.OBJLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager
import net.minecraft.init.Blocks as VBlocks
import net.minecraft.init.Items as VItems

/**
 * Main entry point for Vanilla Immersion
 */
@Mod(modid = VanillaImmersion.MODID,
    name = VanillaImmersion.MOD_NAME,
    version = VanillaImmersion.VERSION,
    acceptedMinecraftVersions = "*",
    dependencies = "required-after:forgelin;required-after:forge",
    updateJSON = "@UPDATE_URL@",
    modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter",
    guiFactory = "de.mineformers.vanillaimmersion.config.gui.GuiFactory")
object VanillaImmersion {
    const val MOD_NAME = "Vanilla Immersion"
    const val MODID = "vimmersion"
    const val VERSION = "@VERSION@"

    /**
     * Proxy for client- or server-specific code
     */
    @SidedProxy
    lateinit var PROXY: Proxy
    /**
     * SimpleImpl network instance for client-server communication
     */
    val NETWORK by lazy {
        SimpleNetworkWrapper(MODID)
    }
    /**
     * Logger for the mod to inform people about things.
     */
    val LOG by lazy {
        LogManager.getLogger(MODID)
    }

    init {
        MinecraftForge.EVENT_BUS.register(Blocks)
        MinecraftForge.EVENT_BUS.register(Items)
        MinecraftForge.EVENT_BUS.register(Sounds)
    }

    /**
     * Runs during the pre-initialization phase of mod loading, registers blocks, items etc.
     */
    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        Configuration.load(event.modConfigurationDirectory, "vimmersion")

        MinecraftForge.EVENT_BUS.register(CraftingHandler)
        MinecraftForge.EVENT_BUS.register(EnchantingHandler)
        MinecraftForge.EVENT_BUS.register(SubSelectionHandler)

        // Register messages and handlers
        NETWORK.registerMessage(AnvilLock.Handler, AnvilLock.Message::class.java,
            0, Side.CLIENT)
        NETWORK.registerMessage(AnvilText.Handler, AnvilText.Message::class.java,
            1, Side.SERVER)
        NETWORK.registerMessage(CraftingDrag.Handler, CraftingDrag.Message::class.java,
            2, Side.SERVER)
        NETWORK.registerMessage(OpenGui.Handler, OpenGui.Message::class.java,
            3, Side.SERVER)
        NETWORK.registerMessage(BeaconScroll.Handler, BeaconScroll.Message::class.java,
            4, Side.SERVER)
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GuiHandler())

        PROXY.preInit(event)
    }

    /**
     * Holder object for all blocks introduced by this mod.
     * Due to be reworked once MinecraftForge's substitutions are fixed.
     * Blocks utilize lazy initialization to guarantee they're not created before first access in [Blocks.init]
     */
    object Blocks {
        /**
         * Immersive Furnace
         */
        val FURNACE by lazy {
            Furnace(false)
        }
        /**
         * Immersive Furnace - Lit variant (required for Vanilla compatibility)
         */
        val LIT_FURNACE by lazy {
            Furnace(true)
        }
        /**
         * Immersive Crafting Table
         */
        val CRAFTING_TABLE by lazy {
            CraftingTable()
        }
        /**
         * Immersive Anvil
         */
        val ANVIL by lazy {
            Anvil()
        }
        /**
         * Immersive Enchantment Table
         */
        val ENCHANTING_TABLE by lazy {
            EnchantingTable()
        }
        /**
         * Immersive Brewing Stand
         */
        val BREWING_STAND by lazy {
            BrewingStand()
        }
        /**
         * Immersive Beacon
         */
        val BEACON by lazy {
            Beacon()
        }

        /**
         * Initializes and registers blocks and related data
         */
        @SubscribeEvent
        fun init(event: RegistryEvent.Register<Block>) {
            // TODO: Unify interaction handling?
            event.registry.registerAll(FURNACE, LIT_FURNACE, CRAFTING_TABLE, ANVIL, ENCHANTING_TABLE, BREWING_STAND, BEACON)

            registerTileEntity(FurnaceLogic::class.java, "furnace")
            registerTileEntity(CraftingTableLogic::class.java, "crafting_table")
            registerTileEntity(AnvilLogic::class.java, "anvil")
            registerTileEntity(EnchantingTableLogic::class.java, "enchanting_table")
            registerTileEntity(BrewingStandLogic::class.java, "brewing_stand")
            registerTileEntity(BeaconLogic::class.java, "beacon")
        }

        @SubscribeEvent
        fun onMissingMappings(event: RegistryEvent.MissingMappings<Block>) {
            for (mapping in event.mappings) {
                mapping.remap(ForgeRegistries.BLOCKS.getValue(ResourceLocation("minecraft", mapping.key.resourcePath)))
            }
        }

        private fun <T : TileEntity> registerTileEntity(clazz: Class<T>, name: String) {
            GameRegistry.registerTileEntity(clazz, "minecraft:$name")
        }
    }

    object Items {
        /**
         * Hammer for interaction with Anvil
         */
        val HAMMER by lazy {
            Hammer()
        }

        /**
         * Initializes and registers blocks and related data
         */
        @SubscribeEvent
        fun init(event: RegistryEvent.Register<Item>) {
            event.registry.register(HAMMER)
        }

        @SubscribeEvent
        fun onMissingMappings(event: RegistryEvent.MissingMappings<Item>) {
            for (mapping in event.mappings) {
                mapping.remap(ForgeRegistries.ITEMS.getValue(ResourceLocation("minecraft", mapping.key.resourcePath)))
            }
        }
    }

    /**
     * Holder object for all sound (events) added by this mod.
     */
    object Sounds {
        /**
         * Page turn sound for enchantment table
         */
        val ENCHANTING_PAGE_TURN = SoundEvent(ResourceLocation("$MODID:enchanting.page_turn"))

        /**
         * Initializes and registers sounds and related data
         */
        @SubscribeEvent
        fun init(event: RegistryEvent.Register<SoundEvent>) {
            event.registry.register(ENCHANTING_PAGE_TURN.setRegistryName(ENCHANTING_PAGE_TURN.soundName))
        }
    }

    /**
     * Interface for client & server proxies
     */
    interface Proxy {
        /**
         * Performs pre-initialization tasks for the proxy's side.
         */
        fun preInit(event: FMLPreInitializationEvent)
    }

    /**
     * The client proxy serves as client-specific interface for the mod.
     * Code that may only be accessed on the client should be put here.
     */
    class ClientProxy : Proxy {
        override fun preInit(event: FMLPreInitializationEvent) {
            OBJLoader.INSTANCE.addDomain(MODID) // We don't have OBJ models yet, but maybe in the future?
            // Initialize (i.e. compile) shaders now, removes delay on initial use later on
            Shaders.init()

            // Register TESRs
            ClientRegistry.bindTileEntitySpecialRenderer(FurnaceLogic::class.java, FurnaceRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(CraftingTableLogic::class.java, CraftingTableRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(AnvilLogic::class.java, AnvilRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(EnchantingTableLogic::class.java, EnchantingTableRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(BrewingStandLogic::class.java, BrewingStandRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(BeaconLogic::class.java, BeaconRenderer())

            // Register item models
            setItemModel(Items.HAMMER, 0, "$MODID:hammer")

            // Register client-specific event handlers
            MinecraftForge.EVENT_BUS.register(CraftingDragHandler)
            MinecraftForge.EVENT_BUS.register(SubSelectionRenderer)
            MinecraftForge.EVENT_BUS.register(BeaconHandler)
        }

        /**
         * Sets the model associated with an item.
         * Assumes the default variant is to be used ("inventory").
         */
        private fun setItemModel(item: Item, meta: Int, resource: String) {
            ModelLoader.setCustomModelResourceLocation(item,
                meta,
                ModelResourceLocation(resource, "normal"))
        }
    }

    /**
     * The server proxy serves as server-specific interface for the mod.
     * Code that may only be accessed on the sver should be put here.
     */
    class ServerProxy : Proxy {
        override fun preInit(event: FMLPreInitializationEvent) {
        }
    }
}
