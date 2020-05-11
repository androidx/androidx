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

package android.view

class MotionEvent(val ix: Int, val iy: Int, val modifiers: Int) {
    companion object {
        @JvmStatic
        val ACTION_DOWN = 0
        @JvmStatic
        val ACTION_UP = 1
        @JvmStatic
        val ACTION_MOVE = 2
    }

    class PointerCoords(
        @JvmField
        var orientation: Float,
        @JvmField
        var pressure: Float,
        @JvmField
        var size: Float,
        @JvmField
        var toolMajor: Float,
        @JvmField
        var toolMinor: Float,
        @JvmField
        var touchMajor: Float,
        @JvmField
        var touchMinor: Float,
        @JvmField
        var x: Float,
        @JvmField
        var y: Float
    ) {
        constructor() : this(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    }

    val eventTime = System.currentTimeMillis()

    fun getActionMasked(): Int {
        return modifiers
    }

    val x: Float
        get() = ix.toFloat()

    val y: Float
        get() = iy.toFloat()

    val rawX: Float
        get() = ix.toFloat()

    val rawY: Float
        get() = iy.toFloat()

    fun offsetLocation(deltaX: Float, deltaY: Float) {
        if (deltaX != 0f || deltaY != 0f)
            println("MotionEvent.offsetLocation by $deltaX $deltaY")
    }

    val pointerCount = 1

    fun getPointerId(index: Int) = 0

    fun getPointerCoords(index: Int, pointerCoords: PointerCoords) {
        pointerCoords.x = x
        pointerCoords.y = y
    }
}