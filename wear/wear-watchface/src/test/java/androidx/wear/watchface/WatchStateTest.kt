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

package androidx.wear.watchface

import android.app.NotificationManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(WatchFaceTestRunner::class)
public class WatchStateTest {

    @Test
    public fun asWatchState_interruptionFilter_isPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Initially set to unknown.
        assertThat(watchState.interruptionFilter.hasValue()).isTrue()
        assertThat(watchState.interruptionFilter.value)
            .isEqualTo(NotificationManager.INTERRUPTION_FILTER_UNKNOWN)
        // Value updated.
        mutableWatchState.interruptionFilter.value =
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        assertThat(watchState.interruptionFilter.hasValue()).isTrue()
        assertThat(watchState.interruptionFilter.value)
            .isEqualTo(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    @Test
    public fun asWatchState_isAmbient_isPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Initially not set.
        assertThat(watchState.isAmbient.hasValue()).isFalse()
        // Value updated.
        mutableWatchState.isAmbient.value = true
        assertThat(watchState.isAmbient.hasValue()).isTrue()
        assertThat(watchState.isAmbient.value).isTrue()
    }

    @Test
    public fun asWatchState_isBatteryLowAndNotCharging_isPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Initially not set.
        assertThat(watchState.isBatteryLowAndNotCharging.hasValue()).isFalse()
        // Value updated.
        mutableWatchState.isBatteryLowAndNotCharging.value = true
        assertThat(watchState.isBatteryLowAndNotCharging.hasValue()).isTrue()
        assertThat(watchState.isBatteryLowAndNotCharging.value).isTrue()
    }

    @Test
    public fun asWatchState_isVisible_isPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Initially not set.
        assertThat(watchState.isVisible.hasValue()).isFalse()
        // Value updated.
        mutableWatchState.isVisible.value = true
        assertThat(watchState.isVisible.hasValue()).isTrue()
        assertThat(watchState.isVisible.value).isTrue()
    }

    @Test
    public fun asWatchFace_hasLowBitAmbient_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to false.
        assertThat(watchState.hasLowBitAmbient).isFalse()
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.hasLowBitAmbient = true
        assertThat(watchState.hasLowBitAmbient).isFalse()
        assertThat(mutableWatchState.asWatchState().hasLowBitAmbient).isTrue()
    }

    @Test
    public fun asWatchFace_hasBurnInProtection_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to false.
        assertThat(watchState.hasBurnInProtection).isFalse()
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.hasBurnInProtection = true
        assertThat(watchState.hasBurnInProtection).isFalse()
        assertThat(mutableWatchState.asWatchState().hasBurnInProtection).isTrue()
    }

    @Test
    public fun asWatchFace_analogPreviewReferenceTimeMillis_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to 0.
        assertThat(watchState.analogPreviewReferenceTimeMillis).isEqualTo(0)
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.analogPreviewReferenceTimeMillis = 1000001
        assertThat(watchState.analogPreviewReferenceTimeMillis).isEqualTo(0)
        assertThat(mutableWatchState.asWatchState().analogPreviewReferenceTimeMillis)
            .isEqualTo(1000001)
    }

    @Test
    public fun asWatchFace_digitalPreviewReferenceTimeMillis_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to 0.
        assertThat(watchState.digitalPreviewReferenceTimeMillis).isEqualTo(0)
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.digitalPreviewReferenceTimeMillis = 1000000
        assertThat(watchState.digitalPreviewReferenceTimeMillis).isEqualTo(0)
        assertThat(mutableWatchState.asWatchState().digitalPreviewReferenceTimeMillis)
            .isEqualTo(1000000)
    }

    @Test
    public fun asWatchFace_chinHeight_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to 0.
        assertThat(watchState.chinHeight).isEqualTo(0)
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.chinHeight = 48
        assertThat(watchState.chinHeight).isEqualTo(0)
        assertThat(mutableWatchState.asWatchState().chinHeight).isEqualTo(48)
    }

    @Test
    public fun asWatchFace_isHeadless_isNotPropagated() {
        val mutableWatchState = MutableWatchState()
        var watchState = mutableWatchState.asWatchState()
        // Defaults to false.
        assertThat(watchState.isHeadless).isFalse()
        // Value updated is not propagated unless a new instance is created.
        mutableWatchState.isHeadless = true
        assertThat(watchState.isHeadless).isFalse()
        assertThat(mutableWatchState.asWatchState().isHeadless).isTrue()
    }
}
