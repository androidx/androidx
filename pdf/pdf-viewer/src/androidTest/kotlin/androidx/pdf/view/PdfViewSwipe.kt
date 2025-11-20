/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.view.MotionEvent
import androidx.test.espresso.UiController
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.action.Swiper

/**
 * Implements [Swiper] to perform linear swipes between provided coordinates at different
 * velocities, namely [SCROLL] and [FLING].
 *
 * This is largely a fork of [androidx.test.espresso.action.Swipe], but it interpolates coordinates
 * differently. Espresso's swipe sends [MotionEvent.ACTION_MOVE] at steps that are slightly too
 * small. This leads to a gap in position between the last move event and the terminal
 * [MotionEvent.ACTION_UP], which does not accurately reflect the stream of events that are issued
 * by a real touch gesture. In contrast, this [Swiper] guarantees the final
 * [MotionEvent.ACTION_MOVE] and the terminal [MotionEvent.ACTION_UP] occur at the same position.
 */
internal enum class PdfViewSwipe : Swiper {
    SCROLL {
        override fun sendSwipe(
            uiController: UiController,
            startCoordinates: FloatArray,
            endCoordinates: FloatArray,
            precision: FloatArray,
        ): Swiper.Status {
            return sendLinearSwipe(
                uiController,
                startCoordinates,
                endCoordinates,
                precision,
                SCROLL_DURATION_MS,
            )
        }
    },
    FLING {
        override fun sendSwipe(
            uiController: UiController,
            startCoordinates: FloatArray,
            endCoordinates: FloatArray,
            precision: FloatArray,
        ): Swiper.Status {
            return sendLinearSwipe(
                uiController,
                startCoordinates,
                endCoordinates,
                precision,
                FLING_DURATION_MS,
            )
        }
    };

    companion object {
        /** The number of motion events to send for each swipe. */
        private const val STEP_COUNT = 10

        /** The duration of a "fling" swipe, in milliseconds */
        private const val FLING_DURATION_MS = 150

        /** The duration of a "scroll" swipe, in milliseconds */
        private const val SCROLL_DURATION_MS = 1500

        /**
         * Returns an [Array] of [FloatArray] representing a stream of Cartesian coordinates between
         * 2 points [start] and [end]
         */
        private fun interpolate(start: FloatArray, end: FloatArray): Array<FloatArray> {
            val res = Array(STEP_COUNT) { FloatArray(2) }

            for (i in 1..STEP_COUNT) {
                res[i - 1][0] = start[0] + (end[0] - start[0]) * i / STEP_COUNT
                res[i - 1][1] = start[1] + (end[1] - start[1]) * i / STEP_COUNT
            }

            return res
        }

        /**
         * Injects a stream of [MotionEvent] to [uiController] representing a linear swipe between
         * [startCoordinates] and [endCoordinates]
         */
        private fun sendLinearSwipe(
            uiController: UiController,
            startCoordinates: FloatArray,
            endCoordinates: FloatArray,
            precision: FloatArray,
            duration: Int,
        ): Swiper.Status {
            val steps: Array<FloatArray> = interpolate(startCoordinates, endCoordinates)

            val events: MutableList<MotionEvent> = ArrayList()
            val downEvent = MotionEvents.obtainDownEvent(startCoordinates, precision)
            events.add(downEvent)

            try {
                val intervalMs = (duration / steps.size).toLong()
                var eventTime = downEvent.downTime
                for (step in steps) {
                    eventTime += intervalMs
                    events.add(MotionEvents.obtainMovement(downEvent, eventTime, step))
                }
                eventTime += intervalMs
                events.add(MotionEvents.obtainUpEvent(downEvent, eventTime, endCoordinates))
                uiController.injectMotionEventSequence(events)
            } catch (e: Exception) {
                return Swiper.Status.FAILURE
            } finally {
                for (event in events) {
                    event.recycle()
                }
            }
            return Swiper.Status.SUCCESS
        }
    }
}
