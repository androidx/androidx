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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.ui.graphics.Color
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class RemoteButtonColorsTest {

    @Test
    fun remoteButtonColors_copies() {
        val original =
            RemoteButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                secondaryContentColor = RemoteColor(Color.Green),
                iconColor = RemoteColor(Color.Yellow),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
                disabledSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledIconColor = RemoteColor(Color.LightGray),
            )

        val copy = original.copy(containerColor = RemoteColor(Color.Magenta))

        assertEquals(RemoteColor(Color.Magenta).constantValue, copy.containerColor.constantValue)
        assertEquals(original.contentColor.constantValue, copy.contentColor.constantValue)
    }

    @Test
    fun remoteIconButtonColors_copies() {
        val original =
            RemoteIconButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
            )

        val copy = original.copy(contentColor = RemoteColor(Color.Yellow))

        assertEquals(RemoteColor(Color.Yellow).constantValue, copy.contentColor.constantValue)
        assertEquals(original.containerColor.constantValue, copy.containerColor.constantValue)
    }

    @Test
    fun remoteTextButtonColors_copies() {
        val original =
            RemoteTextButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
            )

        val copy = original.copy(disabledContainerColor = RemoteColor(Color.Black))

        assertEquals(
            RemoteColor(Color.Black).constantValue,
            copy.disabledContainerColor.constantValue,
        )
        assertEquals(original.contentColor.constantValue, copy.contentColor.constantValue)
    }
}
