package de.mineformers.vanillaimmersion.config

/**
 * Represents any manager capable of storing configuration values.
 */
interface ConfigStorage {
    /**
     * Tries to get a configuration value for a given path from the storage.
     * Returns `null` if the path does not exist.
     */
    fun get(path: String): Any?

    /**
     * Stores the specified value under the provided path in the storage.
     */
    fun set(path: String, value: Any?)
}