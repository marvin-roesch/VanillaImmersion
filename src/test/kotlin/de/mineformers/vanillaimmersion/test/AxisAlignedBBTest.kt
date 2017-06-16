package de.mineformers.vanillaimmersion.test

import de.mineformers.vanillaimmersion.util.rotateY
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB

/**
 * ${JDOC}
 */
fun main(args: Array<String>) {
    val box = AxisAlignedBB(.0, .0, .0, .3, .3, .3)
    println(box.rotateY(Rotation.CLOCKWISE_180))
}