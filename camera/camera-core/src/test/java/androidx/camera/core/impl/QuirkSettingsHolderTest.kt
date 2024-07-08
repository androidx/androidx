/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core.impl

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.core.util.Consumer
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [QuirkSettingsHolder]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QuirkSettingsHolderTest {

    private val quirkSettingsHolder = QuirkSettingsHolder.instance()

    @After
    fun tearDown() {
        quirkSettingsHolder.reset() // Reset after each test
        shadowOf(getMainLooper()).idle() // Ensure any pending updates are processed after reset.
    }

    @Test
    fun getInstance_returnsSingleton() {
        val holder1 = quirkSettingsHolder
        val holder2 = quirkSettingsHolder
        assertThat(holder1).isEqualTo(holder2)
    }

    @Test
    fun get_initiallyReturnsDefaultSettings() {
        val settings = quirkSettingsHolder.get()
        assertThat(settings).isEqualTo(QuirkSettingsHolder.DEFAULT)
        assertThat(settings).isEqualTo(QuirkSettings.withDefaultBehavior())
    }

    @Test
    fun set_updatesSettings() {
        val newSettings = QuirkSettings.withAllQuirksDisabled()
        quirkSettingsHolder.set(newSettings)
        assertThat(quirkSettingsHolder.get()).isEqualTo(newSettings)
    }

    @Test
    fun observe_withDirectExecutor_notifiesImmediately() {
        var settings: QuirkSettings? = null
        val listener = Consumer<QuirkSettings> { settings = it }
        quirkSettingsHolder.observe(directExecutor(), listener)

        assertThat(settings).isEqualTo(QuirkSettingsHolder.DEFAULT)
    }

    @Test
    fun observe_notifiesOnSet() {
        var settings: QuirkSettings? = null
        var updateCount = 0
        val listener =
            Consumer<QuirkSettings> {
                updateCount++
                settings = it
            }
        quirkSettingsHolder.observe(mainThreadExecutor(), listener)
        shadowOf(getMainLooper()).idle() // Ensure update is processed

        assertThat(updateCount).isEqualTo(1)
        assertThat(settings).isEqualTo(QuirkSettingsHolder.DEFAULT)

        val newSettings = QuirkSettings.withAllQuirksDisabled()
        quirkSettingsHolder.set(newSettings)
        shadowOf(getMainLooper()).idle() // Ensure update is processed

        assertThat(updateCount).isEqualTo(2)
        assertThat(settings).isEqualTo(newSettings)
    }
}
