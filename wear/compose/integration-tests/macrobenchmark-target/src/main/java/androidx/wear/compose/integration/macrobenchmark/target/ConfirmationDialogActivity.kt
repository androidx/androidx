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

package androidx.wear.compose.integration.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog

class ConfirmationDialogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showDialog by remember { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Chip(
                        onClick = { showDialog = true },
                        label = { Text("Open") },
                        modifier =
                            Modifier.semantics { contentDescription = OPEN_CONFIRMATION_DIALOG },
                    )
                }
                Dialog(
                    showDialog = showDialog,
                    onDismissRequest = { showDialog = false },
                    modifier = Modifier.semantics { contentDescription = CONFIRMATION_DIALOG },
                ) {
                    Confirmation(
                        onTimeout = { showDialog = false },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_favorite_rounded),
                                modifier =
                                    Modifier.size(48.dp).wrapContentSize(align = Alignment.Center),
                                contentDescription = "Favorite",
                            )
                        },
                    ) {
                        Text(text = "Success", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

private const val OPEN_CONFIRMATION_DIALOG = "OPEN_CONFIRMATION_DIALOG"
private const val CONFIRMATION_DIALOG = "CONFIRMATION_DIALOG"
