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

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.wear.watchface.data.LayerParameterWireFormat
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
     * Used by editors, this layer should be rendered with a outline or similar graphical
     * highlighting with [RenderParameters.outlineTint]. See also
     * [RenderParameters.selectedComplicationId] for use in combination with
     * [Layer.COMPLICATIONS].
     *
     * Note a highlight for background complications won't be drawn since this would typically be
     * off screen.
     */
    DRAW_OUTLINED,

    /** This layer should not be drawn. */
    HIDE
}

/**
 * Used to parameterize watch face rendering.
 *
 * @param drawMode The overall drawing parameters based on system state.
 * @param layerParameters Parameters for rendering individual layers. Generally these will all be
 *     [LayerMode#DRAW] in normal operation, but the editor may make more complicated requests
 *     which need to be honored to function properly.
 * @param selectedComplicationId Optional parameter which if non null specifies that a particular
 *     complication should be drawn with a special highlight to indicate it's been selected.
 * @param outlineTint Specifies the tint should be used with [LayerMode.DRAW_OUTLINED]
 */
public class RenderParameters constructor(
    public val drawMode: DrawMode,
    public val layerParameters: Map<Layer, LayerMode>,
    @SuppressWarnings("AutoBoxing")
    @get:SuppressWarnings("AutoBoxing")
    public val selectedComplicationId: Int?,
    @ColorInt
    @get:ColorInt
    public val outlineTint: Int
) {
    /**
     * Constructs [RenderParameters] without an explicit [outlineTint]. This constructor doesn't
     * support [LayerMode.DRAW_OUTLINED].
     *
     * @param drawMode The overall drawing parameters based on system state.
     * @param layerParameters Parameters for rendering individual layers. Generally these will all
     *     be [LayerMode#DRAW] in normal operation, but the editor may make more complicated
     *     requests which need to be honored to function properly.
     * @param selectedComplicationId Optional parameter which if non null specifies that a
     *     particular complication should be drawn with a special highlight to indicate it's been
     *     selected.
     */
    public constructor(
        drawMode: DrawMode,
        layerParameters: Map<Layer, LayerMode>,
        @SuppressWarnings("AutoBoxing")
        selectedComplicationId: Int?,
    ) : this(drawMode, layerParameters, selectedComplicationId, Color.RED) {
        for (layerMode in layerParameters.values) {
            require(layerMode != LayerMode.DRAW_OUTLINED) {
                "LayerMode.DRAW_OUTLINED is not supported by this constructor, use the primary " +
                    "one instead"
            }
        }
    }

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
        wireFormat.selectedComplicationId,
        wireFormat.outlineTint
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): RenderParametersWireFormat = RenderParametersWireFormat(
        drawMode.ordinal,
        layerParameters.map {
            LayerParameterWireFormat(
                it.key.ordinal,
                it.value.ordinal
            )
        },
        selectedComplicationId,
        outlineTint
    )

    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("RenderParameters:")
        writer.increaseIndent()
        writer.println("drawMode=${drawMode.name}")
        writer.println("selectedComplicationId=$selectedComplicationId")
        writer.println("outlineTint=$outlineTint")
        val params = layerParameters.map { "${it.key} -> ${it.value.name}" }.joinToString { it }
        writer.println("layerParameters=[$params]")
        writer.decreaseIndent()
    }
}
