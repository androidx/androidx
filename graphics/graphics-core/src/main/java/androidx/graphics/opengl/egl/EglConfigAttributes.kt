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

package androidx.graphics.opengl.egl

import android.opengl.EGL14

/**
 * EGL configuration attribute used to expose EGLConfigs that support formats with floating point
 * RGBA components. This attribute is exposed through the EGL_EXT_pixel_format_float EGL extension
 *
 * See: https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_pixel_format_float.txt
 */
const val EglColorComponentTypeExt = 0x3339

/**
 * EGL configuration attribute value that represents fixed point RGBA components
 */
const val EglColorComponentTypeFixedExt = 0x333A

/**
 * EGL configuration attribute value that represents floating point RGBA components
 */
const val EglColorComponentTypeFloatExt = 0x333B

/**
 * EGL Attributes to create an 8 bit EGL config for red, green, blue, and alpha channels as well
 * as an 8 bit stencil size
 */
val EglConfigAttributes8888 = EglConfigAttributes {
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
 * 2 bit alpha channels as well as an 8 bit stencil size
 */
val EglConfigAttributes1010102 = EglConfigAttributes {
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
 * EGL Attributes to create a 16 bit floating point EGL config for red, green and blue channels
 * along with a
 */
val EglConfigAttributesF16 = EglConfigAttributes {
    EGL14.EGL_RENDERABLE_TYPE to EGL14.EGL_OPENGL_ES2_BIT
    EglColorComponentTypeExt to EglColorComponentTypeFloatExt
    EGL14.EGL_RED_SIZE to 16
    EGL14.EGL_GREEN_SIZE to 16
    EGL14.EGL_BLUE_SIZE to 16
    EGL14.EGL_ALPHA_SIZE to 16
    EGL14.EGL_DEPTH_SIZE to 0
    EGL14.EGL_STENCIL_SIZE to 0
    EGL14.EGL_SURFACE_TYPE to EGL14.EGL_WINDOW_BIT
}

/**
 * Construct an instance of [EglConfigAttributes] that includes a mapping of EGL attributes
 * to their corresponding value. The full set of attributes can be found here:
 * https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml
 *
 * The resultant array of attributes automatically is terminated with EGL_NONE.
 *
 * For example to create an 8888 configuration, this can be done with the following:
 *
 * EglConfigAttributes {
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
 * @see EglConfigAttributes8888
 */
inline fun EglConfigAttributes(block: EglConfigAttributes.Builder.() -> Unit): EglConfigAttributes =
    EglConfigAttributes.Builder().apply { block() }.build()

// klint does not support value classes yet, see b/197692691
@Suppress("INLINE_CLASS_DEPRECATED")
inline class EglConfigAttributes internal constructor(
    @PublishedApi internal val attrs: IntArray
) {

    /**
     * Builder used to create an instance of [EglConfigAttributes]
     * Allows for a mapping of EGL configuration attributes to their corresponding
     * values as well as including a previously generated [EglConfigAttributes]
     * instance to be used as a template and conditionally update individual mapped values
     */
    // Suppressing build method as EglConfigAttributes is created using Kotlin DSL syntax
    // via the function constructor defined above
    @SuppressWarnings("MissingBuildMethod")
    class Builder @PublishedApi internal constructor() {
        private val attrs = HashMap<Int, Int>()

        /**
         * Map a given EGL configuration attribute key to the given EGL configuration value
         */
        @SuppressWarnings("BuilderSetStyle")
        infix fun Int.to(that: Int) {
            attrs[this] = that
        }

        /**
         * Include all the attributes of the given EglConfigAttributes instance.
         * This is useful for creating a new EglConfigAttributes instance with all the same
         * attributes as another, allowing for modification of attributes after the fact.
         * For example, the following code snippet can be used to create an EglConfigAttributes
         * instance that has all the same configuration as [EglConfigAttributes8888] but with a
         * 16 bit stencil buffer size:
         *
         * EglConfigAttributes {
         *      include(EglConfigAttributes8888)
         *      EGL14.EGL_STENCIL_SIZE to 16
         * }
         *
         *
         * That is all attributes configured after the include will overwrite the attributes
         * configured previously.
         */
        @SuppressWarnings("BuilderSetStyle")
        fun include(attributes: EglConfigAttributes) {
            val attrsArray = attributes.attrs
            for (i in 0 until attrsArray.size - 1 step 2) {
                attrs[attrsArray[i]] = attrsArray[i + 1]
            }
        }

        @PublishedApi internal fun build(): EglConfigAttributes {
            val entries = attrs.entries
            val attrArray = IntArray(entries.size * 2 + 1) // Array must end with EGL_NONE
            var index = 0
            for (entry in entries) {
                attrArray[index] = entry.key
                attrArray[index + 1] = entry.value
                index += 2
            }
            attrArray[index] = EGL14.EGL_NONE
            return EglConfigAttributes(attrArray)
        }
    }
}