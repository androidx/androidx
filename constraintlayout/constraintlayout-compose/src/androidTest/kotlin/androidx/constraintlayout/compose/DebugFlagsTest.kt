/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.constraintlayout.compose

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

@OptIn(ExperimentalMotionApi::class)
class DebugFlagsTest {
    @Test
    fun testFlags() {
        assertEquals(
            DebugFlags.None,
            DebugFlags()
        )
        assertEquals(
            DebugFlags.None,
            DebugFlags(
                showBounds = false,
                showPaths = false,
                showKeyPositions = false
            )
        )

        // Not equals because All includes potential future flags
        assertNotEquals(
            DebugFlags.All,
            DebugFlags(
                showBounds = true,
                showPaths = true,
                showKeyPositions = true
            )
        )

        var flags = DebugFlags.All
        assertTrue(flags.showBounds)
        assertTrue(flags.showPaths)
        assertTrue(flags.showKeyPositions)

        flags = DebugFlags.None
        assertFalse(flags.showBounds)
        assertFalse(flags.showPaths)
        assertFalse(flags.showKeyPositions)

        flags = DebugFlags(showBounds = true, showPaths = true, showKeyPositions = true)
        assertTrue(flags.showBounds)
        assertTrue(flags.showPaths)
        assertTrue(flags.showKeyPositions)

        flags = DebugFlags(showBounds = false, showPaths = false, showKeyPositions = false)
        assertFalse(flags.showBounds)
        assertFalse(flags.showPaths)
        assertFalse(flags.showKeyPositions)

        flags = DebugFlags(showBounds = true)
        assertTrue(flags.showBounds)
        assertFalse(flags.showPaths)
        assertFalse(flags.showKeyPositions)

        flags = DebugFlags(showPaths = true)
        assertFalse(flags.showBounds)
        assertTrue(flags.showPaths)
        assertFalse(flags.showKeyPositions)

        flags = DebugFlags(showKeyPositions = true)
        assertFalse(flags.showBounds)
        assertFalse(flags.showPaths)
        assertTrue(flags.showKeyPositions)
    }

    @Test
    fun testToString() {
        assertEquals(
            "DebugFlags(showBounds = true, showPaths = true, showKeyPositions = true)",
            DebugFlags.All.toString()
        )
        assertEquals(
            "DebugFlags(showBounds = false, showPaths = false, showKeyPositions = false)",
            DebugFlags.None.toString()
        )

        assertEquals(
            "DebugFlags(showBounds = false, showPaths = false, showKeyPositions = false)",
            DebugFlags().toString()
        )
        assertEquals(
            "DebugFlags(showBounds = true, showPaths = true, showKeyPositions = true)",
            DebugFlags(showBounds = true, showPaths = true, showKeyPositions = true).toString()
        )
        assertEquals(
            "DebugFlags(showBounds = true, showPaths = false, showKeyPositions = false)",
            DebugFlags(showBounds = true, showPaths = false, showKeyPositions = false).toString()
        )
        assertEquals(
            "DebugFlags(showBounds = false, showPaths = true, showKeyPositions = false)",
            DebugFlags(showBounds = false, showPaths = true, showKeyPositions = false).toString()
        )
        assertEquals(
            "DebugFlags(showBounds = false, showPaths = false, showKeyPositions = true)",
            DebugFlags(showBounds = false, showPaths = false, showKeyPositions = true).toString()
        )
    }
}