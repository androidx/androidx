/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import androidx.ink.strokes.InProgressStroke

internal interface InProgressStrokePool {
    fun obtain(): InProgressStroke

    fun recycle(inProgressStroke: InProgressStroke)

    fun trimToSize(maxSize: Int)

    companion object {
        fun create(): InProgressStrokePool = InProgressStrokePoolImpl()
    }
}

private class InProgressStrokePoolImpl : InProgressStrokePool {

    private val pool = mutableListOf<InProgressStroke>()

    override fun obtain(): InProgressStroke = pool.removeFirstOrNull() ?: InProgressStroke()

    override fun recycle(inProgressStroke: InProgressStroke) {
        // Will be started with the actual brush when this InProgressStroke is reused, but
        // defensively
        // clear its data for now. This does not deallocate the space for its data, so it's ready to
        // be
        // reused with minimal cost compared to allocating a new one.
        inProgressStroke.clear()
        pool.add(inProgressStroke)
    }

    override fun trimToSize(maxSize: Int) {
        require(maxSize >= 0)
        if (pool.size <= maxSize) return
        pool.subList(maxSize, pool.size).clear()
        check(pool.size == maxSize)
    }
}
