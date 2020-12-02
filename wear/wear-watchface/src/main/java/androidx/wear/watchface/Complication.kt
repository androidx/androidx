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

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.support.wearable.complications.ComplicationData
import androidx.annotation.UiThread
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.IdAndComplicationData
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyleSetting

/** Interface for rendering complications onto a [Canvas]. */
public interface CanvasComplication {
    /**
     * Called when the CanvasComplication attaches to a [Complication]. This will get called during
     * [Complication] initialization and if [Complication.renderer] is assigned with this
     * CanvasComplication.
     */
    @UiThread
    public fun onAttach(complication: Complication)

    /**
     * Called when the CanvasComplication detaches from a [Complication]. This will get called if
     * [Complication.renderer] is assigned to a different CanvasComplication.
     */
    @UiThread
    public fun onDetach()

    /**
     * Draws the complication defined by [idAndData] into the canvas with the specified bounds. This
     * will usually be called by user watch face drawing code, but the system may also call it
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
     * Whether the complication should be drawn highlighted. This is to provide visual feedback when
     * the user taps on a complication.
     */
    @Suppress("INAPPLICABLE_JVM_NAME") // https://stackoverflow.com/questions/47504279
    @get:JvmName("isHighlighted")
    @set:JvmName("setIsHighlighted")
    public var isHighlighted: Boolean

    /** The [IdAndComplicationData] to render. */
    public var idAndData: IdAndComplicationData?
}

/**
 * A complication rendered with [ComplicationDrawable] which renders complications in a material
 * design style. This renderer can't be shared by multiple complications.
 */
