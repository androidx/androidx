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

package androidx.ui.temputils

import androidx.ui.unit.Duration
import androidx.ui.unit.inMilliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/*
 * This file is a temporary place to utilize coroutines before they are work in the IR compiler.
 */

/**
 * Run [block] after [duration] time passes using [context].
 *
 * @return [Job] which is a reference to the running coroutine such that it can be cancelled via [Job.cancel].
 */
fun delay(duration: Duration, context: CoroutineContext, block: () -> Unit) =
    CoroutineScope(context).launch {
        kotlinx.coroutines.delay(duration.inMilliseconds())
        block()
    }