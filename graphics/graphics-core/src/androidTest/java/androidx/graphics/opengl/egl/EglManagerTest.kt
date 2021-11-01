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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EglManagerTest {

    @Test
    fun testInitializeAndRelease() {
        testEglManager {
            initialize()
            val config = loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            if (config == null) {
                fail("Config 8888 should be supported")
            }
            // Even though EGL v 1.5 was introduced in API level 29 not all devices will advertise
            // support for it. However, all devices should at least support EGL v 1.4
            assertTrue(
                "Unexpected EGL version, received $eglVersion",
                eglVersion == EglVersion.V14 || eglVersion == EglVersion.V15
            )
            assertNotNull(eglContext)
            assertNotNull(eglConfig)
        }
    }

    @Test
    fun testMultipleInitializeCallsIgnored() {
        testEglManager {
            initialize()
            loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            val currentContext = eglContext
            val currentConfig = eglConfig
            assertNotEquals(EGL14.EGL_NO_CONTEXT, currentContext)
            // Subsequent calls to initialize should be ignored
            // and the current EglContext should be the same as the previous call
            initialize()
            assertTrue(currentContext === eglContext)
            assertTrue(currentConfig === eglConfig)
        }
    }

    @Test
    fun testMultipleReleaseCallsIgnored() {
        testEglManager {
            initialize()
            loadConfig(EglConfigAttributes8888)?.also {
                createContext(it)
            }
            // Multiple attempts to release should act as no-ops, i.e. we should not crash
            // and the corresponding context should be nulled out
            release()
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)

            release()
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }

    @Test
    fun testDefaultSurface() {
        testEglManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EglConfigAttributes8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }

            createContext(config!!)

            if (isExtensionSupported(EglKhrSurfacelessContext)) {
                assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            } else {
                assertNotEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            }

            assertEquals(currentDrawSurface, defaultSurface)
            assertEquals(currentReadSurface, defaultSurface)

            release()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)
        }
    }

    @Test
    fun testCreatePBufferSurface() {
        testEglManager {
            initialize()

            assertEquals(defaultSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentDrawSurface, EGL14.EGL_NO_SURFACE)
            assertEquals(currentReadSurface, EGL14.EGL_NO_SURFACE)

            val config = loadConfig(EglConfigAttributes8888)

            if (config == null) {
                fail("Config 8888 should be supported")
            }
            createContext(config!!)

            val pBuffer = eglSpec.eglCreatePBufferSurface(
                config,
                EglConfigAttributes {
                    EGL14.EGL_WIDTH to 1
                    EGL14.EGL_HEIGHT to 1
                })

            makeCurrent(pBuffer)

            assertNotEquals(EGL14.EGL_NO_SURFACE, currentReadSurface)
            assertNotEquals(EGL14.EGL_NO_SURFACE, currentDrawSurface)
            assertNotEquals(EGL14.EGL_NO_SURFACE, pBuffer)

            assertEquals(pBuffer, currentReadSurface)
            assertEquals(pBuffer, currentDrawSurface)

            eglSpec.eglDestroySurface(pBuffer)
            release()
        }
    }

    /**
     * Helper method to ensure EglManager has the corresponding release calls
     * made to it and verifies that no exceptions were thrown as part of the test.
     */
    private fun testEglManager(block: EglManager.() -> Unit = {}) {
        with(EglManager()) {
            assertEquals(EglVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
            block()
            release()
            assertEquals(EglVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }
}