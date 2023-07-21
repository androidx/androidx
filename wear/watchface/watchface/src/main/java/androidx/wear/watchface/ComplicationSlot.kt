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

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.wearable.complications.ComplicationData as WireComplicationData
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.wear.watchface.RenderParameters.HighlightedElement
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.data.BoundingArcWireFormat
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import java.lang.Integer.min
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Objects
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for rendering complicationSlots onto a [Canvas]. These should be created by
 * [CanvasComplicationFactory.create]. If state needs to be shared with the [Renderer] that should
 * be set up inside [onRendererCreated].
 */
@JvmDefaultWithCompatibility
public interface CanvasComplication {

    /** Interface for observing when a [CanvasComplication] needs the screen to be redrawn. */
    public interface InvalidateCallback {
        /** Signals that the complication needs to be redrawn. Can be called on any thread. */
        public fun onInvalidate()
    }

    /**
     * Called once on a background thread before any subsequent UI thread rendering to inform the
     * CanvasComplication of the [Renderer] which is useful if they need to share state. Note the
     * [Renderer] is created asynchronously which is why we can't pass it in via
     * [CanvasComplicationFactory.create] as it may not be available at that time.
     */
    @WorkerThread public fun onRendererCreated(renderer: Renderer) {}

    /**
     * Draws the complication defined by [getData] into the canvas with the specified bounds. This
     * will usually be called by user watch face drawing code, but the system may also call it for
     * complication selection UI rendering. The width and height will be the same as that computed
     * by computeBounds but the translation and canvas size may differ.
     *
     * @param canvas The [Canvas] to render into
     * @param bounds A [Rect] describing the bounds of the complication
     * @param zonedDateTime The [ZonedDateTime] to render with
     * @param renderParameters The current [RenderParameters]
     * @param slotId The Id of the [ComplicationSlot] being rendered
     */
    @UiThread
    public fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters,
        slotId: Int
    )

    /**
     * Draws a highlight for a [ComplicationSlotBoundsType.ROUND_RECT] complication. The default
     * implementation does this by drawing a dashed line around the complication, other visual
     * effects may be used if desired.
     *
     * @param canvas The [Canvas] to render into
     * @param bounds A [Rect] describing the bounds of the complication
     * @param boundsType The [ComplicationSlotBoundsType] of the complication
     * @param zonedDateTime The [ZonedDateTime] to render the highlight with
     * @param color The color to render the highlight with
     */
    // TODO(b/230364881): Deprecate this when BoundingArc is no longer experimental.
    public fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        @ComplicationSlotBoundsType boundsType: Int,
        zonedDateTime: ZonedDateTime,
        @ColorInt color: Int
    )

    /**
     * Draws a highlight for a [ComplicationSlotBoundsType.ROUND_RECT] complication. The default
     * implementation does this by drawing a dashed line around the complication, other visual
     * effects may be used if desired.
     *
     * @param canvas The [Canvas] to render into
     * @param bounds A [Rect] describing the bounds of the complication
     * @param boundsType The [ComplicationSlotBoundsType] of the complication
     * @param zonedDateTime The [ZonedDateTime] to render the highlight with
     * @param color The color to render the highlight with
     */
    @ComplicationExperimental
    public fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        @ComplicationSlotBoundsType boundsType: Int,
        zonedDateTime: ZonedDateTime,
        @ColorInt color: Int,
        boundingArc: BoundingArc?
    ) {
        drawHighlight(canvas, bounds, boundsType, zonedDateTime, color)
    }

    /** Returns the [ComplicationData] to render with. */
    public fun getData(): ComplicationData

    /**
     * Sets the [ComplicationData] to render with and loads any [Drawable]s contained within the
     * ComplicationData. You can choose whether this is done synchronously or asynchronously via
     * [loadDrawablesAsynchronous]. When any asynchronous loading has completed
     * [InvalidateCallback.onInvalidate] must be called.
     *
     * @param complicationData The [ComplicationData] to render with
     * @param loadDrawablesAsynchronous Whether or not any drawables should be loaded asynchronously
     */
    public fun loadData(complicationData: ComplicationData, loadDrawablesAsynchronous: Boolean)
}

/** Interface for determining whether a tap hits a complication. */
@JvmDefaultWithCompatibility
public interface ComplicationTapFilter {
    /**
     * Performs a hit test, returning `true` if the supplied coordinates in pixels are within the
     * the provided [complicationSlot] scaled to [screenBounds].
     *
     * @param complicationSlot The [ComplicationSlot] to perform a hit test for.
     * @param screenBounds A [Rect] describing the bounds of the display.
     * @param x The screen space X coordinate in pixels.
     * @param y The screen space Y coordinate in pixels.
     * @param includeMargins Whether or not the margins should be included
     */
    @Suppress("DEPRECATION")
    public fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int,
        includeMargins: Boolean
    ): Boolean = hitTest(complicationSlot, screenBounds, x, y)

    /**
     * Performs a hit test, returning `true` if the supplied coordinates in pixels are within the
     * the provided [complicationSlot] scaled to [screenBounds].
     *
     * @param complicationSlot The [ComplicationSlot] to perform a hit test for.
     * @param screenBounds A [Rect] describing the bounds of the display.
     * @param x The screen space X coordinate in pixels.
     * @param y The screen space Y coordinate in pixels.
     */
    @Deprecated(
        "hitTest without specifying includeMargins is deprecated",
        replaceWith = ReplaceWith("hitTest(ComplicationSlot, Rect, Int, Int, Boolean)")
    )
    public fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int
    ): Boolean = hitTest(complicationSlot, screenBounds, x, y, false)
}

