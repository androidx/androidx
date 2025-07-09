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
import androidx.xr.runtime.internal.PointerCaptureComponent.StateListener
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePointerCaptureComponentTest {
    private lateinit var underTest: FakePointerCaptureComponent

    @Test
    fun onStateChanged_withExecutor_returnsSetPointerCaptureState() {
        // Arrange
        var state = PointerCaptureState.POINTER_CAPTURE_STATE_STOPPED
        val stateListener: StateListener =
            object : StateListener {
                override fun onStateChanged(@PointerCaptureState newState: Int) {
                    state = newState
                }
            }
        val executor = Executor { commands -> commands.run() }
        underTest = FakePointerCaptureComponent(executor, stateListener)

        // Act
        underTest.onStateChanged(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)

        // Assert
        assertThat(state).isEqualTo(PointerCaptureState.POINTER_CAPTURE_STATE_ACTIVE)
    }
}
