/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.layout

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.glance.Applier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

suspend fun runTestingComposition(content: @Composable () -> Unit): EmittableBox = coroutineScope {
    val root = EmittableBox()
    val applier = Applier(root)
    val recomposer = Recomposer(currentCoroutineContext())
    val composition = Composition(applier, recomposer)
    val frameClock = BroadcastFrameClock()

    composition.setContent { content() }

    launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }

    recomposer.close()
    recomposer.join()

    root
}
