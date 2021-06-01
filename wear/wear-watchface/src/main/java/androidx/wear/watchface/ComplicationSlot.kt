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
import android.icu.util.Calendar
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.UiThread
import androidx.wear.complications.ComplicationSlotBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.ObservableWatchData.MutableObservableWatchData
import androidx.wear.watchface.data.ComplicationSlotBoundsType
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.RenderParameters.HighlightedElement

/** Interface for rendering complicationSlots onto a [Canvas]. */
public interface CanvasComplication {

    /** Interface for observing when a [CanvasComplication] needs the screen to be redrawn. */
    public interface InvalidateCallback {
        /** Signals that the complication needs to be redrawn. Can be called on any thread. */
        public fun onInvalidate()
    }

    /**
     * Draws the complication defined by [getData] into the canvas with the specified bounds.
     * This will usually be called by user watch face drawing code, but the system may also call it
     * for complication selection UI rendering. The width and height will be the same as that
     * computed by computeBounds but the translation and canvas size may differ.
     *
     * @param canvas The [Canvas] to render into
     * @param bounds A [Rect] describing the bounds of the complication
     * @param calendar The current [Calendar]
     * @param renderParameters The current [RenderParameters]
     */
    @UiThread
    public fun render(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        renderParameters: RenderParameters
    )

    /**
     * Draws a highlight for a [ComplicationSlotBoundsType.ROUND_RECT] complication. The default
     * implementation does this by drawing a dashed line around the complication, other visual
     * effects may be used if desired.
     *
     * @param canvas The [Canvas] to render into
     * @param bounds A [Rect] describing the bounds of the complication
     * @param boundsType The [ComplicationSlotBoundsType] of the complication
     * @param calendar The current [Calendar]
     * @param color The color to render the highlight with
     */
    public fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        @ComplicationSlotBoundsType boundsType: Int,
        calendar: Calendar,
        @ColorInt color: Int
    )

    /**
     * Whether the complication should be drawn highlighted. This is to provide visual feedback when
     * the user taps on a complication.
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://stackoverflow.com/questions/47504279
    @get:JvmName("isHighlighted")
    @set:JvmName("setIsHighlighted")
    public var isHighlighted: Boolean

    /** Returns the [ComplicationData] to render with. */
    public fun getData(): ComplicationData?

    /**
     * Sets the [ComplicationData] to render with and loads any [Drawable]s contained within the
     * ComplicationData. You can choose whether this is done synchronously or asynchronously via
     * [loadDrawablesAsynchronous]. When any asynchronous loading has completed
     * [InvalidateCallback.onInvalidate] must be called.
     *
     * @param complicationData The [ComplicationData] to render with
     * @param loadDrawablesAsynchronous Whether or not any drawables should be loaded asynchronously
     */
    public fun loadData(complicationData: ComplicationData?, loadDrawablesAsynchronous: Boolean)
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

/**
 * Represents the slot an individual complication on the screen may go in. The number of
 * ComplicationSlots is fixed (see [ComplicationSlotsManager]) but ComplicationSlots can be
 * enabled or disabled via [UserStyleSetting.ComplicationSlotsUserStyleSetting].
 *
 * @param id The Watch Face's ID for the complication.
 * @param accessibilityTraversalIndex Used to sort Complications when generating accessibility
 * content description labels.
 * @param boundsType The [ComplicationSlotBoundsType] of the complication.
 * @param bounds The complication's [ComplicationSlotBounds].
 * @param canvasComplicationFactory The [CanvasComplicationFactory] used to generate a
 * [CanvasComplication] for rendering the complication. The factory allows us to decouple
 * ComplicationSlot from potentially expensive asset loading.
 * @param supportedTypes The list of [ComplicationType]s accepted by this complication. Used
 * during complication, this list should be non-empty.
 * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] which controls the initial
 * provider when the watch face is first installed.
 * @param defaultProviderType The default [ComplicationType] for the default provider.
 * @param initiallyEnabled At creation a complication is either enabled or disabled. This can be
 * overridden by a [ComplicationSlotsUserStyleSetting] (see [ComplicationSlotOverlay.enabled]).
 * Editors need to know the initial state of a complication to predict the effects of making a
 * style change.
 * @param configExtras Extras to be merged into the Intent sent when invoking the provider chooser
 * activity.
 * @param fixedComplicationProvider  Whether or not the complication provider is fixed (i.e.
 * can't be changed by the user).  This is useful for watch faces built around specific
 * complicationSlots.
 * @param tapFilter The [ComplicationTapFilter] used to determine whether or not a tap hit the
 * complication.
 */
