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

package androidx.compose.foundation

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.ViewConfiguration
import android.widget.EdgeEffect
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

internal object EdgeEffectCompat {

    fun create(context: Context): EdgeEffect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.create(context, null)
        } else {
            GlowEdgeEffectCompat(context)
        }
    }

    fun EdgeEffect.onPullDistanceCompat(deltaDistance: Float, displacement: Float): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Api31Impl.onPullDistance(this, deltaDistance, displacement)
        }
        this.onPull(deltaDistance, displacement)
        return deltaDistance
    }

    fun EdgeEffect.onAbsorbCompat(velocity: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return this.onAbsorb(velocity)
        } else if (this.isFinished) {
            // Only absorb the glow effect if it is not active (finished) - dragging to start a glow
            // and then releasing shouldn't add to the existing glow, it should just decay.
            this.onAbsorb(velocity)
        }
    }

    /**
     * When relaxing a stretch with velocity (fling), if the velocity would fully relax the stretch
     * with some remaining velocity, instead of absorbing we want to propagate the velocity and
     * relax the overscroll as part of scroll, called within the fling - so for this case we do
     * nothing and consume no velocity.
     *
     * If the velocity is not enough to fully relax the stretch, then we absorb it and consume all
     * the velocity.
     *
     * @param velocity to absorb if needed
     * @param edgeEffectLength the main axis bounds for this edge effect, needed to transform the
     *   relative distance from [distanceCompat] into the absolute distance
     * @return how much velocity was consumed
     */
    fun EdgeEffect.absorbToRelaxIfNeeded(
        velocity: Float,
        edgeEffectLength: Float,
        density: Density
    ): Float {
        val flingDistance = flingDistance(density, velocity)
        val actualDistance = distanceCompat * edgeEffectLength
        return if (flingDistance <= actualDistance) {
            onAbsorbCompat(velocity.roundToInt())
            // Consume all velocity when absorbing
            velocity
        } else {
            // Consume nothing, we will relax the stretch in applyToScroll
            0f
        }
    }

    /**
     * Used for calls to [EdgeEffect.onRelease] that happen because of scroll delta in the opposite
     * direction to the overscroll. See [GlowEdgeEffectCompat].
     */
    fun EdgeEffect.onReleaseWithOppositeDelta(delta: Float) {
        if (this is GlowEdgeEffectCompat) {
            releaseWithOppositeDelta(delta)
        } else {
            onRelease()
        }
    }

    val EdgeEffect.distanceCompat: Float
        get() {
            return if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
                Api31Impl.getDistance(this)
            } else 0f
        }
}

/**
 * Compat class to work around a framework issue (b/242864658) - small negative deltas that release
 * an overscroll followed by positive deltas cause the glow overscroll effect to instantly
 * disappear. This can happen when you pull the overscroll, and keep it there - small fluctuations
 * in the pointer position can cause these small negative deltas, even though on average it is not
 * really moving. To workaround this we only release the overscroll if the cumulative negative
 * deltas are larger than a minimum value - this should catch the majority of cases.
 */
private class GlowEdgeEffectCompat(context: Context) : EdgeEffect(context) {
    // Minimum distance in the opposite scroll direction to trigger a release
    private val oppositeReleaseDeltaThreshold = with(Density(context)) { 1.dp.toPx() }
    private var oppositeReleaseDelta = 0f

    override fun onPull(deltaDistance: Float, displacement: Float) {
        oppositeReleaseDelta = 0f
        super.onPull(deltaDistance, displacement)
    }

    override fun onPull(deltaDistance: Float) {
        oppositeReleaseDelta = 0f
        super.onPull(deltaDistance)
    }

    override fun onRelease() {
        oppositeReleaseDelta = 0f
        super.onRelease()
    }

    override fun onAbsorb(velocity: Int) {
        oppositeReleaseDelta = 0f
        super.onAbsorb(velocity)
    }

    /**
     * Increments the current cumulative delta, and calls [onRelease] if it is greater than
     * [oppositeReleaseDeltaThreshold].
     */
    fun releaseWithOppositeDelta(delta: Float) {
        oppositeReleaseDelta += delta
        if (abs(oppositeReleaseDelta) > oppositeReleaseDeltaThreshold) {
            onRelease()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private object Api31Impl {
    fun create(context: Context, attrs: AttributeSet?): EdgeEffect {
        return try {
            EdgeEffect(context, attrs)
        } catch (t: Throwable) {
            EdgeEffect(context) // Old preview release
        }
    }

    fun onPullDistance(edgeEffect: EdgeEffect, deltaDistance: Float, displacement: Float): Float {
        return try {
            edgeEffect.onPullDistance(deltaDistance, displacement)
        } catch (t: Throwable) {
            edgeEffect.onPull(deltaDistance, displacement) // Old preview release
            0f
        }
    }

    fun getDistance(edgeEffect: EdgeEffect): Float {
        return try {
            edgeEffect.getDistance()
        } catch (t: Throwable) {
            0f // Old preview release
        }
    }
}

// These constants are copied from the Android spline decay rate
private const val Inflection = 0.35f // Tension lines cross at (Inflection, 1)
private val PlatformFlingScrollFriction = ViewConfiguration.getScrollFriction()
private const val GravityEarth = 9.80665f
private const val InchesPerMeter = 39.37f
private val DecelerationRate = ln(0.78) / ln(0.9)
private val DecelMinusOne = DecelerationRate - 1.0

/**
 * Copied from OverScroller, this returns the distance that a fling with the given velocity will go.
 *
 * @return The absolute distance that will be traveled by a fling of the given velocity
 */
private fun flingDistance(density: Density, velocity: Float): Float {
    val magicPhysicalCoefficient =
        (GravityEarth * InchesPerMeter * density.density * 160f * 0.84f).toDouble()
    val l =
        ln(Inflection * abs(velocity) / (PlatformFlingScrollFriction * magicPhysicalCoefficient))
    val distance =
        PlatformFlingScrollFriction *
            magicPhysicalCoefficient *
            exp(DecelerationRate / DecelMinusOne * l)
    return distance.toFloat()
}
