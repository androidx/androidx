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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest

@Sampled
@OptIn(ExperimentalTestApi::class)
fun RunComposeUiTestSample() = runComposeUiTest {
    var counter by mutableIntStateOf(1)
    setContent {
        Column {
            Text(text = "Count: $counter", modifier = Modifier.testTag("text_tag"))
            Button(onClick = { counter++ }, modifier = Modifier.testTag("button_tag")) {
                Text("Click Me!")
            }
        }
    }

    onNodeWithTag("button_tag").performClick()
    onNodeWithTag("text_tag").assert(hasText("Count: 2"))
}
