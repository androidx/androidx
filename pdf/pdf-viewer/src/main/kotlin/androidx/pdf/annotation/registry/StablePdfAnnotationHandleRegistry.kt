/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.annotation.registry

import androidx.pdf.annotation.AnnotationHandleIdGenerator
import androidx.pdf.annotation.AnnotationHandleIdGenerator.composeAnnotationId
import java.util.Collections

/** A thread-safe, in-memory implementation of [AnnotationHandleRegistry]. */
internal class StablePdfAnnotationHandleRegistry : AnnotationHandleRegistry {
    private val lock = Any()

    private val handleToSourceMap: MutableMap<String, String> =
        Collections.synchronizedMap(HashMap())

    private val sourceToHandleMap: MutableMap<AnnotationSourceKey, String> =
        Collections.synchronizedMap(HashMap())

    override fun getHandleId(pageNum: Int, sourceId: String): String {
        synchronized(lock) {
            val key = AnnotationSourceKey(pageNum, sourceId)

            sourceToHandleMap[key]?.let {
                return it
            }

            val newHandleId =
                composeAnnotationId(pageNum, id = AnnotationHandleIdGenerator.generateId())

            handleToSourceMap[newHandleId] = sourceId
            sourceToHandleMap[key] = newHandleId

            return newHandleId
        }
    }

    override fun getSourceId(handleId: String): String? {
        return handleToSourceMap[handleId]
    }

    override fun clear() {
        handleToSourceMap.clear()
        sourceToHandleMap.clear()
    }

    /** Represents the key for [sourceToHandleMap]. */
    private data class AnnotationSourceKey(public val pageNum: Int, public val sourceId: String)
}
