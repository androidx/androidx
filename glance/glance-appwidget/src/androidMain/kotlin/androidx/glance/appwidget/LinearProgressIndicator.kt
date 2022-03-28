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
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Adds a determinate linear progress indicator view to the glance view.
 *
 * @param progress of this progress indicator, where 0.0 represents no progress and 1.0 represents full progress
 * @param modifier the modifier to apply to the progress bar
 * @param color The color of the progress indicator.
 * @param backgroundColor The color of the background behind the indicator, visible when the
 * progress has not reached that area of the overall indicator yet.
 */
@Composable
public fun LinearProgressIndicator(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
    color: ColorProvider = ProgressIndicatorDefaults.IndicatorColorProvider,
    backgroundColor: ColorProvider = ProgressIndicatorDefaults.BackgroundColorProvider
) {
    GlanceNode(
        factory = ::EmittableLinearProgressIndicator,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(progress) { this.progress = it }
            this.set(color) { this.color = it }
            this.set(backgroundColor) { this.backgroundColor = it }
        }
    )
}

/**
 * Adds an indeterminate linear progress indicator view to the glance view.
 *
 * @param modifier the modifier to apply to the progress bar
 * @param color The color of the progress indicator.
 * @param backgroundColor The color of the background behind the indicator, visible when the
 * progress has not reached that area of the overall indicator yet.
 */
@Composable
public fun LinearProgressIndicator(
    modifier: GlanceModifier = GlanceModifier,
    color: ColorProvider = ProgressIndicatorDefaults.IndicatorColorProvider,
    backgroundColor: ColorProvider = ProgressIndicatorDefaults.BackgroundColorProvider
) {
    GlanceNode(
        factory = ::EmittableLinearProgressIndicator,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(true) { this.indeterminate = it }
            this.set(color) { this.color = it }
            this.set(backgroundColor) { this.backgroundColor = it }
          }
    )
}

internal class EmittableLinearProgressIndicator : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var progress: Float = 0.0f
    var indeterminate: Boolean = false
    var color: ColorProvider = ProgressIndicatorDefaults.IndicatorColorProvider
    var backgroundColor: ColorProvider = ProgressIndicatorDefaults.BackgroundColorProvider

    override fun toString(): String = "EmittableLinearProgressIndicator(" +
        "modifier=$modifier, " +
        "progress=$progress, " +
        "indeterminate=$indeterminate, " +
        "color=$color, " +
        "backgroundColor=$backgroundColor" +
        ")"
}

/**
 * Contains the default values used for [LinearProgressIndicator].
 */
public object ProgressIndicatorDefaults {

  /**
   * Default color for [LinearProgressIndicator].
   * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
   */
  private val Color = Color(0xFF6200EE)

  /**
   * Default ColorProvider for the progress indicator in [LinearProgressIndicator].
   */
  val IndicatorColorProvider = ColorProvider(Color)

  /**
   * Default ColorProvider for the background in [LinearProgressIndicator].
   */
  val BackgroundColorProvider = ColorProvider(Color.copy(alpha = 0.24f))
}