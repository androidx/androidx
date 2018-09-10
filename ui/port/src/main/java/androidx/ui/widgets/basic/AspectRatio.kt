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

package androidx.ui.widgets.basic

import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderAspectRatio
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

// /// A widget that attempts to size the child to a specific aspect ratio.
// ///
// /// The widget first tries the largest width permitted by the layout
// /// constraints. The height of the widget is determined by applying the
// /// given aspect ratio to the width, expressed as a ratio of width to height.
// ///
// /// For example, a 16:9 width:height aspect ratio would have a value of
// /// 16.0/9.0. If the maximum width is infinite, the initial width is determined
// /// by applying the aspect ratio to the maximum height.
// ///
// /// Now consider a second example, this time with an aspect ratio of 2.0 and
// /// layout constraints that require the width to be between 0.0 and 100.0 and
// /// the height to be between 0.0 and 100.0. We'll select a width of 100.0 (the
// /// biggest allowed) and a height of 50.0 (to match the aspect ratio).
// ///
// /// In that same situation, if the aspect ratio is 0.5, we'll also select a
// /// width of 100.0 (still the biggest allowed) and we'll attempt to use a height
// /// of 200.0. Unfortunately, that violates the constraints because the child can
// /// be at most 100.0 pixels tall. The widget will then take that value
// /// and apply the aspect ratio again to obtain a width of 50.0. That width is
// /// permitted by the constraints and the child receives a width of 50.0 and a
// /// height of 100.0. If the width were not permitted, the widget would
// /// continue iterating through the constraints. If the widget does not
// /// find a feasible size after consulting each constraint, the widget
// /// will eventually select a size for the child that meets the layout
// /// constraints but fails to meet the aspect ratio constraints.
// ///
// /// See also:
// ///
// ///  * [Align], a widget that aligns its child within itself and optionally
// ///    sizes itself based on the child's size.
// ///  * [ConstrainedBox], a widget that imposes additional constraints on its
// ///    child.
// ///  * [UnconstrainedBox], a container that tries to let its child draw without
// ///    constraints.
// ///  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
class AspectRatio(
    key: Key? = null,
    // /// The aspect ratio to attempt to use.
    // ///
    // /// The aspect ratio is expressed as a ratio of width to height. For example,
    // /// a 16:9 width:height aspect ratio would have a value of 16.0/9.0.
    val aspectRatio: Double,
    child: Widget? = null
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderAspectRatio(aspectRatio = aspectRatio)
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) {
        (renderObject as RenderAspectRatio).aspectRatio = aspectRatio
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DoubleProperty.create("aspectRatio", aspectRatio))
    }
}