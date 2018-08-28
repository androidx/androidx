/*
 * Copyright 2018 The Android Open Source Project
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

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.developer.timeline

import android.util.Log

// TODO(Migration/Andrey): This should be true only on debug
internal const val _isProduct: Boolean = false // = const bool.fromEnvironment("dart.vm.product");

/**
 * A typedef for the function argument to [Timeline.timeSync].
 */
typealias TimelineSyncFunction = () -> Any

fun _argumentsAsJson(arguments: Map<*, *>?): String {
    return if (arguments == null || arguments.isEmpty()) {
        // Fast path no arguments. Avoid calling jsonEncode.
        "{}"
    } else {
        arguments.toString()
        // TODO("Migration/Andrey): Kotlin has no build-in json library")
    }
}

/**
 * Returns true if the Dart Timeline stream is enabled.
 */
// TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
internal fun _isDartStreamEnabled(): Boolean = !_isProduct

/**
 * Returns the next async task id.
 */
internal fun _getNextAsyncId(): Int {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    return nextId++
}

private var nextId: Int = 0

/**
 * Returns the current value from the trace clock.
 */
internal fun _getTraceClock(): Long {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    return System.currentTimeMillis()
}

/**
 * Returns the current value from the thread CPU usage clock.
 */
internal fun _getThreadCpuClock(): Long {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    return System.currentTimeMillis()
}

/**
 * Reports a complete synchronous event.
 */
internal fun _reportCompleteEvent(
    start: Long,
    startCpu: Long,
    category: String,
    name: String?,
    argumentsAsJson: String?
) {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    reportInLogcat("$name took ${_getTraceClock() - start} ms to complete. " +
            "Arguments: $argumentsAsJson. Category: $category.")
}

/**
 * Reports a flow event.
 */
internal fun _reportFlowEvent(
    start: Long,
    startCpu: Long,
    category: String,
    name: String?,
    type: Int,
    id: Int,
    argumentsAsJson: String?
) {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    reportInLogcat("Flow $name took ${_getTraceClock() - start} ms to complete. " +
            "Type: $type. Id: $id. Arguments: $argumentsAsJson Category: $category.")
}

/**
 * Reports an instant event.
 */
internal fun _reportInstantEvent(
    start: Long,
    category: String,
    name: String?,
    argumentsAsJson: String?
) {
    // TODO(Migration/Andrey): Fun is external in the original sources; temporary impl.
    reportInLogcat("Instant event $name at $start. " +
            "Arguments: $argumentsAsJson Category: $category.")
}

// TODO(Migration/Andrey): Temporary impl for reporting. Not a part of Flutter.
private fun reportInLogcat(s: String) {
    Log.d("Timeline", s)
}