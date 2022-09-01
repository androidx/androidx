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

package androidx.camera.camera2.pipe

import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraSurfaceManagerTest {
    private val surfaceManager = CameraSurfaceManager()
    private var activeInvokeCount = 0
    private var inactiveInvokeCount = 0
    private val listener = object : CameraSurfaceManager.SurfaceListener {
        override fun onSurfaceActive(surface: Surface) {
            activeInvokeCount++
        }

        override fun onSurfaceInactive(surface: Surface) {
            inactiveInvokeCount++
        }
    }
    private val imageReader1 = ImageReader.newInstance(1280, 720, ImageFormat.YUV_420_888, 4)
    private val imageReader2 = ImageReader.newInstance(1920, 1080, ImageFormat.YUV_420_888, 4)

    @Test
    fun testTokenClosed() {
        surfaceManager.addListener(listener)
        surfaceManager.registerSurface(imageReader1.surface).close()
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(1)
    }

    @Test
    fun testTokenNotClosed() {
        surfaceManager.addListener(listener)
        surfaceManager.registerSurface(imageReader1.surface)
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(0)
    }

    @Test
    fun testTokenDoubleClose() {
        surfaceManager.addListener(listener)
        val token = surfaceManager.registerSurface(imageReader1.surface)
        token.close()
        token.close()
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(1)
    }

    @Test
    fun testRemoveListener() {
        surfaceManager.addListener(listener)
        val token = surfaceManager.registerSurface(imageReader1.surface)
        surfaceManager.removeListener(listener)
        token.close()
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(0)
    }

    @Test
    fun testRemoveListenerDuringSurfaceListener() {
        surfaceManager.addListener(object : CameraSurfaceManager.SurfaceListener {
            override fun onSurfaceActive(surface: Surface) {
                // No-op
            }

            override fun onSurfaceInactive(surface: Surface) {
                surfaceManager.removeListener(this)
            }
        })
        surfaceManager.registerSurface(imageReader1.surface).close()
    }

    @Test
    fun testTwoSurfaces() {
        surfaceManager.addListener(listener)
        val token1 = surfaceManager.registerSurface(imageReader1.surface)
        val token2 = surfaceManager.registerSurface(imageReader2.surface)
        assertThat(activeInvokeCount).isEqualTo(2)

        token1.close()
        assertThat(inactiveInvokeCount).isEqualTo(1)

        token2.close()
        assertThat(inactiveInvokeCount).isEqualTo(2)
    }

    @Test
    fun testSameSurfaceInConsecutiveSessions() {
        surfaceManager.addListener(listener)
        val token1 = surfaceManager.registerSurface(imageReader1.surface)
        token1.close()
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(1)

        val token2 = surfaceManager.registerSurface(imageReader1.surface)
        token2.close()
        assertThat(activeInvokeCount).isEqualTo(2)
        assertThat(inactiveInvokeCount).isEqualTo(2)
    }

    @Test
    fun testSameSurfaceSharedInConsecutiveSessions() {
        surfaceManager.addListener(listener)
        val token1 = surfaceManager.registerSurface(imageReader1.surface)
        val token2 = surfaceManager.registerSurface(imageReader1.surface)

        // The same Surface should be given a different token because it's considered another "use".
        assertThat(token1).isNotEqualTo(token2)

        token1.close()
        // The use count would start with 2, with 1 remaining at this point.
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(0)

        token2.close()
        // All tokens for the same Surface have been closed, so the Surface should be inactive now.
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(1)
    }

    @Test
    fun testAddListenerAfterRegisterSurface() {
        val token = surfaceManager.registerSurface(imageReader1.surface)
        surfaceManager.addListener(listener)
        // We should be notified that the Surface is active even though the listener was added
        // after.
        assertThat(activeInvokeCount).isEqualTo(1)
        token.close()
        assertThat(inactiveInvokeCount).isEqualTo(1)
    }

    @Test
    fun testAddListenerAfterRegisterSurfaceAndCloseToken() {
        surfaceManager.registerSurface(imageReader1.surface).close()
        surfaceManager.addListener(listener)
        // Since no Surfaces are active, the listener shouldn't be invoked.
        assertThat(activeInvokeCount).isEqualTo(0)
        assertThat(inactiveInvokeCount).isEqualTo(0)

        surfaceManager.registerSurface(imageReader2.surface).close()
        assertThat(activeInvokeCount).isEqualTo(1)
        assertThat(inactiveInvokeCount).isEqualTo(1)
    }
}