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

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputEventTest {

    @Test
    fun inputEventSource_toString() {
        assertThat(InputEvent.Source.UNKNOWN.toString()).isEqualTo("UNKNOWN")
        assertThat(InputEvent.Source.HEAD.toString()).isEqualTo("HEAD")
        assertThat(InputEvent.Source.CONTROLLER.toString()).isEqualTo("CONTROLLER")
        assertThat(InputEvent.Source.HANDS.toString()).isEqualTo("HANDS")
        assertThat(InputEvent.Source.MOUSE.toString()).isEqualTo("MOUSE")
        assertThat(InputEvent.Source.GAZE_AND_GESTURE.toString()).isEqualTo("GAZE_AND_GESTURE")
    }

    @Test
    fun inputEventPointer_toString() {
        assertThat(InputEvent.Pointer.DEFAULT.toString()).isEqualTo("DEFAULT")
        assertThat(InputEvent.Pointer.LEFT.toString()).isEqualTo("LEFT")
        assertThat(InputEvent.Pointer.RIGHT.toString()).isEqualTo("RIGHT")
    }

    @Test
    fun inputEventAction_toString() {
        assertThat(InputEvent.Action.DOWN.toString()).isEqualTo("DOWN")
        assertThat(InputEvent.Action.UP.toString()).isEqualTo("UP")
        assertThat(InputEvent.Action.MOVE.toString()).isEqualTo("MOVE")
        assertThat(InputEvent.Action.CANCEL.toString()).isEqualTo("CANCEL")
        assertThat(InputEvent.Action.HOVER_MOVE.toString()).isEqualTo("HOVER_MOVE")
        assertThat(InputEvent.Action.HOVER_ENTER.toString()).isEqualTo("HOVER_ENTER")
        assertThat(InputEvent.Action.HOVER_EXIT.toString()).isEqualTo("HOVER_EXIT")
    }
}
