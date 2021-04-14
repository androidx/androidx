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
import androidx.wear.watchface.RenderParameters.HighlightLayer
import androidx.wear.watchface.data.RenderParametersWireFormat
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.watchface.style.UserStyleSetting

/* Used to parameterize watch face drawing based on the current system state. */
public enum class DrawMode {
    /**
     * This mode is used when the user is interacting with the watch face.
     *
     * This is currently the only mode that is supported when editing the watch face.
     */
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

/**
 * Used to parameterize watch face rendering.
 *
 * Watch face rendering is split up in a number of layers: the base layer [WatchFaceLayer.BASE], the
 * complications layer [WatchFaceLayer.COMPLICATIONS], and the layer above the complications
 * [WatchFaceLayer.COMPLICATIONS_OVERLAY]. These layers are always drawn in this order, one on top of
 * the previous one. These are the layers that are used to render the watch face itself.
 *
 * An additional layer, the highlight layer, can be drawn during editing the watch face to highlight
 * different elements of the watch face, namely a set of complications (which may be a single
 * complication) or the area of the watch face that is affected by a single user style setting.
 *
 * The watch face should provide a way to highlight any of the above combination so that its own
 * editor on the one provided by the Wear OS phone app is able to highlight the editable part of the
 * watch face to the user. If a complication of single user style setting is meant to affect the
 * entire watch face, the entire watch face should be highlighted.
 *
 * The watch face layers and highlight layer can be configured independently, so that it is possible
 * to draw only the highlight layer by passing an empty set of [watchFaceLayers].
 *
 * The semantics of rendering different layers is such that if each individual layer is rendered
 * independently and the resulting images are composited with alpha blending, the result is
 * identical to rendering all of the layers in a single request.
 *
 * @param drawMode The overall drawing parameters based on system state.
 * @param watchFaceLayers The parts of the watch face to draw.
 * @param highlightLayer Optional [HighlightLayer] used by editors to visually highlight an
 * aspect of the watch face. Rendered last on top of [watchFaceLayers]. If highlighting isn't needed
 * this will be `null`.
 */
public class RenderParameters @JvmOverloads constructor(
    public val drawMode: DrawMode,
    public val watchFaceLayers: Set<WatchFaceLayer>,
    public val highlightLayer: HighlightLayer? = null
) {
    init {
        require(watchFaceLayers.isNotEmpty() || highlightLayer != null) {
            "Either watchFaceLayers must be non empty or " +
                "renderParameters.highlightLayer must be non-null."
        }
    }

    /** An element of the watch face to highlight. */
    public sealed class HighlightedElement {
        /** All [Complication]s will be highlighted. */
        public object AllComplications : HighlightedElement()

        /** A single [Complication] with the specified [id] will be highlighted. */
        public class Complication(public val id: Int) : HighlightedElement()

        /**
         * A [UserStyleSetting] to highlight. E.g. for a setting that controls watch hands, the
         * location of the hands should be highlighted.
         *
         * @param id The [UserStyleSetting.Id] of the [UserStyleSetting] to highlight.
         */
        public class UserStyle(public val id: UserStyleSetting.Id) : HighlightedElement()
    }

    /**
     * The definition of what to include in the highlight layer.
     *
     * The highlight layer is used by editors to show the parts of the watch face affected by a
     * setting. E.g. a set of complications or a user style setting.
     *
     * The highlight layer is composited on top of the watch face with an alpha blend. It should
     * be cleared with [backgroundTint]. The solid or semi-transparent outline around
     * [highlightedElement] should be rendered using the provided [highlightTint]. The highlighted
     * element itself should be rendered as fully transparent (an alpha value of 0) to leave it
     * unaffected.
     *
     * @param highlightedElement The [HighlightedElement] to draw highlighted with [highlightTint].
     * @param highlightTint The highlight tint to apply to [highlightedElement].
     * @param backgroundTint The tint to apply to everything other than [highlightedElement].
     * Typically this will darken everything else to increase contrast.
     */
    public class HighlightLayer(
        public val highlightedElement: HighlightedElement,

        @ColorInt
        @get:ColorInt
        public val highlightTint: Int,

        @ColorInt
        @get:ColorInt
        public val backgroundTint: Int
    )

    public companion object {
        /** Default RenderParameters which draws everything in interactive mode. */
        @JvmField
        public val DEFAULT_INTERACTIVE: RenderParameters =
            RenderParameters(DrawMode.INTERACTIVE, WatchFaceLayer.ALL_WATCH_FACE_LAYERS, null)
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public constructor(wireFormat: RenderParametersWireFormat) : this(
        DrawMode.values()[wireFormat.drawMode],
        HashSet<WatchFaceLayer>().apply {
            WatchFaceLayer.values().forEachIndexed { index, layer ->
                if (wireFormat.watchFaceLayerSetBitfield and 1.shl(index) != 0) {
                    add(layer)
                }
            }
        },
        when (wireFormat.elementType) {
            RenderParametersWireFormat.ELEMENT_TYPE_NONE -> null

            RenderParametersWireFormat.ELEMENT_TYPE_ALL_COMPLICATIONS -> {
                HighlightLayer(
                    HighlightedElement.AllComplications,
                    wireFormat.highlightTint,
                    wireFormat.backgroundTint
                )
            }

            RenderParametersWireFormat.ELEMENT_TYPE_COMPLICATION -> {
                HighlightLayer(
                    HighlightedElement.Complication(wireFormat.elementComplicationId),
                    wireFormat.highlightTint,
                    wireFormat.backgroundTint
                )
            }

            RenderParametersWireFormat.ELEMENT_TYPE_USER_STYLE -> {
                HighlightLayer(
                    HighlightedElement.UserStyle(
                        UserStyleSetting.Id(wireFormat.elementUserStyleSettingId!!)
                    ),
                    wireFormat.highlightTint,
                    wireFormat.backgroundTint
                )
            }

            else -> null
        }
    )

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun toWireFormat(): RenderParametersWireFormat =
        when (val thingHighlighted = highlightLayer?.highlightedElement) {
            is HighlightedElement.AllComplications -> RenderParametersWireFormat(
                drawMode.ordinal,
                computeLayersBitfield(),
                RenderParametersWireFormat.ELEMENT_TYPE_ALL_COMPLICATIONS,
                0,
                null,
                highlightLayer!!.highlightTint,
                highlightLayer.backgroundTint
            )

            is HighlightedElement.Complication -> RenderParametersWireFormat(
                drawMode.ordinal,
                computeLayersBitfield(),
                RenderParametersWireFormat.ELEMENT_TYPE_COMPLICATION,
                thingHighlighted.id,
                null,
                highlightLayer!!.highlightTint,
                highlightLayer.backgroundTint
            )

            is HighlightedElement.UserStyle -> RenderParametersWireFormat(
                drawMode.ordinal,
                computeLayersBitfield(),
                RenderParametersWireFormat.ELEMENT_TYPE_USER_STYLE,
                0,
                thingHighlighted.id.value,
                highlightLayer!!.highlightTint,
                highlightLayer.backgroundTint
            )

            else -> RenderParametersWireFormat(
                drawMode.ordinal,
                computeLayersBitfield(),
                RenderParametersWireFormat.ELEMENT_TYPE_NONE,
                0,
                null,
                Color.BLACK,
                Color.BLACK
            )
        }

    private fun computeLayersBitfield(): Int {
        var bitfield = 0
        WatchFaceLayer.values().forEachIndexed { index, layer ->
            if (watchFaceLayers.contains(layer)) {
                bitfield += 1.shl(index)
            }
        }
        return bitfield
    }

    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("RenderParameters:")
        writer.increaseIndent()
        writer.println("drawMode=${drawMode.name}")
        writer.println("watchFaceLayers=${watchFaceLayers.joinToString()}")

        highlightLayer?.let {
            writer.println("HighlightLayer:")
            writer.increaseIndent()
            when (it.highlightedElement) {
                is HighlightedElement.AllComplications -> {
                    writer.println("HighlightedElement.AllComplications:")
                }

                is HighlightedElement.Complication -> {
                    writer.println("HighlightedElement.Complication:")
                    writer.increaseIndent()
                    writer.println("id=${it.highlightedElement.id}")
                    writer.decreaseIndent()
                }

                is HighlightedElement.UserStyle -> {
                    writer.println("HighlightedElement.UserStyle:")
                    writer.increaseIndent()
                    writer.println("id=${it.highlightedElement.id}")
                    writer.decreaseIndent()
                }
            }
            writer.println("highlightTint=${it.highlightTint}")
            writer.println("backgroundTint=${it.backgroundTint}")
            writer.decreaseIndent()
        }
        writer.decreaseIndent()
    }
}
