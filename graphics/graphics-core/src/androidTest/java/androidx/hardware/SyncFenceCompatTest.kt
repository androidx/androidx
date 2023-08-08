/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.hardware

import android.opengl.EGL14
import android.opengl.GLES20
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.EGLVersion
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(Build.VERSION_CODES.O)
class SyncFenceCompatTest {
    @Test
    fun testSyncFenceCompat_Create() {
        testEglManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val syncFenceCompat = SyncFenceCompat.createNativeSyncFence()
                assert(syncFenceCompat.isValid())
                syncFenceCompat.close()
            }
        }
    }

    @Test
    fun testSyncFenceCompat_Await() {
        testEglManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {

                val syncFenceCompat = SyncFenceCompat.createNativeSyncFence()
                assert(syncFenceCompat.isValid())
                GLES20.glFlush()
                assertTrue(syncFenceCompat.await(1000))

                syncFenceCompat.close()
            }
        }
    }

    @Test
    fun testSyncFenceCompat_AwaitForever() {
        testEglManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val syncFenceCompat = SyncFenceCompat.createNativeSyncFence()
                assert(syncFenceCompat.isValid())
                assertTrue(syncFenceCompat.awaitForever())

                syncFenceCompat.close()
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun testSyncFenceCompat_SignalTime() {
        testEglManager {
            initializeWithDefaultConfig()
            if (supportsNativeAndroidFence()) {
                val start = System.nanoTime()
                val syncFenceCompat = SyncFenceCompat.createNativeSyncFence()
                assertTrue(syncFenceCompat.isValid())
                assertTrue(syncFenceCompat.getSignalTimeNanos() !=
                    SyncFenceCompat.SIGNAL_TIME_INVALID)
                assertTrue(syncFenceCompat.awaitForever())

                assertTrue(syncFenceCompat.getSignalTimeNanos() > start)
                assertTrue(
                    syncFenceCompat.getSignalTimeNanos() !=
                        SyncFenceCompat.SIGNAL_TIME_PENDING
                )

                syncFenceCompat.close()
            }
        }
    }

    // Helper method used in testing to initialize EGL and default
    // EGLConfig to the ARGB8888 configuration
    private fun EGLManager.initializeWithDefaultConfig() {
        initialize()
        val config = loadConfig(EGLConfigAttributes.RGBA_8888)
        if (config == null) {
            fail("Config 8888 should be supported")
        }
        createContext(config!!)
    }

    /**
     * Helper method to ensure EglManager has the corresponding release calls
     * made to it and verifies that no exceptions were thrown as part of the test.
     */
    private fun testEglManager(
        eglSpec: EGLSpec = EGLSpec.V14,
        block: EGLManager.() -> Unit = {}
    ) {
        with(EGLManager(eglSpec)) {
            assertEquals(EGLVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
            block()
            release()
            assertEquals(EGLVersion.Unknown, eglVersion)
            assertEquals(EGL14.EGL_NO_CONTEXT, eglContext)
        }
    }
}
