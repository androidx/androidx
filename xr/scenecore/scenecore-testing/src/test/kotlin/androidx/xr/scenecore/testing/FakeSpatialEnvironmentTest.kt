/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.filters.SdkSuppress
import androidx.xr.scenecore.runtime.SpatialEnvironment
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeSpatialEnvironmentTest {

    lateinit var underTest: FakeSpatialEnvironment

    @Before
    fun setUp() {
        underTest = FakeSpatialEnvironment()
        underTest.onRenderingFeatureReady(FakeSpatialEnvironmentFeature())

        check(underTest.preferredSpatialEnvironment == null)
        check(
            underTest.preferredPassthroughOpacity ==
                SpatialEnvironment.Companion.NO_PASSTHROUGH_OPACITY_PREFERENCE
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun getCurrentPassthroughOpacity_fireDefaultListener_returnsCorrectValue() {
        check(underTest.currentPassthroughOpacity == 0.0f)

        underTest.passthroughOpacityChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(1.0f) }
        }

        assertThat(underTest.currentPassthroughOpacity).isEqualTo(1.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun addOnPassthroughOpacityChangedListener_fireCustomListener_returnsCorrectValue() {
        val listener = TestFloatConsumer()

        check(!listener.wasCalled)
        check(listener.receivedValue == null)

        underTest.addOnPassthroughOpacityChangedListener({ command -> command.run() }, listener)
        underTest.passthroughOpacityChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(0.5f) }
        }

        assertThat(listener.wasCalled).isTrue()
        assertThat(listener.receivedValue).isEqualTo(0.5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun removeOnPassthroughOpacityChangedListener_fireDefaultListener_returnsCorrectValue() {
        check(underTest.currentPassthroughOpacity == 0.0f)

        val listener = TestFloatConsumer()

        check(!listener.wasCalled)
        check(listener.receivedValue == null)

        underTest.addOnPassthroughOpacityChangedListener({ command -> command.run() }, listener)
        underTest.removeOnPassthroughOpacityChangedListener(listener)
        underTest.passthroughOpacityChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(0.5f) }
        }

        assertThat(listener.wasCalled).isFalse()
        assertThat(listener.receivedValue).isNull()
        // Default listener is invoked.
        assertThat(underTest.currentPassthroughOpacity).isEqualTo(0.5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun getIsPreferredSpatialEnvironmentActive_fireDefaultListener_returnsCorrectValue() {
        check(!underTest.isPreferredSpatialEnvironmentActive)

        underTest.spatialEnvironmentChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(true) }
        }

        assertThat(underTest.isPreferredSpatialEnvironmentActive).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun addOnSpatialEnvironmentChangedListener_fireCustomListener_returnsCorrectValue() {
        val listener = TestBooleanConsumer()

        check(!listener.wasCalled)
        check(listener.receivedValue == null)

        underTest.addOnSpatialEnvironmentChangedListener({ command -> command.run() }, listener)
        underTest.spatialEnvironmentChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(true) }
        }

        assertThat(listener.wasCalled).isTrue()
        assertThat(listener.receivedValue).isTrue()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun removeOnSpatialEnvironmentChangedListener_fireDefaultListener_returnsCorrectValue() {
        check(!underTest.isPreferredSpatialEnvironmentActive)

        val listener = TestBooleanConsumer()

        check(!listener.wasCalled)
        check(listener.receivedValue == null)

        underTest.addOnSpatialEnvironmentChangedListener({ command -> command.run() }, listener)
        underTest.removeOnSpatialEnvironmentChangedListener(listener)
        underTest.spatialEnvironmentChangedListenerMap.forEach { (executor, consumer) ->
            executor.execute { consumer.accept(true) }
        }

        assertThat(listener.wasCalled).isFalse()
        assertThat(listener.receivedValue).isNull()
        // Default listener is invoked
        assertThat(underTest.isPreferredSpatialEnvironmentActive).isTrue()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class TestFloatConsumer : Consumer<Float> {
        var wasCalled = false
            private set

        var receivedValue: Float? = null
            private set

        override fun accept(value: Float) {
            wasCalled = true
            receivedValue = value
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class TestBooleanConsumer : Consumer<Boolean> {
        var wasCalled = false
            private set

        var receivedValue: Boolean? = null
            private set

        override fun accept(value: Boolean) {
            wasCalled = true
            receivedValue = value
        }
    }
}
