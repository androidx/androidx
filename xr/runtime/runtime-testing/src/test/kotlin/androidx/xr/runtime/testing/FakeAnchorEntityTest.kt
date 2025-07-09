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

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.internal.AnchorEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class FakeAnchorEntityTest {
    private lateinit var underTest: FakeAnchorEntity

    @Before
    fun setUp() {
        underTest = FakeAnchorEntity()
    }

    @Test
    fun onStateChanged_returnNewState() {
        check(underTest.state == AnchorEntity.State.UNANCHORED)

        underTest.onStateChanged(AnchorEntity.State.ANCHORED)

        assertThat(underTest.state).isEqualTo(AnchorEntity.State.ANCHORED)

        underTest.onStateChanged(AnchorEntity.State.TIMED_OUT)

        assertThat(underTest.state).isEqualTo(AnchorEntity.State.TIMED_OUT)

        underTest.onStateChanged(AnchorEntity.State.ERROR)

        assertThat(underTest.state).isEqualTo(AnchorEntity.State.ERROR)
    }

    @Test
    fun setOnStateChangedListener_callLastListenersOnAnchorStateUpdated() {
        var state = AnchorEntity.State.UNANCHORED
        val listener = AnchorEntity.OnStateChangedListener { newState -> state = newState }

        underTest.setOnStateChangedListener(listener)
        underTest.onStateChanged(AnchorEntity.State.ANCHORED)

        assertThat(state).isEqualTo(AnchorEntity.State.ANCHORED)

        underTest.onStateChanged(AnchorEntity.State.TIMED_OUT)

        assertThat(state).isEqualTo(AnchorEntity.State.TIMED_OUT)

        underTest.onStateChanged(AnchorEntity.State.ERROR)

        assertThat(state).isEqualTo(AnchorEntity.State.ERROR)
    }
}
