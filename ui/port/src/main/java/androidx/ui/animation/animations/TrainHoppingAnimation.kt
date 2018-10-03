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

import androidx.ui.VoidCallback
import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationEagerListenerMixin
import androidx.ui.animation.AnimationStatus
import androidx.ui.runtimeType

private enum class TrainHoppingMode {
    MINIMIZE,
    MAXIMIZE
}

/**
 * This animation starts by proxying one animation, but can be given a
 * second animation. When their times cross (either because the second is
 * going in the opposite direction, or because the one overtakes the other),
 * the animation hops over to proxying the second animation, and the second
 * animation becomes the new "first" performance.
 *
 * Since this object must track the two animations even when it has no
 * listeners of its own, instead of shutting down when all its listeners are
 * removed, it exposes a [dispose()] method. Call this method to shut this
 * object down.
 */
class TrainHoppingAnimation(
    /** The animation that is current driving this animation. */
    currentTrain: Animation<Double>,
    private var nextTrain: Animation<Double>?,
    /** Called when this animation switches to be driven by a different animation. */
    private val onSwitchedTrain: VoidCallback? = null
) : AnimationEagerListenerMixin<Double>() {

    var currentTrain: Animation<Double> = currentTrain
        private set

    private var mode: TrainHoppingMode? = null

    private var lastStatus: AnimationStatus? = null
    private val statusChangeHandler = { status: AnimationStatus ->
        if (status != lastStatus) {
            notifyListeners()
            lastStatus = status
        }
        assert(lastStatus != null)
    }

    private var lastValue: Double? = null
    private val valueChangeHandler = { onValueChanged() }

    private fun onValueChanged() {
        var hop = false
        val finalNextTrain = nextTrain
        if (finalNextTrain != null) {
            hop = when (mode!!) {
                TrainHoppingMode.MINIMIZE -> finalNextTrain.value <= currentTrain.value
                TrainHoppingMode.MAXIMIZE -> finalNextTrain.value >= currentTrain.value
            }
            if (hop) {
                currentTrain.removeStatusListener(statusChangeHandler)
                currentTrain.removeListener(valueChangeHandler)
                currentTrain = finalNextTrain
                nextTrain = null
                currentTrain.addStatusListener(statusChangeHandler)
                statusChangeHandler(currentTrain.status)
            }
        }
        val newValue = value
        if (newValue != lastValue) {
            notifyListeners()
            lastValue = newValue
        }
        assert(lastValue != null)
        if (hop && onSwitchedTrain != null) {
            onSwitchedTrain.invoke()
        }
    }

    init {
        if (nextTrain != null) {
            if (this.currentTrain.value > nextTrain!!.value) {
                mode = TrainHoppingMode.MAXIMIZE
            } else {
                mode = TrainHoppingMode.MINIMIZE
                if (this.currentTrain.value == nextTrain!!.value) {
                    this.currentTrain = nextTrain!!
                    nextTrain = null
                }
            }
        }
        this.currentTrain.addStatusListener(statusChangeHandler)
        this.currentTrain.addListener(valueChangeHandler)
        nextTrain?.addListener(valueChangeHandler)
        assert(mode != null)
    }

    override val status: AnimationStatus
        get() = currentTrain.status

    override val value: Double
        get() = currentTrain.value

    /**
     * Frees all the resources used by this performance.
     * After this is called, this object is no longer usable.
     */
    override fun dispose() {
        currentTrain.removeStatusListener(statusChangeHandler)
        currentTrain.removeListener(valueChangeHandler)
        // TODO(Migration|Andrey) Not sure it worth making currentTrain nullable here
//        currentTrain = null;
        nextTrain?.removeListener(valueChangeHandler)
        nextTrain = null
        super.dispose()
    }

    override fun toString(): String {
        if (nextTrain != null)
            return "$currentTrain\u27A9${runtimeType()}(next: $nextTrain)"
        return "$currentTrain\u27A9${runtimeType()}(no next)"
    }
}