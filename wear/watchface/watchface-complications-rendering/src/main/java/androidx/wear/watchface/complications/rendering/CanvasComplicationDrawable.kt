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

package androidx.wear.watchface.complications.rendering

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.CanvasComplication
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime

/**
 * A complication rendered with [ComplicationDrawable] which renders complicationSlots in a material
 * design style. This renderer can't be shared by multiple complicationSlots.
 *
 * @param drawable The [ComplicationDrawable] to render with.
 * @param watchState The watch's [WatchState] which contains details pertaining to (low-bit) ambient
 * mode and burn in protection needed to render correctly.
 * @param invalidateCallback The [CanvasComplication.InvalidateCallback] associated with which can
 * be used to request screen redrawing and to report updates
 */
public open class CanvasComplicationDrawable
@SuppressWarnings("ExecutorRegistration") // invalidateCallback is owned by the library and
// the callback is thread safe.
constructor(
    drawable: ComplicationDrawable,
    private val watchState: WatchState,
    private val invalidateCallback: CanvasComplication.InvalidateCallback
) : CanvasComplication {

    internal companion object {
        // Complications are highlighted when tapped and after this delay the highlight is removed.
        internal const val COMPLICATION_HIGHLIGHT_DURATION_MS = 300L

        internal const val EXPANSION_DP = 6.0f
        internal const val STROKE_WIDTH_DP = 3.0f
    }

    private val complicationHighlightRenderer by lazy {
        ComplicationHighlightRenderer(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                EXPANSION_DP,
                Resources.getSystem().displayMetrics
            ),

            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                STROKE_WIDTH_DP,
                Resources.getSystem().displayMetrics
            )
        )
    }

    init {
        drawable.callback = object : Drawable.Callback {
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {}

            @SuppressLint("SyntheticAccessor")
            override fun invalidateDrawable(who: Drawable) {
                invalidateCallback.onInvalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
        }
    }

    /** The [ComplicationDrawable] to render with. */
    public var drawable: ComplicationDrawable = drawable
        set(value) {
            // Copy the ComplicationData otherwise the complication will be blank until the next
            // update.
            value.setComplicationData(field.complicationData, false)
            field = value
            value.isLowBitAmbient = watchState.hasLowBitAmbient
            value.isBurnInProtectionOn = watchState.hasBurnInProtection
        }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters,
        slotId: Int
    ) {
        if (!renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS)) {
            return
        }

        drawable.isInAmbientMode = renderParameters.drawMode == DrawMode.AMBIENT
        drawable.bounds = bounds
        drawable.currentTime = zonedDateTime.toInstant()
        drawable.isHighlighted = renderParameters.lastComplicationTapDownEvents[slotId]?.let {
            val startTime = it.tapTime.toEpochMilli()
            val endTime = it.tapTime.toEpochMilli() + COMPLICATION_HIGHLIGHT_DURATION_MS
            zonedDateTime.toInstant().toEpochMilli() in startTime until endTime
        } ?: false
        drawable.draw(canvas)
    }

    override fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        boundsType: Int,
        zonedDateTime: ZonedDateTime,
        @ColorInt color: Int
    ) {
        if (boundsType == ComplicationSlotBoundsType.ROUND_RECT) {
            complicationHighlightRenderer.drawComplicationHighlight(
                canvas,
                bounds,
                color
            )
        }
    }

    private var _data: ComplicationData = NoDataComplicationData()

    /** Returns the [ComplicationData] to render with. This defaults to [NoDataComplicationData]. */
    override fun getData(): ComplicationData = _data

    /**
     * Updates the [ComplicationData] used for rendering and loads any [Drawable]s within the
     * [complicationData].
     *
     * @param complicationData The new [ComplicationData] for which any [Drawable]s should be loaded
     * @param loadDrawablesAsynchronous Whether any [Drawable]s within [complicationData] should be
     * loaded asynchronously or not. If they are loaded asynchronously then upon completion,
     * [ComplicationDrawable.setComplicationData] will call [Drawable.Callback.invalidateDrawable]
     * registered in our init section above, which invalidates the attachedComplication and
     * ultimately the watch face.
     */
    @CallSuper
    override fun loadData(
        complicationData: ComplicationData,
        loadDrawablesAsynchronous: Boolean
    ): Unit = TraceEvent("CanvasComplicationDrawable.setIdAndData").use {
        _data = complicationData
        drawable.setComplicationData(
            complicationData,
            loadDrawablesAsynchronous
        )
    }
}
