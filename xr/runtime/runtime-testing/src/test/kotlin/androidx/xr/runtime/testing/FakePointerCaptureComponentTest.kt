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

package androidx.xr.runtime.testing

import androidx.kruth.assertThat
import androidx.xr.runtime.internal.PointerCaptureComponent.PointerCaptureState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePointerCaptureComponentTest {
    private lateinit var underTest: FakePointerCaptureComponent

    @Before
    fun setUp() {
        underTest = FakePointerCaptureComponent()
    }

    @Test
    fun setPointerCaptureState_getPointerCaptureStateReturnsSetPointerCaptureState() {
        // Default value.
        check(underTest.pointerCaptureState == PointerCaptureState.POINTER_CAPTURE_STATE_PAUSED)

        underTest.stateListener.onStateChanged(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)

        assertThat(underTest.pointerCaptureState)
            .isEqualTo(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)

        underTest.stateListener.onStateChanged(PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED)

        assertThat(underTest.pointerCaptureState)
            .isEqualTo(PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED)
    }
}
