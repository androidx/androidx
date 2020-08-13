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

package com.android.tools.build.jetifier.processor

import java.nio.file.attribute.FileTime
import java.time.Instant

/**
 * Used to generate new modified time for files in archives.
 *
 * @param timestampProvider Used to generate the new modified time. The argument is the previous
 * modified time of a file from the original archive.
 */
class TimestampsPolicy(private val timestampProvider: (FileTime?) -> FileTime?) {
    /**
     * Generates a new modified time based on the previous one.
     *
     * @param previousTimestamp The previous modified time of a file from the original archive.
     * @return The new modified time to be set.
     */
    fun getModifiedTime(previousTimestamp: FileTime?) = timestampProvider(previousTimestamp)

    companion object {
        val EPOCH = TimestampsPolicy {
            FileTime.from(Instant.EPOCH)
        }
        val NOW = TimestampsPolicy {
            FileTime.from(Instant.now())
        }
        val KEEP_PREVIOUS = TimestampsPolicy {
            it // Return the previous time
        }
    }
}