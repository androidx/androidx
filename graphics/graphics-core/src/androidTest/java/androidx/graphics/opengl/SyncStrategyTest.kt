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

package androidx.graphics.opengl

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.FrontBufferSyncStrategy
import androidx.graphics.lowlatency.FrontBufferUtils
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.supportsNativeAndroidFence
import androidx.graphics.withEgl
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(Build.VERSION_CODES.Q)
class SyncStrategyTest {
    private val mUsageFlags = FrontBufferUtils.obtainHardwareBufferUsageFlags()

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun testSyncStrategy_Always() {
        withEgl { egl ->
            if (egl.supportsNativeAndroidFence()) {
                val strategy = SyncStrategy.ALWAYS
                val fence = strategy.createSyncFence(egl.eglSpec)
                assertTrue(fence != null)
                fence?.close()
            }
        }
    }

    @Test
    fun testSyncStrategy_onFirstShow_FrontBufferUsageOff_Invisible() {
        withEgl { egl ->
            if (egl.supportsNativeAndroidFence()) {
                val strategy = FrontBufferSyncStrategy(0L)
                val fence = strategy.createSyncFence(EGLSpec.V14)
                assertTrue(fence != null)
                fence?.close()
            }
        }
    }

    @Test
    fun testSyncStrategy_onFirstShow_FrontBufferUsageOff_Visible() {
        withEgl { egl ->
            if (egl.supportsNativeAndroidFence()) {
                val strategy = FrontBufferSyncStrategy(0L)
                strategy.isVisible = true
                val fence = strategy.createSyncFence(EGLSpec.V14)
                assertTrue(fence == null)
                fence?.close()
            }
        }
    }

    @Test
    fun testSyncStrategy_onFirstShow_FrontBufferUsageOn_Invisible() {
        withEgl { egl ->
            if (egl.supportsNativeAndroidFence()) {
                val strategy = FrontBufferSyncStrategy(mUsageFlags)
                val fence = strategy.createSyncFence(egl.eglSpec)
                assertTrue(fence != null)
                fence?.close()
            }
        }
    }

    @Test
    fun testSyncStrategy_onFirstShow_FrontBufferUsageOn_Visible() {
        withEgl { egl ->
            if (egl.supportsNativeAndroidFence()) {
                val strategy = FrontBufferSyncStrategy(mUsageFlags)
                strategy.isVisible = true
                val fence = strategy.createSyncFence(EGLSpec.V14)
                assertTrue(fence == null)
                fence?.close()
            }
        }
    }
}
