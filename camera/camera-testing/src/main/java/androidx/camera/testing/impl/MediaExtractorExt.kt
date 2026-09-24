/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.media.MediaExtractor
import android.media.MediaFormat

public fun MediaExtractor.useAndRelease(block: (MediaExtractor) -> Unit) {
    try {
        block(this)
    } finally {
        release()
    }
}

/**
 * Extracts sample presentation timestamps (in microseconds) for the first video track.
 *
 * @param maxSamples the maximum number of sample timestamps to retrieve. Defaults to
 *   [Int.MAX_VALUE].
 */
public fun MediaExtractor.getVideoSampleTimesUs(maxSamples: Int = Int.MAX_VALUE): List<Long> {
    for (i in 0 until trackCount) {
        val format = getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("video/")) {
            selectTrack(i)
            val sampleTimes = mutableListOf<Long>()
            while (sampleTime != -1L && sampleTimes.size < maxSamples) {
                sampleTimes.add(sampleTime)
                advance()
            }
            return sampleTimes
        }
    }
    return emptyList()
}
