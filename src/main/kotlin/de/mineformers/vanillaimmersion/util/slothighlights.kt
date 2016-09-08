package de.mineformers.vanillaimmersion.util

import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d

/**
 * TileEntities inheriting from this interface will have a preview/highlight drawn for each slot if it is not occupied.
 */
interface SlotHighlights {
    val highlights: Map<Int, SlotHighlight>
}

/**
 * Contains information about a slot highlight.
 * The stored bounding box is relative to the block's position.
 */
data class SlotHighlight(val box: AxisAlignedBB,
                         val icon: ResourceLocation?,
                         val rotation: Rotation = Rotation.NONE,
                         val uvs: List<Vec3d> = listOf(Vec3d(.0, .0, .0),
                                                       Vec3d(.0, .0, 1.0),
                                                       Vec3d(1.0, .0, 1.0),
                                                       Vec3d(1.0, .0, .0)))