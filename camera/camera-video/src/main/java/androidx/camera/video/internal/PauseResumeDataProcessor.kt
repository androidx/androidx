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

package androidx.camera.video.internal

import android.util.Range
import androidx.annotation.GuardedBy
import androidx.camera.core.Logger
import androidx.camera.video.internal.encoder.EncodedData

/**
 * Processes encoded data and adjusts timestamps across pause/resume intervals for
 * [androidx.camera.video.Recorder.RecordingRecord].
 *
 * Filters out buffers in active pause ranges, subtracts total paused duration from presentation
 * timestamps, and enforces per-stream timestamp monotonicity across video and audio encoders.
 */
public class PauseResumeDataProcessor {
    private val lock = Any()

    @GuardedBy("lock")
    private val activePauseResumeTimeRanges: MutableList<Range<Long>> = ArrayList()

    @GuardedBy("lock") private var lastVideoAdjustedTimeUs: Long = -1L

    @GuardedBy("lock") private var lastAudioAdjustedTimeUs: Long = -1L

    /**
     * Records a "pause" time in microseconds.
     *
     * @param pauseTriggerTimeUs timestamp when recording paused
     */
    public fun pause(pauseTriggerTimeUs: Long) {
        synchronized(lock) {
            activePauseResumeTimeRanges.add(Range.create(pauseTriggerTimeUs, Long.MAX_VALUE))
            Logger.d(TAG, "Pause on $pauseTriggerTimeUs us")
        }
    }

    /**
     * Records a "resume" time in microseconds.
     *
     * @param resumeTriggerTimeUs timestamp when recording resumed
     */
    public fun resume(resumeTriggerTimeUs: Long) {
        synchronized(lock) {
            if (activePauseResumeTimeRanges.isNotEmpty()) {
                val lastIndex = activePauseResumeTimeRanges.lastIndex
                val pauseRange = activePauseResumeTimeRanges[lastIndex]
                if (pauseRange.upper == Long.MAX_VALUE) {
                    val pauseTimeUs = pauseRange.lower
                    activePauseResumeTimeRanges[lastIndex] =
                        Range.create(pauseTimeUs, resumeTriggerTimeUs)
                    Logger.d(
                        TAG,
                        "Resume on $resumeTriggerTimeUs us, current paused duration = " +
                            "${resumeTriggerTimeUs - pauseTimeUs} us",
                    )
                }
            }
        }
    }

    /**
     * Processes an [EncodedData] buffer from an encoder.
     *
     * Filters out buffers in active pause ranges, subtracts completed pause durations from
     * presentation timestamps, and enforces per-stream timestamp monotonicity.
     *
     * @param encodedData raw buffer from video or audio encoder
     * @param isVideo true for video stream, false for audio stream
     * @return true if buffer is valid and adjusted, false if it should be dropped
     */
    public fun processEncodedData(encodedData: EncodedData, isVideo: Boolean): Boolean {
        synchronized(lock) {
            val bufferInfo = encodedData.bufferInfo
            val presentationTimeUs = bufferInfo.presentationTimeUs

            if (isInPauseRange(presentationTimeUs)) {
                Logger.d(
                    TAG,
                    "Drop buffer by pause in PauseResumeDataProcessor (${if (isVideo) "video" else "audio"}).",
                )
                return false
            }

            val totalPausedDurationUs = getPausedDurationBefore(presentationTimeUs)
            val adjustedTimeUs =
                if (totalPausedDurationUs > 0L) {
                    presentationTimeUs - totalPausedDurationUs
                } else {
                    presentationTimeUs
                }

            val lastSentAdjustedTimeUs =
                if (isVideo) lastVideoAdjustedTimeUs else lastAudioAdjustedTimeUs
            if (lastSentAdjustedTimeUs != -1L && adjustedTimeUs <= lastSentAdjustedTimeUs) {
                Logger.d(
                    TAG,
                    "Drop buffer because adjusted time is less than or equal to the last sent time " +
                        "(${if (isVideo) "video" else "audio"}: $adjustedTimeUs <= $lastSentAdjustedTimeUs).",
                )
                return false
            }

            if (isVideo) {
                lastVideoAdjustedTimeUs = adjustedTimeUs
            } else {
                lastAudioAdjustedTimeUs = adjustedTimeUs
            }

            bufferInfo.presentationTimeUs = adjustedTimeUs
            return true
        }
    }

    @GuardedBy("lock")
    private fun getPausedDurationBefore(timeUs: Long): Long {
        var totalPausedDurationUs = 0L
        for (range in activePauseResumeTimeRanges) {
            if (timeUs > range.upper) {
                totalPausedDurationUs += (range.upper - range.lower)
            } else if (timeUs < range.lower) {
                break
            }
        }
        return totalPausedDurationUs
    }

    @GuardedBy("lock")
    private fun isInPauseRange(timeUs: Long): Boolean {
        for (range in activePauseResumeTimeRanges) {
            if (range.contains(timeUs)) {
                return true
            } else if (timeUs < range.lower) {
                return false
            }
        }
        return false
    }

    private companion object {
        private const val TAG = "PauseResumeDataProc"
    }
}
