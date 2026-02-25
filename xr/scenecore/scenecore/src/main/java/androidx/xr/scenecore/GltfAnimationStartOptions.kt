/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration

/**
 * Animation options for starting a [GltfAnimation].
 *
 * @property shouldLoop Whether the animation plays in a loop.
 * @property speed The playback rate. Default is 1.0.
 * * **1.0:** Normal speed.
 * * **> 1.0:** Faster playback.
 * * **> 0.0 and < 1.0:** Slower playback (e.g., 0.5 is half speed).
 * * **0.0:** Freezes the animation at the current frame while keeping it active.
 * * **< 0.0:** Plays the animation in reverse.
 *
 * @property seekStartTime The start offset. Default is [Duration.ZERO].
 *
 * This value defines the **starting boundary** of the playback window. The effective playback
 * window is the range between [seekStartTime] and the animation's total duration.
 *
 * **Constraints:**
 * * Must be greater than or equal to zero.
 * * If [seekStartTime] is greater than the duration, it is clamped to the duration (resulting in a
 *   zero-length playback window).
 *
 * **Note on Reverse Playback:** If [speed] is negative and [shouldLoop] is false, the animation
 * stops when it reaches [seekStartTime]. To play in reverse, the application must explicitly call
 * [GltfAnimation.seekTo] to set the playhead to a valid position between [seekStartTime] and the
 * duration.
 *
 * @throws IllegalArgumentException if [seekStartTime] is negative.
 */
@RequiresApi(Build.VERSION_CODES.O)
public class GltfAnimationStartOptions
@JvmOverloads
constructor(
    @get:JvmName("shouldLoop") public val shouldLoop: Boolean = false,
    public val speed: Float = 1.0f,
    public val seekStartTime: Duration = Duration.ZERO,
) {
    init {
        require(!seekStartTime.isNegative) { "seekStartTime must be non-negative." }
    }

    /** Creates a copy of the [GltfAnimationStartOptions] object with the given values. */
    @JvmOverloads
    public fun copy(
        shouldLoop: Boolean = this.shouldLoop,
        speed: Float = this.speed,
        seekStartTime: Duration = this.seekStartTime,
    ): GltfAnimationStartOptions {
        return GltfAnimationStartOptions(shouldLoop, speed, seekStartTime)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GltfAnimationStartOptions) return false

        if (shouldLoop != other.shouldLoop) return false
        // Use toBits() to correctly handle Float.NaN comparisons
        if (speed.toBits() != other.speed.toBits()) return false
        if (seekStartTime != other.seekStartTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shouldLoop.hashCode()
        result = 31 * result + speed.toBits().hashCode()
        result = 31 * result + seekStartTime.hashCode()
        return result
    }

    override fun toString(): String {
        return "GltfAnimationStartOptions(shouldLoop=$shouldLoop, speed=$speed, seekStartTime=${seekStartTime.toMillis()} ms)"
    }
}
