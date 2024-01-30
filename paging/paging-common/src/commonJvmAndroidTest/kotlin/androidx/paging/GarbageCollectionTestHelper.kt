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

package androidx.paging

import androidx.kruth.assertWithMessage
import androidx.paging.internal.AtomicBoolean
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

internal class GarbageCollectionTestHelper {
    private val queue = ReferenceQueue<Any>()
    private val references = mutableListOf<WeakReference<Any>>()
    private var size = 0

    fun track(item: Any) {
        references.add(WeakReference(item, queue))
        size++
    }

    fun assertLiveObjects(
        vararg expected: Pair<KClass<*>, Int>
    ) {
        val continueTriggeringGc = AtomicBoolean(true)
        thread {
            val leak: ArrayList<ByteArray> = ArrayList()
            do {
                val arraySize = Random.nextInt(1000)
                leak.add(ByteArray(arraySize))
                System.gc()
            } while (continueTriggeringGc.get())
        }
        var collectedItemCount = 0
        val expectedItemCount = size - expected.sumOf { it.second }
        while (collectedItemCount < expectedItemCount &&
            queue.remove(10.seconds.inWholeMilliseconds) != null
        ) {
            collectedItemCount++
        }
        continueTriggeringGc.set(false)
        val leakedObjects = countLiveObjects()
        val leakedObjectToStrings = references.mapNotNull {
            it.get()
        }.joinToString("\n")
        assertWithMessage(
            """
            expected to collect $expectedItemCount, collected $collectedItemCount.
            live objects: $leakedObjectToStrings
            """.trimIndent()
        ).that(leakedObjects).containsExactlyElementsIn(expected)
    }

    /**
     * Tries to trigger garbage collection until an element is available in the given queue.
     */
    fun assertEverythingIsCollected() {
        assertLiveObjects()
    }

    private fun countLiveObjects(): List<Pair<KClass<*>, Int>> {
        return references.mapNotNull {
            it.get()
        }.groupBy {
            it::class
        }.map { entry ->
            entry.key to entry.value.size
        }
    }
}
