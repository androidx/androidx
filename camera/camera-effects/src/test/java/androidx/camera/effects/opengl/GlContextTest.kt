/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.opengl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** Unit tests for [GlContext] EGLDisplay cleanup when [GlRenderer.init] fails. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK], shadows = [GlContextTest.ShadowTrackingEgl14::class])
class GlContextTest {

    @Before
    fun setUp() {
        ShadowTrackingEgl14.reset()
    }

    @Test
    fun glRendererInit_whenEglGetDisplayReturnsNoDisplay_doesNotTerminateDisplay() {
        ShadowTrackingEgl14.returnNoDisplay = true

        val thrown = assertThrows(IllegalStateException::class.java) { GlRenderer(0).init() }

        assertThat(thrown).hasMessageThat().contains("Unable to get EGL14 display")
        assertThat(ShadowTrackingEgl14.initializeCount).isEqualTo(0)
        assertThat(ShadowTrackingEgl14.terminateCount).isEqualTo(0)
    }

    @Test
    fun glRendererInit_whenEglInitializeFails_doesNotTerminateDisplay() {
        ShadowTrackingEgl14.initializeResult = false

        val thrown = assertThrows(IllegalStateException::class.java) { GlRenderer(0).init() }

        assertThat(thrown).hasMessageThat().contains("Unable to initialize EGL14")
        assertThat(ShadowTrackingEgl14.initializeCount).isEqualTo(1)
        assertThat(ShadowTrackingEgl14.terminateCount).isEqualTo(0)
    }

    @Test
    fun glRendererInit_whenEglChooseConfigFails_terminatesDisplayExactlyOnce() {
        ShadowTrackingEgl14.chooseConfigResult = false

        val thrown = assertThrows(IllegalStateException::class.java) { GlRenderer(0).init() }

        assertThat(thrown).hasMessageThat().contains("Unable to find a suitable EGLConfig")
        assertThat(ShadowTrackingEgl14.initializeCount).isEqualTo(1)
        assertThat(ShadowTrackingEgl14.terminateCount).isEqualTo(1)
    }

    @Implements(EGL14::class)
    class ShadowTrackingEgl14 {
        companion object {
            // Use a non-zero handle: a zero handle equals EGL14.EGL_NO_DISPLAY.
            private val fakeDisplay: EGLDisplay =
                ReflectionHelpers.callConstructor(
                    EGLDisplay::class.java,
                    ClassParameter.from(Long::class.javaPrimitiveType, 1L),
                )

            var returnNoDisplay: Boolean = false
            var initializeResult: Boolean = true
            var chooseConfigResult: Boolean = true
            var initializeCount: Int = 0
            var terminateCount: Int = 0

            fun reset() {
                returnNoDisplay = false
                initializeResult = true
                chooseConfigResult = true
                initializeCount = 0
                terminateCount = 0
            }

            @Implementation
            @JvmStatic
            fun eglGetDisplay(displayId: Int): EGLDisplay? =
                if (returnNoDisplay) EGL14.EGL_NO_DISPLAY else fakeDisplay

            @Implementation
            @JvmStatic
            fun eglInitialize(
                dpy: EGLDisplay?,
                major: IntArray?,
                majorOffset: Int,
                minor: IntArray?,
                minorOffset: Int,
            ): Boolean {
                initializeCount++
                return initializeResult
            }

            @Implementation
            @JvmStatic
            fun eglChooseConfig(
                dpy: EGLDisplay?,
                attribList: IntArray?,
                attribListOffset: Int,
                configs: Array<EGLConfig?>?,
                configsOffset: Int,
                configSize: Int,
                numConfig: IntArray?,
                numConfigOffset: Int,
            ): Boolean = chooseConfigResult

            @Implementation
            @JvmStatic
            fun eglMakeCurrent(
                dpy: EGLDisplay?,
                draw: EGLSurface?,
                read: EGLSurface?,
                ctx: EGLContext?,
            ): Boolean = true

            @Implementation
            @JvmStatic
            fun eglTerminate(dpy: EGLDisplay?): Boolean {
                terminateCount++
                return true
            }

            @Implementation @JvmStatic fun eglReleaseThread(): Boolean = true
        }
    }
}
