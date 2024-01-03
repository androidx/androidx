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

import androidx.opengl.EGLExt
import androidx.opengl.EGLExt.Companion.EGL_ANDROID_NATIVE_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_EXT_BUFFER_AGE
import androidx.opengl.EGLExt.Companion.EGL_EXT_GL_COLORSPACE_BT2020_PQ
import androidx.opengl.EGLExt.Companion.EGL_EXT_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH
import androidx.opengl.EGLExt.Companion.EGL_EXT_GL_COLORSPACE_SCRGB
import androidx.opengl.EGLExt.Companion.EGL_EXT_PIXEL_FORMAT_FLOAT
import androidx.opengl.EGLExt.Companion.EGL_IMG_CONTEXT_PRIORITY
import androidx.opengl.EGLExt.Companion.EGL_KHR_FENCE_SYNC
import androidx.opengl.EGLExt.Companion.EGL_KHR_GL_COLORSPACE
import androidx.opengl.EGLExt.Companion.EGL_KHR_NO_CONFIG_CONTEXT
import androidx.opengl.EGLExt.Companion.EGL_KHR_PARTIAL_UPDATE
import androidx.opengl.EGLExt.Companion.EGL_KHR_SURFACELESS_CONTEXT
import androidx.opengl.EGLExt.Companion.EGL_KHR_SWAP_BUFFERS_WITH_DAMAGE
import androidx.opengl.EGLExt.Companion.EGL_KHR_WAIT_SYNC
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EGLExtensionsTest {

    @Test
    fun testSupportsBufferAge() {
        assertTrue(EGLExt.parseExtensions("EGL_EXT_buffer_age").contains(EGL_EXT_BUFFER_AGE))
    }

    @Test
    fun testSupportBufferAgeFromPartialUpdate() {
        // Buffer age can be supported from either EGL_EXT_buffer_age or EGL_KHR_partial_update
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_partial_update").contains(EGL_KHR_PARTIAL_UPDATE)
        )
    }

    @Test
    fun testSetDamage() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_partial_update")
                .contains(EGL_KHR_PARTIAL_UPDATE)
        )
    }

    @Test
    fun testSwapBuffersWithDamage() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_swap_buffers_with_damage")
                .contains(EGL_KHR_SWAP_BUFFERS_WITH_DAMAGE)
        )
    }

    @Test
    fun testColorSpace() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_gl_colorspace")
                .contains(EGL_KHR_GL_COLORSPACE)
        )
    }

    @Test
    fun testNoConfigContext() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_no_config_context")
                .contains(EGL_KHR_NO_CONFIG_CONTEXT)
        )
    }

    @Test
    fun testPixelFormatFloat() {
        assertTrue(
            EGLExt.parseExtensions("EGL_EXT_pixel_format_float")
                .contains(EGL_EXT_PIXEL_FORMAT_FLOAT)
        )
    }

    @Test
    fun testScRgb() {
        assertTrue(
            EGLExt.parseExtensions("EGL_EXT_gl_colorspace_scrgb")
                .contains(EGL_EXT_GL_COLORSPACE_SCRGB)
        )
    }

    @Test
    fun testDisplayP3() {
        assertTrue(
            EGLExt.parseExtensions("EGL_EXT_gl_colorspace_display_p3_passthrough")
                .contains(EGL_EXT_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH)
        )
    }

    @Test
    fun testHDR() {
        assertTrue(
            EGLExt.parseExtensions("EGL_EXT_gl_colorspace_bt2020_pq")
                .contains(EGL_EXT_GL_COLORSPACE_BT2020_PQ)
        )
    }

    @Test
    fun testContextPriority() {
        assertTrue(
            EGLExt.parseExtensions("EGL_IMG_context_priority")
                .contains(EGL_IMG_CONTEXT_PRIORITY)
        )
    }

    @Test
    fun testSurfacelessContext() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_surfaceless_context")
                .contains(EGL_KHR_SURFACELESS_CONTEXT)
        )
    }

    @Test
    fun testFenceSync() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_fence_sync")
                .contains(EGL_KHR_FENCE_SYNC)
        )
    }

    @Test
    fun testWaitSync() {
        assertTrue(
            EGLExt.parseExtensions("EGL_KHR_wait_sync")
            .contains(EGL_KHR_WAIT_SYNC))
    }

    @Test
    fun testNativeFenceSync() {
        assertTrue(
            EGLExt.parseExtensions("EGL_ANDROID_native_fence_sync")
                .contains(EGL_ANDROID_NATIVE_FENCE_SYNC)
        )
    }

    @Test
    fun testExtensionsQueryStringParsing() {
        val extensionQuery = "EGL_EXT_buffer_age " +
            "EGL_KHR_partial_update " +
            "EGL_KHR_swap_buffers_with_damage " +
            "EGL_KHR_gl_colorspace " +
            "EGL_KHR_no_config_context " +
            "EGL_EXT_pixel_format_float " +
            "EGL_EXT_gl_colorspace_scrgb " +
            "EGL_EXT_gl_colorspace_display_p3_passthrough " +
            "EGL_EXT_gl_colorspace_bt2020_pq " +
            "EGL_IMG_context_priority " +
            "EGL_KHR_surfaceless_context " +
            "EGL_KHR_fence_sync " +
            "EGL_KHR_wait_sync " +
            "EGL_ANDROID_native_fence_sync "
        with(EGLExt.parseExtensions(extensionQuery)) {
            assertTrue(contains(EGL_EXT_BUFFER_AGE))
            assertTrue(contains(EGL_KHR_PARTIAL_UPDATE))
            assertTrue(contains(EGL_KHR_SWAP_BUFFERS_WITH_DAMAGE))
            assertTrue(contains(EGL_KHR_GL_COLORSPACE))
            assertTrue(contains(EGL_KHR_NO_CONFIG_CONTEXT))
            assertTrue(contains(EGL_EXT_PIXEL_FORMAT_FLOAT))
            assertTrue(contains(EGL_EXT_GL_COLORSPACE_SCRGB))
            assertTrue(contains(EGL_EXT_GL_COLORSPACE_DISPLAY_P3_PASSTHROUGH))
            assertTrue(contains(EGL_EXT_GL_COLORSPACE_BT2020_PQ))
            assertTrue(contains(EGL_IMG_CONTEXT_PRIORITY))
            assertTrue(contains(EGL_KHR_SURFACELESS_CONTEXT))
            assertTrue(contains(EGL_KHR_FENCE_SYNC))
            assertTrue(contains(EGL_KHR_WAIT_SYNC))
            assertTrue(contains(EGL_ANDROID_NATIVE_FENCE_SYNC))
        }
    }
}
