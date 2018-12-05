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

package androidx.ui.widgets.implicitanimation

import androidx.ui.animation.Curve
import androidx.ui.animation.Curves
import androidx.ui.core.Duration
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.IntProperty
import androidx.ui.widgets.framework.StatefulWidget

/**
 * An abstract widget for building widgets that gradually change their
 * values over a period of time.
 *
 * Subclasses' States must provide a way to visit the subclass's relevant
 * fields to animate. [ImplicitlyAnimatedWidget] will then automatically
 * interpolate and animate those fields using the provided duration and
 * curve when those fields change.
 */
abstract class ImplicitlyAnimatedWidget(
    key: Key? = null,
    /** The curve to apply when animating the parameters of this container. */
    val curve: Curve = Curves.linear,
    /** The duration over which to animate the parameters of this container. */
    val duration: Duration
) : StatefulWidget(key) {

    abstract override fun createState(): AnimatedWidgetBaseState<out ImplicitlyAnimatedWidget>

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(IntProperty("duration", duration.inMilliseconds.toInt(), unit = "ms"))
    }
}
