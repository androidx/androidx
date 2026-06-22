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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.PasswordInputTransformation
import androidx.compose.foundation.text.SplitVisibilitySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalFoundationApi::class)
class PasswordInputTransformationTest {

    private fun createTransformation(
        touch: Boolean,
        physical: Boolean,
        mode: TextObfuscationMode = TextObfuscationMode.System,
        scheduleHide: () -> Unit = {},
    ): PasswordInputTransformation {
        val settings = SplitVisibilitySettings(touch = touch, physical = physical)
        return PasswordInputTransformation(
            scheduleHide = scheduleHide,
            textObfuscationMode = { mode },
            platformAllowsReveal = { settings },
        )
    }

    @Test
    fun touchSource_respectsTouchSetting_true() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = true,
                physical = false,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = false)

        with(transformation) { buffer.transformInput() }

        assertEquals(4, transformation.revealCodepointIndex)
        assertTrue(hideScheduled)
    }

    @Test
    fun touchSource_respectsTouchSetting_false() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = false,
                physical = true,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = false)

        with(transformation) { buffer.transformInput() }

        assertEquals(-1, transformation.revealCodepointIndex)
        assertFalse(hideScheduled)
    }

    @Test
    fun hardwareSource_respectsPhysicalSetting_true() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = false,
                physical = true,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = true)

        with(transformation) { buffer.transformInput() }

        assertEquals(4, transformation.revealCodepointIndex)
        assertTrue(hideScheduled)
    }

    @Test
    fun hardwareSource_respectsPhysicalSetting_false() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = true,
                physical = false,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = true)

        with(transformation) { buffer.transformInput() }

        assertEquals(-1, transformation.revealCodepointIndex)
        assertFalse(hideScheduled)
    }

    @Test
    fun revealLastTypedMode_alwaysReveals() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = false,
                physical = false,
                mode = TextObfuscationMode.RevealLastTyped,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = true)

        with(transformation) { buffer.transformInput() }

        assertEquals(4, transformation.revealCodepointIndex)
        assertTrue(hideScheduled)
    }

    @Test
    fun hiddenMode_neverReveals() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = true,
                physical = true,
                mode = TextObfuscationMode.Hidden,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = false)

        with(transformation) { buffer.transformInput() }

        assertEquals(-1, transformation.revealCodepointIndex)
        assertFalse(hideScheduled)
    }

    @Test
    fun visibleMode_neverReveals() {
        var hideScheduled = false
        val transformation =
            createTransformation(
                touch = true,
                physical = true,
                mode = TextObfuscationMode.Visible,
                scheduleHide = { hideScheduled = true },
            )

        val buffer = TextFieldBuffer(TextFieldCharSequence("****"))
        buffer.replace(4, 4, "d", isFromHardwareSource = false)

        with(transformation) { buffer.transformInput() }

        // While the UI layer bypasses the mask for Visible, the transformation
        // safely defaults to a no-op state (revealCodepointIndex = -1) and does not trigger
        // scheduleHide.
        assertEquals(-1, transformation.revealCodepointIndex)
        assertFalse(hideScheduled)
    }
}
