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

package androidx.tracing.perfetto.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.tracing.perfetto.Tracing
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TracingTest {
    @Test
    fun test_endToEnd_binaryDependenciesPresent() {
        assertThat(Tracing.isEnabled).isEqualTo(false)

        // Note: no path to binary dependencies provided, so we are testing the case where the app
        // directly depends on :tracing:tracing-perfetto-binary
        Tracing.enable()
        assertThat(Tracing.isEnabled).isEqualTo(true)

        Tracing.enable()
        assertThat(Tracing.isEnabled).isEqualTo(true)

        Tracing.traceEventStart(123, "foo")
        Tracing.traceEventStart(321, "bar")
        Tracing.traceEventEnd()
        Tracing.traceEventEnd()

        Tracing.flushEvents()

        // TODO(214562374): verify the content by getting it back from Perfetto
    }
}