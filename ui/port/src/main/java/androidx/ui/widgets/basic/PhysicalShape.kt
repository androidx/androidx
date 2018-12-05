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
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.painting.Color
import androidx.ui.painting.Path
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.CustomClipper
import androidx.ui.rendering.proxybox.RenderPhysicalShape
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.Widget

/**
 * A widget representing a physical layer that clips its children to a path.
 *
 * Physical layers cast shadows based on an [elevation] which is nominally in
 * logical pixels, coming vertically out of the rendering surface.
 *
 * [PhysicalModel] does the same but only supports shapes that can be expressed
 * as rectangles with rounded corners.
 *
 * See also:
 *
 *  * [ShapeBorderClipper], which converts a [ShapeBorder] to a [CustomerClipper], as
 *    needed by this widget.
 */
class PhysicalShape(
    key: Key? = null,
    /**
     * Determines which clip to use.
     *
     * If the path in question is expressed as a [ShapeBorder] subclass,
     * consider using the [ShapeBorderClipper] delegate class to adapt the
     * shape for use with this widget.
     */
    val clipper: CustomClipper<Path>,
    /** The z-coordinate at which to place this physical object. */
    val elevation: Double = 0.0,
    /** The background color. */
    val color: Color,
    /** When elevation is non zero the color to use for the shadow color. */
    val shadowColor: Color = Color(0xFF000000.toInt()),
    child: Widget
) : SingleChildRenderObjectWidget(key, child) {

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderPhysicalShape(
            clipper = clipper,
            elevation = elevation,
            color = color,
            shadowColor = shadowColor
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        (renderObject as RenderPhysicalShape).apply {
            clipper = clipper
            elevation = elevation
            color = color
            shadowColor = shadowColor
        }
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("clipper", clipper))
        properties.add(DoubleProperty.create("elevation", elevation))
        properties.add(DiagnosticsProperty.create("color", color))
        properties.add(DiagnosticsProperty.create("shadowColor", shadowColor))
    }
}