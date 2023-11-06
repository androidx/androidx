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
package androidx.work

/**
 * An abstract class that allows the user to define how to merge a list of inputs to a
 * [ListenableWorker].
 *
 * Before workers run, they receive input [Data] from their parent workers, as well as
 * anything specified directly to them via [WorkRequest.Builder.setInputData].  An
 * InputMerger takes all of these objects and converts them to a single merged [Data] to be
 * used as the worker input.  [WorkManager] offers two concrete InputMerger implementations:
 * [OverwritingInputMerger] and [ArrayCreatingInputMerger].
 *
 * Note that the list of inputs to merge is in an unspecified order.  You should not make
 * assumptions about the order of inputs.
 */
abstract class InputMerger {
    /**
     * Merges a list of [Data] and outputs a single Data object.
     *
     * @param inputs A list of [Data]
     * @return The merged output
     */
    abstract fun merge(inputs: List<Data>): Data
}

private val TAG = Logger.tagWithPrefix("InputMerger")

internal fun fromClassName(className: String): InputMerger? {
    try {
        val clazz = Class.forName(className)
        return clazz.getDeclaredConstructor().newInstance() as InputMerger
    } catch (e: Exception) {
        Logger.get().error(TAG, "Trouble instantiating $className", e)
    }
    return null
}
