/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

// TODO (b/527915161): Remove if we get something nice from Compose.
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.runRecomposition() {
    runCurrent()
    Snapshot.sendApplyNotifications()
    runCurrent()
}

// TODO (b/527915161): Remove if we get something nice from Compose.
internal val TestScope.testDispatcher: CoroutineDispatcher
    get() = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
