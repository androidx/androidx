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

class FakeAnnotationHandleRegistry : AnnotationHandleRegistry {
    private val handleToSource = mutableMapOf<String, String>()
    private val sourceToHandle = mutableMapOf<String, String>()

    override fun getHandleId(pageNum: Int, sourceId: String): String {
        return sourceToHandle.getOrPut(sourceId) {
            val newHandle = composeAnnotationId(pageNum, AnnotationHandleIdGenerator.generateId())
            handleToSource[newHandle] = sourceId
            newHandle
        }
    }

    override fun getSourceId(handleId: String): String? {
        return handleToSource[handleId]
    }

    override fun clear() {
        handleToSource.clear()
        sourceToHandle.clear()
    }
}