/**
 * Default [ComplicationTapFilter] for [ComplicationSlotBoundsType.ROUND_RECT] complicationSlots.
 */
public class RoundRectComplicationTapFilter : ComplicationTapFilter {
    override fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int,
        includeMargins: Boolean
    ): Boolean = complicationSlot.computeBounds(screenBounds, includeMargins).contains(x, y)
}

/**
 * Default [ComplicationTapFilter] for [ComplicationSlotBoundsType.BACKGROUND] complicationSlots.
 */
public class BackgroundComplicationTapFilter : ComplicationTapFilter {
    override fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int,
        includeMargins: Boolean
    ): Boolean = false
}

@IntDef(
    value =
        [
            ComplicationSlotBoundsType.ROUND_RECT,
            ComplicationSlotBoundsType.BACKGROUND,
            ComplicationSlotBoundsType.EDGE
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class ComplicationSlotBoundsType {
    public companion object {
        /** The default, most complication slots are either circular or rounded rectangles. */
        public const val ROUND_RECT: Int = 0

        /**
         * For a full screen image complication slot drawn behind the watch face. Note you can only
         * have a single background complication slot.
         */
        public const val BACKGROUND: Int = 1

        /** For edge of screen complication slots. */
        public const val EDGE: Int = 2
    }
}

/**
 * In combination with a bounding [Rect], BoundingArc describes the geometry of an edge
 * complication.
 *
 * @property startAngle The staring angle of the arc in degrees (0 degrees = 12 o'clock position).
 * @property totalAngle The total angle of the arc on degrees.
 * @property thickness The thickness of the arc as a fraction of min(boundingRect.width,
 *   boundingRect.height).
 */
@ComplicationExperimental
public class BoundingArc(val startAngle: Float, val totalAngle: Float, @Px val thickness: Float) {
    /**
     * Detects whether the supplied point falls within the edge complication's arc.
     *
     * @param rect The bounding [Rect] of the edge complication
     * @param x The x-coordinate of the point to test in pixels
     * @param y The y-coordinate of the point to test in pixels
     * @return Whether or not the point is within the arc
     */
    fun hitTest(rect: Rect, @Px x: Float, @Px y: Float): Boolean {
        val width = rect.width()
        val height = rect.height()
        val thicknessPx = min(width, height).toDouble() * thickness

        val halfWidth = width.toDouble() * 0.5
        val halfHeight = height.toDouble() * 0.5

        // Rotate to a local coordinate space where the y axis is in the middle of the arc
        var x0 = (x - rect.left).toDouble() - halfWidth
        var y0 = (y - rect.top).toDouble() - halfHeight
        val angle = startAngle + 0.5f * totalAngle
        val rotAngle = -Math.toRadians(angle.toDouble())
        x0 = x0 * cos(rotAngle) - y0 * sin(rotAngle) + halfWidth
        y0 = x0 * sin(rotAngle) + y0 * cos(rotAngle) + halfHeight

        // Copied from WearCurvedTextView...
        val radius2 = min(width, height).toDouble() / 2.0
        val radius1 = radius2 - thicknessPx
        val dx = x0 - (width.toDouble() / 2.0)
        val dy = y0 - (height.toDouble() / 2.0)
        val r2 = dx * dx + dy * dy
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        return Math.toDegrees(atan2(abs(dx), -dy)) < (totalAngle / 2.0)
    }

    override fun toString(): String {
        return "ArcParams(startAngle=$startAngle, totalArcAngle=$totalAngle, " +
            "thickness=$thickness)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoundingArc

        if (startAngle != other.startAngle) return false
        if (totalAngle != other.totalAngle) return false
        if (thickness != other.thickness) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startAngle.hashCode()
        result = 31 * result + totalAngle.hashCode()
        result = 31 * result + thickness.hashCode()
        return result
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun toWireFormat() = BoundingArcWireFormat(startAngle, totalAngle, thickness)
}

/**
 * Represents the slot an individual complication on the screen may go in. The number of
 * ComplicationSlots is fixed (see [ComplicationSlotsManager]) but ComplicationSlots can be enabled
 * or disabled via [UserStyleSetting.ComplicationSlotsUserStyleSetting].
 *
 * Taps on the watch are tested first against each ComplicationSlot's
 * [ComplicationSlotBounds.perComplicationTypeBounds] for the relevant [ComplicationType]. Its
 * assumed that [ComplicationSlotBounds.perComplicationTypeBounds] don't overlap. If no intersection
 * was found then taps are checked against [ComplicationSlotBounds.perComplicationTypeBounds]
 * expanded by [ComplicationSlotBounds.perComplicationTypeMargins]. Expanded bounds can overlap so
 * the [ComplicationSlot] with the lowest id that intersects the coordinates, if any, is selected.
 *
 * @param accessibilityTraversalIndex Used to sort Complications when generating accessibility
 *   content description labels.
 * @param bounds The complication slot's [ComplicationSlotBounds].
 * @param supportedTypes The list of [ComplicationType]s accepted by this complication slot. Used
 *   during complication data source selection, this list should be non-empty.
 * @param defaultPolicy The [DefaultComplicationDataSourcePolicy] which controls the initial
 *   complication data source when the watch face is first installed.
 * @param defaultDataSourceType The default [ComplicationType] for the default complication data
 *   source.
 * @param configExtras Extras to be merged into the Intent sent when invoking the complication data
 *   source chooser activity. This features is intended for OEM watch faces where they have elements
 *   that behave like a complication but are in fact entirely watch face specific.
 * @property id The Watch Face's ID for the complication slot.
 * @property boundsType The [ComplicationSlotBoundsType] of the complication slot.
 * @property canvasComplicationFactory The [CanvasComplicationFactory] used to generate a
 *   [CanvasComplication] for rendering the complication. The factory allows us to decouple
 *   ComplicationSlot from potentially expensive asset loading.
 * @property initiallyEnabled At creation a complication slot is either enabled or disabled. This
 *   can be overridden by a [ComplicationSlotsUserStyleSetting] (see
 *   [ComplicationSlotOverlay.enabled]). Editors need to know the initial state of a complication
 *   slot to predict the effects of making a style change.
 * @property fixedComplicationDataSource Whether or not the complication data source is fixed (i.e.
 *   can't be changed by the user). This is useful for watch faces built around specific
 *   complications.
 * @property tapFilter The [ComplicationTapFilter] used to determine whether or not a tap hit the
 *   complication slot.
 */
public class ComplicationSlot
@ComplicationExperimental
internal constructor(
    public val id: Int,
    accessibilityTraversalIndex: Int,
    @ComplicationSlotBoundsType public val boundsType: Int,
    bounds: ComplicationSlotBounds,
    public val canvasComplicationFactory: CanvasComplicationFactory,
    public val supportedTypes: List<ComplicationType>,
    defaultPolicy: DefaultComplicationDataSourcePolicy,
    defaultDataSourceType: ComplicationType,
    @get:JvmName("isInitiallyEnabled") public val initiallyEnabled: Boolean,
    configExtras: Bundle,
    @get:JvmName("isFixedComplicationDataSource") public val fixedComplicationDataSource: Boolean,
    public val tapFilter: ComplicationTapFilter,
    nameResourceId: Int?,
    screenReaderNameResourceId: Int?,
    // TODO(b/230364881): This should really be public but some metalava bug is preventing
    // @ComplicationExperimental from working on the getter so it's currently hidden.
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val boundingArc: BoundingArc?
) {
    /**
     * The [ComplicationSlotsManager] this is attached to. Only set after the
     * [ComplicationSlotsManager] has been created.
     */
    internal lateinit var complicationSlotsManager: ComplicationSlotsManager

    /**
     * Extras to be merged into the Intent sent when invoking the complication data source chooser
     * activity.
     */
    public var configExtras: Bundle = configExtras
        set(value) {
            field = value
            complicationSlotsManager.configExtrasChangeCallback
                ?.onComplicationSlotConfigExtrasChanged()
        }

    /**
     * The [CanvasComplication] used to render the complication. This can't be used until after
     * [WatchFaceService.createWatchFace] has completed.
     */
    public val renderer: CanvasComplication by lazy {
        canvasComplicationFactory.create(
            complicationSlotsManager.watchState,
            object : CanvasComplication.InvalidateCallback {
                override fun onInvalidate() {
                    if (this@ComplicationSlot::invalidateListener.isInitialized) {
                        invalidateListener.onInvalidate()
                    }
                }
            }
        )
    }

    private var lastComplicationUpdate = Instant.EPOCH

    private class ComplicationDataHistoryEntry(
        val complicationData: ComplicationData,
        val time: Instant
    )

    /**
     * There doesn't seem to be a convenient ring buffer in the standard library so implement our
     * own one.
     */
    private class RingBuffer(val size: Int) : Iterable<ComplicationDataHistoryEntry> {
        private val entries = arrayOfNulls<ComplicationDataHistoryEntry>(size)
        private var readIndex = 0
        private var writeIndex = 0

        fun push(entry: ComplicationDataHistoryEntry) {
            writeIndex = (writeIndex + 1) % size
            if (writeIndex == readIndex) {
                readIndex = (readIndex + 1) % size
            }
            entries[writeIndex] = entry
        }

        override fun iterator() =
            object : Iterator<ComplicationDataHistoryEntry> {
                var iteratorReadIndex = readIndex

                override fun hasNext() = iteratorReadIndex != writeIndex

                override fun next(): ComplicationDataHistoryEntry {
                    iteratorReadIndex = (iteratorReadIndex + 1) % size
                    return entries[iteratorReadIndex]!!
                }
            }
    }

    /**
     * In userdebug builds maintain a history of the last [MAX_COMPLICATION_HISTORY_ENTRIES]-1
     * complications, which is logged in dumpsys to help debug complication issues.
     */
    private val complicationHistory =
        if (Build.TYPE.equals("userdebug")) {
            RingBuffer(MAX_COMPLICATION_HISTORY_ENTRIES)
        } else {
            null
        }

    init {
        require(id >= 0) { "id must be >= 0" }
        require(accessibilityTraversalIndex >= 0) { "accessibilityTraversalIndex must be >= 0" }
    }

    public companion object {
        /** The maximum number of entries in [complicationHistory] plus one. */
        private const val MAX_COMPLICATION_HISTORY_ENTRIES = 50

        internal val unitSquare = RectF(0f, 0f, 1f, 1f)

        internal val screenLockedFallback = NoDataComplicationData()

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationSlotBoundsType.ROUND_RECT]. This is the most common type of complication.
         * These can be tapped by the user to trigger the associated intent.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *   unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         *   [CanvasComplication] to use for rendering. Note renderers should not be shared between
         *   complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         *   during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         *   the initial complication data source when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds].
         */
        @JvmStatic
        @OptIn(ComplicationExperimental::class)
        public fun createRoundRectComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
            bounds: ComplicationSlotBounds
        ): Builder =
            Builder(
                id,
                canvasComplicationFactory,
                supportedTypes,
                defaultDataSourcePolicy,
                ComplicationSlotBoundsType.ROUND_RECT,
                bounds,
                RoundRectComplicationTapFilter(),
                null
            )

        /**
         * Constructs a [Builder] for a complication with bound type
         * [ComplicationSlotBoundsType.BACKGROUND] whose bounds cover the entire screen. A
         * background complication is for watch faces that wish to have a full screen user
         * selectable backdrop. This sort of complication isn't clickable and at most one may be
         * present in the list of complicationSlots.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *   unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         *   [CanvasComplication] to use for rendering. Note renderers should not be shared between
         *   complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         *   during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         *   the initial complication data source when the watch is first installed.
         */
        @JvmStatic
        @OptIn(ComplicationExperimental::class)
        public fun createBackgroundComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy
        ): Builder =
            Builder(
                id,
                canvasComplicationFactory,
                supportedTypes,
                defaultDataSourcePolicy,
                ComplicationSlotBoundsType.BACKGROUND,
                ComplicationSlotBounds(RectF(0f, 0f, 1f, 1f)),
                BackgroundComplicationTapFilter(),
                null
            )

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationSlotBoundsType.EDGE].
         *
         * An edge complication is drawn around the border of the display and has custom hit test
         * logic (see [complicationTapFilter]). When tapped the associated intent is dispatched.
         * Edge complicationSlots should have a custom [renderer] with
         * [CanvasComplication.drawHighlight] overridden.
         *
         * Note hit detection in an editor for [ComplicationSlot]s created with this method is not
         * supported.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *   unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         *   [CanvasComplication] to use for rendering. Note renderers should not be shared between
         *   complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         *   during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         *   the initial complication data source when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds]. Its likely the bounding rect
         *   will be much larger than the complication and shouldn't directly be used for hit
         *   testing.
         * @param complicationTapFilter The [ComplicationTapFilter] used to determine whether or not
         *   a tap hit the complication.
         */
        // TODO(b/230364881): Deprecate when BoundingArc is no longer experimental.
        @JvmStatic
        @OptIn(ComplicationExperimental::class)
        public fun createEdgeComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
            bounds: ComplicationSlotBounds,
            complicationTapFilter: ComplicationTapFilter
        ): Builder =
            Builder(
                id,
                canvasComplicationFactory,
                supportedTypes,
                defaultDataSourcePolicy,
                ComplicationSlotBoundsType.EDGE,
                bounds,
                complicationTapFilter,
                null
            )

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationSlotBoundsType.EDGE], whose contents are contained within [boundingArc].
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *   unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         *   [CanvasComplication] to use for rendering. Note renderers should not be shared between
         *   complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         *   during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         *   the initial complication data source when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds]. Its likely the bounding rect
         *   will be much larger than the complication and shouldn't directly be used for hit
         *   testing.
         */
        @JvmStatic
        @JvmOverloads
        @ComplicationExperimental
        @Suppress("UnavailableSymbol")
        public fun createEdgeComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
            bounds: ComplicationSlotBounds,
            @Suppress("HiddenTypeParameter") boundingArc: BoundingArc,
            complicationTapFilter: ComplicationTapFilter =
                object : ComplicationTapFilter {
                    override fun hitTest(
                        complicationSlot: ComplicationSlot,
                        screenBounds: Rect,
                        x: Int,
                        y: Int,
                        @Suppress("UNUSED_PARAMETER") includeMargins: Boolean
                    ) =
                        boundingArc.hitTest(
                            complicationSlot.computeBounds(screenBounds),
                            x.toFloat(),
                            y.toFloat()
                        )
                }
        ): Builder =
            Builder(
                id,
                canvasComplicationFactory,
                supportedTypes,
                defaultDataSourcePolicy,
                ComplicationSlotBoundsType.EDGE,
                bounds,
                complicationTapFilter,
                boundingArc
            )
    }

    /**
     * Builder for constructing [ComplicationSlot]s.
     *
     * @param id The watch face's ID for this complication. Can be any integer but should be unique
     *   within the watch face.
     * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
     *   [CanvasComplication] to use for rendering. Note renderers should not be shared between
     *   complicationSlots.
     * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
     *   during complication, this list should be non-empty.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select the
     *   initial complication data source when the watch is first installed.
     * @param boundsType The [ComplicationSlotBoundsType] of the complication.
     * @param bounds The complication's [ComplicationSlotBounds].
     * @param complicationTapFilter The [ComplicationTapFilter] used to perform hit testing for this
     *   complication.
     */
    @OptIn(ComplicationExperimental::class)
    public class Builder
    internal constructor(
        private val id: Int,
        private val canvasComplicationFactory: CanvasComplicationFactory,
        private val supportedTypes: List<ComplicationType>,
        private var defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        @ComplicationSlotBoundsType private val boundsType: Int,
        private val bounds: ComplicationSlotBounds,
        private val complicationTapFilter: ComplicationTapFilter,
        private val boundingArc: BoundingArc?
    ) {
        private var accessibilityTraversalIndex = id
        private var defaultDataSourceType = ComplicationType.NOT_CONFIGURED
        private var initiallyEnabled = true
        private var configExtras: Bundle = Bundle.EMPTY
        private var fixedComplicationDataSource = false
        private var nameResourceId: Int? = null
        private var screenReaderNameResourceId: Int? = null

        init {
            require(id >= 0) { "id must be >= 0" }
        }

        /**
         * Sets the initial value used to sort Complications when generating accessibility content
         * description labels. By default this is [id].
         */
        public fun setAccessibilityTraversalIndex(accessibilityTraversalIndex: Int): Builder {
            this.accessibilityTraversalIndex = accessibilityTraversalIndex
            require(accessibilityTraversalIndex >= 0) { "accessibilityTraversalIndex must be >= 0" }
            return this
        }

        /**
         * Sets the initial [ComplicationType] to use with the initial complication data source.
         * Note care should be taken to ensure [defaultDataSourceType] is compatible with the
         * [DefaultComplicationDataSourcePolicy].
         */
        @Deprecated(
            "Instead set DefaultComplicationDataSourcePolicy" +
                ".systemDataSourceFallbackDefaultType."
        )
        public fun setDefaultDataSourceType(defaultDataSourceType: ComplicationType): Builder {
            require(defaultDataSourceType in supportedTypes) {
                "Can't set $defaultDataSourceType because it's not in the supportedTypes list:" +
                    " $supportedTypes"
            }
            defaultDataSourcePolicy =
                when {
                    defaultDataSourcePolicy.secondaryDataSource != null ->
                        DefaultComplicationDataSourcePolicy(
                            defaultDataSourcePolicy.primaryDataSource!!,
                            defaultDataSourcePolicy.primaryDataSourceDefaultType
                                ?: defaultDataSourceType,
                            defaultDataSourcePolicy.secondaryDataSource!!,
                            defaultDataSourcePolicy.secondaryDataSourceDefaultType
                                ?: defaultDataSourceType,
                            defaultDataSourcePolicy.systemDataSourceFallback,
                            defaultDataSourceType
                        )
                    defaultDataSourcePolicy.primaryDataSource != null ->
                        DefaultComplicationDataSourcePolicy(
                            defaultDataSourcePolicy.primaryDataSource!!,
                            defaultDataSourcePolicy.primaryDataSourceDefaultType
                                ?: defaultDataSourceType,
                            defaultDataSourcePolicy.systemDataSourceFallback,
                            defaultDataSourceType
                        )
                    else ->
                        DefaultComplicationDataSourcePolicy(
                            defaultDataSourcePolicy.systemDataSourceFallback,
                            defaultDataSourceType
                        )
                }
            this.defaultDataSourceType = defaultDataSourceType
            return this
        }

        /**
         * Whether the complication is initially enabled or not (by default its enabled). This can
         * be overridden by [ComplicationSlotsUserStyleSetting].
         */
        public fun setEnabled(enabled: Boolean): Builder {
            this.initiallyEnabled = enabled
            return this
        }

        /**
         * Sets optional extras to be merged into the Intent sent when invoking the complication
         * data source chooser activity.
         */
        public fun setConfigExtras(extras: Bundle): Builder {
            this.configExtras = extras
            return this
        }

        /** Whether or not the complication source is fixed (i.e. the user can't change it). */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setFixedComplicationDataSource(fixedComplicationDataSource: Boolean): Builder {
            this.fixedComplicationDataSource = fixedComplicationDataSource
            return this
        }

        /**
         * If non-null sets the ID of a string resource containing the name of this complication
         * slot, for use visually in an editor. This resource should be short and should not contain
         * the word "Complication". E.g. "Left" for the left complication.
         */
        public fun setNameResourceId(@Suppress("AutoBoxing") nameResourceId: Int?): Builder {
            this.nameResourceId = nameResourceId
            return this
        }

        /**
         * If non-null sets the ID of a string resource containing the name of this complication
         * slot, for use by a screen reader. This resource should be a short sentence. E.g. "Left
         * complication" for the left complication.
         */
        public fun setScreenReaderNameResourceId(
            @Suppress("AutoBoxing") screenReaderNameResourceId: Int?
        ): Builder {
            this.screenReaderNameResourceId = screenReaderNameResourceId
            return this
        }

        /** Constructs the [ComplicationSlot]. */
        public fun build(): ComplicationSlot {
            require(
                defaultDataSourcePolicy.primaryDataSourceDefaultType == null ||
                    defaultDataSourcePolicy.primaryDataSourceDefaultType in supportedTypes
            ) {
                "defaultDataSourcePolicy.primaryDataSourceDefaultType " +
                    "${defaultDataSourcePolicy.primaryDataSourceDefaultType} must be in the" +
                    " supportedTypes list: $supportedTypes"
            }

            require(
                defaultDataSourcePolicy.secondaryDataSourceDefaultType == null ||
                    defaultDataSourcePolicy.secondaryDataSourceDefaultType in supportedTypes
            ) {
                "defaultDataSourcePolicy.secondaryDataSourceDefaultType " +
                    "${defaultDataSourcePolicy.secondaryDataSourceDefaultType} must be in the" +
                    " supportedTypes list: $supportedTypes"
            }

            require(
                defaultDataSourcePolicy.systemDataSourceFallbackDefaultType ==
                    ComplicationType.NOT_CONFIGURED ||
                    defaultDataSourcePolicy.systemDataSourceFallbackDefaultType in supportedTypes
            ) {
                "defaultDataSourcePolicy.systemDataSourceFallbackDefaultType " +
                    "${defaultDataSourcePolicy.systemDataSourceFallbackDefaultType} must be in " +
                    "the supportedTypes list: $supportedTypes"
            }

            return ComplicationSlot(
                id,
                accessibilityTraversalIndex,
                boundsType,
                bounds,
                canvasComplicationFactory,
                supportedTypes,
                defaultDataSourcePolicy,
                defaultDataSourceType,
                initiallyEnabled,
                configExtras,
                fixedComplicationDataSource,
                complicationTapFilter,
                nameResourceId,
                screenReaderNameResourceId,
                boundingArc
            )
        }
    }

    internal interface InvalidateListener {
        /** Requests redraw. Can be called on any thread */
        fun onInvalidate()
    }

    private lateinit var invalidateListener: InvalidateListener

    internal var complicationBoundsDirty = true

    /**
     * The complication's [ComplicationSlotBounds] which are converted to pixels during rendering.
     *
     * Note it's not allowed to change the bounds of a background complication because they are
     * assumed to always cover the entire screen.
     */
    public var complicationSlotBounds: ComplicationSlotBounds = bounds
        @UiThread get
        @UiThread
        internal set(value) {
            require(boundsType != ComplicationSlotBoundsType.BACKGROUND)
            if (field == value) {
                return
            }
            field = value
            complicationBoundsDirty = true
        }

    internal var enabledDirty = true

    /** Whether or not the complication should be drawn and accept taps. */
    public var enabled: Boolean = initiallyEnabled
        @JvmName("isEnabled") @UiThread get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            enabledDirty = true
        }

    internal var defaultDataSourcePolicyDirty = true

    /**
     * The [DefaultComplicationDataSourcePolicy] which defines the default complicationSlots
     * providers selected when the user hasn't yet made a choice. See also [defaultDataSourceType].
     */
    public var defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy = defaultPolicy
        @UiThread get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            defaultDataSourcePolicyDirty = true
        }

    internal var defaultDataSourceTypeDirty = true

    /** The default [ComplicationType] to use alongside [defaultDataSourcePolicy]. */
    @Deprecated(
        "Use DefaultComplicationDataSourcePolicy." + "systemDataSourceFallbackDefaultType instead"
    )
    public var defaultDataSourceType: ComplicationType = defaultDataSourceType
        @UiThread get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            defaultDataSourceTypeDirty = true
        }

    internal var accessibilityTraversalIndexDirty = true

    /**
     * This is used to determine the order in which accessibility labels for the watch face are read
     * to the user. Accessibility labels are automatically generated for the time and
     * complicationSlots. See also [Renderer.additionalContentDescriptionLabels].
     */
    public var accessibilityTraversalIndex: Int = accessibilityTraversalIndex
        @UiThread get
        @UiThread
        internal set(value) {
            require(value >= 0) { "accessibilityTraversalIndex must be >= 0" }
            if (field == value) {
                return
            }
            field = value
            accessibilityTraversalIndexDirty = true
        }

    internal var nameResourceIdDirty = true

    /**
     * The optional ID of string resource (or `null` if absent) to identify the complication slot on
     * screen in an editor. These strings should be short (perhaps 10 characters max) E.g.
     * complication slots named 'left' and 'right' might be shown by the editor in a list from which
     * the user selects a complication slot for editing.
     */
    public var nameResourceId: Int? = nameResourceId
        @Suppress("AutoBoxing") @UiThread get
        @UiThread
        internal set(value) {
            require(value != 0)
            if (field == value) {
                return
            }
            field = value
            nameResourceIdDirty = true
        }

    internal var screenReaderNameResourceIdDirty = true

    /**
     * The optional ID of a string resource (or `null` if absent) for use by a watch face editor to
     * identify the complication slot in a screen reader. While similar to [nameResourceId] this
     * string can be longer and should be more descriptive. E.g. saying 'left complication' rather
     * than just 'left'.
     */
    public var screenReaderNameResourceId: Int? = screenReaderNameResourceId
        @Suppress("AutoBoxing") @UiThread get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            screenReaderNameResourceIdDirty = true
        }

    internal var dataDirty = true

    /**
     * The [androidx.wear.watchface.complications.data.ComplicationData] associated with the
     * [ComplicationSlot]. This defaults to [NoDataComplicationData].
     */
    public val complicationData: StateFlow<ComplicationData> =
        MutableStateFlow(NoDataComplicationData())

    /**
     * The complication data sent by the system. This may contain a timeline out of which
     * [complicationData] is selected.
     */
    private var timelineComplicationData: ComplicationData = NoDataComplicationData()
    private var timelineEntries: List<WireComplicationData>? = null

    /**
     * Sets the current [ComplicationData] and if it's a timeline, the correct override for
     * [instant] is chosen.
     */
    internal fun setComplicationData(
        complicationData: ComplicationData,
        loadDrawablesAsynchronous: Boolean,
        instant: Instant
    ) {
        lastComplicationUpdate = instant
        complicationHistory?.push(ComplicationDataHistoryEntry(complicationData, instant))
        timelineComplicationData = complicationData
        timelineEntries = complicationData.asWireComplicationData().timelineEntries?.toList()
        selectComplicationDataForInstant(instant, loadDrawablesAsynchronous, true)
    }

    /**
     * If the current [ComplicationData] is a timeline, the correct override for [instant] is
     * chosen.
     */
    internal fun selectComplicationDataForInstant(
        instant: Instant,
        loadDrawablesAsynchronous: Boolean,
        forceUpdate: Boolean
    ) {
        var previousShortest = Long.MAX_VALUE
        val time = instant.epochSecond
        var best = timelineComplicationData

        // Select the shortest valid timeline entry.
        timelineEntries?.let {
            for (wireEntry in it) {
                val start = wireEntry.timelineStartEpochSecond
                val end = wireEntry.timelineEndEpochSecond
                if (start != null && end != null && time >= start && time < end) {
                    val duration = end - start
                    if (duration < previousShortest) {
                        previousShortest = duration
                        best = wireEntry.toApiComplicationData()
                    }
                }
            }
        }

        // If the screen is locked and our policy is to not display it when locked then select
        // screenLockedFallback instead.
        if (
            (best.displayPolicy and ComplicationDisplayPolicies.DO_NOT_SHOW_WHEN_DEVICE_LOCKED) !=
                0 && complicationSlotsManager.watchState.isLocked.value
        ) {
            best = screenLockedFallback // This is NoDataComplicationData.
        }

        if (!forceUpdate && complicationData.value == best) return
        renderer.loadData(best, loadDrawablesAsynchronous)
        (complicationData as MutableStateFlow).value = best

        // forceUpdate is used for screenshots, don't set the dirty flag for those.
        if (!forceUpdate) {
            dataDirty = true
        }
    }

    /**
     * Whether or not the complication should be considered active and should be rendered at the
     * specified time.
     */
    public fun isActiveAt(instant: Instant): Boolean {
        return when (complicationData.value.type) {
            ComplicationType.NO_DATA -> false
            ComplicationType.NO_PERMISSION -> false
            ComplicationType.EMPTY -> false
            else -> complicationData.value.validTimeRange.contains(instant)
        }
    }

    /**
     * Watch faces should use this method to render a complication. Note the system may call this.
     *
     * @param canvas The [Canvas] to render into
     * @param zonedDateTime The [ZonedDateTime] to render with
     * @param renderParameters The current [RenderParameters]
     */
    @UiThread
    public fun render(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters
    ) {
        val bounds = computeBounds(Rect(0, 0, canvas.width, canvas.height))
        renderer.render(canvas, bounds, zonedDateTime, renderParameters, id)
    }

    /**
     * Watch faces should use this method to render non-fixed complicationSlots for any highlight
     * layer pass. Note the system may call this.
     *
     * @param canvas The [Canvas] to render into
     * @param zonedDateTime The [ZonedDateTime] to render with
     * @param renderParameters The current [RenderParameters]
     */
    @UiThread
    @OptIn(ComplicationExperimental::class)
    public fun renderHighlightLayer(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters
    ) {
        // It's only sensible to render a highlight for non-fixed ComplicationSlots because you
        // can't edit fixed complicationSlots.
        if (fixedComplicationDataSource) {
            return
        }

        val bounds = computeBounds(Rect(0, 0, canvas.width, canvas.height))
        when (val highlightedElement = renderParameters.highlightLayer?.highlightedElement) {
            is HighlightedElement.AllComplicationSlots -> {
                renderer.drawHighlight(
                    canvas,
                    bounds,
                    boundsType,
                    zonedDateTime,
                    renderParameters.highlightLayer.highlightTint,
                    boundingArc
                )
            }
            is HighlightedElement.ComplicationSlot -> {
                if (highlightedElement.id == id) {
                    renderer.drawHighlight(
                        canvas,
                        bounds,
                        boundsType,
                        zonedDateTime,
                        renderParameters.highlightLayer.highlightTint,
                        boundingArc
                    )
                }
            }
            is HighlightedElement.UserStyle -> {
                // Nothing
            }
            null -> {
                // Nothing
            }
        }
    }

    internal fun init(invalidateListener: InvalidateListener, isHeadless: Boolean) {
        this.invalidateListener = invalidateListener

        if (isHeadless) {
            timelineComplicationData = EmptyComplicationData()
            (complicationData as MutableStateFlow).value = EmptyComplicationData()
        }
    }

    /**
     * Computes the bounds of the complication by converting the unitSquareBounds of the specified
     * [complicationType] to pixels based on the [screen]'s dimensions.
     *
     * @param screen A [Rect] describing the dimensions of the screen.
     * @param complicationType The [ComplicationType] to use when looking up the slot's
     *   [ComplicationSlotBounds.perComplicationTypeBounds].
     * @param applyMargins Whether or not the margins should be applied to the computed [Rect].
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun computeBounds(
        screen: Rect,
        complicationType: ComplicationType,
        applyMargins: Boolean = false
    ): Rect {
        val unitSquareBounds =
            RectF(complicationSlotBounds.perComplicationTypeBounds[complicationType]!!)
        if (applyMargins) {
            val unitSquareMargins =
                complicationSlotBounds.perComplicationTypeMargins[complicationType]!!
            // Apply the margins
            unitSquareBounds.set(
                unitSquareBounds.left - unitSquareMargins.left,
                unitSquareBounds.top - unitSquareMargins.top,
                unitSquareBounds.right + unitSquareMargins.right,
                unitSquareBounds.bottom + unitSquareMargins.bottom
            )
        }
        unitSquareBounds.intersect(unitSquare)
        // We add 0.5 to make toInt() round to the nearest whole number rather than truncating.
        return Rect(
            (0.5f + unitSquareBounds.left * screen.width()).toInt(),
            (0.5f + unitSquareBounds.top * screen.height()).toInt(),
            (0.5f + unitSquareBounds.right * screen.width()).toInt(),
            (0.5f + unitSquareBounds.bottom * screen.height()).toInt()
        )
    }

    /**
     * Computes the bounds of the complication by converting the unitSquareBounds of the current
     * complication type to pixels based on the [screen]'s dimensions.
     *
     * @param screen A [Rect] describing the dimensions of the screen.
     * @param applyMargins Whether or not the margins should be applied to the computed [Rect].
     */
    @JvmOverloads
    public fun computeBounds(screen: Rect, applyMargins: Boolean = false): Rect =
        computeBounds(screen, complicationData.value.type, applyMargins)

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("ComplicationSlot $id:")
        writer.increaseIndent()
        writer.println("fixedComplicationDataSource=$fixedComplicationDataSource")
        writer.println("enabled=$enabled")
        writer.println("boundsType=$boundsType")
        writer.println("configExtras=$configExtras")
        writer.println("supportedTypes=${supportedTypes.joinToString { it.toString() }}")
        writer.println("initiallyEnabled=$initiallyEnabled")
        writer.println(
            "defaultDataSourcePolicy.primaryDataSource=${defaultDataSourcePolicy.primaryDataSource}"
        )
        writer.println(
            "defaultDataSourcePolicy.primaryDataSourceDefaultDataSourceType=" +
                defaultDataSourcePolicy.primaryDataSourceDefaultType
        )
        writer.println(
            "defaultDataSourcePolicy.secondaryDataSource=" +
                defaultDataSourcePolicy.secondaryDataSource
        )
        writer.println(
            "defaultDataSourcePolicy.secondaryDataSourceDefaultDataSourceType=" +
                defaultDataSourcePolicy.secondaryDataSourceDefaultType
        )
        writer.println(
            "defaultDataSourcePolicy.systemDataSourceFallback=" +
                defaultDataSourcePolicy.systemDataSourceFallback
        )
        writer.println(
            "defaultDataSourcePolicy.systemDataSourceFallbackDefaultType=" +
                defaultDataSourcePolicy.systemDataSourceFallbackDefaultType
        )
        writer.println("timelineComplicationData=$timelineComplicationData")
        writer.println("timelineEntries=" + timelineEntries?.joinToString())
        writer.println("data=${renderer.getData()}")
        @OptIn(ComplicationExperimental::class) writer.println("boundingArc=$boundingArc")
        writer.println("complicationSlotBounds=$complicationSlotBounds")
        writer.println("lastComplicationUpdate=$lastComplicationUpdate")
        writer.println("data history")
        complicationHistory?.let {
            writer.increaseIndent()
            for (entry in it) {
                val localDateTime = LocalDateTime.ofInstant(entry.time, ZoneId.systemDefault())
                writer.println("${entry.complicationData} @ $localDateTime")
            }
            writer.decreaseIndent()
        }
        writer.decreaseIndent()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationSlot

        if (id != other.id) return false
        if (accessibilityTraversalIndex != other.accessibilityTraversalIndex) return false
        if (boundsType != other.boundsType) return false
        if (complicationSlotBounds != other.complicationSlotBounds) return false
        if (
            supportedTypes.size != other.supportedTypes.size ||
                !supportedTypes.containsAll(other.supportedTypes)
        )
            return false
        if (defaultDataSourcePolicy != other.defaultDataSourcePolicy) return false
        if (initiallyEnabled != other.initiallyEnabled) return false
        if (fixedComplicationDataSource != other.fixedComplicationDataSource) return false
        if (nameResourceId != other.nameResourceId) return false
        if (screenReaderNameResourceId != other.screenReaderNameResourceId) return false
        @OptIn(ComplicationExperimental::class) if (boundingArc != other.boundingArc) return false

        return true
    }

    override fun hashCode(): Int {
        @OptIn(ComplicationExperimental::class)
        return Objects.hash(
            id,
            accessibilityTraversalIndex,
            boundsType,
            complicationSlotBounds,
            supportedTypes.sorted(),
            defaultDataSourcePolicy,
            initiallyEnabled,
            fixedComplicationDataSource,
            nameResourceId,
            screenReaderNameResourceId,
            boundingArc
        )
    }
}
