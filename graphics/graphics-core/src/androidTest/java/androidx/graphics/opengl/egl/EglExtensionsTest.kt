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

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EglExtensionsTest {

    @Test
    fun testSupportsBufferAge() {
        assertTrue(EglExtensions(setOf("EGL_EXT_buffer_age")).supportsExtension(EglExtBufferAge))
    }

    @Test
    fun testSupportBufferAgeFromPartialUpdate() {
        // Buffer age can be supported from either EGL_EXT_buffer_age or EGL_KHR_partial_update
        assertTrue(
            EglExtensions(setOf("EGL_KHR_partial_update")).supportsExtension(EglKhrPartialUpdate)
        )
    }

    @Test
    fun testSetDamage() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_partial_update"))
                .supportsExtension(EglKhrPartialUpdate)
        )
    }

    @Test
    fun testSwapBuffersWithDamage() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_swap_buffers_with_damage"))
                .supportsExtension(EglKhrSwapBuffersWithDamage)
        )
    }

    @Test
    fun testColorSpace() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_gl_colorspace"))
                .supportsExtension(EglKhrGlColorSpace)
        )
    }

    @Test
    fun testNoConfigContext() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_no_config_context"))
                .supportsExtension(EglKhrNoConfigContext)
        )
    }

    @Test
    fun testPixelFormatFloat() {
        assertTrue(
            EglExtensions(setOf("EGL_EXT_pixel_format_float"))
                .supportsExtension(EglExtPixelFormatFloat)
        )
    }

    @Test
    fun testScRgb() {
        assertTrue(
            EglExtensions(setOf("EGL_EXT_gl_colorspace_scrgb"))
                .supportsExtension(EglExtGlColorSpaceScRgb)
        )
    }

    @Test
    fun testDisplayP3() {
        assertTrue(
            EglExtensions(setOf("EGL_EXT_gl_colorspace_display_p3_passthrough"))
                .supportsExtension(EglExtColorSpaceDisplayP3Passthrough)
        )
    }

    @Test
    fun testHDR() {
        assertTrue(
            EglExtensions(setOf("EGL_EXT_gl_colorspace_bt2020_pq"))
                .supportsExtension(EglExtGlColorSpaceBt2020Pq)
        )
    }

    @Test
    fun testContextPriority() {
        assertTrue(
            EglExtensions(setOf("EGL_IMG_context_priority"))
                .supportsExtension(EglImgContextPriority)
        )
    }

    @Test
    fun testSurfacelessContext() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_surfaceless_context"))
                .supportsExtension(EglKhrSurfacelessContext)
        )
    }

    @Test
    fun testFenceSync() {
        assertTrue(
            EglExtensions(setOf("EGL_KHR_fence_sync")).supportsExtension(EglKhrFenceSync)
        )
    }

    @Test
    fun testWaitSync() {
        assertTrue(EglExtensions(setOf("EGL_KHR_wait_sync")).supportsExtension(EglKhrWaitSync))
    }

    @Test
    fun testNativeFenceSync() {
        assertTrue(
            EglExtensions(setOf("EGL_ANDROID_native_fence_sync"))
                .supportsExtension(EglAndroidNativeFenceSync)
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
        with(EglExtensions.from(extensionQuery)) {
            assertTrue(supportsExtension(EglExtBufferAge))
            assertTrue(supportsExtension(EglKhrPartialUpdate))
            assertTrue(supportsExtension(EglKhrSwapBuffersWithDamage))
            assertTrue(supportsExtension(EglKhrGlColorSpace))
            assertTrue(supportsExtension(EglKhrNoConfigContext))
            assertTrue(supportsExtension(EglExtPixelFormatFloat))
            assertTrue(supportsExtension(EglExtGlColorSpaceScRgb))
            assertTrue(supportsExtension(EglExtColorSpaceDisplayP3Passthrough))
            assertTrue(supportsExtension(EglExtGlColorSpaceBt2020Pq))
            assertTrue(supportsExtension(EglImgContextPriority))
            assertTrue(supportsExtension(EglKhrSurfacelessContext))
            assertTrue(supportsExtension(EglKhrFenceSync))
            assertTrue(supportsExtension(EglKhrWaitSync))
            assertTrue(supportsExtension(EglAndroidNativeFenceSync))
        }
    }
}