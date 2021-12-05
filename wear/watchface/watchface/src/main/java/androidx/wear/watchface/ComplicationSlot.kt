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
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.RenderParameters.HighlightedElement
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Interface for rendering complicationSlots onto a [Canvas]. These should be created by
 * [CanvasComplicationFactory.create]. If state needs to be shared with the [Renderer] that should
 * be set up inside [onRendererCreated].
 */
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
    @WorkerThread
    public fun onRendererCreated(renderer: Renderer) {}

    /**
     * Draws the complication defined by [getData] into the canvas with the specified bounds.
     * This will usually be called by user watch face drawing code, but the system may also call it
     * for complication selection UI rendering. The width and height will be the same as that
     * computed by computeBounds but the translation and canvas size may differ.
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
    public fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        @ComplicationSlotBoundsType boundsType: Int,
        zonedDateTime: ZonedDateTime,
        @ColorInt color: Int
    )

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
public interface ComplicationTapFilter {
    /**
     * Performs a hit test, returning `true` if the supplied coordinates in pixels are within the
     * the provided [complicationSlot] scaled to [screenBounds].
     *
     * @param complicationSlot The [ComplicationSlot] to perform a hit test for.
     * @param screenBounds A [Rect] describing the bounds of the display.
     * @param x The screen space X coordinate in pixels.
     * @param y The screen space Y coordinate in pixels.
     */
    public fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int
    ): Boolean
}

/** Default [ComplicationTapFilter] for [ComplicationSlotBoundsType.ROUND_RECT] complicationSlots. */
public class RoundRectComplicationTapFilter : ComplicationTapFilter {
    override fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int
    ): Boolean = complicationSlot.computeBounds(screenBounds).contains(x, y)
}

/** Default [ComplicationTapFilter] for [ComplicationSlotBoundsType.BACKGROUND] complicationSlots. */
public class BackgroundComplicationTapFilter : ComplicationTapFilter {
    override fun hitTest(
        complicationSlot: ComplicationSlot,
        screenBounds: Rect,
        @Px x: Int,
        @Px y: Int
    ): Boolean = false
}

