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

package androidx.xr.arcore.testing.internal

import androidx.kruth.assertThat
import androidx.xr.arcore.runtime.TrackingState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeQrCodeTest {

    @Test
    fun constructor_withDefaultArguments_isTracked() {
        val underTest = FakeRuntimeQrCode()

        assertThat(underTest.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun constructor_withDefaultArguments_hasEmptyData() {
        val underTest = FakeRuntimeQrCode()

        assertThat(underTest.data).isEmpty()
    }
}