public open class CanvasComplicationDrawable(
    /** The [ComplicationDrawable] to render with. */
    drawable: ComplicationDrawable,

    /**
     * The watch's [WatchState] which contains details pertaining to (low-bit) ambient mode and
     * burn in protection needed to render correctly.
     */
    private val watchState: WatchState
) : CanvasComplication {

    init {
        drawable.callback = object :
            Drawable.Callback {
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {}

            @SuppressLint("SyntheticAccessor")
            override fun invalidateDrawable(who: Drawable) {
                attachedComplication?.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
        }
    }

    /** The [ComplicationDrawable] to render with. */
    public var drawable: ComplicationDrawable = drawable
        set(value) {
            field = value
            value.isInAmbientMode = watchState.isAmbient.value
            value.isLowBitAmbient = watchState.hasLowBitAmbient
            value.setBurnInProtection(watchState.hasBurnInProtection)

            attachedComplication?.scheduleUpdateComplications()
        }

    private val isAmbientObserver = Observer<Boolean> {
        drawable.isInAmbientMode = it
    }

    private var attachedComplication: Complication? = null

    /** {@inheritDoc} */
    override fun onAttach(complication: Complication) {
        attachedComplication = complication
        watchState.isAmbient.addObserver(isAmbientObserver)
    }

    /** {@inheritDoc} */
    override fun onDetach() {
        watchState.isAmbient.removeObserver(isAmbientObserver)
        attachedComplication = null
    }

    /** {@inheritDoc} */
    override fun render(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        renderParameters: RenderParameters
    ) {
        when (renderParameters.layerParameters[Layer.COMPLICATIONS]) {
            LayerMode.DRAW -> {
                drawable.bounds = bounds
                drawable.currentTimeMillis = calendar.timeInMillis
                drawable.draw(canvas)
            }
            LayerMode.DRAW_HIGHLIGHTED -> {
                drawable.bounds = bounds
                drawable.currentTimeMillis = calendar.timeInMillis
                drawable.draw(canvas)
                // If [RenderParameters.highlightedComplicationId] is set then only highlight if
                // the ids match.
                if (renderParameters.highlightedComplicationId == null ||
                    renderParameters.highlightedComplicationId == idAndData?.complicationId
                ) {
                    drawOutline(canvas, bounds, calendar)
                }
            }
            LayerMode.HIDE -> return
        }
    }

    /** Used (indirectly) by the editor, draws a dashed line around the complication. */
    public open fun drawOutline(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar
    ) {
        ComplicationOutlineRenderer.drawComplicationSelectOutline(canvas, bounds)
    }

    /**
     * Whether or not the complication should be drawn highlighted. Used to provide visual feedback
     * when the complication is tapped.
     */
    override var isHighlighted: Boolean
        @Suppress("INAPPLICABLE_JVM_NAME") // https://stackoverflow.com/questions/47504279
        @JvmName("isHighlighted")
        @UiThread
        get() = drawable.isHighlighted
        @Suppress("INAPPLICABLE_JVM_NAME") // https://stackoverflow.com/questions/47504279
        @JvmName("setIsHighlighted")
        @UiThread
        set(value) {
            drawable.isHighlighted = value
        }

    /** The [IdAndComplicationData] to use when rendering the complication. */
    override var idAndData: IdAndComplicationData? = null
        @UiThread
        set(value) {
            drawable.complicationData = value?.complicationData?.asWireComplicationData()
            field = value
        }
}

/**
 * Represents a individual complication on the screen. The number of complications is fixed
 * (see [ComplicationsManager]) but complications can be enabled or disabled via
 * [UserStyleSetting.ComplicationsUserStyleSetting].
 */
public class Complication internal constructor(
    internal val id: Int,
    @ComplicationBoundsType internal val boundsType: Int,
    complicationBounds: ComplicationBounds,
    canvasComplication: CanvasComplication,
    supportedTypes: List<ComplicationType>,
    defaultProviderPolicy: DefaultComplicationProviderPolicy,
    defaultProviderType: ComplicationType
) {
    public companion object {
        internal val unitSquare = RectF(0f, 0f, 1f, 1f)

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationBoundsType.ROUND_RECT]. This is the most common type of complication.
         * These can be single tapped by the user to either trigger the associated intent or
         * double tapped to open the provider selector.
         */
        @JvmStatic
        public fun createRoundRectComplicationBuilder(
            /**
             * The watch face's ID for this complication. Can be any integer but should be unique
             * within the watch face.
             */
            id: Int,

            /**
             * The [CanvasComplication] to use for rendering. Note renderers should not be shared
             * between complications.
             */
            renderer: CanvasComplication,

            /**
             * The types of complication supported by this Complication. Passed into
             * [ComplicationHelperActivity.createProviderChooserHelperIntent] during complication
             * configuration. This list should be non-empty.
             */
            supportedTypes: List<ComplicationType>,

            /**
             * The [DefaultComplicationProviderPolicy] used to select the initial complication
             * provider.
             */
            defaultProviderPolicy: DefaultComplicationProviderPolicy,

            /** The initial [ComplicationBounds]. */
            complicationBounds: ComplicationBounds
        ): Builder = Builder(
            id,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            ComplicationBoundsType.ROUND_RECT,
            complicationBounds
        )
        /**
         * Constructs a [Builder] for a complication with bound type
         * [ComplicationBoundsType.BACKGROUND] whose bounds cover the entire screen. A background
         * complication is for watch faces that wish to have a full screen user selectable
         * backdrop. This sort of complication isn't clickable and at most one may be present in
         * the list of complications.
         */
        @JvmStatic
        public fun createBackgroundComplicationBuilder(
            /**
             * The watch face's ID for this complication. Can be any integer but should be unique
             * within the watch face.
             */
            id: Int,

            /**
             * The [CanvasComplication] to use for rendering. Note renderers should not be shared
             * between complications.
             */
            renderer: CanvasComplication,

            /**
             * The types of complication supported by this Complication. Passed into
             * [ComplicationHelperActivity.createProviderChooserHelperIntent] during complication
             * configuration. This list should be non-empty.
             */
            supportedTypes: List<ComplicationType>,

            /**
             * The [DefaultComplicationProviderPolicy] used to select the initial complication
             * provider.
             */
            defaultProviderPolicy: DefaultComplicationProviderPolicy
        ): Builder = Builder(
            id,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            ComplicationBoundsType.BACKGROUND,
            ComplicationBounds(RectF(0f, 0f, 1f, 1f))
        )
    }

    /** Builder for constructing [Complication]s. */
    public class Builder internal constructor(
        private val id: Int,
        private val renderer: CanvasComplication,
        private val supportedTypes: List<ComplicationType>,
        private val defaultProviderPolicy: DefaultComplicationProviderPolicy,
        @ComplicationBoundsType private val boundsType: Int,
        private val complicationBounds: ComplicationBounds
    ) {
        private var defaultProviderType = ComplicationType.NOT_CONFIGURED

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

        /** Constructs the [Complication]. */
        public fun build(): Complication = Complication(
            id,
            boundsType,
            complicationBounds,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            defaultProviderType
        )
    }

    init {
        canvasComplication.onAttach(this)
    }

    internal interface InvalidateListener {
        /** Requests redraw. */
        @UiThread
        fun onInvalidate()
    }

    private lateinit var complicationsManager: ComplicationsManager
    private lateinit var invalidateListener: InvalidateListener

    internal var complicationBoundsDirty = true

    /**
     * The complication's [ComplicationBounds] which are converted to pixels during rendering.
     *
     * Note it's not allowed to change the bounds of a background complication because
     * they are assumed to always cover the entire screen.
     */
    public var complicationBounds: ComplicationBounds = complicationBounds
        @UiThread
        get
        @UiThread
        set(value) {
            require(boundsType != ComplicationBoundsType.BACKGROUND)
            if (field == value) {
                return
            }
            field = value
            complicationBoundsDirty = true

            // The caller might modify a number of complications. For efficiency we need to coalesce
            // these into one update task.
            complicationsManager.scheduleUpdate()
        }

    internal var enabledDirty = true

    /** Whether or not the complication should be drawn and accept taps. */
    public var enabled: Boolean = true
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

            // The caller might enable/disable a number of complications. For efficiency we need
            // to coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    /** The [CanvasComplication] used to render the complication. */
    public var renderer: CanvasComplication = canvasComplication
        @UiThread
        get
        @UiThread
        set(value) {
            if (field == value) {
                return
            }
            renderer.onDetach()
            value.idAndData = renderer.idAndData
            field = value
            value.onAttach(this)
        }

    internal var supportedTypesDirty = true

    /** The types of complications the complication supports. Must be non-empty. */
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

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    internal var defaultProviderPolicyDirty = true

    /**
     * The [DefaultComplicationProviderPolicy] which defines the default complications providers
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

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    internal var defaultProviderTypeDirty = true

    /**
     * The default [ComplicationData.ComplicationType] to use alongside [defaultProviderPolicy].
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

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    internal var dataDirty = true

    /**
     * The [androidx.wear.complications.data.ComplicationData] associated with the [Complication].
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
            else -> complicationData.value.isActiveAt(dateTimeMillis)
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
     * Sets whether the complication should be drawn highlighted or not. This is to provide visual
     * feedback when the user taps on a complication.
     *
     * @param highlight Whether or not the complication should be drawn highlighted.
     */
    internal fun setIsHighlighted(highlight: Boolean) {
        renderer.isHighlighted = highlight
    }

    /**
     * Requests redraw of the watch face. Useful when initialization is asynchronous, e.g. when
     * loading a [Drawable].
     */
    public fun invalidate() {
        invalidateListener.onInvalidate()
    }

    internal fun init(
        complicationsManager: ComplicationsManager,
        invalidateListener: InvalidateListener
    ) {
        this.complicationsManager = complicationsManager
        this.invalidateListener = invalidateListener
    }

    internal fun scheduleUpdateComplications() {
        // In tests this may not be initialized.
        if (this::complicationsManager.isInitialized) {
            // Update active complications to ensure accessibility data is up to date.
            complicationsManager.scheduleUpdate()
        }
    }

    /** Computes the bounds of the complication by converting the unitSquareBounds to pixels. */
    internal fun computeBounds(screen: Rect): Rect {
        // Try the current type if there is one, otherwise fall back to the bounds for the default
        // provider type.
        val unitSquareBounds =
            renderer.idAndData?.let {
                complicationBounds.perComplicationTypeBounds[it.complicationData.type]
            } ?: complicationBounds.perComplicationTypeBounds[defaultProviderType]!!
        unitSquareBounds.intersect(unitSquare)
        return Rect(
            (unitSquareBounds.left * screen.width()).toInt(),
            (unitSquareBounds.top * screen.height()).toInt(),
            (unitSquareBounds.right * screen.width()).toInt(),
            (unitSquareBounds.bottom * screen.height()).toInt()
        )
    }
}
