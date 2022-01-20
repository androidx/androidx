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

package androidx.glance.appwidget

import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode

/**
 * Adds a determinate linear progress indicator view to the glance view.
 *
 * @param progress of this progress indicator, where 0.0 represents no progress and 1.0 represents full progress
 * @param modifier the modifier to apply to the progress bar
 */
@Composable
public fun LinearProgressIndicator(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    GlanceNode(
        factory = ::EmittableLinearProgressIndicator,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(progress) { this.progress = it }
        }
    )
}

/**
 * Adds an indeterminate linear progress indicator view to the glance view.
 *
 * @param modifier the modifier to apply to the progress bar
 */
@Composable
public fun LinearProgressIndicator(
    modifier: GlanceModifier = GlanceModifier,
) {
    GlanceNode(
        factory = ::EmittableLinearProgressIndicator,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(true) { this.indeterminate = it }
          }
    )
}

internal class EmittableLinearProgressIndicator : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var progress: Float = 0.0f
    var indeterminate: Boolean = false

    override fun toString(): String = "EmittableLinearProgressIndicator(" +
        "modifier=$modifier, " +
        "progress=$progress, " +
        "indeterminate=$indeterminate " +
        ")"
}
