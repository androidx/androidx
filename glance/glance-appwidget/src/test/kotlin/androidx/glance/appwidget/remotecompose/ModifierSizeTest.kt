/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.operations.layout.modifiers.DimensionModifierOperation
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.extractModifier
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentWidth
import androidx.glance.unit.Dimension
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ModifierSizeTest : BaseRemoteComposeTest() {

    private fun extractWidthModifier(modifier: GlanceModifier) =
        modifier.extractModifier<WidthModifier>().first!!

    private fun extractHeightModifier(modifier: GlanceModifier) =
        modifier.extractModifier<HeightModifier>().first!!

    @Test
    fun translateModifier_width_fixed() {
        val sizeDp = 100f
        val input: WidthModifier = extractWidthModifier(GlanceModifier.width(sizeDp.dp))
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            widthModifier = input,
            heightModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.WidthModifier
        assertEquals(DimensionModifierOperation.Type.EXACT, mod.type)
        assertEquals(sizeDp, mod.value, .0001f)
    }

    @Test
    fun translateModifier_width_wrap() {
        val input: WidthModifier = extractWidthModifier(GlanceModifier.wrapContentWidth())
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            widthModifier = input,
            heightModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.WidthModifier
        assertEquals(DimensionModifierOperation.Type.WRAP, mod.type)
    }

    @Test
    fun translateModifier_width_fill() {
        val input: WidthModifier = extractWidthModifier(GlanceModifier.fillMaxWidth())
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            widthModifier = input,
            heightModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.WidthModifier
        assertEquals(DimensionModifierOperation.Type.FILL, mod.type)
    }

    @Test
    fun translateModifier_width_expand() {
        val input: WidthModifier = WidthModifier(Dimension.Expand)
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            widthModifier = input,
            heightModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.WidthModifier
        assertEquals(DimensionModifierOperation.Type.WEIGHT, mod.type)
    }

    @Test
    fun translateModifier_height_fixed() {
        val sizeDp = 100f
        val input: HeightModifier = extractHeightModifier(GlanceModifier.height(sizeDp.dp))
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            heightModifier = input,
            widthModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.HeightModifier
        assertEquals(DimensionModifierOperation.Type.EXACT, mod.type)
        assertEquals(sizeDp, mod.value, .0001f)
    }

    @Test
    fun translateModifier_height_wrap() {
        val input: HeightModifier = extractHeightModifier(GlanceModifier.wrapContentHeight())
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            heightModifier = input,
            widthModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.HeightModifier
        assertEquals(DimensionModifierOperation.Type.WRAP, mod.type)
    }

    @Test
    fun translateModifier_height_fill() {
        val input: HeightModifier = extractHeightModifier(GlanceModifier.fillMaxHeight())
        val output = RecordingModifier()

        applySizeModifiers(
            context = context,
            heightModifier = input,
            widthModifier = null,
            outputModifier = output,
        )

        val mod = output.list[0] as androidx.compose.remote.creation.modifiers.HeightModifier
        assertEquals(DimensionModifierOperation.Type.FILL, mod.type)
    }
}
