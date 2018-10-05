/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.multitap

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureArenaEntry
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.pointer_router.PointerRoute

/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * TapTracker helps track individual tap sequences as part of a
 * larger gesture.
 */
internal open class TapTracker(val event: PointerDownEvent, val entry: GestureArenaEntry) {
    private val initialPosition = event.position
    private var isTrackingPointer = false
    val pointer = event.pointer

    fun startTrackingPointer(route: PointerRoute) {
        if (!isTrackingPointer) {
            isTrackingPointer = true
            GestureBinding.instance!!.pointerRouter.addRoute(pointer, route)
        }
    }

    open fun stopTrackingPointer(route: PointerRoute) {
        if (isTrackingPointer) {
            isTrackingPointer = false
            GestureBinding.instance!!.pointerRouter.removeRoute(pointer, route)
        }
    }

    fun isWithinTolerance(event: PointerEvent, tolerance: Double): Boolean {
        val offset: Offset = event.position - initialPosition
        return (offset.getDistance() <= tolerance)
    }
}