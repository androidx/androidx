/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.fragment.app

import android.graphics.Rect
import android.transition.Transition
import android.transition.TransitionValues
import android.view.View
import android.view.ViewGroup

/**
 * A transition that tracks which targets are applied to it. It will assume any target that it
 * applies to will have differences between the start and end state, regardless of the differences
 * that actually exist. In other words, it doesn't actually check any size or position differences
 * or any other property of the view. It just records the difference.
 *
 * Both start and end value Views are recorded, but no actual animation is created.
 */
class TrackingTransition : Transition(), TargetTracking {
    override val enteringTargets = mutableListOf<View>()
    override val exitingTargets = mutableListOf<View>()
    override val capturedEpicenter: Rect = Rect()

    override fun getTransitionProperties(): Array<String> {
        return PROPS
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        transitionValues.values[PROP] = 0
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        transitionValues.values[PROP] = 1
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?,
    ) =
        null.also {
            if (startValues != null) {
                exitingTargets.add(startValues.view)
            }
            if (endValues != null) {
                enteringTargets.add(endValues.view)
            }
            if (epicenter != null) {
                capturedEpicenter.set(Rect(epicenter))
            }
        }

    override fun clearTargets() {
        enteringTargets.clear()
        exitingTargets.clear()
        capturedEpicenter.set(Rect())
    }

    companion object {
        private val PROP = "tracking:prop"
        private val PROPS = arrayOf(PROP)
    }
}
