package de.mineformers.vanillaimmersion.util

import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import javax.vecmath.Matrix4f
import javax.vecmath.Vector4f

/**
 * A collection of utility functions to deal with ray tracing.
 */
object Rays {
    /**
     * Performs a ray trace on a collection of boxes from an entity's eyes,
     * returns the index of the first one hit, `-1` otherwise.
     */
    fun rayTraceBoxes(entity: Entity, boxes: List<AxisAlignedBB>) =
        rayTraceBoxes(Rendering.getEyePosition(entity, Rendering.partialTicks), entity.getLook(Rendering.partialTicks),
                      boxes)

    /**
     * Performs a ray trace on a collection of boxes, returns the index of the first one hit, `-1` otherwise.
     */
    fun rayTraceBoxes(origin: Vec3d, dir: Vec3d, boxes: List<AxisAlignedBB>) =
        boxes.mapIndexed { i, box -> Pair(i, rayTraceBox(origin, dir, box)) }
            .filter { it.second != null }
            .minBy { it.second!!.squareDistanceTo(origin) }?.first ?: -1

    /**
     * Performs a ray-box intersection from an entity's eyes.
     */
    fun rayTraceBox(entity: Entity, box: AxisAlignedBB) =
        rayTraceBox(Rendering.getEyePosition(entity, Rendering.partialTicks), entity.getLook(Rendering.partialTicks),
                    box)

    /**
     * Fast Ray-Box Intersection
     * from "Graphics Gems", Academic Press, 1990
     *
     * Original is in C
     *
     * @author Andrew Woo
     */
    fun rayTraceBox(origin: Vec3d, dir: Vec3d, box: AxisAlignedBB): Vec3d? {
        var inside = true;
        val quadrant = arrayOf(0, 0, 0)
        var whichPlane = -1
        val maxT = arrayOf(.0, .0, .0)
        val candidatePlane = arrayOf(.0, .0, .0)
        val RIGHT = 0
        val LEFT = 1
        val MIDDLE = 2

        /* Find candidate planes; this loop can be avoided if
           rays cast all from the eye(assume perpsective view) */
        for (i in quadrant.indices)
            if (origin[i] < box.min[i]) {
                quadrant[i] = LEFT
                candidatePlane[i] = box.min[i]
                inside = false
            } else if (origin[i] > box.max[i]) {
                quadrant[i] = RIGHT
                candidatePlane[i] = box.max[i]
                inside = false
            } else {
                quadrant[i] = MIDDLE
            }

        /* Ray origin inside bounding box */
        if (inside) {
            return origin
        }


        /* Calculate T distances to candidate planes */
        for (i in quadrant.indices)
            if (quadrant[i] != MIDDLE && dir[i] != .0)
                maxT[i] = (candidatePlane[i] - origin[i]) / dir[i];
            else
                maxT[i] = -1.0

        /* Get largest of the maxT's for final choice of intersection */
        whichPlane = 0;
        for (i in quadrant.indices)
            if (maxT[whichPlane] < maxT[i])
                whichPlane = i;

        /* Check final candidate actually inside box */
        if (maxT[whichPlane] < .0) return null
        val result = arrayOf(.0, .0, .0)
        for (i in quadrant.indices)
            if (whichPlane != i) {
                result[i] = origin[i] + maxT[whichPlane] * dir[i];
                if (result[i] < box.min[i] || result[i] > box.max[i])
                    return null;
            } else {
                result[i] = candidatePlane[i];
            }
        return Vec3d(result[0], result[1], result[2]);
    }

    /**
     * Sends a ray though a quad from an entity's eyes, optionally applying some transformations to it beforehand.
     * If there was a result, it will be returned in coordinates local to the quad, i.e. with the inverse
     * of the transformations applied.
     */
    fun rayTraceQuad(entity: Entity, vertices: List<Vec3d>, transformations: Matrix4f? = null, epsilon: Double = 1e-6) =
        rayTraceQuad(Rendering.getEyePosition(entity, Rendering.partialTicks), entity.getLook(Rendering.partialTicks),
                     vertices, transformations, epsilon)

    /**
     * Sends a ray though a quad, optionally applying some transformations to it beforehand.
     * If there was a result, it will be returned in coordinates local to the quad, i.e. with the inverse
     * of the transformations applied.
     */
    fun rayTraceQuad(origin: Vec3d, dir: Vec3d, vertices: List<Vec3d>, transformations: Matrix4f? = null,
                     epsilon: Double = 1e-6): Vec3d? {
        val quad =
            if (transformations != null) {
                // Transform all vertices
                vertices.map {
                    val v = Vector4f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat(), 1f)
                    transformations.transform(v)
                    Vec3d(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
                }
            } else {
                vertices
            }
        // Intersect with both triangles making up the quad

        val hit = Rays.moellerTrumbore(origin, dir, quad[0], quad[1], quad[2], epsilon) ?:
                  Rays.moellerTrumbore(origin, dir, quad[0], quad[2], quad[3], epsilon)
        if (hit != null && transformations != null) {
            val invertedMatrix = Matrix4f(transformations)
            invertedMatrix.invert()
            val v = Vector4f(hit.x.toFloat(), hit.y.toFloat(), hit.z.toFloat(), 1f)
            invertedMatrix.transform(v)
            return Vec3d(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
        } else {
            return hit
        }
    }

    /**
     * Performs a Möller-Trumbore intersection on a triangle from an entity's eyes and
     * returns the position on the triangle that was hit, or `null` if there is no intersection.
     * Ignores the winding of the triangle, i.e. does not support backface culling.
     */
    fun moellerTrumbore(entity: Entity, v1: Vec3d, v2: Vec3d, v3: Vec3d, epsilon: Double = 1e-6) =
        moellerTrumbore(Rendering.getEyePosition(entity, Rendering.partialTicks),
                        entity.getLook(Rendering.partialTicks),
                        v1, v2, v3, epsilon)

    /**
     * Performs a Möller-Trumbore intersection on a triangle and returns the position on the triangle that was hit,
     * or `null` if there is no intersection.
     * Ignores the winding of the triangle, i.e. does not support backface culling.
     */
    fun moellerTrumbore(origin: Vec3d, dir: Vec3d, v1: Vec3d, v2: Vec3d, v3: Vec3d, epsilon: Double = 1e-6): Vec3d? {
        // Edges with v1 adjacent to them
        val e1 = v2 - v1
        val e2 = v3 - v1
        // Required for determinant and calculation of u
        val p = dir.crossProduct(e2)
        val det = e1.dotProduct(p)
        // Make sure determinant isn't near zero, otherwise we lie in the triangle's plane
        if (det > -epsilon && det < epsilon) {
            return null
        }
        // Distance from v1 to origin
        val t = origin - v1
        // Calculate u parameter and check whether it's in the triangle's bounds
        val u = t.dotProduct(p) / det
        if (u < 0 || u > 1) {
            return null
        }
        // Calculate v parameter and check whether it's in the triangle's bounds
        val q = t.crossProduct(e1)
        val v = dir.dotProduct(q) / det
        if (v < 0 || u + v > 1) {
            return null
        }
        // Actual intersection test
        val d = e2.dotProduct(q) / det
        if (d > epsilon) {
            // u and v are barycentric coordinates on the triangle, convert them to "normal" ones
            return v1 + u * e1 + v * e2
        }
        return null
    }
}