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

package androidx.ui.test.inputdispatcher

import androidx.compose.ui.geometry.Offset
import androidx.ui.test.InputDispatcher
import androidx.ui.test.android.AndroidInputDispatcher
import com.google.common.truth.Truth.assertThat

internal fun AndroidInputDispatcher.sendDownAndCheck(pointerId: Int, position: Offset) {
    sendDown(pointerId, position)
    assertThat(getCurrentPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.movePointerAndCheck(pointerId: Int, position: Offset) {
    movePointer(pointerId, position)
    assertThat(getCurrentPosition(pointerId)).isEqualTo(position)
}

internal fun AndroidInputDispatcher.sendUpAndCheck(pointerId: Int, delay: Long? = null) {
    if (delay != null) {
        sendUp(pointerId, delay)
    } else {
        sendUp(pointerId)
    }
    assertThat(getCurrentPosition(pointerId)).isNull()
}

internal fun AndroidInputDispatcher.sendCancelAndCheck(delay: Long? = null) {
    if (delay != null) {
        sendCancel(delay)
    } else {
        sendCancel()
    }
    verifyNoGestureInProgress()
}

internal fun InputDispatcher.verifyNoGestureInProgress() {
    assertThat(isGestureInProgress).isFalse()
}
