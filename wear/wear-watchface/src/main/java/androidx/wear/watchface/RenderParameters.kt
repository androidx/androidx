/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface

import androidx.annotation.RestrictTo
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.style.Layer

/* Used to parameterize watch face drawing based on the current system state. */
enum class DrawMode {
    /** This mode is used when the user is interacting with the watch face. */
    INTERACTIVE,

    /**
     * This mode is used when the user is interacting with the watch face but the battery is
     * low, the watch face should render fewer pixels, ideally with darker colors.
     */
    LOW_BATTERY_INTERACTIVE,

    /**
     * This mode is used when there's an interruption filter. The watch face should look muted.
     */
    MUTE,

    /**
     * In this mode as few pixels as possible should be turned on, ideally with darker colors.
     */
    AMBIENT
}

/** Used to parameterize per layer drawing. */
enum class LayerMode {
    /** This layer should be rendered normally. */
    DRAW,

    /** This layer should be rendered with highlighting (used by the editor) */
    DRAW_HIGHLIGHTED,

    /** This layer should not be drawn. */
    HIDE
}

/** Used to parameterize watch face rendering. */
class RenderParameters(
    /** The overall drawing parameters based on system state. */
    val drawMode: DrawMode,

    /**
     * Parameters for rendering individual layers. Generally these will all be [LayerMode#DRAW]
     * in normal operation, but the editor may make more complicated requests which need to be
     * honored to function properly.
     */
    val layerParameters: Map<Layer, LayerMode>
) {
    companion object {
        /** A layerParameters map where all Layers have [LayerMode.DRAW]. */
        val DRAW_ALL_LAYERS = Layer.values().associateBy({ it }, { LayerMode.DRAW })

        /** Default RenderParameters which draws everything in interactive mode. */
        val DEFAULT_INTERACTIVE = RenderParameters(DrawMode.INTERACTIVE, DRAW_ALL_LAYERS)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    constructor(wireFormat: RenderParametersWireFormat) : this(
        DrawMode.values()[wireFormat.drawMode],
        wireFormat.layerParameters.associateBy(
            { Layer.values()[it.layer] },
            { LayerMode.values()[it.layerMode] }
        )
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    fun toWireFormat() = RenderParametersWireFormat(
        drawMode.ordinal, layerParameters.map {
            RenderParametersWireFormat.LayerParameterWireFormat(
                it.key.ordinal,
                it.value.ordinal
            )
        }
    )
}