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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.DialogState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DialogTest {

    @Test
    fun dialogIsCenteredInWindow() = runSkikoComposeUiTest(
        size = Size(100f, 100f)
    ) {
        val dialog = DialogState(
            IntSize(40, 40)
        )

        setContent {
            Box(Modifier.size(10.dp)) {
                dialog.Content()
            }
        }
        onNodeWithTag(dialog.tag).assertPositionInRootIsEqualTo(30.dp, 30.dp)
    }
}