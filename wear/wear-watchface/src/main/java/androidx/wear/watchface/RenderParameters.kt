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
public enum class DrawMode {
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
public enum class LayerMode {
    /** This layer should be rendered normally. */
    DRAW,

    /**
     * This layer should be rendered with highlighting (used by the editor). See also
     * [RenderParameters.highlightedComplicationId] for use in combination with
     * [Layer.COMPLICATIONS].
     */
    DRAW_HIGHLIGHTED,

    /** This layer should not be drawn. */
    HIDE
}

/** Used to parameterize watch face rendering. */
public class RenderParameters constructor(
    /** The overall drawing parameters based on system state. */
    public val drawMode: DrawMode,

    /**
     * Parameters for rendering individual layers. Generally these will all be [LayerMode#DRAW]
     * in normal operation, but the editor may make more complicated requests which need to be
     * honored to function properly.
     */
    public val layerParameters: Map<Layer, LayerMode>,

    /**
     * Optional parameter which non null specifies that a particular complication, rather than all
     * complications, should be highlighted when [Layer.COMPLICATIONS] is
     * [LayerMode.DRAW_HIGHLIGHTED].
     */
    @SuppressWarnings("AutoBoxing")
    @get:SuppressWarnings("AutoBoxing")
    public val highlightedComplicationId: Int?
) {
    public companion object {
        /** A layerParameters map where all Layers have [LayerMode.DRAW]. */
        @JvmField
        public val DRAW_ALL_LAYERS: Map<Layer, LayerMode> =
            Layer.values().associateBy({ it }, { LayerMode.DRAW })

        /** Default RenderParameters which draws everything in interactive mode. */
        @JvmField
        public val DEFAULT_INTERACTIVE: RenderParameters =
            RenderParameters(DrawMode.INTERACTIVE, DRAW_ALL_LAYERS, null)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(wireFormat: RenderParametersWireFormat) : this(
        DrawMode.values()[wireFormat.drawMode],
        wireFormat.layerParameters.associateBy(
            { Layer.values()[it.layer] },
            { LayerMode.values()[it.layerMode] }
        ),
        wireFormat.highlightedComplicationId
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): RenderParametersWireFormat = RenderParametersWireFormat(
        drawMode.ordinal,
        layerParameters.map {
            RenderParametersWireFormat.LayerParameterWireFormat(
                it.key.ordinal,
                it.value.ordinal
            )
        },
        highlightedComplicationId
    )
}