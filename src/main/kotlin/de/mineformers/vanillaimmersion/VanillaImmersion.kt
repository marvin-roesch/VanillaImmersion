package de.mineformers.vanillaimmersion

import de.mineformers.vanillaimmersion.block.*
import de.mineformers.vanillaimmersion.client.BeaconHandler
import de.mineformers.vanillaimmersion.client.CraftingDragHandler
import de.mineformers.vanillaimmersion.client.renderer.*
import de.mineformers.vanillaimmersion.config.Configuration
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.immersion.EnchantingHandler
import de.mineformers.vanillaimmersion.item.Hammer
import de.mineformers.vanillaimmersion.network.*
import de.mineformers.vanillaimmersion.tileentity.*
import de.mineformers.vanillaimmersion.util.SubSelectionHandler
import de.mineformers.vanillaimmersion.util.SubSelectionRenderer
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.renderer.block.statemap.StateMapperBase
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.obj.OBJLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.common.registry.ForgeRegistries
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.registries.IForgeRegistryEntry
import org.apache.logging.log4j.LogManager

/**
 * Main entry point for Vanilla Immersion
 */
@Mod(modid = VanillaImmersion.MODID,
     name = VanillaImmersion.MOD_NAME,
     version = VanillaImmersion.VERSION,
     acceptedMinecraftVersions = "*",
     dependencies = "required-after:forge;required-after:forgelin",
     updateJSON = "@UPDATE_URL@",
     modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter",
     guiFactory = "de.mineformers.vanillaimmersion.config.gui.GuiFactory",
     certificateFingerprint = "6c67ec97cb96c64a0bbd82a3cb1ec48cbde61bb2")
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
        MinecraftForge.EVENT_BUS.register(BlockRegistration)
        MinecraftForge.EVENT_BUS.register(Items)
        MinecraftForge.EVENT_BUS.register(Sounds)
    }

    /**
     * Runs during the pre-initialization phase of mod loading, registers blocks, items etc.
     */
    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        Configuration.load(event.modConfigurationDirectory, "vimmersion")

        if (!Configuration.shouldKeepVanilla("crafting_table")) {
            MinecraftForge.EVENT_BUS.register(CraftingHandler)
        }
        if (!Configuration.shouldKeepVanilla("enchanting_table")) {
            MinecraftForge.EVENT_BUS.register(EnchantingHandler)
        }
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
     * Verifies the JAR signature and logs a major warning if it is not present or invalid.
     * Note that this is *not* a viable security measure and only serves for users to verify the files they've downloaded.
     */
    @EventHandler
    fun onFingerprintViolation(event: FMLFingerprintViolationEvent) {
        LOG.warn("Vanilla Immersion is running with an invalid JAR fingerprint, the version of the mod you're using may have been tampered with by third parties!")
    }

    /**
     * Holder object for all blocks introduced by this mod.
     */
    object BlockRegistration {
        /**
         * Initializes and registers blocks and related data
         */
        @SubscribeEvent
        fun init(event: RegistryEvent.Register<Block>) {
            // TODO: Unify interaction handling?
            if (!Configuration.shouldKeepVanilla("furnace")) {
                LOG.info("Overriding furnace with immersive version!")
                event.registry.register(Furnace(false))
                event.registry.register(Furnace(true))
                registerTileEntity(FurnaceLogic::class.java, "furnace")
            }
            if (!Configuration.shouldKeepVanilla("crafting_table")) {
                LOG.info("Overriding crafting table with immersive version!")
                event.registry.register(CraftingTable())
                registerTileEntity(CraftingTableLogic::class.java, "crafting_table")
            }
            if (!Configuration.shouldKeepVanilla("anvil")) {
                LOG.info("Overriding anvil with immersive version!")
                event.registry.register(Anvil())
                registerTileEntity(AnvilLogic::class.java, "anvil")
            }
            if (!Configuration.shouldKeepVanilla("enchanting_table")) {
                LOG.info("Overriding enchantment table with immersive version!")
                event.registry.register(EnchantingTable())
                registerTileEntity(EnchantingTableLogic::class.java, "enchanting_table")
            }
            if (!Configuration.shouldKeepVanilla("brewing_stand")) {
                LOG.info("Overriding brewing stand with immersive version!")
                event.registry.register(BrewingStand())
                registerTileEntity(BrewingStandLogic::class.java, "brewing_stand")
            }
            if (!Configuration.shouldKeepVanilla("beacon")) {
                LOG.info("Overriding beacon with immersive version!")
                event.registry.register(Beacon())
                registerTileEntity(BeaconLogic::class.java, "beacon")
            }
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
        @JvmStatic
        @ObjectHolder("vimmersion:hammer")
        lateinit var HAMMER: Item

        /**
         * Initializes and registers blocks and related data
         */
        @SubscribeEvent
        fun init(event: RegistryEvent.Register<Item>) {
            event.registry.register(Hammer())
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
            event.registry.register(ENCHANTING_PAGE_TURN.setRegistryName(ResourceLocation("$MODID:enchanting.page_turn")))
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
    @Mod.EventBusSubscriber(Side.CLIENT, modid = MODID)
    class ClientProxy : Proxy {
        override fun preInit(event: FMLPreInitializationEvent) {
            OBJLoader.INSTANCE.addDomain(MODID) // We don't have OBJ models yet, but maybe in the future?
            // Initialize (i.e. compile) shaders now, removes delay on initial use later on
            Shaders.init()

            // Register client-side block features
            if (!Configuration.shouldKeepVanilla("furnace")) {
                ClientRegistry.bindTileEntitySpecialRenderer(FurnaceLogic::class.java, FurnaceRenderer())
            }
            if (!Configuration.shouldKeepVanilla("crafting_table")) {
                ClientRegistry.bindTileEntitySpecialRenderer(CraftingTableLogic::class.java, CraftingTableRenderer())
                MinecraftForge.EVENT_BUS.register(CraftingDragHandler)
            }
            if (!Configuration.shouldKeepVanilla("anvil")) {
                ClientRegistry.bindTileEntitySpecialRenderer(AnvilLogic::class.java, AnvilRenderer())
            }
            if (!Configuration.shouldKeepVanilla("enchanting_table")) {
                ClientRegistry.bindTileEntitySpecialRenderer(EnchantingTableLogic::class.java, EnchantingTableRenderer())
            }
            if (!Configuration.shouldKeepVanilla("brewing_stand")) {
                ClientRegistry.bindTileEntitySpecialRenderer(BrewingStandLogic::class.java, BrewingStandRenderer())
            }
            if (!Configuration.shouldKeepVanilla("beacon")) {
                ClientRegistry.bindTileEntitySpecialRenderer(BeaconLogic::class.java, BeaconRenderer())
                MinecraftForge.EVENT_BUS.register(BeaconHandler)
            }

            // Register client-specific event handlers
            MinecraftForge.EVENT_BUS.register(SubSelectionRenderer)
        }

        companion object {
            private object OverrideStateMapper : StateMapperBase() {
                override fun getModelResourceLocation(state: IBlockState) =
                    ModelResourceLocation(ResourceLocation(MODID, state.block.registryName!!.resourcePath), getPropertyString(state.properties))
            }

            inline fun <T : IForgeRegistryEntry<T>> ifOwner(entry: IForgeRegistryEntry<T>, action: (T) -> Unit) {
                //TODO: Add proper override owner support
            }

            @JvmStatic
            @SubscribeEvent
            fun registerModels(event: ModelRegistryEvent) {
                ModelLoader.setCustomModelResourceLocation(Items.HAMMER, 0, ModelResourceLocation("$MODID:hammer", "inventory"))
                if (!Configuration.shouldKeepVanilla("furnace")) {
                    ModelLoader.setCustomStateMapper(Blocks.FURNACE, OverrideStateMapper)
                    ModelLoader.setCustomStateMapper(Blocks.LIT_FURNACE, OverrideStateMapper)
                }
                if (!Configuration.shouldKeepVanilla("crafting_table")) {
                    ModelLoader.setCustomStateMapper(Blocks.CRAFTING_TABLE, OverrideStateMapper)
                }
                if (!Configuration.shouldKeepVanilla("anvil")) {
                    ModelLoader.setCustomStateMapper(Blocks.ANVIL, OverrideStateMapper)
                }
                if (!Configuration.shouldKeepVanilla("enchanting_table")) {
                    ModelLoader.setCustomStateMapper(Blocks.ENCHANTING_TABLE, OverrideStateMapper)
                }
                if (!Configuration.shouldKeepVanilla("brewing_stand")) {
                    ModelLoader.setCustomStateMapper(Blocks.BREWING_STAND, OverrideStateMapper)
                }
                if (!Configuration.shouldKeepVanilla("beacon")) {
                    ModelLoader.setCustomStateMapper(Blocks.BEACON, OverrideStateMapper)
                }
            }
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
