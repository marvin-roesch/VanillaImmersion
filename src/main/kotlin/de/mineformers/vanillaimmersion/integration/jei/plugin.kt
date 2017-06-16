package de.mineformers.vanillaimmersion.integration.jei

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic.CraftingTableContainer
import mezz.jei.api.BlankModPlugin
import mezz.jei.api.IIngredientListOverlay
import mezz.jei.api.IJeiRuntime
import mezz.jei.api.IModRegistry
import mezz.jei.api.IRecipeRegistry
import mezz.jei.api.JEIPlugin
import mezz.jei.api.gui.IRecipeLayout
import mezz.jei.api.recipe.VanillaRecipeCategoryUid
import mezz.jei.api.recipe.transfer.IRecipeTransferError
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler
import mezz.jei.gui.overlay.IngredientListOverlay
import mezz.jei.gui.recipes.RecipeLayout
import mezz.jei.transfer.RecipeTransferErrorInternal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.ContainerWorkbench
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Optional

/**
 * Vanilla Immersion JEI Integration plugin
 */
@JEIPlugin
class JEIIntegration : BlankModPlugin() {
    companion object {
        lateinit var ingredientListOverlay: IIngredientListOverlay
            private set
        lateinit var recipeRegistry: IRecipeRegistry
            private set
    }

    override fun register(registry: IModRegistry) {
        registry.recipeTransferRegistry.addRecipeTransferHandler(CraftingTransferHandler(), VanillaRecipeCategoryUid.CRAFTING)
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        ingredientListOverlay = jeiRuntime.ingredientListOverlay
        recipeRegistry = jeiRuntime.recipeRegistry
    }
}

object JEIProxy {
    fun focusSearch() {
        if (Loader.isModLoaded("jei"))
            focusSearchImpl()
    }

    @Optional.Method(modid = "jei")
    private fun focusSearchImpl() {
        val overlay = JEIIntegration.ingredientListOverlay
        if (overlay is IngredientListOverlay)
            overlay.setKeyboardFocus(true)
    }
}

class CraftingTransferHandler : IRecipeTransferHandler<CraftingTableContainer> {
    override fun transferRecipe(container: CraftingTableContainer, recipeLayout: IRecipeLayout, player: EntityPlayer,
                                maxTransfer: Boolean, doTransfer: Boolean): IRecipeTransferError? {
        val category = (recipeLayout as RecipeLayout).recipeCategory
        val vanillaContainer = ContainerWorkbench(player.inventory, player.world, BlockPos.ORIGIN)
        val transferHandler =
            JEIIntegration.recipeRegistry.getRecipeTransferHandler(vanillaContainer, category)
        if (transferHandler == null) {
            if (doTransfer) {
                VanillaImmersion.LOG.error("No Recipe Transfer handler for container {}", container.javaClass)
            }
            return RecipeTransferErrorInternal.INSTANCE
        }

        val result = transferHandler.transferRecipe(container, recipeLayout, player, maxTransfer, doTransfer)
        if (doTransfer && result == null) {
            player.closeScreen()
        }
        return result
    }

    override fun getContainerClass() = CraftingTableContainer::class.java
}