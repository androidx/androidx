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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Text
import androidx.ui.graphics.Color
import androidx.ui.material.Snackbar
import androidx.ui.material.TextButton

@Sampled
@Composable
fun SimpleSnackbar() {
    Snackbar(
        text = "Action has been done",
        actionText = "UNDO",
        onActionClick = { /* call undo here */ }
    )
}

@Sampled
@Composable
fun SlotsSnackbar() {
    val customActionColor = Color.Cyan
    Snackbar(
        text = {
            Text(
                text = "This song already exists in the current playlist",
                maxLines = 2
            )
        },
        action = {
            TextButton(
                onClick = { /* call long action here */ },
                contentColor = customActionColor
            ) {
                Text("ADD THIS SONG ANYWAY")
            }
        },
        actionOnNewLine = true
    )
}