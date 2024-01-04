/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.input.motionprediction

import android.view.MotionEvent
import androidx.test.core.view.MotionEventBuilder

class MotionEventGenerator(val xGenerator: (Long) -> Float, val yGenerator: (Long) -> Float) {
    private val downEventTime: Long = 0
    private var currentEventTime: Long = downEventTime
    private val startX = 500f
    private val startY = 500f
    private var sentDown = false

    fun next(): MotionEvent {
        val motionEventBuilder = MotionEventBuilder.newBuilder()
            .setEventTime(currentEventTime)
            .setDownTime(downEventTime)
            .setActionIndex(0)

        if (sentDown) {
            motionEventBuilder.setAction(MotionEvent.ACTION_MOVE)
        } else {
            motionEventBuilder.setAction(MotionEvent.ACTION_DOWN)
            sentDown = true
        }

        val pointerProperties = MotionEvent.PointerProperties()
        pointerProperties.id = 0
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_STYLUS

        val coords = MotionEvent.PointerCoords()
        coords.x = startX + xGenerator(currentEventTime - downEventTime)
        coords.y = startY + yGenerator(currentEventTime - downEventTime)
        coords.pressure = 1f

        motionEventBuilder.setPointer(pointerProperties, coords)

        currentEventTime += MOTIONEVENT_RATE_MS
        return motionEventBuilder.build()
    }

    fun getRateMs(): Long {
        return MOTIONEVENT_RATE_MS
    }
}

const val MOTIONEVENT_RATE_MS: Long = 5
