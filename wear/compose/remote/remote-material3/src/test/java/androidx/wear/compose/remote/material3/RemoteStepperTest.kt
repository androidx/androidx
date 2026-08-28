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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteStepperTest {

    @Test
    fun defaultValues() {
        assertEquals(24f, RemoteStepperDefaults.IconSize.value.constantValue, 0.001f)
        assertEquals(60f, RemoteStepperDefaults.ButtonWidth.value.constantValue, 0.001f)
        assertEquals(48f, RemoteStepperDefaults.ButtonHeight.value.constantValue, 0.001f)
        assertEquals(8f, RemoteStepperDefaults.VerticalSpacing.value.constantValue, 0.001f)
        assertEquals(0.35f, RemoteStepperDefaults.ButtonWeight, 0.001f)
        assertEquals(0.3f, RemoteStepperDefaults.ContentWeight, 0.001f)
        assertEquals(RemoteCircleShape, RemoteStepperDefaults.buttonShape)
        assertEquals(RemoteCircleShape, RemoteStepperDefaults.StepperButtonShape)
    }

    @Test
    fun stepperColors_copy() {
        val original =
            RemoteStepperColors(
                contentColor = RemoteColor(Color.White),
                buttonContainerColor = RemoteColor(Color.Red),
                buttonIconColor = RemoteColor(Color.Blue),
                disabledContentColor = RemoteColor(Color.Gray),
                disabledButtonContainerColor = RemoteColor(Color.DarkGray),
                disabledButtonIconColor = RemoteColor(Color.LightGray),
            )

        val newContentColor = RemoteColor(Color.Yellow)
        val copy = original.copy(contentColor = newContentColor)

        assertEquals(newContentColor, copy.contentColor)
        assertEquals(original.buttonContainerColor, copy.buttonContainerColor)
        assertEquals(original.buttonIconColor, copy.buttonIconColor)
        assertEquals(original.disabledContentColor, copy.disabledContentColor)
        assertEquals(original.disabledButtonContainerColor, copy.disabledButtonContainerColor)
        assertEquals(original.disabledButtonIconColor, copy.disabledButtonIconColor)
    }

    @Test
    fun stepperColors_equalsAndHashCode() {
        val colors1 =
            RemoteStepperColors(
                contentColor = RemoteColor(Color.White),
                buttonContainerColor = RemoteColor(Color.Red),
                buttonIconColor = RemoteColor(Color.Blue),
                disabledContentColor = RemoteColor(Color.Gray),
                disabledButtonContainerColor = RemoteColor(Color.DarkGray),
                disabledButtonIconColor = RemoteColor(Color.LightGray),
            )
        val colors2 = colors1.copy()
        val colors3 = colors1.copy(contentColor = RemoteColor(Color.Black))

        assertEquals(colors1, colors2)
        assertEquals(colors1.hashCode(), colors2.hashCode())
        assertNotEquals(colors1, colors3)
    }
}
