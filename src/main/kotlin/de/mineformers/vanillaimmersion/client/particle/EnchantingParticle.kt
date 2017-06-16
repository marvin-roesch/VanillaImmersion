package de.mineformers.vanillaimmersion.client.particle

import net.minecraft.client.particle.Particle
import net.minecraft.world.World

/**
 * 'Standard galactic alphabet' particle to display during an enchantment process.
 * The particle will move from its start position to a designated point (e.g. the enchanted object).
 */
class EnchantingParticle(world: World,
                         private val coordX: Double, private val coordY: Double, private val coordZ: Double,
                         destinationX: Double, destinationY: Double, destinationZ: Double) :
    Particle(world, coordX, coordY, coordZ, 0.0, 0.0, 0.0) {
    private val oSize: Float

    init {
        this.posX = coordX
        this.posY = coordY
        this.posZ = coordZ
        this.motionX = destinationX - coordX
        this.motionY = destinationY - coordY
        this.motionZ = destinationZ - coordZ
        val tint = this.rand.nextFloat() * 0.6f + 0.4f
        this.oSize = this.rand.nextFloat() * 0.5f + 0.2f
        this.particleScale = oSize
        this.particleBlue = 1.0f * tint
        this.particleGreen = particleBlue * 0.9f
        this.particleRed = particleBlue * 0.9f
        this.particleMaxAge = (Math.random() * 10.0).toInt() + 40
        this.setParticleTextureIndex((Math.random() * 26.0 + 1.0 + 224.0).toInt())
    }

    override fun move(x: Double, y: Double, z: Double) {
        this.boundingBox = this.boundingBox.offset(x, y, z)
        this.resetPositionToBB()
    }

    override fun getBrightnessForRender(p_189214_1_: Float): Int {
        val brightness = super.getBrightnessForRender(p_189214_1_)
        var t = this.particleAge.toFloat() / this.particleMaxAge.toFloat()
        t *= t
        t *= t
        val j = brightness and 255
        var k = brightness shr 16 and 255
        k += (t * 15.0f * 16.0f).toInt()

        if (k > 240) {
            k = 240
        }

        return j or (k shl 16)
    }

    override fun onUpdate() {
        this.prevPosX = this.posX
        this.prevPosY = this.posY
        this.prevPosZ = this.posZ
        val t = this.particleAge / this.particleMaxAge.toFloat()
        val t2 = t * t
        this.posX = this.coordX + this.motionX * t2
        this.posY = this.coordY + this.motionY * t2
        this.posZ = this.coordZ + this.motionZ * t2

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired()
        }
    }
}