/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.arcore.apps.whitebox.mobile.samplerender

import android.content.res.AssetManager
import android.opengl.GLES30
import android.opengl.GLException
import android.util.Log
import androidx.collection.MutableIntList
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectIntMap
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableObjectIntMapOf
import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import java.util.regex.Matcher
import kotlin.collections.Map

/**
 * Represents a GPU shader, the state of its associated uniforms, and some additional draw state.
 *
 * @param vertexShaderCode The vertex shader code
 * @param fragmentShaderCode The fragment shader code
 * @param defines A map of shader precompiler symbols to be defined with the given names and values
 */
public class Shader(
    vertexShaderCode: String,
    fragmentShaderCode: String,
    defines: Map<String, String>?,
) : Closeable {

    private var programId: Int = 0

    private val uniforms: MutableIntObjectMap<Uniform> = mutableIntObjectMapOf()
    private var maxTextureUnit: Int = 0

    private val uniformLocations: MutableObjectIntMap<String> = mutableObjectIntMapOf()

    private val uniformNames: MutableIntObjectMap<String> = mutableIntObjectMapOf()

    private var depthTest: Boolean = true

    private var depthWrite: Boolean = true

    private var cullFace: Boolean = true

    private var sourceRgbBlend: BlendFactor = BlendFactor.ONE

    private var destRgbBlend: BlendFactor = BlendFactor.ZERO

    private var sourceAlphaBlend: BlendFactor = BlendFactor.ONE

    private var destAlphaBlend: BlendFactor = BlendFactor.ZERO

    init {
        var vertexShaderId: Int = 0
        var fragmentShaderId: Int = 0
        val definesCode: String = createShaderDefinesCode(defines)
        try {
            vertexShaderId =
                createShader(
                    GLES30.GL_VERTEX_SHADER,
                    insertShaderDefinesCode(vertexShaderCode, definesCode),
                )
            fragmentShaderId =
                createShader(
                    GLES30.GL_FRAGMENT_SHADER,
                    insertShaderDefinesCode(fragmentShaderCode, definesCode),
                )

            programId = GLES30.glCreateProgram()
            maybeThrowGLException("Shader program creation failed", "glCreateProgram")
            GLES30.glAttachShader(programId, vertexShaderId)
            maybeThrowGLException("Failed to attach vertex shader", "glAttachShader")
            GLES30.glAttachShader(programId, fragmentShaderId)
            maybeThrowGLException("Failed to attach fragment shader", "glAttachShader")
            GLES30.glLinkProgram(programId)
            maybeThrowGLException("Failed to link shader program", "glLinkProgram")

            val linkStatus: IntArray = intArrayOf(1)
            GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == GLES30.GL_FALSE) {
                val infoLog: String = GLES30.glGetProgramInfoLog(programId)
                maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to retrieve shader program info log",
                    "glGetProgramInfoLog",
                )
                throw GLException(0, "Shader link failed: " + infoLog)
            }
        } catch (t: Throwable) {
            close()
            throw t
        } finally {
            // Shader objects can be flagged for deletion immediately after program creation.
            if (vertexShaderId != 0) {
                GLES30.glDeleteShader(vertexShaderId)
                maybeLogGLError(Log.WARN, TAG, "Failed to free vertex shader", "glDeleteShader")
            }
            if (fragmentShaderId != 0) {
                GLES30.glDeleteShader(fragmentShaderId)
                maybeLogGLError(Log.WARN, TAG, "Failed to free fragment shader", "glDeleteShader")
            }
        }
    }

    /**
     * A factor to be used in a blend function.
     *
     * See
     * [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFunc.xhtml)
     */
    enum class BlendFactor(val glesEnum: Int) {
        ZERO(GLES30.GL_ZERO),
        ONE(GLES30.GL_ONE),
        SRC_COLOR(GLES30.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GLES30.GL_ONE_MINUS_SRC_COLOR),
        DST_COLOR(GLES30.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GLES30.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GLES30.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GLES30.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GLES30.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GLES30.GL_ONE_MINUS_DST_ALPHA),
        CONSTANT_COLOR(GLES30.GL_CONSTANT_COLOR),
        ONE_MINUS_CONSTANT_COLOR(GLES30.GL_ONE_MINUS_CONSTANT_COLOR),
        CONSTANT_ALPHA(GLES30.GL_CONSTANT_ALPHA),
        ONE_MINUS_CONSTANT_ALPHA(GLES30.GL_ONE_MINUS_CONSTANT_ALPHA),
    }

    override fun close() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
    }

    /**
     * Sets depth test state.
     *
     * See
     * [glEnable(GL_DEPTH_TEST)](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml)
     */
    fun setDepthTest(depthTest: Boolean): Shader {
        this.depthTest = depthTest
        return this
    }

    /**
     * Sets depth write state.
     *
     * See
     * [glDepthMask](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDepthMask.xhtml)
     */
    fun setDepthWrite(depthWrite: Boolean): Shader {
        this.depthWrite = depthWrite
        return this
    }

    /**
     * Sets cull face state.
     *
     * See
     * [glEnable(GL_CULL_FACE)](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glEnable.xhtml)
     */
    fun setCullFace(cullFace: Boolean): Shader {
        this.cullFace = cullFace
        return this
    }

    /**
     * Sets blending function.
     *
     * See
     * [glBlendFunc](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFunc.xhtml)
     */
    fun setBlend(sourceBlend: BlendFactor, destBlend: BlendFactor): Shader {
        this.sourceRgbBlend = sourceBlend
        this.destRgbBlend = destBlend
        this.sourceAlphaBlend = sourceBlend
        this.destAlphaBlend = destBlend
        return this
    }

    /**
     * Sets blending functions separately for RGB and alpha channels.
     *
     * See
     * [glBlendFuncSeparate](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glBlendFuncSeparate.xhtml)
     */
    fun setBlend(
        sourceRgbBlend: BlendFactor,
        destRgbBlend: BlendFactor,
        sourceAlphaBlend: BlendFactor,
        destAlphaBlend: BlendFactor,
    ): Shader {
        this.sourceRgbBlend = sourceRgbBlend
        this.destRgbBlend = destRgbBlend
        this.sourceAlphaBlend = sourceAlphaBlend
        this.destAlphaBlend = destAlphaBlend
        return this
    }

    /** Sets a texture uniform. */
    fun setTexture(name: String, texture: Texture): Shader {
        // Special handling for Textures. If replacing an existing texture uniform, reuse the
        // texture
        // unit.
        val location: Int = getUniformLocation(name)
        val uniform: Uniform? = uniforms.get(location)
        val textureUnit: Int
        if (!(uniform is UniformTexture)) {
            textureUnit = maxTextureUnit++
        } else {
            textureUnit = uniform.getTextureUnit()
        }
        uniforms.put(location, UniformTexture(textureUnit, texture))
        return this
    }

    /** Sets a boolean uniform. */
    fun setBool(name: String, v0: Boolean): Shader {
        val values: IntArray = intArrayOf(if (v0) 1 else 0)
        uniforms.put(getUniformLocation(name), UniformInt(values))
        return this
    }

    /** Sets an integer uniform. */
    fun setInt(name: String, v0: Int): Shader {
        val values: IntArray = intArrayOf(v0)
        uniforms.put(getUniformLocation(name), UniformInt(values))
        return this
    }

    /** Sets a float uniform. */
    fun setFloat(name: String, v0: Float): Shader {
        val values: FloatArray = floatArrayOf(v0)
        uniforms.put(getUniformLocation(name), Uniform1f(values))
        return this
    }

    /** Sets a 2D vector uniform. */
    fun setVec2(name: String, values: FloatArray): Shader {
        if (values.size != 2) {
            throw IllegalArgumentException("Value array length must be 2")
        }
        uniforms.put(getUniformLocation(name), Uniform2f(values.clone()))
        return this
    }

    /** Sets a 3D vector uniform. */
    fun setVec3(name: String, values: FloatArray): Shader {
        if (values.size != 3) {
            throw IllegalArgumentException("Value array length must be 3")
        }
        uniforms.put(getUniformLocation(name), Uniform3f(values.clone()))
        return this
    }

    /** Sets a 4D vector uniform. */
    fun setVec4(name: String, values: FloatArray): Shader {
        if (values.size != 4) {
            throw IllegalArgumentException("Value array length must be 4")
        }
        uniforms.put(getUniformLocation(name), Uniform4f(values.clone()))
        return this
    }

    /** Sets a 2x2 matrix uniform. */
    fun setMat2(name: String, values: FloatArray): Shader {
        if (values.size != 4) {
            throw IllegalArgumentException("Value array length must be 4 (2x2)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix2f(values.clone()))
        return this
    }

    /** Sets a 3x3 matrix uniform. */
    fun setMat3(name: String, values: FloatArray): Shader {
        if (values.size != 9) {
            throw IllegalArgumentException("Value array length must be 9 (3x3)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix3f(values.clone()))
        return this
    }

    /** Sets a 4x4 matrix uniform. */
    fun setMat4(name: String, values: FloatArray): Shader {
        if (values.size != 16) {
            throw IllegalArgumentException("Value array length must be 16 (4x4)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix4f(values.clone()))
        return this
    }

    /** Sets a boolean array uniform. */
    fun setBoolArray(name: String, values: BooleanArray): Shader {
        val intValues: IntArray = IntArray(values.size)
        for (i in values.indices) {
            intValues[i] = if (values[i]) 1 else 0
        }
        uniforms.put(getUniformLocation(name), UniformInt(intValues))
        return this
    }

    /** Sets an integer array uniform. */
    fun setIntArray(name: String, values: IntArray): Shader {
        uniforms.put(getUniformLocation(name), UniformInt(values.clone()))
        return this
    }

    /** Sets a float array uniform. */
    fun setFloatArray(name: String, values: FloatArray): Shader {
        uniforms.put(getUniformLocation(name), Uniform1f(values.clone()))
        return this
    }

    /** Sets a 2D vector array uniform. */
    fun setVec2Array(name: String, values: FloatArray): Shader {
        if (values.size % 2 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 2")
        }
        uniforms.put(getUniformLocation(name), Uniform2f(values.clone()))
        return this
    }

    /** Sets a 3D vector array uniform. */
    fun setVec3Array(name: String, values: FloatArray): Shader {
        if (values.size % 3 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 3")
        }
        uniforms.put(getUniformLocation(name), Uniform3f(values.clone()))
        return this
    }

    /** Sets a 4D vector array uniform. */
    fun setVec4Array(name: String, values: FloatArray): Shader {
        if (values.size % 4 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 4")
        }
        uniforms.put(getUniformLocation(name), Uniform4f(values.clone()))
        return this
    }

    /** Sets a 2x2 matrix array uniform. */
    fun setMat2Array(name: String, values: FloatArray): Shader {
        if (values.size % 4 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 4 (2x2)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix2f(values.clone()))
        return this
    }

    /** Sets a 3x3 matrix array uniform. */
    fun setMat3Array(name: String, values: FloatArray): Shader {
        if (values.size % 9 != 0) {
            throw IllegalArgumentException("Values array length must be divisible by 9 (3x3)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix3f(values.clone()))
        return this
    }

    /** Sets a 4x4 matrix uniform. */
    fun setMat4Array(name: String, values: FloatArray): Shader {
        if (values.size % 16 != 0) {
            throw IllegalArgumentException("Value array length must be divisible by 16 (4x4)")
        }
        uniforms.put(getUniformLocation(name), UniformMatrix4f(values.clone()))
        return this
    }

    /**
     * Activates the shader. Don't call this directly unless you are doing low level OpenGL code;
     * instead, prefer [SampleRender#draw}.
     */
    fun lowLevelUse() {
        // Make active shader/set uniforms
        if (programId == 0) {
            throw IllegalStateException("Attempted to use freed shader")
        }
        GLES30.glUseProgram(programId)
        maybeThrowGLException("Failed to use shader program", "glUseProgram")
        GLES30.glBlendFuncSeparate(
            sourceRgbBlend.glesEnum,
            destRgbBlend.glesEnum,
            sourceAlphaBlend.glesEnum,
            destAlphaBlend.glesEnum,
        )
        maybeThrowGLException("Failed to set blend mode", "glBlendFuncSeparate")
        GLES30.glDepthMask(depthWrite)
        maybeThrowGLException("Failed to set depth write mask", "glDepthMask")
        if (depthTest) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            maybeThrowGLException("Failed to enable depth test", "glEnable")
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            maybeThrowGLException("Failed to disable depth test", "glDisable")
        }
        if (cullFace) {
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            maybeThrowGLException("Failed to enable backface culling", "glEnable")
        } else {
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            maybeThrowGLException("Failed to disable backface culling", "glDisable")
        }
        try {
            // Remove all non-texture uniforms from the map after setting them, since they're stored
            // as
            // part of the program.
            val obsoleteEntries: MutableIntList = mutableIntListOf(uniforms.size)
            uniforms.forEach { key, value ->
                try {
                    value.use(key)
                    if (!(value is UniformTexture)) {
                        obsoleteEntries.add(key)
                    }
                } catch (e: GLException) {
                    val name: String = uniformNames.get(key) ?: "unknown"
                    throw IllegalArgumentException("Error setting uniform `" + name + "'", e)
                }
            }
            obsoleteEntries.forEach { obsoleteEntry -> uniforms.remove(obsoleteEntry) }
        } finally {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            maybeLogGLError(Log.WARN, TAG, "Failed to set active texture", "glActiveTexture")
        }
    }

    private fun getUniformLocation(name: String): Int {
        val locationObject: Int? =
            if (uniformLocations.containsKey(name)) {
                uniformLocations.get(name)
            } else null

        if (locationObject != null) {
            return locationObject
        }
        val location: Int = GLES30.glGetUniformLocation(programId, name)
        maybeThrowGLException("Failed to find uniform", "glGetUniformLocation")
        if (location == -1) {
            throw IllegalArgumentException("Shader uniform does not exist: " + name)
        }
        uniformLocations.put(name, Integer.valueOf(location))
        uniformNames.put(Integer.valueOf(location), name)
        return location
    }

    companion object {
        private const val TAG: String = "Shader"

        /**
         * Creates a [Shader] from the given asset file names.
         *
         * The file contents are interpreted as UTF-8 text.
         *
         * @param render The [SampleRender] context
         * @param vertexShaderFileName The name of the vertex shader file
         * @param fragmentShaderFileName The name of the fragment shader file
         * @param defines A map of shader precompiler symbols to be defined with the given names and
         *   values
         */
        fun createFromAssets(
            render: SampleRender,
            vertexShaderFileName: String,
            fragmentShaderFileName: String,
            defines: Map<String, String>?,
        ): Shader {
            val assets: AssetManager = render.getAssets()
            return Shader(
                inputStreamToString(assets.open(vertexShaderFileName)),
                inputStreamToString(assets.open(fragmentShaderFileName)),
                defines,
            )
        }

        private fun interface Uniform {
            public fun use(location: Int)
        }

        private class UniformInt(private val values: IntArray) : Uniform {
            override fun use(location: Int) {
                GLES30.glUniform1iv(location, values.size, values, 0)
                maybeThrowGLException("Failed to set shader uniform 1i", "glUniform1iv")
            }
        }

        private class UniformTexture(private val textureUnit: Int, private val texture: Texture) :
            Uniform {

            public fun getTextureUnit(): Int {
                return textureUnit
            }

            public override fun use(location: Int) {
                check(texture.textureId != 0) { "Tried to draw with freed texture" }
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureUnit)
                maybeThrowGLException("Failed to set active texture", "glActiveTexture")
                GLES30.glBindTexture(texture.target.glesEnum, texture.textureId)
                maybeThrowGLException("Failed to bind texture", "glBindTexture")
                GLES30.glUniform1i(location, textureUnit)
                maybeThrowGLException("Failed to set shader texture uniform", "glUniform1i")
            }
        }

        private class Uniform1f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniform1fv(location, values.size, values, 0)
                maybeThrowGLException("Failed to set shader uniform 1f", "glUniform1fv")
            }
        }

        private class Uniform2f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniform2fv(location, values.size / 2, values, 0)
                maybeThrowGLException("Failed to set shader uniform 2f", "glUniform2fv")
            }
        }

        private class Uniform3f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniform3fv(location, values.size / 3, values, 0)
                maybeThrowGLException("Failed to set shader uniform 3f", "glUniform3fv")
            }
        }

        private class Uniform4f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniform4fv(location, values.size / 4, values, 0)
                maybeThrowGLException("Failed to set shader uniform 4f", "glUniform4fv")
            }
        }

        private class UniformMatrix2f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniformMatrix2fv(
                    location,
                    values.size / 4,
                    /* transpose= */ false,
                    values,
                    0,
                )
                maybeThrowGLException(
                    "Failed to set shader uniform matrix 2f",
                    "glUniformMatrix2fv",
                )
            }
        }

        private class UniformMatrix3f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniformMatrix3fv(
                    location,
                    values.size / 9,
                    /* transpose= */ false,
                    values,
                    0,
                )
                maybeThrowGLException(
                    "Failed to set shader uniform matrix 3f",
                    "glUniformMatrix3fv",
                )
            }
        }

        private class UniformMatrix4f(private val values: FloatArray) : Uniform {
            public override fun use(location: Int) {
                GLES30.glUniformMatrix4fv(
                    location,
                    values.size / 16,
                    /* transpose= */ false,
                    values,
                    0,
                )
                maybeThrowGLException(
                    "Failed to set shader uniform matrix 4f",
                    "glUniformMatrix4fv",
                )
            }
        }

        private fun createShader(type: Int, code: String): Int {
            val shaderId: Int = GLES30.glCreateShader(type)
            maybeThrowGLException("Shader creation failed", "glCreateShader")
            GLES30.glShaderSource(shaderId, code)
            maybeThrowGLException("Shader source failed", "glShaderSource")
            GLES30.glCompileShader(shaderId)
            maybeThrowGLException("Shader compilation failed", "glCompileShader")

            val compileStatus: IntArray = intArrayOf(1)
            GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == GLES30.GL_FALSE) {
                val infoLog: String = GLES30.glGetShaderInfoLog(shaderId)
                maybeLogGLError(
                    Log.WARN,
                    TAG,
                    "Failed to retrieve shader info log",
                    "glGetShaderInfoLog",
                )
                GLES30.glDeleteShader(shaderId)
                maybeLogGLError(Log.WARN, TAG, "Failed to free shader", "glDeleteShader")
                throw GLException(0, "Shader compilation failed: " + infoLog)
            }

            return shaderId
        }

        private fun createShaderDefinesCode(defines: Map<String, String>?): String {
            if (defines == null) {
                return ""
            }
            val builder: StringBuilder = StringBuilder()
            for (entry in defines.entries) {
                builder.append("#define " + entry.key + " " + entry.value + "\n")
            }
            return builder.toString()
        }

        private fun insertShaderDefinesCode(sourceCode: String, definesCode: String): String {
            val result: String =
                sourceCode.replace(
                    Regex("(?m)^(\\s*#\\s*version\\s+.*)$"),
                    "$1\n" + Matcher.quoteReplacement(definesCode),
                )
            if (result.equals(sourceCode)) {
                // No #version specified, so just prepend source
                return definesCode + sourceCode
            }
            return result
        }

        private fun inputStreamToString(stream: InputStream): String {
            val reader: InputStreamReader = InputStreamReader(stream, UTF_8.name())
            val buffer: CharArray = CharArray(1024 * 4)
            val builder: StringBuilder = StringBuilder()
            var amount: Int
            do {
                amount = reader.read(buffer)
                if (amount != -1) {
                    builder.append(buffer, 0, amount)
                }
            } while (amount != -1)
            reader.close()
            return builder.toString()
        }
    }
}
