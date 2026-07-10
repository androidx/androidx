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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.creation.modifiers.RecordingModifier
import org.junit.Assert.assertEquals
import org.junit.Test

class ModifierTest {

    @Test
    fun testModifierConcatenation() {
        val modifier = Modifier.padding(10f).size(100f, 200f).background(0xFFFF0000.toInt())

        var count = 0
        modifier.foldIn(Unit) { _, _ -> count++ }
        assertEquals(3, count)
    }

    @Test
    fun testModifierApplyTo() {
        // We use a custom RecordingModifier to verify calls
        val recordingModifier =
            object : RecordingModifier() {
                var paddingCalled = false
                var sizeCalled = false
                var backgroundCalled = false

                override fun padding(
                    left: Float,
                    top: Float,
                    right: Float,
                    bottom: Float,
                ): RecordingModifier {
                    paddingCalled = true
                    return this
                }

                override fun width(width: Float): RecordingModifier {
                    sizeCalled = true
                    return this
                }

                override fun height(height: Float): RecordingModifier {
                    sizeCalled = true
                    return this
                }

                override fun background(color: Int): RecordingModifier {
                    backgroundCalled = true
                    return this
                }
            }

        val modifier = Modifier.padding(10f).size(100f).background(0xFF00FF00.toInt())

        modifier.foldIn(Unit) { _, element -> element.applyTo(recordingModifier) }

        assertEquals(true, recordingModifier.paddingCalled)
        assertEquals(true, recordingModifier.sizeCalled)
        assertEquals(true, recordingModifier.backgroundCalled)
    }

    @Test
    fun testUnitSafeModifiers() {
        val modifier = Modifier.padding(10.rdp).size(100.rdp).padding(5.rpx)

        // Verify foldIn works with unit-safe versions
        var count = 0
        modifier.foldIn(Unit) { _, _ -> count++ }
        assertEquals(3, count)
    }
}
