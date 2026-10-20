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

package androidx.camera.camera2.compat

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Size
import android.view.Display
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.testing.TestShadowWindowManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS], shadows = [TestShadowWindowManager::class])
class DisplayCompatTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val context = WindowContextCountingContext(appContext)

    @Test
    fun getDisplaySize_returnsDisplayRealSize() {
        val display = addDisplay(480, 640)

        assertThat(DisplayCompat.getDisplaySize(context, display)).isEqualTo(display.realSize())
    }

    @Test
    @Config(maxSdk = 30)
    fun getDisplaySize_doesNotCreateWindowContext_onApi30AndBelow() {
        val display = addDisplay(480, 640)

        // On API 30, each WindowContext holds one of the app's limited window tokens until it is
        // finalized, so repeated calls must not create any.
        repeat(10) { DisplayCompat.getDisplaySize(context, display) }

        assertThat(context.windowContextCount).isEqualTo(0)
    }

    @Test
    @Config(minSdk = 31)
    fun getDisplaySize_usesWindowContext_onApi31AndAbove() {
        val display = addDisplay(480, 640)

        // A WindowContext is required on API 31+ to get WindowManager without a StrictMode
        // IncorrectContextUseViolation.
        DisplayCompat.getDisplaySize(context, display)

        assertThat(context.windowContextCount).isEqualTo(1)
    }

    private fun addDisplay(width: Int, height: Int): Display {
        val displayId = ShadowDisplayManager.addDisplay("w${width}dp-h${height}dp")
        val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return displayManager.getDisplay(displayId)
    }

    @Suppress("DEPRECATION") // getRealSize
    private fun Display.realSize(): Size = Point().also { getRealSize(it) }.let { Size(it.x, it.y) }

    /**
     * A [ContextWrapper] that counts [createWindowContext] calls, including on display contexts.
     */
    private class WindowContextCountingContext(
        base: Context,
        private val counter: IntArray = IntArray(1),
    ) : ContextWrapper(base) {
        val windowContextCount: Int
            get() = counter[0]

        override fun createDisplayContext(display: Display): Context =
            WindowContextCountingContext(super.createDisplayContext(display), counter)

        override fun createWindowContext(type: Int, options: Bundle?): Context {
            counter[0]++
            return super.createWindowContext(type, options)
        }
    }
}