public class ComplicationSlot internal constructor(
    public val id: Int,
    accessibilityTraversalIndex: Int,
    @ComplicationSlotBoundsType public val boundsType: Int,
    bounds: ComplicationSlotBounds,
    public val canvasComplicationFactory: CanvasComplicationFactory,
    supportedTypes: List<ComplicationType>,
    defaultProviderPolicy: DefaultComplicationProviderPolicy,
    defaultProviderType: ComplicationType,
    @get:JvmName("isInitiallyEnabled")
    public val initiallyEnabled: Boolean,
    public val configExtras: Bundle,
    @get:JvmName("isFixedComplicationProvider")
    public val fixedComplicationProvider: Boolean,
    public val tapFilter: ComplicationTapFilter
) {
    /**
     * The [ComplicationSlotsManager] this is attached to. Only set after the
     * [ComplicationSlotsManager] has been created.
     */
    internal lateinit var complicationSlotsManager: ComplicationSlotsManager

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
         * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
         * the initial complication provider when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds].
         */
        @JvmStatic
        public fun createRoundRectComplicationBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultProviderPolicy: DefaultComplicationProviderPolicy,
            bounds: ComplicationSlotBounds
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultProviderPolicy,
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
         * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
         * the initial complication provider when the watch is first installed.
         */
        @JvmStatic
        public fun createBackgroundComplicationBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultProviderPolicy: DefaultComplicationProviderPolicy
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultProviderPolicy,
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
         * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
         * the initial complication provider when the watch is first installed.
         * @param bounds The complication's [ComplicationSlotBounds]. Its likely the bounding rect
         * will be much larger than the complication and shouldn't directly be used for hit testing.
         * @param complicationTapFilter The [ComplicationTapFilter] used to determine whether or
         * not a tap hit the complication.
         */
        @JvmStatic
        public fun createEdgeComplicationBuilder(
            id: Int,
            canvasComplicationFactory: CanvasComplicationFactory,
            supportedTypes: List<ComplicationType>,
            defaultProviderPolicy: DefaultComplicationProviderPolicy,
            bounds: ComplicationSlotBounds,
            complicationTapFilter: ComplicationTapFilter
        ): Builder = Builder(
            id,
            canvasComplicationFactory,
            supportedTypes,
            defaultProviderPolicy,
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
     * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
     * the initial complication provider when the watch is first installed.
     * @param boundsType The [ComplicationSlotBoundsType] of the complication.
     * @param bounds The complication's [ComplicationSlotBounds].
     * @param complicationTapFilter The [ComplicationTapFilter] used to perform hit testing for this
     * complication.
     */
    public class Builder internal constructor(
        private val id: Int,
        private val canvasComplicationFactory: CanvasComplicationFactory,
        private val supportedTypes: List<ComplicationType>,
        private val defaultProviderPolicy: DefaultComplicationProviderPolicy,
        @ComplicationSlotBoundsType private val boundsType: Int,
        private val bounds: ComplicationSlotBounds,
        private val complicationTapFilter: ComplicationTapFilter
    ) {
        private var accessibilityTraversalIndex = id
        private var defaultProviderType = ComplicationType.NOT_CONFIGURED
        private var initiallyEnabled = true
        private var configExtras: Bundle = Bundle.EMPTY
        private var fixedComplicationProvider = false

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
         * Sets the initial [ComplicationType] to use with the initial complication provider.
         * Note care should be taken to ensure [defaultProviderType] is compatible with the
         * [DefaultComplicationProviderPolicy].
         */
        public fun setDefaultProviderType(
            defaultProviderType: ComplicationType
        ): Builder {
            this.defaultProviderType = defaultProviderType
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
         * Sets optional extras to be merged into the Intent sent when invoking the provider chooser
         * activity.
         */
        public fun setConfigExtras(extras: Bundle): Builder {
            this.configExtras = extras
            return this
        }

        /**
         * Whether or not the complication is fixed (i.e. the user can't change it).
         */
        public fun setFixedComplicationProvider(fixedComplicationProvider: Boolean): Builder {
            this.fixedComplicationProvider = fixedComplicationProvider
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
            defaultProviderPolicy,
            defaultProviderType,
            initiallyEnabled,
            configExtras,
            fixedComplicationProvider,
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

    internal var defaultProviderPolicyDirty = true

    /**
     * The [DefaultComplicationProviderPolicy] which defines the default complicationSlots providers
     * selected when the user hasn't yet made a choice. See also [defaultProviderType].
     */
    public var defaultProviderPolicy: DefaultComplicationProviderPolicy = defaultProviderPolicy
        @UiThread
        get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            defaultProviderPolicyDirty = true
        }

    internal var defaultProviderTypeDirty = true

    /**
     * The default [ComplicationType] to use alongside [defaultProviderPolicy].
     */
    public var defaultProviderType: ComplicationType = defaultProviderType
        @UiThread
        get
        @UiThread
        internal set(value) {
            if (field == value) {
                return
            }
            field = value
            defaultProviderTypeDirty = true
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
     * The [androidx.wear.complications.data.ComplicationData] associated with the
     * [ComplicationSlot].
     */
    public val complicationData:
        ObservableWatchData<androidx.wear.complications.data.ComplicationData> =
            MutableObservableWatchData()

    /**
     * Whether or not the complication should be considered active and should be rendered at the
     * specified time.
     */
    public fun isActiveAt(dateTimeMillis: Long): Boolean {
        if (!complicationData.hasValue()) {
            return false
        }
        return when (complicationData.value.type) {
            ComplicationType.NO_DATA -> false
            ComplicationType.NO_PERMISSION -> false
            ComplicationType.EMPTY -> false
            else -> complicationData.value.validTimeRange.contains(dateTimeMillis)
        }
    }

    /**
     * Watch faces should use this method to render a complication. Note the system may call this.
     *
     * @param canvas The [Canvas] to render into
     * @param calendar The current [Calendar]
     * @param renderParameters The current [RenderParameters]
     */
    @UiThread
    public fun render(
        canvas: Canvas,
        calendar: Calendar,
        renderParameters: RenderParameters
    ) {
        val bounds = computeBounds(Rect(0, 0, canvas.width, canvas.height))
        renderer.render(canvas, bounds, calendar, renderParameters)
    }

    /**
     * Watch faces should use this method to render non-fixed complicationSlots for any highlight
     * layer pass. Note the system may call this.
     *
     * @param canvas The [Canvas] to render into
     * @param calendar The current [Calendar]
     * @param renderParameters The current [RenderParameters]
     */
    @UiThread
    public fun renderHighlightLayer(
        canvas: Canvas,
        calendar: Calendar,
        renderParameters: RenderParameters
    ) {
        // It's only sensible to render a highlight for non-fixed ComplicationSlots because you
        // can't edit fixed complicationSlots.
        if (fixedComplicationProvider) {
            return
        }

        val bounds = computeBounds(Rect(0, 0, canvas.width, canvas.height))
        when (val highlightedElement = renderParameters.highlightLayer?.highlightedElement) {
            is HighlightedElement.AllComplicationSlots -> {
                renderer.drawHighlight(
                    canvas,
                    bounds,
                    boundsType,
                    calendar,
                    renderParameters.highlightLayer.highlightTint
                )
            }

            is HighlightedElement.ComplicationSlot -> {
                if (highlightedElement.id == id) {
                    renderer.drawHighlight(
                        canvas,
                        bounds,
                        boundsType,
                        calendar,
                        renderParameters.highlightLayer.highlightTint
                    )
                }
            }
        }
    }

    /**
     * Sets whether the complication should be drawn highlighted or not. This is to provide visual
     * feedback when the user taps on a complication.
     *
     * @param highlight Whether or not the complication should be drawn highlighted.
     */
    internal fun setIsHighlighted(highlight: Boolean) {
        renderer.isHighlighted = highlight
    }

    internal fun init(invalidateListener: InvalidateListener) {
        this.invalidateListener = invalidateListener
    }

    /** Computes the bounds of the complication by converting the unitSquareBounds to pixels. */
    public fun computeBounds(screen: Rect): Rect {
        // Try the current type if there is one, otherwise fall back to the bounds for the default
        // provider type.
        val unitSquareBounds =
            renderer.getData()?.let {
                complicationSlotBounds.perComplicationTypeBounds[it.type]
            } ?: complicationSlotBounds.perComplicationTypeBounds[defaultProviderType]!!
        unitSquareBounds.intersect(unitSquare)
        // We add 0.5 to make toInt() round to the nearest whole number rather than truncating.
        return Rect(
            (0.5f + unitSquareBounds.left * screen.width()).toInt(),
            (0.5f + unitSquareBounds.top * screen.height()).toInt(),
            (0.5f + unitSquareBounds.right * screen.width()).toInt(),
            (0.5f + unitSquareBounds.bottom * screen.height()).toInt()
        )
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("ComplicationSlot $id:")
        writer.increaseIndent()
        writer.println("fixedComplicationProvider=$fixedComplicationProvider")
        writer.println("enabled=$enabled")
        writer.println("renderer.isHighlighted=${renderer.isHighlighted}")
        writer.println("boundsType=$boundsType")
        writer.println("configExtras=$configExtras")
        writer.println("supportedTypes=${supportedTypes.joinToString { it.toString() }}")
        writer.println("initiallyEnabled=$initiallyEnabled")
        writer.println(
            "defaultProviderPolicy.primaryProvider=${defaultProviderPolicy.primaryProvider}"
        )
        writer.println(
            "defaultProviderPolicy.secondaryProvider=${defaultProviderPolicy.secondaryProvider}"
        )
        writer.println(
            "defaultProviderPolicy.systemProviderFallback=" +
                "${defaultProviderPolicy.systemProviderFallback}"
        )
        writer.println("data=${renderer.getData()}")
        val bounds = complicationSlotBounds.perComplicationTypeBounds.map {
            "${it.key} -> ${it.value}"
        }
        writer.println("bounds=[$bounds]")
        writer.decreaseIndent()
    }
}
