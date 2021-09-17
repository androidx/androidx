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

/**
 * Determines if applications can query the age of the back buffer contents for an
 * EGL surface as the number of frames elapsed since the contents were recently defined
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_buffer_age.txt
 */
const val EglExtBufferAge = "EGL_EXT_buffer_age"

/**
 * Allows for efficient partial updates to an area of a **buffer** that has changed since
 * the last time the buffer was used
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_partial_update.txt
 */
const val EglKhrPartialUpdate = "EGL_KHR_partial_update"

/**
 * Allows for efficient partial updates to an area of a **surface** that changes between
 * frames for the surface. This relates to the differences between two buffers, the current
 * back buffer and the current front buffer.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_swap_buffers_with_damage.txt
 */
const val EglKhrSwapBuffersWithDamage = "EGL_KHR_swap_buffers_with_damage"

/**
 * Determines whether to use sRGB format default framebuffers to render sRGB
 * content to display devices. Supports creation of EGLSurfaces which will be rendered to in
 * sRGB by OpenGL contexts supporting that capability.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_gl_colorspace.txt
 */
const val EglKhrGlColorSpace = "EGL_KHR_gl_colorspace"

/**
 * Determines whether creation of GL and ES contexts without an EGLConfig is allowed
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_no_config_context.txt
 */
const val EglKhrNoConfigContext = "EGL_KHR_no_config_context"

/**
 * Determines whether floating point RGBA components are supported
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_pixel_format_float.txt
 */
const val EglExtPixelFormatFloat = "EGL_EXT_pixel_format_float"

/**
 * Determines whether extended sRGB color spaces are supported options for EGL Surfaces
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_scrgb.txt
 */
const val EglExtGlColorSpaceScRgb = "EGL_EXT_gl_colorspace_scrgb"

/**
 * Determines whether the underlying platform can support rendering framebuffers in the
 * non-linear Display-P3 color space
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_display_p3_passthrough.txt
 */
const val EglExtColorSpaceDisplayP3Passthrough = "EGL_EXT_gl_colorspace_display_p3_passthrough"

/**
 * Determines whether the platform framebuffers support rendering in a larger color gamut
 * specified in the BT.2020 color space
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/EXT/EGL_EXT_gl_colorspace_bt2020_linear.txt
 */
const val EglExtGlColorSpaceBt2020Pq = "EGL_EXT_gl_colorspace_bt2020_pq"

/**
 * Determines whether an EGLContext can be created with a priority hint. Not all implementations
 * are guaranteed to honor the hint.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/IMG/EGL_IMG_context_priority.txt
 */
const val EglImgContextPriority = "EGL_IMG_context_priority"

/**
 * Determines whether creation of an EGL Context without a surface is supported.
 * This is useful for applications that only want to render to client API targets (such as
 * OpenGL framebuffer objects) and avoid the need to a throw-away EGL surface just to get
 * a current context.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_surfaceless_context.txt
 */
const val EglKhrSurfacelessContext = "EGL_KHR_surfaceless_context"

/**
 * Determines whether sync objects are supported. Sync objects are synchronization primitives
 * that represent events whose completion can be tested or waited upon.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_fence_sync.txt
 */
const val EglKhrFenceSync = "EGL_KHR_fence_sync"

/**
 * Determines whether waiting for signaling of sync objects is supported. This form of wait
 * does not necessarily block the application thread which issued the wait. Therefore
 * applications may continue to issue commands to the client API or perform other work
 * in parallel leading to increased performance.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/KHR/EGL_KHR_wait_sync.txt
 */
const val EglKhrWaitSync = "EGL_KHR_wait_sync"

/**
 * Determines whether creation of platform specific sync objects are supported. These
 * objects that are associated with a native synchronization fence object using a file
 * descriptor.
 *
 * See:
 * https://www.khronos.org/registry/EGL/extensions/ANDROID/EGL_ANDROID_native_fence_sync.txt
 */
const val EglAndroidNativeFenceSync = "EGL_ANDROID_native_fence_sync"

/**
 * Class determining the types of OpenGL extensions supported by the given
 * EGL spec.
 */
// klint does not support value classes yet, see b/197692691
@Suppress("INLINE_CLASS_DEPRECATED")
inline class EglExtensions(
    private val extensionSet: Set<String>
) {

    /**
     * Determines whether the extension with the provided name is supported. The string
     * provided is expected to be one of the named extensions defined within the OpenGL
     * extension documentation.
     *
     * Returns true if the extension is supported, false otherwise
     */
    fun supportsExtension(extensionName: String): Boolean =
        extensionSet.contains(extensionName)

    companion object {

        /**
         * Creates an instance of [EglExtensions] from a space separated string
         * that represents the set of OpenGL extensions supported
         */
        @JvmStatic
        fun from(queryString: String): EglExtensions =
            HashSet<String>().let {
                it.addAll(queryString.split(' '))
                EglExtensions(it)
            }
    }
}