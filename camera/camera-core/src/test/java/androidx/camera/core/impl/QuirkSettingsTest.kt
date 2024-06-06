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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [QuirkSettings]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class QuirkSettingsTest {

    private val anyQuirk = AnyQuirk::class.java
    private val quirk1 = Quirk1::class.java
    private val quirk2 = Quirk2::class.java
    private val quirk3 = Quirk3::class.java
    private val quirk4 = Quirk4::class.java

    @Test
    fun withDefaultBehavior_enableAllQuirks() {
        val settings = QuirkSettings.withDefaultBehavior()

        assertThat(settings.isEnabledWhenDeviceHasQuirk).isTrue()
        assertThat(settings.forceEnabledQuirks).isEmpty()
        assertThat(settings.forceDisabledQuirks).isEmpty()
        assertThat(settings.shouldEnableQuirk(anyQuirk, true)).isTrue()
        assertThat(settings.shouldEnableQuirk(anyQuirk, false)).isFalse()
    }

    @Test
    fun withAllQuirksDisabled_disablesAllQuirks() {
        val settings = QuirkSettings.withAllQuirksDisabled()

        assertThat(settings.isEnabledWhenDeviceHasQuirk).isFalse()
        assertThat(settings.forceEnabledQuirks).isEmpty()
        assertThat(settings.forceDisabledQuirks).isEmpty()
        assertThat(settings.shouldEnableQuirk(anyQuirk, true)).isFalse()
        assertThat(settings.shouldEnableQuirk(anyQuirk, false)).isFalse()
    }

    @Test
    fun withQuirksForceEnabled_enablesSpecificQuirks() {
        val quirks = setOf(quirk1)
        val settings = QuirkSettings.withQuirksForceEnabled(quirks)

        assertThat(settings.isEnabledWhenDeviceHasQuirk).isTrue()
        assertThat(settings.forceEnabledQuirks).containsExactlyElementsIn(quirks)
        assertThat(settings.forceDisabledQuirks).isEmpty()
        assertThat(settings.shouldEnableQuirk(quirk1, true)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk1, false)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk2, true)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk2, false)).isFalse()
    }

    @Test
    fun withQuirksForceDisabled_disablesSpecificQuirks() {
        val quirks = setOf(quirk1)
        val settings = QuirkSettings.withQuirksForceDisabled(quirks)

        assertThat(settings.isEnabledWhenDeviceHasQuirk).isTrue()
        assertThat(settings.forceEnabledQuirks).isEmpty()
        assertThat(settings.forceDisabledQuirks).containsExactlyElementsIn(quirks)
        assertThat(settings.shouldEnableQuirk(quirk1, true)).isFalse()
        assertThat(settings.shouldEnableQuirk(quirk1, false)).isFalse()
        assertThat(settings.shouldEnableQuirk(quirk2, true)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk2, false)).isFalse()
    }

    @Test
    fun builder_createsCorrectSettings() {
        val quirksToEnable = setOf(quirk1, quirk2)
        val quirksToDisable = setOf(quirk3)
        val settings =
            QuirkSettings.Builder()
                .setEnabledWhenDeviceHasQuirk(false)
                .forceEnableQuirks(quirksToEnable)
                .forceDisableQuirks(quirksToDisable)
                .build()

        assertThat(settings.isEnabledWhenDeviceHasQuirk).isFalse()
        assertThat(settings.forceEnabledQuirks).containsExactlyElementsIn(quirksToEnable)
        assertThat(settings.forceDisabledQuirks).containsExactlyElementsIn(quirksToDisable)
        assertThat(settings.shouldEnableQuirk(quirk1, true)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk2, false)).isTrue()
        assertThat(settings.shouldEnableQuirk(quirk3, true)).isFalse()
        assertThat(settings.shouldEnableQuirk(quirk4, true)).isFalse()
    }

    @Test
    fun equals_hashCode() {
        val settings1 = QuirkSettings.withQuirksForceEnabled(setOf(anyQuirk))
        val settings2 = QuirkSettings.withQuirksForceEnabled(setOf(anyQuirk)) // Same as settings1
        val settings3 =
            QuirkSettings.withQuirksForceDisabled(setOf(anyQuirk)) // Different from settings1

        // Equals
        assertThat(settings1).isEqualTo(settings2)
        assertThat(settings1).isNotEqualTo(settings3)

        // HashCode
        assertThat(settings1.hashCode()).isEqualTo(settings2.hashCode())
    }

    private class AnyQuirk : Quirk

    private class Quirk1 : Quirk

    private class Quirk2 : Quirk

    private class Quirk3 : Quirk

    private class Quirk4 : Quirk
}
