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

package androidx.compose.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class IsIndirectPointerGestureInProgressTest : InputDispatcherTest() {
    companion object {
        private val anyPosition = Offset.Zero
    }

    @Test
    fun downUp() {
        assertThat(subject.isIndirectPointerGestureInProgress).isFalse()
        subject.enqueueIndirectPointerDown(
            1,
            anyPosition,
            IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        assertThat(subject.isIndirectPointerGestureInProgress).isTrue()
        subject.enqueueIndirectPointerUp(1)
        assertThat(subject.isIndirectPointerGestureInProgress).isFalse()
    }

    @Test
    fun downCancel() {
        assertThat(subject.isIndirectPointerGestureInProgress).isFalse()
        subject.enqueueIndirectPointerDown(
            1,
            anyPosition,
            IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        assertThat(subject.isIndirectPointerGestureInProgress).isTrue()
        subject.enqueueIndirectPointerCancel()
        assertThat(subject.isIndirectPointerGestureInProgress).isFalse()
    }
}
