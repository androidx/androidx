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

package androidx.compose.ui.test.utils

import androidx.compose.test.utils.endAllTouches
import androidx.compose.test.utils.endTouch
import androidx.compose.test.utils.getTouchesEvent
import androidx.compose.test.utils.send
import androidx.compose.test.utils.setLocationInWindow
import androidx.compose.test.utils.setPhase
import androidx.compose.test.utils.touchAtPoint
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.unit.DpOffset
import kotlin.time.Duration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UITouchTypeDirect
import platform.UIKit.UITouchTypeIndirect
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
internal fun UIWindow.touchDown(location: DpOffset, fromEdge: Boolean = false): UITouch {
    return UITouch.touchAtPoint(
        point = location.toCGPoint(),
        withType = UITouchTypeDirect,
        inWindow = this,
        tapCount = 1L,
        fromEdge = fromEdge
    ).also {
        it.send()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun UIWindow.mouseDown(location: DpOffset): UITouch {
    return UITouch.touchAtPoint(
        point = location.toCGPoint(),
        withType = UITouchTypeIndirect,
        inWindow = this,
        tapCount = 1L,
        fromEdge = false
    ).also {
        it.send()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun UIWindow.getTouchesEvent(): UIEvent {
    return UITouch.getTouchesEvent()
}

@OptIn(ExperimentalForeignApi::class)
internal fun UIWindow.resetTouches() {
    UITouch.endAllTouches()
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.moveToLocationOnWindow(location: DpOffset) {
    setLocationInWindow(location.toCGPoint())
    setPhase(UITouchPhase.UITouchPhaseMoved)
    send()
}

internal fun UITouch.wait(duration: Duration): UITouch {
    UIKitInstrumentedTest.delay(duration.inWholeMilliseconds)
    return this
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.hold(): UITouch {
    setPhase(UITouchPhase.UITouchPhaseStationary)
    send()
    return this
}

@OptIn(ExperimentalForeignApi::class)
internal fun UITouch.up() {
    setPhase(UITouchPhase.UITouchPhaseEnded)
    send()
    endTouch()
}
