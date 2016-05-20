package de.mineformers.vanillaimmersion

import de.mineformers.vanillaimmersion.block.Anvil
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.block.EnchantingTable
import de.mineformers.vanillaimmersion.block.Furnace
import de.mineformers.vanillaimmersion.client.CraftingDragHandler
import de.mineformers.vanillaimmersion.client.EnchantingUIHandler
import de.mineformers.vanillaimmersion.client.renderer.*
import de.mineformers.vanillaimmersion.immersion.CraftingHandler
import de.mineformers.vanillaimmersion.immersion.RepairHandler
import de.mineformers.vanillaimmersion.network.AnvilLock
import de.mineformers.vanillaimmersion.network.AnvilText
import de.mineformers.vanillaimmersion.network.CraftingDrag
import de.mineformers.vanillaimmersion.network.EnchantingAction
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemAnvilBlock
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.client.model.obj.OBJLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side

/**
 * Main entry point for Vanilla Immersion
 */
@Mod(modid = VanillaImmersion.MODID,
     name = VanillaImmersion.MOD_NAME,
     version = VanillaImmersion.VERSION,
     acceptedMinecraftVersions = "*",
     dependencies = "required-after:Forge",
     updateJSON = "@UPDATE_URL@",
     modLanguageAdapter = "de.mineformers.vanillaimmersion.KotlinAdapter")
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
        Blocks.init()
        Sounds.init()

        MinecraftForge.EVENT_BUS.register(CraftingHandler)
        MinecraftForge.EVENT_BUS.register(RepairHandler)

        // Register messages and handlers
        NETWORK.registerMessage(AnvilLock.Handler, AnvilLock.Message::class.java,
                                0, Side.CLIENT)
        NETWORK.registerMessage(AnvilText.Handler, AnvilText.Message::class.java,
                                1, Side.SERVER)
        NETWORK.registerMessage(CraftingDrag.Handler, CraftingDrag.Message::class.java,
                                2, Side.SERVER)
        NETWORK.registerMessage(EnchantingAction.PageHitHandler, EnchantingAction.PageHitMessage::class.java,
                                3, Side.SERVER)

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
         * Initializes and registers blocks and related data
         */
        fun init() {
            // TODO: Unify interaction handling?
            register(FURNACE)
            register(LIT_FURNACE, null)
            register(CRAFTING_TABLE)
            register(ANVIL, ::ItemAnvilBlock)
            register(ENCHANTING_TABLE)

            GameRegistry.registerTileEntity(FurnaceLogic::class.java, "$MODID:furnace")
            GameRegistry.registerTileEntity(CraftingTableLogic::class.java, "$MODID:crafting_table")
            GameRegistry.registerTileEntity(AnvilLogic::class.java, "$MODID:anvil")
            GameRegistry.registerTileEntity(EnchantingTableLogic::class.java, "$MODID:enchanting_table")
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

            // Register TESRs
            ClientRegistry.bindTileEntitySpecialRenderer(FurnaceLogic::class.java, FurnaceRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(CraftingTableLogic::class.java, CraftingTableRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(AnvilLogic::class.java, AnvilRenderer())
            ClientRegistry.bindTileEntitySpecialRenderer(EnchantingTableLogic::class.java, EnchantingTableRenderer())

            // Register client-specific event handlers
            MinecraftForge.EVENT_BUS.register(CraftingDragHandler)
            MinecraftForge.EVENT_BUS.register(EnchantingUIHandler)
        }

        /**
         * Sets the model associated with the item representation of a block.
         * Assumes the default variant is to be used ("inventory").
         */
        private fun setItemModel(block: Block, meta: Int, resource: String) {
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block),
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
