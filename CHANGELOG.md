Changelog
=========

### Version 2.0.3.2 for Minecraft 1.12.2
**Note:** This version of the mod requires at least Forge version *1.12.2-14.23.1.2582*.
  - **Fixes**
    - Player-dependent (e.g. RecipeStages) recipes should generally work now
    - The anvil's max text length matches Vanilla now and longer strings don't cause crashes anymore
    - Toggling a block's config outside of a dev environment doesn't crash the game anymore
    - Mods like Better with Mods and others that add overrides should now be (more) compatible

### Version 2.0.3.1 for Minecraft 1.12.2
**Note:** This version of the mod requires at least Forge version *1.12.2-14.23.1.2582*.
  - **Changes**
    - Update to MC 1.12.2 as base
    - New Forgelin dependency: 1.6.0 (Kotlin 1.2)
    - Brewing stands now always accept fuel when clicking the rod
  - **Fixes**
    - Brewing Stand model doesn't have empty spots anymore
    - Bottles on brewing stand are rendered in the correct location
    - Non-empty item stacks should be properly handled everywhere now
    - Right clicking blocks no longer causes random crashes
    - Hammering the anvil no longer crashes on servers due to failed particle sending

### Version 2.0.2.4 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Fixes**
    - Properly simulate crafting results in order to enable specific on-crafting behaviour of items (map scaling, for example)

### Version 2.0.2.3 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Fixes**
    - Update URL points at correct branch now

### Version 2.0.2.2 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Fixes**
    - Sub box interaction no longer uses client-only methods anymore, fixes usage on server

### Version 2.0.2.1 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Fixes**
    - Sub box interaction no longer uses client-only methods anymore, fixes usage on server

### Version 2.0.2.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Additions**
    - The mod JAR is signed now, official channels will only be distributing signed artifacts, check the repo for the signature to look out for

### Version 2.0.1.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2426*.
  - **Changes**
    - Properly depend on versioned Forgelin
    - Updated to newest Forge, addresses most issues with VI on servers
    - Override use of Vanilla state mappers with custom ones to prevent issues with resource packs

### Version 2.0.0.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2415*.
  - **Changes**
    - Automatic extraction out of the crafting table no longer works, since automation and players were too indistinguishable code-wise. Insertion still works.
  - **Fixes**
    - Furnace uses Vanilla methods to ensure items don't drop on state changes now, fixes issues with external heater etc.
    - Anvil doesn't ignore its empty state anymore

### Version 1.1.2.4 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2400*.
  - **Fixes**
    - Prevent arbitrary chunk generation through the server
    - Don't depend on a version of Forgelin, just the mod

### Version 1.1.2.3 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2400*.
  - **Fixes**
    - Properly include version information in main class

### Version 1.1.2.2 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2400*.
  - **Fixes**
    - Hammer model registers properly now, removes conflicts with other (buggy) mods

### Version 1.1.2.1 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2400*.
  - **Changes**
    - Crafting handler uses the proper facilities for cancelling the events now
  - **Fixes**
    - Mod now usable on servers, still requires a fix from Forge, though

### Version 1.1.2.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.1.2400*.
  - **Additions**
    - Acquiring an iron block or the anvil recipe will now unlock the hammer recipe
  - **Changes**
    - Update to latest Forge (14.21.1.2400)
  - **Fixes**
    - Fix hopper extraction crashing due to fake player networking
    - Fix crafting table interaction not working with special items like buckets

### Version 1.1.1.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.0.2373*.
  - **Additions**
    - Configuration options to disable immersive variants selectively
  - **Changes**
    - The crafting table no longer uses metadata for storing its facing but the TileEntity

### Version 1.1.0.2 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.0.2373*.
  - **Fixes**
    - Switch to newer CurseGradle version

### Version 1.1.0.1 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.0.2373*.
  - **Fixes**
    - Switch to CurseForge Maven for Forgelin dependency