/** @hide */
@IntDef(
    value = [
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
 * Represents the slot an individual complication on the screen may go in. The number of
 * ComplicationSlots is fixed (see [ComplicationSlotsManager]) but ComplicationSlots can be
 * enabled or disabled via [UserStyleSetting.ComplicationSlotsUserStyleSetting].
 *
 * @param id The Watch Face's ID for the complication slot.
 * @param accessibilityTraversalIndex Used to sort Complications when generating accessibility
 * content description labels.
 * @param boundsType The [ComplicationSlotBoundsType] of the complication slot.
 * @param bounds The complication slot's [ComplicationSlotBounds].
 * @param canvasComplicationFactory The [CanvasComplicationFactory] used to generate a
 * [CanvasComplication] for rendering the complication. The factory allows us to decouple
 * ComplicationSlot from potentially expensive asset loading.
 * @param supportedTypes The list of [ComplicationType]s accepted by this complication slot. Used
 * during complication data source selection, this list should be non-empty.
 * @param defaultPolicy The [DefaultComplicationDataSourcePolicy] which controls the
 * initial complication data source when the watch face is first installed.
 * @param defaultDataSourceType The default [ComplicationType] for the default complication data
 * source.
 * @param initiallyEnabled At creation a complication slot is either enabled or disabled. This
 * can be overridden by a [ComplicationSlotsUserStyleSetting] (see
 * [ComplicationSlotOverlay.enabled]).
 * Editors need to know the initial state of a complication slot to predict the effects of making a
 * style change.
 * @param configExtras Extras to be merged into the Intent sent when invoking the complication data
 * source chooser activity. This features is intended for OEM watch faces where they have elements
 * that behave like a complication but are in fact entirely watch face specific.
 * @param fixedComplicationDataSource  Whether or not the complication data source is fixed (i.e.
 * can't be changed by the user).  This is useful for watch faces built around specific
 * complications.
 * @param tapFilter The [ComplicationTapFilter] used to determine whether or not a tap hit the
 * complication slot.
 */
public class ComplicationSlot internal constructor(
    public val id: Int,
    accessibilityTraversalIndex: Int,
    @ComplicationSlotBoundsType public val boundsType: Int,
    bounds: ComplicationSlotBounds,
    public val canvasComplicationFactory: CanvasComplicationFactory,
    supportedTypes: List<ComplicationType>,
    defaultPolicy: DefaultComplicationDataSourcePolicy,
    defaultDataSourceType: ComplicationType,
    @get:JvmName("isInitiallyEnabled")
    public val initiallyEnabled: Boolean,
    configExtras: Bundle,
    @get:JvmName("isFixedComplicationDataSource")
    public val fixedComplicationDataSource: Boolean,
    public val tapFilter: ComplicationTapFilter
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

    init {
        require(id >= 0) { "id must be >= 0" }
        require(accessibilityTraversalIndex >= 0) {
            "accessibilityTraversalIndex must be >= 0"
        }
    }

    public companion object {
        internal val unitSquare = RectF(0f, 0f, 1f, 1f)

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationSlotBoundsType.ROUND_RECT]. This is the most common type of complication. These
         * can be tapped by the user to trigger the associated intent.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         * unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         * [CanvasComplication] to use for rendering. Note renderers should not be shared between
         * complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         * during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         * the initial complication data source when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds].
         */
        @JvmStatic
        public fun createRoundRectComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
            bounds: ComplicationSlotBounds
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultDataSourcePolicy,
            ComplicationSlotBoundsType.ROUND_RECT,
            bounds,
            RoundRectComplicationTapFilter()
        )

        /**
         * Constructs a [Builder] for a complication with bound type
         * [ComplicationSlotBoundsType.BACKGROUND] whose bounds cover the entire screen. A
         * background complication is for watch faces that wish to have a full screen user
         * selectable  backdrop. This sort of complication isn't clickable and at most one may be
         * present in the list of complicationSlots.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         * unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         * [CanvasComplication] to use for rendering. Note renderers should not be shared between
         * complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         * during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         * the initial complication data source when the watch is first installed.
         */
        @JvmStatic
        public fun createBackgroundComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultDataSourcePolicy,
            ComplicationSlotBoundsType.BACKGROUND,
            ComplicationSlotBounds(RectF(0f, 0f, 1f, 1f)),
            BackgroundComplicationTapFilter()
        )

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationSlotBoundsType.EDGE].
         *
         * An edge complication is drawn around the border of the display and has custom hit test
         * logic (see [complicationTapFilter]). When tapped the associated intent is
         * dispatched. Edge complicationSlots should have a custom [renderer] with
         * [CanvasComplication.drawHighlight] overridden.
         *
         * Note we don't support edge complication hit testing from an editor.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         * unique within the watch face.
         * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
         * [CanvasComplication] to use for rendering. Note renderers should not be shared between
         * complicationSlots.
         * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
         * during complication, this list should be non-empty.
         * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
         * the initial complication data source when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds]. Its likely the bounding rect
         * will be much larger than the complication and shouldn't directly be used for hit testing.
         * @param complicationTapFilter The [ComplicationTapFilter] used to determine whether or
         * not a tap hit the complication.
         */
        @JvmStatic
        public fun createEdgeComplicationSlotBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
            bounds: ComplicationSlotBounds,
            complicationTapFilter: ComplicationTapFilter
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultDataSourcePolicy,
            ComplicationSlotBoundsType.EDGE,
            bounds,
            complicationTapFilter
        )
    }

    /**
     * Builder for constructing [ComplicationSlot]s.
     *
     * @param id The watch face's ID for this complication. Can be any integer but should be unique
     * within the watch face.
     * @param canvasComplicationFactory The [CanvasComplicationFactory] to supply the
     * [CanvasComplication] to use for rendering. Note renderers should not be shared between
     * complicationSlots.
     * @param supportedTypes The types of complication supported by this ComplicationSlot. Used
     * during complication, this list should be non-empty.
     * @param defaultDataSourcePolicy The [DefaultComplicationDataSourcePolicy] used to select
     * the initial complication data source when the watch is first installed.
     * @param boundsType The [ComplicationSlotBoundsType] of the complication.
     * @param bounds The complication's [ComplicationSlotBounds].
     * @param complicationTapFilter The [ComplicationTapFilter] used to perform hit testing for this
     * complication.
     */
    public class Builder internal constructor(
        private val id: Int,
        private val canvasComplicationFactory: CanvasComplicationFactory,
        private val supportedTypes: List<ComplicationType>,
        private var defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy,
        @ComplicationSlotBoundsType private val boundsType: Int,
        private val bounds: ComplicationSlotBounds,
        private val complicationTapFilter: ComplicationTapFilter
    ) {
        private var accessibilityTraversalIndex = id
        private var defaultDataSourceType = ComplicationType.NOT_CONFIGURED
        private var initiallyEnabled = true
        private var configExtras: Bundle = Bundle.EMPTY
        private var fixedComplicationDataSource = false

        init {
            require(id >= 0) { "id must be >= 0" }
        }

        /**
         * Sets the initial value used to sort Complications when generating accessibility content
         * description labels. By default this is [id].
         */
        public fun setAccessibilityTraversalIndex(accessibilityTraversalIndex: Int): Builder {
            this.accessibilityTraversalIndex = accessibilityTraversalIndex
            require(accessibilityTraversalIndex >= 0) {
                "accessibilityTraversalIndex must be >= 0"
            }
            return this
        }

        /**
         * Sets the initial [ComplicationType] to use with the initial complication data source.
         * Note care should be taken to ensure [defaultDataSourceType] is compatible with the
         * [DefaultComplicationDataSourcePolicy].
         */
        @Deprecated("Instead set DefaultComplicationDataSourcePolicy" +
            ".systemDataSourceFallbackDefaultType.")
        public fun setDefaultDataSourceType(
            defaultDataSourceType: ComplicationType
        ): Builder {
            defaultDataSourcePolicy = when {
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

                else -> DefaultComplicationDataSourcePolicy(
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

        /**
         * Whether or not the complication source is fixed (i.e. the user can't change it).
         */
        @Suppress("MissingGetterMatchingBuilder")
        public fun setFixedComplicationDataSource(fixedComplicationDataSource: Boolean): Builder {
            this.fixedComplicationDataSource = fixedComplicationDataSource
            return this
        }

        /** Constructs the [ComplicationSlot]. */
        public fun build(): ComplicationSlot = ComplicationSlot(
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
            complicationTapFilter
        )
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
     * Note it's not allowed to change the bounds of a background complication because
     * they are assumed to always cover the entire screen.
     */
    public var complicationSlotBounds: ComplicationSlotBounds = bounds
        @UiThread
        get
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
        @JvmName("isEnabled")
        @UiThread
        get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            enabledDirty = true
        }

    internal var supportedTypesDirty = true

    /** The types of complicationSlots the complication supports. Must be non-empty. */
    public var supportedTypes: List<ComplicationType> = supportedTypes
        @UiThread
        get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            require(value.isNotEmpty())
            field = value
            supportedTypesDirty = true
        }

    internal var defaultDataSourcePolicyDirty = true

    /**
     * The [DefaultComplicationDataSourcePolicy] which defines the default complicationSlots
     * providers selected when the user hasn't yet made a choice. See also [defaultDataSourceType].
     */
    public var defaultDataSourcePolicy: DefaultComplicationDataSourcePolicy = defaultPolicy
        @UiThread
        get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            defaultDataSourcePolicyDirty = true
        }

    internal var defaultDataSourceTypeDirty = true

    /**
     * The default [ComplicationType] to use alongside [defaultDataSourcePolicy].
     */
    @Deprecated("Use DefaultComplicationDataSourcePolicy." +
        "systemDataSourceFallbackDefaultType instead")
    public var defaultDataSourceType: ComplicationType = defaultDataSourceType
        @UiThread
        get
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
     * This is used to determine the order in which accessibility labels for the watch face are
     * read to the user. Accessibility labels are automatically generated for the time and
     * complicationSlots.  See also [Renderer.additionalContentDescriptionLabels].
     */
    public var accessibilityTraversalIndex: Int = accessibilityTraversalIndex
        @UiThread
        get
        @UiThread
        internal set(value) {
            require(value >= 0) {
                "accessibilityTraversalIndex must be >= 0"
            }
            if (field == value) {
                return
            }
            field = value
            accessibilityTraversalIndexDirty = true
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
    private var timelineEntries: List<ComplicationData>? = null

    /**
     * Sets the current [ComplicationData] and if it's a timeline, the correct override for
     * [instant] is chosen.
     */
    internal fun setComplicationData(
        complicationData: ComplicationData,
        loadDrawablesAsynchronous: Boolean,
        instant: Instant
    ) {
        timelineComplicationData = complicationData
        timelineEntries = complicationData.asWireComplicationData().timelineEntries?.map {
            it.toApiComplicationData()
        }
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
            for (entry in it) {
                val wireEntry = entry.asWireComplicationData()
                val start = wireEntry.timelineStartInstant?.epochSecond
                val end = wireEntry.timelineEndInstant?.epochSecond
                if (start != null && end != null && time >= start && time < end) {
                    val duration = end - start
                    if (duration < previousShortest) {
                        previousShortest = duration
                        best = entry
                    }
                }
            }
        }

        if (forceUpdate || complicationData.value != best) {
            (complicationData as MutableStateFlow).value = best
            renderer.loadData(best, loadDrawablesAsynchronous)
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
                    renderParameters.highlightLayer.highlightTint
                )
            }

            is HighlightedElement.ComplicationSlot -> {
                if (highlightedElement.id == id) {
                    renderer.drawHighlight(
                        canvas,
                        bounds,
                        boundsType,
                        zonedDateTime,
                        renderParameters.highlightLayer.highlightTint
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
     * [ComplicationSlotBounds.perComplicationTypeBounds].
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun computeBounds(screen: Rect, complicationType: ComplicationType): Rect {
        val unitSquareBounds = complicationSlotBounds.perComplicationTypeBounds[complicationType]!!
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
     */
    public fun computeBounds(screen: Rect): Rect =
        computeBounds(screen, complicationData.value.type)

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
        writer.println("defaultDataSourcePolicy.primaryDataSourceDefaultDataSourceType=" +
            defaultDataSourcePolicy.primaryDataSourceDefaultType)
        writer.println(
            "defaultDataSourcePolicy.secondaryDataSource=" +
                defaultDataSourcePolicy.secondaryDataSource
        )
        writer.println("defaultDataSourcePolicy.secondaryDataSourceDefaultDataSourceType=" +
            defaultDataSourcePolicy.secondaryDataSourceDefaultType)
        writer.println(
            "defaultDataSourcePolicy.systemDataSourceFallback=" +
                defaultDataSourcePolicy.systemDataSourceFallback
        )
        writer.println("defaultDataSourcePolicy.systemDataSourceFallbackDefaultType=" +
            defaultDataSourcePolicy.systemDataSourceFallbackDefaultType)
        writer.println("data=${renderer.getData()}")
        val bounds = complicationSlotBounds.perComplicationTypeBounds.map {
            "${it.key} -> ${it.value}"
        }
        writer.println("bounds=[$bounds]")
        writer.decreaseIndent()
    }
}
