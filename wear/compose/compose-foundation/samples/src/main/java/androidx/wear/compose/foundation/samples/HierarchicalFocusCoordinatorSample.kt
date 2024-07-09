/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.foundation.rememberActiveFocusRequester

@Sampled
@Composable
fun HierarchicalFocusCoordinatorSample() {
    var selected by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth()) {
            repeat(5) { ix ->
                var focused by remember { mutableStateOf(false) }
                HierarchicalFocusCoordinator(requiresFocus = { selected == ix }) {
                    val focusRequester = rememberActiveFocusRequester()
                    BasicText(
                        "$ix",
                        style =
                            TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            ),
                        modifier =
                            Modifier.weight(1f)
                                .clickable { selected = ix }
                                .onFocusChanged { focused = it.isFocused }
                                .focusRequester(focusRequester)
                                .focusable()
                                .then(
                                    if (focused) {
                                        Modifier.border(BorderStroke(2.dp, Color.Red))
                                    } else {
                                        Modifier
                                    }
                                )
                    )
                }
            }
        }
    }
}