### Version 1.1.0.0 for Minecraft 1.12
**Note:** This version of the mod requires at least Forge version *1.12-14.21.0.2373*.
  - **Changes**
    - Update to Minecraft 1.12
    - Blocks no longer are separate from their Vanilla counterparts but replace them completely
    - Removed conversion unnecessary recipes

### Version 0.0.9.0 for Minecraft 1.10.2
**Note:** This version of the mod requires at least Forge version *1.10.2-12.18.1.2077*.
  - **Additions**
    - Immersive version of the beacon that is interacted with by enabling and edit mode through right clicking and then scrolling
  - **Changes**
    - Hammers now are iron tools and take damage on use
  - **Fixes**
    - The book on enchantment tables is now correctly textured

### Version 0.0.8.0 for Minecraft 1.10.2
**Note:** This version of the mod requires at least Forge version *1.10.2-12.18.1.2076*.
  - **Additions**
    - Hammer that can be used to get the output of a repair operation on the anvil
    - Naming an item on the anvil now displays a cursor
    - Individual selection boxes for all slots in all blocks apart from the enchantment table
  - **Changes**
    - Anvil output no longer is displayed in favor of a hammer slot
    - Naming an item on the anvil now defaults to the item's current name, mimicking Vanilla behavior
    - Items now appear flat on the crafting table and it was reduced in height for easier access
  - **Fixes**
    - Middle-clicking and breaking the Furnace now selects the immersive variant

### Version 0.0.7.0 for Minecraft 1.10.2
**Note:** This version of the mod requires at least Forge version *1.10.2-12.18.1.2076*.
  - **Fixes**
    - State-aware TESRs should not crash the game anymore

### Version 0.0.6.0 for Minecraft 1.9.4
  - **Additions**
    - Configuration options for various things, check out the config GUI
  - **Changes**
    - The brewing stand respects shift clicking (sneaking) now

### Version 0.0.5.2 for Minecraft 1.9.4
  - **Fixes**
    - Kotlin does not interfere with runnability anymore

### Version 0.0.5.1 for Minecraft 1.9.4
  - **Fixes**
    - The enchantment table should actually be usable now

### Version 0.0.5.0 for Minecraft 1.9.4
  - **Additions**
    - Immersive Brewing Stand - Interaction should be fairly self-explanatory
  - **Fixes**
    - Any previous interaction issues with the enchantment table should be resolved now

### Version 0.0.4.0 for Minecraft 1.9.4
  - **Changes**
    - Furnace respects sneaking now, one item from the stack is inserted by default, the whole stack when sneaking
    - Crafting Table now only consumes one item at a time rather than the whole stack without sneaking
  - **Fixes**
    - The Crafting Table should now properly cancel all placing of blocks or other item uses

### Version 0.0.3.1 for Minecraft 1.9.4
  - **Fixes**
    - JEI no longer ships with the mod

### Version 0.0.3.0 for Minecraft 1.9.4
  - **Additions**
    - JEI Integration with recipe transfer for the crafting table
  - **Changes**
    - Minimum required Forge version now is 12.17.0.1910
  - **Fixes**
    - Furnace should now get properly lit when surrounded by other blocks
    - Furnace now properly synchronises to the client when items are inserted manually

### Version 0.0.2.0 for Minecraft 1.9.4
  - **Additions**
    - Shapeless crafting recipes to convert from Vanilla to immersive blocks and vice-versa
  - **Fixes**
    - Crafting table changes output again if there already was one but the recipe is different

### Version 0.0.1.1 for Minecraft 1.9.4
  - Build Fixes
    
### Version 0.0.1.0 for Minecraft 1.9.4
  - **Fixes**
    - Crafting Table now respects changing output while automating
    - Enchantment table interaction won't trigger hovered blocks anymore
    - All automatable blocks don't crash your game with an NPE anymore

### Version 0.0.0.1 for Minecraft 1.9.4
  - Initial Alpha Release