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

package androidx.wear.watchface.client

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod

// Without this we get test failures with an error:
// "failed to access class kotlin.jvm.internal.DefaultConstructorMarker".
public class EditorTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(
            super.createClassLoaderConfig(method)
        )
            .doNotInstrumentPackage("androidx.wear.watchface.client")
            .build()
}

@RunWith(EditorTestRunner::class)
public class WatchFaceIdTest {
    @Test
    public fun watchFaceId_equals() {
        val a1 = WatchFaceId("A")
        val a2 = WatchFaceId("A")
        val b1 = WatchFaceId("B")

        assertThat(a1).isEqualTo(a1)
        assertThat(a1).isEqualTo(a2)
        assertThat(a1).isNotEqualTo(b1)
        assertThat(a1).isNotEqualTo(false)
    }

    @Test
    public fun watchFaceId_hashCode() {
        val a1 = WatchFaceId("A")
        val a2 = WatchFaceId("A")
        val b1 = WatchFaceId("B")

        assertThat(a1.hashCode()).isEqualTo(a2.hashCode())
        assertThat(a1.hashCode()).isNotEqualTo(b1.hashCode())
    }
}