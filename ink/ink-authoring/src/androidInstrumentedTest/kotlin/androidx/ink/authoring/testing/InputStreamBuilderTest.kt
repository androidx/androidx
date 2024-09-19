/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.testing

import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class InputStreamBuilderTest {

    @Test
    fun fingerLine_eventsHaveToolTypeFinger() {
        InputStreamBuilder.fingerLine(0F, 0F, 100F, 200F).runInputStreamWith { event ->
            assertThat(event.pointerCount).isEqualTo(1)
            assertThat(event.getToolType(0)).isEqualTo(MotionEvent.TOOL_TYPE_FINGER)
        }
    }

    @Test
    fun mouseLine_eventsHaveToolTypeMouse() {
        InputStreamBuilder.mouseLine(MotionEvent.BUTTON_PRIMARY, 0F, 0F, 100F, 200F)
            .runInputStreamWith { event ->
                assertThat(event.pointerCount).isEqualTo(1)
                assertThat(event.getToolType(0)).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)
            }
    }

    @Test
    fun mouseLine_eventsHaveButtonState() {
        val buttons =
            arrayOf(
                MotionEvent.BUTTON_PRIMARY,
                MotionEvent.BUTTON_SECONDARY,
                MotionEvent.BUTTON_TERTIARY
            )
        for (button in buttons) {
            val builder = InputStreamBuilder.mouseLine(button, 0F, 0F, 100F, 200F)
            builder.runWithDownEvent { event -> assertThat(event.buttonState).isEqualTo(button) }
            builder.runWithMoveEvent { event -> assertThat(event.buttonState).isEqualTo(button) }
            // The button should no longer be held down on the up event.
            builder.runWithUpEvent { event -> assertThat(event.buttonState).isEqualTo(0) }
        }
    }

    @Test
    fun stylusLine_eventsHaveToolTypeStylus() {
        InputStreamBuilder.stylusLine(0F, 0F, 100F, 200F).runInputStreamWith { event ->
            assertThat(event.pointerCount).isEqualTo(1)
            assertThat(event.getToolType(0)).isEqualTo(MotionEvent.TOOL_TYPE_STYLUS)
        }
    }

    @Test
    fun stylusLine_eventsHaveCorrespondingActions() {
        val builder = InputStreamBuilder.stylusLine(0F, 0F, 100F, 200F)
        builder.runWithDownEvent { event ->
            assertThat(event.actionMasked).isEqualTo(MotionEvent.ACTION_DOWN)
        }
        builder.runWithMoveEvent { event ->
            assertThat(event.actionMasked).isEqualTo(MotionEvent.ACTION_MOVE)
        }
        builder.runWithUpEvent { event ->
            assertThat(event.actionMasked).isEqualTo(MotionEvent.ACTION_UP)
        }
    }

    @Test
    fun stylusLine_pointerPositionsFollowSegment() {
        val builder = InputStreamBuilder.stylusLine(0F, 0F, 100F, 200F)
        builder.runWithDownEvent { event ->
            assertThat(event.getX()).isEqualTo(0F)
            assertThat(event.getY()).isEqualTo(0F)
        }
        builder.runWithMoveEvent { event ->
            assertThat(event.getX()).isEqualTo(50F)
            assertThat(event.getY()).isEqualTo(100F)
        }
        builder.runWithUpEvent { event ->
            assertThat(event.getX()).isEqualTo(100F)
            assertThat(event.getY()).isEqualTo(200F)
        }
    }

    @Test
    fun scrollWheel_hasMouseToolTypeAndSource() {
        InputStreamBuilder.scrollWheel(
            1F,
            -1F,
            { event ->
                assertThat(event.pointerCount).isEqualTo(1)
                assertThat(event.getToolType(0)).isEqualTo(MotionEvent.TOOL_TYPE_MOUSE)
                assertThat(event.isFromSource(InputDevice.SOURCE_MOUSE)).isTrue()
            },
        )
    }

    @Test
    fun scrollWheel_hasSpecifiedScrollAxes() {
        InputStreamBuilder.scrollWheel(
            0.75F,
            -0.5F,
            { event ->
                assertThat(event.getAxisValue(MotionEvent.AXIS_HSCROLL)).isEqualTo(0.75F)
                assertThat(event.getAxisValue(MotionEvent.AXIS_VSCROLL)).isEqualTo(-0.5F)
            },
        )
    }
}
