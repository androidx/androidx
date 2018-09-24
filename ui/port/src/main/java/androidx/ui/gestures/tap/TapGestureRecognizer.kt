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

package androidx.ui.gestures.tap

import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.FlagProperty
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.kPressTimeout
import androidx.ui.gestures.recognizer.GestureRecognizerState
import androidx.ui.gestures.recognizer.PrimaryPointerGestureRecognizer

// / Recognizes taps.
// /
// / [TapGestureRecognizer] considers all the pointers involved in the pointer
// / event sequence as contributing to one gesture. For this reason, extra
// / pointer interactions during a tap sequence are not recognized as additional
// / taps. For example, down-1, down-2, up-1, up-2 produces only one tap on up-1.
// /
// / See also:
// /
// /  * [MultiTapGestureRecognizer]
// TODO(Migration/shepshapard): Needs tests, which rely on some Mixin stuff.
class TapGestureRecognizer(debugOwner: Any? = null) : PrimaryPointerGestureRecognizer(
    kPressTimeout,
    debugOwner
) {
    // / A pointer that might cause a tap has contacted the screen at a particular
    // / location.
    var onTapDown: GestureTapDownCallback? = null

    // / A pointer that will trigger a tap has stopped contacting the screen at a
    // / particular location.
    var onTapUp: GestureTapUpCallback? = null

    // / A tap has occurred.
    var onTap: GestureTapCallback? = null

    // / The pointer that previously triggered [onTapDown] will not end up causing
    // / a tap.
    var onTapCancel: GestureTapCancelCallback? = null

    private var _sentTapDown: Boolean = false
    private var _wonArenaForPrimaryPointer: Boolean = false
    private var _finalPosition: Offset? = null

    override fun handlePrimaryPointer(event: PointerEvent) {
        if (event is PointerUpEvent) {
            _finalPosition = event.position
            _checkUp()
        } else if (event is PointerCancelEvent) {
            _reset()
        }
    }

    override fun resolve(disposition: GestureDisposition) {
        if (_wonArenaForPrimaryPointer && disposition == GestureDisposition.rejected) {
            // This can happen if the superclass decides the primary pointer
            // exceeded the touch slop, or if the recognizer is disposed.
            if (onTapCancel != null)
                invokeCallback("spontaneous onTapCancel", ::onTapCancel)
            _reset()
        }
        super.resolve(disposition)
    }

    override fun didExceedDeadline() {
        _checkDown()
    }

    override fun acceptGesture(pointer: Int) {
        super.acceptGesture(pointer)
        if (pointer == primaryPointer) {
            _checkDown()
            _wonArenaForPrimaryPointer = true
            _checkUp()
        }
    }

    override fun rejectGesture(pointer: Int) {
        super.rejectGesture(pointer)
        if (pointer == primaryPointer) {
            // Another gesture won the arena.
            assert(state != GestureRecognizerState.possible)
            if (onTapCancel != null)
                invokeCallback("forced onTapCancel", ::onTapCancel)
            _reset()
        }
    }

    private fun _checkDown() {
        if (!_sentTapDown) {
            if (onTapDown != null)
                invokeCallback("onTapDown", callback = {
                    onTapDown?.invoke(TapDownDetails(globalPosition = initialPosition!!))
                })
            _sentTapDown = true
        }
    }

    private fun _checkUp() {
        if (_wonArenaForPrimaryPointer && _finalPosition != null) {
            resolve(GestureDisposition.accepted)
            if (!_wonArenaForPrimaryPointer || _finalPosition == null) {
                // It is possible that resolve has just recursively called _checkUp
                // (see https://github.com/flutter/flutter/issues/12470).
                // In that case _wonArenaForPrimaryPointer will be false (as _checkUp
                // calls _reset) and we return here to avoid double invocation of the
                // tap callbacks.
                return
            }
            if (onTapUp != null)
                invokeCallback("onTapUp", callback = {
                    onTapUp?.invoke(TapUpDetails(globalPosition = _finalPosition!!))
                })
            if (onTap != null)
                invokeCallback("onTap", ::onTap)
            _reset()
        }
    }

    private fun _reset() {
        _sentTapDown = false
        _wonArenaForPrimaryPointer = false
        _finalPosition = null
    }

    override val debugDescription = "tap"

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(
            FlagProperty(
                "wonArenaForPrimaryPointer",
                value = _wonArenaForPrimaryPointer,
                ifTrue = "won arena"
            )
        )
        properties.add(
            DiagnosticsProperty.create(
                "finalPosition",
                _finalPosition,
                defaultValue = null
            )
        )
        properties.add(
            FlagProperty(
                "sentTapDown",
                value = _sentTapDown,
                ifTrue = "sent tap down"
            )
        )
    }
}