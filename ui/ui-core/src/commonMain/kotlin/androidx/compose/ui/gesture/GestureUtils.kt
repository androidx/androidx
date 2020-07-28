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

package androidx.compose.ui.gesture

import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.platform.PointerInputChange
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.inMilliseconds
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

// TODO(shepshapard): This continues to be very confusing to use.  Have to come up with a better
//  way of easily expressing this.
/**
 * Utility method that determines if any pointers are currently in [bounds].
 *
 * A pointer is considered in bounds if it is currently down and it's current
 * position is within the provided [bounds]
 *
 * @return True if at least one pointer is in bounds.
 */
fun List<PointerInputChange>.anyPointersInBounds(bounds: IntSize) =
    fastAny {
        it.current.down &&
                it.current.position!!.x >= 0 &&
                it.current.position.x < bounds.width &&
                it.current.position.y >= 0 &&
                it.current.position.y < bounds.height
    }

internal data class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

/**
 * Run [block] after [duration] time passes using [context].
 *
 * @return [Job] which is a reference to the running coroutine such that it can be cancelled via [Job.cancel].
 */
internal fun delay(duration: Duration, context: CoroutineContext, block: () -> Unit): Job =
    CoroutineScope(context).launch {
        kotlinx.coroutines.delay(duration.inMilliseconds())
        block()
    }