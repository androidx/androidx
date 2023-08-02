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

package androidx.camera.integration.camera2.pipe

import androidx.camera.integration.camera2.pipe.extensions.get1DRunnable

/** Manages all the threads generating data */
class DataGenerator(
    private val dataListener: DataListener,
    private val visualizationDefaults: VisualizationDefaults
) {

    /** Saves the timestamp from when data generation starts */
    var beginTimeNanos: Long = 0L

    /** List of all the threads generating data */
    private var threads: MutableList<Thread> = mutableListOf()

    /**
     * Start each thread that is supposed to generate data.
     * TODO("Implement this using coroutines in the future so the calling thread isn't blocked")
     */
    fun runDataGenerators() {
        DATA_GENERATION_PARAMS.forEach { entry ->
            val key = entry.key
            val default = entry.value
            val runnable = get1DRunnable(default) { point ->
                /** For graphs both keyValue and graph DataHolders need to be updated */
                if (visualizationDefaults.keysVisualizedAsValueGraph.contains(key) ||
                    visualizationDefaults.keysVisualizedAsStateGraph.contains(key)
                ) {
                    dataListener.newGraphData(
                        entry.key,
                        point.frameNumber,
                        point.timestampNanos,
                        point.value
                    )
                    dataListener.newKeyValueData(entry.key, point.value)
                } else if (visualizationDefaults.keysVisualizedAsKeyValuePair.contains(key))
                    dataListener.newKeyValueData(entry.key, point.value)
            }
            threads.add(Thread(runnable))
        }

        threads.forEach { it.start() }
    }

    /** Stop each thread generating data */
    fun quitDataGenerators() {
        threads.forEach {
            it.interrupt()
            it.join()
        }
    }
}
