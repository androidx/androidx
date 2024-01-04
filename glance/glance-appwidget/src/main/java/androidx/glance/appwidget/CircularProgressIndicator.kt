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

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.unit.ColorProvider

/**
 * Adds a circular progress indicator view to the glance view.
 *
 * @param modifier the modifier to apply to the progress bar
 * @param color The color of the progress indicator.
 */
@Composable
fun CircularProgressIndicator(
    modifier: GlanceModifier = GlanceModifier,
    color: ColorProvider = ProgressIndicatorDefaults.IndicatorColorProvider,
) {
    GlanceNode(
        factory = ::EmittableCircularProgressIndicator,
        update = {
            this.set(modifier) { this.modifier = it }
            this.set(color) { this.color = it }
        }
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableCircularProgressIndicator : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var color: ColorProvider = ProgressIndicatorDefaults.IndicatorColorProvider

    override fun copy(): Emittable = EmittableCircularProgressIndicator().also {
        it.modifier = modifier
        it.color = color
    }

    override fun toString(): String = "EmittableCircularProgressIndicator(" +
        "modifier=$modifier, " +
        "color=$color" +
        ")"
}
