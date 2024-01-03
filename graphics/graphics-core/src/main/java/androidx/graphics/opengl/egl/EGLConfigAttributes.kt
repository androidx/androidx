/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("AcronymName")
package androidx.graphics.opengl.egl

import android.opengl.EGL14

/**
 * Construct an instance of [EGLConfigAttributes] that includes a mapping of EGL attributes
 * to their corresponding value. The full set of attributes can be found here:
 * https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml
 *
 * The resultant array of attributes automatically is terminated with EGL_NONE.
 *
 * For example to create an 8888 configuration, this can be done with the following:
 *
 * EGLConfigAttributes {
 *      EGL14.EGL_RENDERABLE_TYPE to EGL14.EGL_OPENGL_ES2_BIT
 *      EGL14.EGL_RED_SIZE to 8
 *      EGL14.EGL_GREEN_SIZE to 8
 *      EGL14.EGL_BLUE_SIZE to 8
 *      EGL14.EGL_ALPHA_SIZE to 8
 *      EGL14.EGL_DEPTH_SIZE to 0
 *      EGL14.EGL_CONFIG_CAVEAT to EGL14.EGL_NONE
 *      EGL14.EGL_STENCIL_SIZE to 8
 *      EGL14.EGL_SURFACE_TYPE to EGL14.EGL_WINDOW_BIT
 * }
 *
 * @see EGLConfigAttributes.RGBA_8888
 */
@JvmSynthetic
inline fun EGLConfigAttributes(block: EGLConfigAttributes.Builder.() -> Unit): EGLConfigAttributes =
    EGLConfigAttributes.Builder().apply { block() }.build()

