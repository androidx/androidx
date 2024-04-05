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

package androidx.compose.foundation.v2

import kotlinx.coroutines.*


private val mainScope = MainScope()

/**
 * In a browser environment it's NOT blocking!
 * We use Dispatchers.Unconfined,
 * so if no suspension occurs, the block will complete before the completion of this function.
 */
internal actual fun runBlockingIfPossible(block: suspend CoroutineScope.() -> Unit) {
    mainScope.launch(context = Dispatchers.Unconfined, block = block)
}