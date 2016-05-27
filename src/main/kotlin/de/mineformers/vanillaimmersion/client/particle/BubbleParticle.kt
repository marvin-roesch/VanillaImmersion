package de.mineformers.vanillaimmersion.client.particle

import net.minecraft.client.particle.Particle
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.World

/**
 * Clone of Vanilla's ParticleBubble, only working outside of water.
 */
class BubbleParticle(world: World, xCoord: Double, yCoord: Double, zCoord: Double,
                     xSpeed: Double, ySpeed: Double, zSpeed: Double) :
    Particle(world, xCoord, yCoord, zCoord, xSpeed, ySpeed, zSpeed) {

    init {
        particleRed = 1.0f
        particleGreen = 1.0f
        particleBlue = 1.0f
        setParticleTextureIndex(32)
        setPosition(xCoord, yCoord, zCoord)
        setSize(0.02f, 0.02f)
        entityBoundingBox = AxisAlignedBB(xCoord - width * 0.5, yCoord - height * 0.5, zCoord - width * 0.5,
                                          xCoord + width * 0.5, yCoord + height * 0.5, zCoord + width * 0.5)
        prevPosX = xCoord
        prevPosY = yCoord
        prevPosZ = zCoord
        particleScale *= this.rand.nextFloat() * 0.3f + 0.2f
        motionX = xSpeed * 0.2 + (Math.random() * 2.0 - 1.0) * 0.02
        motionY = ySpeed * 0.2 + Math.random() * 0.04
        motionZ = zSpeed * 0.2 + (Math.random() * 2.0 - 1.0) * 0.02
        particleMaxAge = (8.0 / (Math.random() * 0.8 + 0.2)).toInt()
    }

    override fun onUpdate() {
        prevPosX = posX
        prevPosY = posY
        prevPosZ = posZ
        motionY += 0.002
        moveEntity(motionX, motionY, motionZ)
        motionX *= 0.85
        motionY *= 0.85
        motionZ *= 0.85

        if (particleMaxAge-- <= 0) {
            setExpired()
        }
    }
}