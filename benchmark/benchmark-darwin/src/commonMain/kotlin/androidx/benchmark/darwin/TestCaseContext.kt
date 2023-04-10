/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin

interface TestCaseContext {
    /**
     * Registers a block of teardown code to run after the current test method ends.
     */
    fun addTeardownBlock(block: () -> Unit)

    /**
     * Records the selected metrics for a block of code.
     */
    fun measureWithMetrics(metrics: List<*>, options: MeasureOptions, block: () -> Unit)
}
