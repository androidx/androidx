/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.compose.samples

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.Sampled
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow

@Sampled
@Composable
fun BackHandler() {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it }
    )

    BackHandler(text.isNotEmpty()) {
        // handle back event
    }
}

@Sampled
@Composable
fun PredictiveBack(callback: SampleCallbackHelper) {
    var text by remember { mutableStateOf("") }

    TextField(
        value = text,
        onValueChange = { text = it }
    )

    PredictiveBackHandler(text.isNotEmpty()) { progress: Flow<BackEventCompat> ->
        callback.preparePop()
        try {
            progress.collect { backEvent ->
                callback.updateProgress(backEvent.progress)
            }
            callback.popBackStack()
        } catch (e: CancellationException) {
            callback.cancelPop()
        }
    }
}

public class SampleCallbackHelper(
    val preparePop: () -> Unit,
    val updateProgress: (progress: Float) -> Unit,
    val popBackStack: () -> Unit,
    val cancelPop: () -> Unit
)
