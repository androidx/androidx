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

package androidx.benchmark

import androidx.annotation.RestrictTo

/**
* Represents actions that effect changes to the state of the app / device outside of the scope
* of the benchmark. Typically used to help reduce the amount of interference during a benchmark.
*
* [SideEffect]s must define a [setup], that is executed when the benchmark starts. The [tearDown]
* method is called during the end of the benchmark to reverse actions so subsequent invocations of
* the benchmark are hermetic.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SideEffect {

    /**
     * Returns the canonical name of the [SideEffect].
     */
    fun name(): String

    /**
     * This method is executed when the benchmark starts.
     */
    fun setup()

    /**
     * This method is executed when the benchmark is complete. A [SideEffect] should undo
     * the changes to the state of the device app, to ensure hermetic benchmarks.
     */
    fun tearDown()
}
