/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.demos

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.VerticalScroller
import androidx.ui.layout.padding
import androidx.ui.unit.dp

@Composable
fun TextFieldWithScrollerDemo() {
    VerticalScroller {
        val state = state { TextFieldValue(
            text = List(100) { "Line: $it" }.joinToString("\n")
        ) }
        TextField(
            value = state.value,
            onValueChange = { state.value = it },
            modifier = Modifier
                .padding(20.dp)
        )
    }
}
