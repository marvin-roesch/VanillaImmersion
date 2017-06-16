package de.mineformers.vanillaimmersion.client.renderer

import gnu.trove.map.hash.TObjectIntHashMap
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.opengl.GL20.*
import scala.io.Source

/**
 * Wraps OpenGL's shader system in an object oriented API.
 * A shader will only take effect when it is supported by the GPU.
 * The locations of the provided shaders has to be of the format `domain:name` and they will be searched for at
 * `/assets/{domain}/shaders/{name}.[vert/frag]`.
 *
 * @param vertex   the location of the vertex shader to use in this program, null if there is no vertex shader
 * @param fragment the location of the fragment shader to use in this program, null if there is no fragment shader
 */
class Shader(vertex: ResourceLocation?, fragment: ResourceLocation?) {
    private val varLocations = TObjectIntHashMap<String>()
    private var lastProgram: Int = 0
    private var initialised: Boolean = false
    private var supported: Boolean = false
    private val program: Int

    init {
        program = glCreateProgram()
        if (vertex != null) {
            addShader("/assets/" + vertex.resourceDomain + "/shaders/" + vertex.resourcePath + ".vert",
                      GL_VERTEX_SHADER)
        }
        if (fragment != null) {
            addShader("/assets/" + fragment.resourceDomain + "/shaders/" + fragment.resourcePath + ".frag",
                      GL_FRAGMENT_SHADER)
        }
    }

    /**
     * Initialises this shader program, i.e. uploading it to the GPU and linking it.
     * Calling this method is not required, but it can save some time when it's called before any performance-dependant operation.
     */
    fun init() {
        glLinkProgram(program)
        glValidateProgram(program)
        initialised = true
        // If the program was successfully compiled, we know it must be supported
        supported = glGetProgrami(program, GL_LINK_STATUS) == GL_TRUE
    }

    /**
     * Activates this shader program, if it is supported.
     * Callers must not take care of storing previously enabled shaders.
     */
    fun activate() {
        if (!initialised) init()
        if (supported) {
            lastProgram = glGetInteger(GL_CURRENT_PROGRAM)
            glUseProgram(program)
        }
    }

    /**
     * Deactivates this shader program and restores the previous one.
     */
    fun deactivate() {
        if (!initialised) init()
        if (supported) glUseProgram(lastProgram)
    }

    /**
     * Adds a shader to this program.

     * @param source the location of the shader source file
     * *
     * @param type   the type of the shader, either [GL_VERTEX_SHADER][org.lwjgl.opengl.GL20.GL_VERTEX_SHADER]
     * *               or [GL_FRAGMENT_SHADER][org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER]
     */
    private fun addShader(source: String, type: Int) {
        // Only allow this operation before the shader was initialised
        if (!initialised) {
            try {
                glAttachShader(program, createShader(source, type))
            } catch (e: Exception) {
                // Catch exceptions if shaders are not supported, no way around this
                e.printStackTrace()
            }

        }
    }

    /**
     * Compiles a shader.

     * @param source the location of the shader source file
     * *
     * @param type   the type of the shader, either [GL_VERTEX_SHADER][org.lwjgl.opengl.GL20.GL_VERTEX_SHADER]
     * *               or [GL_FRAGMENT_SHADER][org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER]
     * *
     * @return the ID of the shader
     */
    private fun createShader(source: String, type: Int): Int {
        var shader = 0
        try {
            shader = glCreateShader(type)
            if (shader == 0) return 0
            glShaderSource(shader,
                           Source.fromInputStream(Shader::class.java.getResourceAsStream(source), "UTF-8").mkString())
            glCompileShader(shader)
            return shader
        } catch (e: Exception) {
            // Catch exceptions if shaders are not supported, no way around this
            glDeleteShader(shader)
            throw e
        }

    }

    /**
     * @param name the name of the uniform to look up
     * *
     * @return OpenGL's internal ID of the given uniform
     */
    fun getUniformLocation(name: String): Int {
        if (!varLocations.containsKey(name)) varLocations.put(name, glGetUniformLocation(program, name))
        return varLocations.get(name)
    }

    /**
     * Sets up to 4 uniform integers with the same name declared in the shader.
     * This should also be used for setting a `sampler2D` uniform.

     * @param name   the name of the uniform
     * *
     * @param values the integer values to set, the length of the vararg array should match the size of the uniform in the shader
     */
    fun setUniformInt(name: String, vararg values: Int) {
        if (supported) {
            val location = getUniformLocation(name)
            when (values.size) {
                1 -> glUniform1i(location, values[0])
                2 -> glUniform2i(location, values[0], values[1])
                3 -> glUniform3i(location, values[0], values[1], values[2])
                4 -> glUniform4i(location, values[0], values[1], values[2], values[3])
            }
        }
    }

    /**
     * Sets up to 4 uniform floats (including n-dimensional vectors) with the same name declared in the shader.

     * @param name   the name of the uniform
     * *
     * @param values the float values to set, the length of the vararg array should match the size of the uniform in the shader
     */
    fun setUniformFloat(name: String, vararg values: Float) {
        if (supported) {
            val location = getUniformLocation(name)
            when (values.size) {
                1 -> glUniform1f(location, values[0])
                2 -> glUniform2f(location, values[0], values[1])
                3 -> glUniform3f(location, values[0], values[1], values[2])
                4 -> glUniform4f(location, values[0], values[1], values[2], values[3])
            }
        }
    }

    /**
     * Sets a `vec3` uniform declared in the shader with a given name.

     * @param name   the name of the uniform
     * *
     * @param vector the value of the vector
     */
    fun setUniform(name: String, vector: Vec3d) {
        setUniformFloat(name, vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
    }

    /**
     * Sets a `bool` uniform declared in the shader with a given name.

     * @param name  the name of the uniform
     * *
     * @param value the boolean value to use
     */
    fun setUniformBool(name: String, value: Boolean) {
        setUniformInt(name, if (value) 1 else 0)
    }
}
