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

package androidx.ui.animation.animations

import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationLazyListenerMixin
import androidx.ui.animation.AnimationStatus
import androidx.ui.runtimeType
import androidx.ui.toStringAsFixed

/**
 * An animation that is a proxy for another animation.
 *
 * A proxy animation is useful because the parent animation can be mutated. For
 * example, one object can create a proxy animation, hand the proxy to another
 * object, and then later change the animation from which the proxy receives
 * its value.
 */
class ProxyAnimation(
    /**
     * If the animation argument is omitted, the proxy animation will have the
     * status [AnimationStatus.DISMISSED] and a value of 0
     */
    animation: Animation<Double>? = null
) : AnimationLazyListenerMixin<Double>() {

    private var _parent: Animation<Double>?
    /**
     * The animation whose value this animation will proxy.
     *
     * This value is mutable. When mutated, the listeners on the proxy animation
     * will be transparently updated to be listening to the new parent animation.
     */
    var parent: Animation<Double>?
        get() = _parent
        set(newParent) {
            val oldParent = _parent
            if (newParent == oldParent)
                return
            if (oldParent != null) {
                _status = oldParent.status
                _value = oldParent.value
                if (isListening) {
                    didStopListening()
                }
            }
            _parent = newParent
            if (newParent != null) {
                if (isListening) {
                    didStartListening()
                }
                if (_value != newParent.value) {
                    notifyListeners()
                }
                if (_status != newParent.status) {
                    notifyStatusListeners(newParent.status)
                }
                _status = null
                _value = null
            }
        }

    private var _status: AnimationStatus? = null
    override val status: AnimationStatus
        get() = _parent?.status ?: _status!!

    private var _value: Double? = null
    override val value: Double
        get() = _parent?.value ?: _value!!

    init {
        _parent = animation
        if (_parent == null) {
            _status = AnimationStatus.DISMISSED
            _value = 0.0
        }
    }

    override fun didStartListening() {
        _parent?.addListener(notifyListeners)
        _parent?.addStatusListener(notifyStatusListeners)
    }

    override fun didStopListening() {
        _parent?.removeListener(notifyListeners)
        _parent?.removeStatusListener(notifyStatusListeners)
    }

    override fun toString(): String {
        if (parent == null)
            return "${runtimeType()}(null; ${super.toStringDetails()} " +
                    "${value.toStringAsFixed(3)})"
        return "$parent\u27A9${runtimeType()}"
    }
}