@Suppress("AcronymName")
class EGLConfigAttributes internal constructor(
    @PublishedApi internal val attrs: IntArray
) {

    /**
     * Return a copy of the created integer array used for EGL methods.
     * Most consumers would pass the [EGLConfigAttributes] instance as a parameter instead, however,
     * this method is provided as a convenience for debugging and testing purposes.
     */
    fun toArray(): IntArray = attrs.clone()

    /**
     * Builder used to create an instance of [EGLConfigAttributes]
     * Allows for a mapping of EGL configuration attributes to their corresponding
     * values as well as including a previously generated [EGLConfigAttributes]
     * instance to be used as a template and conditionally update individual mapped values
     */
    class Builder @PublishedApi internal constructor() {
        private val attrs = HashMap<Int, Int>()

        /**
         * Map a given EGL configuration attribute key to the given EGL configuration value
         */
        @SuppressWarnings("BuilderSetStyle")
        @JvmSynthetic
        infix fun Int.to(that: Int) {
            setAttribute(this, that)
        }

        /**
         * Map a given EGL configuration attribute key to the given EGL configuration value
         * @param attribute EGL attribute name such as [EGL14.EGL_RED_SIZE]
         * @param value Corresponding value for the [attribute]
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAttribute(attribute: Int, value: Int): Builder {
            attrs[attribute] = value
            return this
        }

        /**
         * Include all the attributes of the given [EGLConfigAttributes] instance.
         * This is useful for creating a new [EGLConfigAttributes] instance with all the same
         * attributes as another, allowing for modification of attributes after the fact.
         * For example, the following code snippet can be used to create an [EGLConfigAttributes]
         * instance that has all the same configuration as [RGBA_8888] but with a
         * 16 bit stencil buffer size:
         *
         * EGLConfigAttributes {
         *      include(EGLConfigAttributes.RGBA_8888)
         *      EGL14.EGL_STENCIL_SIZE to 16
         * }
         *
         *
         * That is all attributes configured after the include will overwrite the attributes
         * configured previously.
         */
        @SuppressWarnings("BuilderSetStyle")
        fun include(attributes: EGLConfigAttributes) {
            val attrsArray = attributes.attrs
            for (i in 0 until attrsArray.size - 1 step 2) {
                attrs[attrsArray[i]] = attrsArray[i + 1]
            }
        }

        /**
         * Construct an instance of [EGLConfigAttributes] with the mappings of integer keys
         * to their respective values. This creates a flat integer array with alternating values
         * for the key value pairs and ends with EGL_NONE
         */
        fun build(): EGLConfigAttributes {
            val entries = attrs.entries
            val attrArray = IntArray(entries.size * 2 + 1) // Array must end with EGL_NONE
            var index = 0
            for (entry in entries) {
                attrArray[index] = entry.key
                attrArray[index + 1] = entry.value
                index += 2
            }
            attrArray[index] = EGL14.EGL_NONE
            return EGLConfigAttributes(attrArray)
        }
    }

    companion object {
        /**
         * EGL configuration attribute used to expose EGLConfigs that support formats with floating
         * point RGBA components. This attribute is exposed through the EGL_EXT_pixel_format_float
         * EGL extension
         *
         * See: https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_pixel_format_float.txt
         */
        const val EGL_COLOR_COMPONENT_TYPE_EXT = 0x3339

        /**
         * EGL configuration attribute value that represents fixed point RGBA components
         */
        const val EGL_COLOR_COMPONENT_TYPE_FIXED_EXT = 0x333A

        /**
         * EGL configuration attribute value that represents floating point RGBA components
         */
        const val EGL_COLOR_COMPONENT_TYPE_FLOAT_EXT = 0x333B

        /**
         * EGL Attributes to create an 8 bit EGL config for red, green, blue, and alpha channels as
         * well as an 8 bit stencil size
         */
        @Suppress("AcronymName")
        @JvmField
        val RGBA_8888 = EGLConfigAttributes {
            EGL14.EGL_RENDERABLE_TYPE to EGL14.EGL_OPENGL_ES2_BIT
            EGL14.EGL_RED_SIZE to 8
            EGL14.EGL_GREEN_SIZE to 8
            EGL14.EGL_BLUE_SIZE to 8
            EGL14.EGL_ALPHA_SIZE to 8
            EGL14.EGL_DEPTH_SIZE to 0
            EGL14.EGL_CONFIG_CAVEAT to EGL14.EGL_NONE
            EGL14.EGL_STENCIL_SIZE to 0
            EGL14.EGL_SURFACE_TYPE to EGL14.EGL_WINDOW_BIT
        }

        /**
         * EGL Attributes to create a 10 bit EGL config for red, green, blue, channels and a
         * 2 bit alpha channels. This does not include any bits for depth and stencil buffers.
         */
        @Suppress("AcronymName")
        @JvmField
        val RGBA_1010102 = EGLConfigAttributes {
            EGL14.EGL_RENDERABLE_TYPE to EGL14.EGL_OPENGL_ES2_BIT
            EGL14.EGL_RED_SIZE to 10
            EGL14.EGL_GREEN_SIZE to 10
            EGL14.EGL_BLUE_SIZE to 10
            EGL14.EGL_ALPHA_SIZE to 2
            EGL14.EGL_DEPTH_SIZE to 0
            EGL14.EGL_STENCIL_SIZE to 0
            EGL14.EGL_SURFACE_TYPE to EGL14.EGL_WINDOW_BIT
        }

        /**
         * EGL Attributes to create a 16 bit floating point EGL config for red, green, blue and
         * alpha channels without a depth or stencil channel.
         */
        @Suppress("AcronymName")
        @JvmField
        val RGBA_F16 = EGLConfigAttributes {
            EGL14.EGL_RENDERABLE_TYPE to EGL14.EGL_OPENGL_ES2_BIT
            EGL_COLOR_COMPONENT_TYPE_EXT to EGL_COLOR_COMPONENT_TYPE_FLOAT_EXT
            EGL14.EGL_RED_SIZE to 16
            EGL14.EGL_GREEN_SIZE to 16
            EGL14.EGL_BLUE_SIZE to 16
            EGL14.EGL_ALPHA_SIZE to 16
            EGL14.EGL_DEPTH_SIZE to 0
            EGL14.EGL_STENCIL_SIZE to 0
            EGL14.EGL_SURFACE_TYPE to EGL14.EGL_WINDOW_BIT
        }
    }
}
