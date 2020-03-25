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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Snackbar
import androidx.ui.material.TextButton
import androidx.ui.material.samples.SimpleSnackbar
import androidx.ui.material.snackbarPrimaryColorFor
import androidx.ui.unit.dp

@Composable
fun SnackbarDemo() {
    Column(Modifier.padding(12.dp, 0.dp, 12.dp, 0.dp)) {
        val textSpacing = Modifier.padding(top = 12.dp, bottom = 12.dp)
        Text("Default Snackbar", modifier = textSpacing)
        SimpleSnackbar()
        Text("Snackbar with long text", modifier = textSpacing)
        Snackbar(
            text = { Text("This song already exists in the current playlist") },
            action = {
                TextButton(
                    contentColor = snackbarPrimaryColorFor(MaterialTheme.colors),
                    onClick = { /* perform undo */ }
                ) {
                    Text("ADD THIS SONG ANYWAY")
                }
            },
            actionOnNewLine = true
        )
    }
}
