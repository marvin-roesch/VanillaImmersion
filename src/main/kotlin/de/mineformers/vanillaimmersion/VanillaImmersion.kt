package de.mineformers.vanillaimmersion

import de.mineformers.vanillaimmersion.block.*
import de.mineformers.vanillaimmersion.client.CraftingDragHandler
import de.mineformers.vanillaimmersion.client.renderer.*
import de.mineformers.vanillaimmersion.config.Configuration
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.immersion.EnchantingHandler
import de.mineformers.vanillaimmersion.immersion.RepairHandler
import de.mineformers.vanillaimmersion.item.Hammer
import de.mineformers.vanillaimmersion.item.SpecialBlockItem
import de.mineformers.vanillaimmersion.network.*
import de.mineformers.vanillaimmersion.tileentity.*
import de.mineformers.vanillaimmersion.util.SubSelectionHandler
import de.mineformers.vanillaimmersion.util.SubSelectionRenderer
import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemAnvilBlock
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.obj.OBJLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
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
     dependencies = "required-after:Forge",
     updateJSON = "@UPDATE_URL@",
     modLanguageAdapter = "de.mineformers.vanillaimmersion.KotlinAdapter",
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
    /**
     * Temporary creative tab for the mod
     * To be removed once substitutions are fixed
     */
    val CREATIVE_TAB = object : CreativeTabs(MODID) {
        override fun getTabIconItem() = Item.getItemFromBlock(Blocks.CRAFTING_TABLE)
    }

    /**
     * Runs during the pre-initialization phase of mod loading, registers blocks, items etc.
     */
    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        Configuration.load(event.modConfigurationDirectory, "vimmersion")
        Blocks.init()
        Items.init()
        Sounds.init()

        MinecraftForge.EVENT_BUS.register(CraftingHandler)
        MinecraftForge.EVENT_BUS.register(RepairHandler)
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
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GuiHandler())

        PROXY.preInit(event)
    }

    /**
     * Runs during the initialization phase of mod loading, registers recipes etc.
     */
    @EventHandler
    fun init(event: FMLInitializationEvent) {
        LOG.info("Adding recipes...")
        GameRegistry.addShapedRecipe(ItemStack(Items.HAMMER),
                                     " IS",
                                     " SI",
                                     "S  ",
                                     'I', VItems.IRON_INGOT, 'S', VItems.STICK)
        if (Configuration.getBoolean("blocks.replace-vanilla-recipes")) {
            LOG.info("Replacing Vanilla recipes...")
            // Add replacement recipes
            GameRegistry.addShapedRecipe(ItemStack(Blocks.FURNACE),
                                         "###",
                                         "# #",
                                         "###",
                                         '#', VBlocks.COBBLESTONE)
            GameRegistry.addShapedRecipe(ItemStack(Blocks.CRAFTING_TABLE),
                                         "##",
                                         "##",
                                         '#', VBlocks.PLANKS)
            GameRegistry.addShapedRecipe(ItemStack(Blocks.ANVIL),
                                         "III",
                                         " i ",
                                         "iii",
                                         'I', VBlocks.IRON_BLOCK, 'i', VItems.IRON_INGOT)
            GameRegistry.addShapedRecipe(ItemStack(Blocks.ENCHANTING_TABLE),
                                         " B ",
                                         "D#D",
                                         "###",
                                         '#', VBlocks.OBSIDIAN, 'B', VItems.BOOK, 'D', VItems.DIAMOND)
            GameRegistry.addShapedRecipe(ItemStack(Blocks.BREWING_STAND),
                                         " B ",
                                         "###",
                                         '#', VBlocks.COBBLESTONE, 'B', VItems.BLAZE_ROD)
            GameRegistry.addShapedRecipe(ItemStack(Blocks.BEACON),
                                         "GGG",
                                         "GSG",
                                         "OOO",
                                         'G', VBlocks.GLASS, 'S', VItems.NETHER_STAR, 'O', VBlocks.OBSIDIAN)
        }
        if (Configuration.getBoolean("blocks.conversion-recipes")) {
            LOG.info("Adding Vanilla <-> Immersive recipes...")
            // Add Vanilla -> immersive block conversion recipes
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.FURNACE), ItemStack(VBlocks.FURNACE))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.CRAFTING_TABLE), ItemStack(VBlocks.CRAFTING_TABLE))
            // Fully intact, slightly damaged and very damaged anvils
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.ANVIL, 1, 0), ItemStack(VBlocks.ANVIL, 1, 0))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.ANVIL, 1, 1), ItemStack(VBlocks.ANVIL, 1, 1))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.ANVIL, 1, 2), ItemStack(VBlocks.ANVIL, 1, 2))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.ENCHANTING_TABLE), ItemStack(VBlocks.ENCHANTING_TABLE))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.BREWING_STAND), ItemStack(VItems.BREWING_STAND))
            GameRegistry.addShapelessRecipe(ItemStack(Blocks.BEACON), ItemStack(VBlocks.BEACON))
            // Add immersive -> Vanilla block conversion recipes
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.FURNACE), ItemStack(Blocks.FURNACE))
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.CRAFTING_TABLE), ItemStack(Blocks.CRAFTING_TABLE))
            // Fully intact, slightly damaged and very damaged anvils
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.ANVIL, 1, 0), ItemStack(Blocks.ANVIL, 1, 0))
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.ANVIL, 1, 1), ItemStack(Blocks.ANVIL, 1, 1))
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.ANVIL, 1, 2), ItemStack(Blocks.ANVIL, 1, 2))
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.ENCHANTING_TABLE), ItemStack(Blocks.ENCHANTING_TABLE))
            GameRegistry.addShapelessRecipe(ItemStack(VItems.BREWING_STAND), ItemStack(Blocks.BREWING_STAND))
            GameRegistry.addShapelessRecipe(ItemStack(VBlocks.BEACON), ItemStack(Blocks.BEACON))
        }
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
        fun init() {
            // TODO: Unify interaction handling?
            register(FURNACE)
            register(LIT_FURNACE, null)
            register(CRAFTING_TABLE)
            register(ANVIL, ::ItemAnvilBlock)
            register(ENCHANTING_TABLE)
            register(BREWING_STAND, ::SpecialBlockItem)
            register(BEACON)

            GameRegistry.registerTileEntity(FurnaceLogic::class.java, "$MODID:furnace")
            GameRegistry.registerTileEntity(CraftingTableLogic::class.java, "$MODID:crafting_table")
            GameRegistry.registerTileEntity(AnvilLogic::class.java, "$MODID:anvil")
            GameRegistry.registerTileEntity(EnchantingTableLogic::class.java, "$MODID:enchanting_table")
            GameRegistry.registerTileEntity(BrewingStandLogic::class.java, "$MODID:brewing_stand")
            GameRegistry.registerTileEntity(BeaconLogic::class.java, "$MODID:beacon")
        }

        /**
         * Helper method for registering blocks.
         *
         * @param block       the block to register
         * @param itemFactory a function reference serving as factory for the block's item representation,
         *                     may be null if no item is necessary
         */
        private fun register(block: Block, itemFactory: ((Block) -> Item)? = ::ItemBlock) {
            GameRegistry.register(block)
            val item = itemFactory?.invoke(block)
            if (item != null) {
                item.registryName = block.registryName
                GameRegistry.register(item)
            }
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
        fun init() {
            GameRegistry.register(HAMMER)
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
        fun init() {
            GameRegistry.register(ENCHANTING_PAGE_TURN, ResourceLocation("$MODID:enchanting.page_turn"))
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

            // Register block item models
            setItemModel(Blocks.FURNACE, 0, "minecraft:furnace")
            setItemModel(Blocks.CRAFTING_TABLE, 0, "minecraft:crafting_table")
            setItemModel(Blocks.ANVIL, 0, "minecraft:anvil_intact")
            setItemModel(Blocks.ANVIL, 1, "minecraft:anvil_slightly_damaged")
            setItemModel(Blocks.ANVIL, 2, "minecraft:anvil_very_damaged")
            setItemModel(Blocks.ENCHANTING_TABLE, 0, "minecraft:enchanting_table")
            setItemModel(Blocks.BREWING_STAND, 0, "minecraft:brewing_stand")
            setItemModel(Blocks.BEACON, 0, "minecraft:beacon")

            // Register TESRs
            ClientRegistry.bindTileEntitySpecialRenderer(FurnaceLogic::class.java, FurnaceRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(CraftingTableLogic::class.java, CraftingTableRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(AnvilLogic::class.java, AnvilRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(EnchantingTableLogic::class.java, EnchantingTableRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(BrewingStandLogic::class.java, BrewingStandRenderer())

            // Register item models
            setItemModel(Items.HAMMER, 0, "$MODID:hammer")

            // Register client-specific event handlers
            MinecraftForge.EVENT_BUS.register(CraftingDragHandler)
            MinecraftForge.EVENT_BUS.register(SubSelectionRenderer)
        }

        /**
         * Sets the model associated with the item representation of a block.
         * Assumes the default variant is to be used ("inventory").
         */
        private fun setItemModel(block: Block, meta: Int, resource: String) {
            setItemModel(Item.getItemFromBlock(block)!!, meta, resource)
        }

        /**
         * Sets the model associated with an item.
         * Assumes the default variant is to be used ("inventory").
         */
        private fun setItemModel(item: Item, meta: Int, resource: String) {
            ModelLoader.setCustomModelResourceLocation(item,
                                                       meta,
                                                       ModelResourceLocation(resource, "inventory"))
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
