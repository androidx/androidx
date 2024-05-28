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

class MotionEventGenerator(
    val firstXGenerator: (Long) -> Float,
    val firstYGenerator: (Long) -> Float,
    val firstPressureGenerator: ((Long) -> Float)?,
    val secondXGenerator: ((Long) -> Float)?,
    val secondYGenerator: ((Long) -> Float)?,
    val secondPressureGenerator: ((Long) -> Float)?,
) {
    constructor(
        firstXGenerator: (Long) -> Float,
        firstYGenerator: (Long) -> Float,
        firstPressureGenerator: ((Long) -> Float)?
    ) : this(firstXGenerator, firstYGenerator, firstPressureGenerator, null, null, null)

    private val downEventTime: Long = System.currentTimeMillis()
    private var currentEventTime: Long = downEventTime
    private val firstStartX = 500f
    private val firstStartY = 500f
    private val secondStartX = 500f
    private val secondStartY = 500f
    private var sentDown = false
    private var sentSecondDown = false

    fun next(): MotionEvent {
        val motionEventBuilder =
            MotionEventBuilder.newBuilder()
                .setEventTime(currentEventTime)
                .setDownTime(downEventTime)

        if (!sentDown) {
            motionEventBuilder.setAction(MotionEvent.ACTION_DOWN)
            motionEventBuilder.setActionIndex(0)
            sentDown = true
            if (secondXGenerator == null || secondYGenerator == null) {
                sentSecondDown = true
            }
        } else if (!sentSecondDown) {
            motionEventBuilder.setAction(MotionEvent.ACTION_POINTER_DOWN)
            motionEventBuilder.setActionIndex(1)
            sentSecondDown = true
        } else {
            motionEventBuilder.setAction(MotionEvent.ACTION_MOVE)
        }

        val pointerProperties = MotionEvent.PointerProperties()
        pointerProperties.id = 0
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_STYLUS

        val coords = MotionEvent.PointerCoords()
        coords.x = firstStartX + firstXGenerator(currentEventTime - downEventTime)
        coords.y = firstStartY + firstYGenerator(currentEventTime - downEventTime)
        if (firstPressureGenerator == null) {
            coords.pressure = 1f
        } else {
            coords.pressure = firstPressureGenerator.invoke(currentEventTime - downEventTime)
        }

        motionEventBuilder.setPointer(pointerProperties, coords)

        if (sentDown && secondXGenerator != null && secondYGenerator != null) {
            val secondPointerProperties = MotionEvent.PointerProperties()
            secondPointerProperties.id = 1
            secondPointerProperties.toolType = MotionEvent.TOOL_TYPE_STYLUS

            val secondCoords = MotionEvent.PointerCoords()
            secondCoords.x =
                secondStartX + secondXGenerator.invoke(currentEventTime - downEventTime)
            secondCoords.y =
                secondStartY + secondYGenerator.invoke(currentEventTime - downEventTime)
            if (secondPressureGenerator == null) {
                secondCoords.pressure = 1f
            } else {
                secondCoords.pressure =
                    secondPressureGenerator.invoke(currentEventTime - downEventTime)
            }

            motionEventBuilder.setPointer(secondPointerProperties, secondCoords)
        }

        if (sentDown && sentSecondDown) {
            currentEventTime += MOTIONEVENT_RATE_MS
        }
        return motionEventBuilder.build()
    }

    fun getRateMs(): Long {
        return MOTIONEVENT_RATE_MS
    }
}

const val MOTIONEVENT_RATE_MS: Long = 5
