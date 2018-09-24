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

package androidx.ui.rendering.layer

import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty

/**
 * A composite layer that clips its children using a rectangle.
 *
 * When debugging, setting [debugDisableClipLayers] to true will cause this
 * layer to be skipped (directly replaced by its children). This can be helpful
 * to track down the cause of performance problems.
 */

/**
 * The constructor taks the rectangle to clip in the parent's coordinate system.
 *
 * The scene must be explicitly recomposited after this property is changed
 * (as described at [Layer]).
 */
class ClipRectLayer(private val clipRect: Rect) : ContainerLayer() {

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        var enabled = true
        // TODO(migration/Mihai)
//        assert({
//            enabled = !debugDisableClipLayers;
//            return true;
//        });
        if (enabled) {
            builder.pushClipRect(clipRect.shift(layerOffset))
        }
        addChildrenToScene(builder, layerOffset)
        if (enabled)
            builder.pop()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("clipRect", clipRect))
    }
}