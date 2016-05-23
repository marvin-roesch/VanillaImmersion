/*
 * The ISC License
 *
 * Copyright (c) 2015 Arkan
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION
 * OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package de.mineformers.vanillaimmersion

import net.minecraftforge.fml.common.FMLModContainer
import net.minecraftforge.fml.common.ILanguageAdapter
import net.minecraftforge.fml.common.ModContainer
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * The Kotlin language adapter is required for this mod to be loadable by MinecraftForge.
 * Based on Forgelin's implementation with modified error handling (more descriptive exception names).
 *
 * @see [Forgelin on GitHub](https://github.com/Emberwalker/Forgelin)
 */
class KotlinAdapter : ILanguageAdapter {
    private val logger = LogManager.getLogger("ILanguageAdapter/Kotlin")

    override fun setProxy(target: Field, proxyTarget: Class<*>, proxy: Any) {
        logger.debug("Setting proxy on target: {}.{} -> {}", target.declaringClass.simpleName, target.name, proxy)
        target.set(proxyTarget.kotlin.objectInstance, proxy)
    }

    override fun getNewInstance(container: FMLModContainer?,
                                objectClass: Class<*>,
                                classLoader: ClassLoader,
                                factoryMarkedMethod: Method?): Any? {
        logger.debug("Constructing new instance of {}", objectClass.simpleName)

        val kotlinClass = objectClass.kotlin
        if (factoryMarkedMethod != null) {
            return factoryMarkedMethod.invoke(null)
        } else {
            return kotlinClass.objectInstance ?: objectClass.newInstance()
        }
    }

    override fun supportsStatics() = false

    override fun setInternalProxies(mod: ModContainer?, side: Side?, loader: ClassLoader?) = Unit

    private fun findInstanceFieldOrThrow(targetClass: Class<*>): Field {
        val instanceField: Field = try {
            targetClass.getField("INSTANCE")
        } catch (exception: NoSuchFieldException) {
            throw NoInstanceFieldFound(exception)
        } catch (exception: SecurityException) {
            throw SecurityViolated(exception)
        }

        return instanceField
    }

    private fun findModObjectOrThrow(instanceField: Field): Any {
        val modObject = try {
            instanceField.get(null)
        } catch (exception: IllegalArgumentException) {
            throw InitializerSignatureUnexpected(exception)
        } catch (exception: IllegalAccessException) {
            throw InitializerNotPublic(exception)
        }

        return modObject
    }

    // Changed exception names to be more descriptive, class names are just as important as the message.
    // If you're interested in to the reasoning behind this, there's a great talk by Kevlin Henney on infoq :P
    private open class AdapterError(message: String, exception: Exception) :
        RuntimeException("Kotlin adapter error - do not report to Forge! " + message, exception)

    private class NoInstanceFieldFound(exception: Exception) :
        AdapterError("Couldn't find INSTANCE singleton on Kotlin @Mod container", exception)

    private class SecurityViolated(exception: Exception) :
        AdapterError("Security violation accessing INSTANCE singleton on Kotlin @Mod container", exception)

    private class InitializerSignatureUnexpected(exception: Exception) :
        AdapterError("Kotlin @Mod object has an unexpected initializer signature, somehow?", exception)

    private class InitializerNotPublic(exception: Exception) :
        AdapterError("Initializer on Kotlin @Mod object isn't `public`", exception)
}