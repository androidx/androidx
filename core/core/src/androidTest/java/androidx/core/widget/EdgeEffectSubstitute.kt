/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.widget

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.widget.EdgeEffect
import kotlin.math.abs

/**
 * This class can be used as a simple substitute for EdgeEffect. When animations are disabled,
 * stretch EdgeEffect doesn't work as expected. This is an implementation of
 * EdgeEffect that supports stretch to the minimum necessary to make tests work.
 */
class EdgeEffectSubstitute(context: Context) : EdgeEffect(context) {
    var width = 0
    var height = 0

    var currentDistance = 0f
    var state = State.Idle

    var absorbed = 0

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun isFinished(): Boolean = state == State.Idle

    override fun finish() {
        currentDistance = 0f
        state = State.Idle
    }

    override fun onPull(deltaDistance: Float) {
        onPull(deltaDistance, 0.5f)
    }

    override fun onPull(deltaDistance: Float, displacement: Float) {
        currentDistance = (currentDistance + deltaDistance).coerceIn(0f, 1f)
        state = if (distance == 0f) {
            State.Idle
        } else {
            State.Pull
        }
    }

    override fun onPullDistance(deltaDistance: Float, displacement: Float): Float {
        val distance = currentDistance
        onPull(deltaDistance, displacement)
        return currentDistance - distance
    }

    override fun getDistance(): Float {
        return currentDistance
    }

    override fun onRelease() {
        state = if (distance == 0f) {
            State.Idle
        } else {
            State.Animating
        }
    }

    override fun onAbsorb(velocity: Int) {
        absorbed = velocity
        state = State.Animating
    }

    override fun setColor(color: Int) {
    }

    override fun setBlendMode(blendmode: BlendMode?) {
    }

    override fun getColor(): Int = 0

    override fun getBlendMode(): BlendMode? {
        return null
    }

    override fun draw(canvas: Canvas): Boolean {
        if (state == State.Idle) {
            return false
        }
        if (absorbed != 0) {
            currentDistance = (distance + (absorbed / height)).coerceIn(0f, 1f)
            absorbed /= 4
        }
        if (currentDistance != 0f && state != State.Pull) {
            currentDistance *= 0.75f
            if (abs(currentDistance) < 0.01f) {
                currentDistance = 0f
                state = State.Idle
            }
        } else {
            state = State.Idle
        }
        return state != State.Idle
    }

    override fun getMaxHeight(): Int = height

    enum class State {
        Idle,
        Pull,
        Animating
    }
}