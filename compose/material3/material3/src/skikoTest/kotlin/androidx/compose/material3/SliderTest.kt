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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SliderTest {
    @Test
    fun changingOnValueChangeFinishedDoesNotTriggerFinish() = runComposeUiTest {
        var finish1Called = false
        fun onFinish1() { finish1Called = true }

        var finish2Called = false
        fun onFinish2() { finish2Called = true }

        var useFinish2 by mutableStateOf(false)

        setContent {
            var value by remember { mutableStateOf(0f) }
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = if (useFinish2) ::onFinish2 else ::onFinish1,
                modifier = Modifier.testTag("slider")
            )
        }

        onNodeWithTag("slider").apply {
            val size = fetchSemanticsNode().size
            performMouseInput {
                moveTo(Offset(x = size.width / 2f, y = size.height / 2f))
                press()
                moveTo(Offset(x = size.width / 3f, y = size.height / 2f))
                useFinish2 = true
                moveTo(Offset(x = size.width / 4f, y = size.height / 2f))
            }
        }

        assertFalse(finish1Called)
        assertFalse(finish2Called)

        onNodeWithTag("slider").performMouseInput {
            release()
        }

        assertFalse(finish1Called)
        assertTrue(finish2Called)
    }
}