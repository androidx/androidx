/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ForwardingLiveData]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ForwardingLiveDataTest {
    private val forwardingLiveData = ForwardingLiveData<Any>()

    @Test
    public fun sourceSet_valueCanBeRetrieved() {
        // Arrange.
        assertThat(forwardingLiveData.value).isNull()
        val sourceLiveData = MutableLiveData(Any())

        // Act.
        forwardingLiveData.setSource(sourceLiveData)

        // Assert.
        assertThat(forwardingLiveData.value).isNotNull()
    }

    @Test
    public fun sourceUpdated_canGetNewValue() {
        // Arrange.
        val sourceLiveData = MutableLiveData(Any())
        forwardingLiveData.setSource(sourceLiveData)

        // Act.
        val newValue = Any()
        sourceLiveData.postValue(newValue)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(forwardingLiveData.value).isEqualTo(newValue)
    }

    @Test
    public fun sourceUpdated_valueIsObserved() {
        // Arrange.
        val sourceLiveData = MutableLiveData(Any())
        forwardingLiveData.setSource(sourceLiveData)

        var observedValue: Any? = null
        forwardingLiveData.observeForever {
            observedValue = it
        }

        // Act.
        val updatedValue = Any()
        sourceLiveData.postValue(updatedValue)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(observedValue).isEqualTo(updatedValue)
    }
}
