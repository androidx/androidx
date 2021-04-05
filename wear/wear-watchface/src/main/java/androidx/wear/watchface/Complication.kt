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
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.ComplicationData
import androidx.wear.utility.TraceEvent
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.Layer
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting

/**
 * A complication rendered with [ComplicationDrawable] which renders complications in a material
 * design style. This renderer can't be shared by multiple complications.
 *
 * @param _drawable The [ComplicationDrawable] to render with.
 * @param watchState The watch's [WatchState] which contains details pertaining to (low-bit) ambient
 *     mode and burn in protection needed to render correctly.
 */
public open class CanvasComplicationDrawable(
    _drawable: ComplicationDrawable,
    private val watchState: WatchState
) {
    init {
        _drawable.callback = object :
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
    public var drawable: ComplicationDrawable = _drawable
        set(value) {
            // Copy the ComplicationData otherwise the complication will be blank until the next
            // update.
            value.setComplicationData(field.complicationData, false)
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

    /**
     * Called when the CanvasComplication attaches to a [Complication]. This will get called during
     * [Complication] initialization and if [Complication.renderer] is assigned with this
     * CanvasComplication.
     */
    @UiThread
    public fun onAttach(complication: Complication) {
        attachedComplication = complication
        watchState.isAmbient.addObserver(isAmbientObserver)
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
     * @param complicationId The Id of the parent [Complication]
     */
    @UiThread
    public open fun render(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        renderParameters: RenderParameters,
        complicationId: Int
    ) {
        when (renderParameters.layerParameters[Layer.COMPLICATIONS]) {
            LayerMode.DRAW -> {
                drawable.bounds = bounds
                drawable.currentTimeMillis = calendar.timeInMillis
                drawable.draw(canvas)
            }
            LayerMode.DRAW_OUTLINED -> {
                drawable.bounds = bounds
                drawable.currentTimeMillis = calendar.timeInMillis
                val wasHighlighted = drawable.isHighlighted
                drawable.isHighlighted = renderParameters.selectedComplicationId == complicationId
                drawable.draw(canvas)
                drawable.isHighlighted = wasHighlighted

                // It's only sensible to render a highlight for non-background complications.
                if (attachedComplication?.boundsType != ComplicationBoundsType.BACKGROUND) {
                    drawOutline(canvas, bounds, calendar, renderParameters.outlineTint)
                }
            }
            LayerMode.HIDE -> return
        }
    }

    /**
     * Used (indirectly) by the editor, draws a dashed line around the complication unless the.
     * [Complication] is fixed in which case it does nothing.
     */
    public open fun drawOutline(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        @ColorInt color: Int
    ) {
        if (!attachedComplication!!.fixedComplicationProvider) {
            ComplicationOutlineRenderer.drawComplicationOutline(
                canvas,
                bounds,
                color
            )
        }
    }

    /**
     * Whether the complication should be drawn highlighted. This is to provide visual feedback when
     * the user taps on a complication.
     */
    public var isHighlighted: Boolean
        @JvmName("isHighlighted")
        @UiThread
        get() = drawable.isHighlighted
        @JvmName("setIsHighlighted")
        @UiThread
        set(value) {
            drawable.isHighlighted = value
        }

    private var _data: ComplicationData? = null

    /** Returns the [ComplicationData] to render with. */
    public fun getData(): ComplicationData? = _data

    /**
     * Sets the [ComplicationData] to render with and loads any [Drawable]s contained within the
     * ComplicationData. You can choose whether this is done synchronously or asynchronously via
     * [loadDrawablesAsynchronous].
     *
     * @param complicationData The [ComplicationData] to render with
     * @param loadDrawablesAsynchronous Whether or not any drawables should be loaded asynchronously
     */
    @CallSuper
    public open fun loadData(
        complicationData: ComplicationData?,
        loadDrawablesAsynchronous: Boolean
    ): Unit = TraceEvent("CanvasComplicationDrawable.setIdAndData").use {
        _data = complicationData
        drawable.setComplicationData(
            complicationData?.asWireComplicationData(),
            loadDrawablesAsynchronous
        )
    }
}

/**
 * Represents a individual complication on the screen. The number of complications is fixed
 * (see [ComplicationsManager]) but complications can be enabled or disabled via
 * [UserStyleSetting.ComplicationsUserStyleSetting].
 *
 * @param id The Watch Face's ID for the complication.
 * @param boundsType The [ComplicationBoundsType] of the complication.
 * @param bounds The complication's [ComplicationBounds].
 * @param renderer The [CanvasComplicationDrawable] used to render the complication.
 * @param supportedTypes The list of [ComplicationType]s accepted by this complication. Passed
 *     into [ComplicationHelperActivity.createProviderChooserHelperIntent] during complication
 *     configuration. This list should be non-empty.
 * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] which controls the initial
 *     provider when the watch face is first installed.
 * @param defaultProviderType The default [ComplicationType] for the default provider.
 * @param initiallyEnabled At creation a complication is either enabled or disabled. This can be
 *     overridden by a [ComplicationsUserStyleSetting] (see [ComplicationOverlay.enabled]).
 *     Editors need to know the initial state of a complication to predict the effects of making a
 *     style change.
 * @param configExtras Extras to be merged into the Intent sent when invoking the provider chooser
 *     activity.
 * @param fixedComplicationProvider  Whether or not the complication provider is fixed (i.e.
 *     can't be changed by the user).  This is useful for watch faces built around specific
 *     complications.
 */
public class Complication internal constructor(
    internal val id: Int,
    @ComplicationBoundsType public val boundsType: Int,
    bounds: ComplicationBounds,
    public val renderer: CanvasComplicationDrawable,
    supportedTypes: List<ComplicationType>,
    defaultProviderPolicy: DefaultComplicationProviderPolicy,
    defaultProviderType: ComplicationType,
    @get:JvmName("isInitiallyEnabled")
    public val initiallyEnabled: Boolean,
    public val configExtras: Bundle,
    @get:JvmName("isFixedComplicationProvider")
    public val fixedComplicationProvider: Boolean
) {
    public companion object {
        internal val unitSquare = RectF(0f, 0f, 1f, 1f)

        /**
         * Constructs a [Builder] for a complication with bounds type
         * [ComplicationBoundsType.ROUND_RECT]. This is the most common type of complication.
         * These can be single tapped by the user to either trigger the associated intent or
         * double tapped to open the provider selector.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *     unique within the watch face.
         * @param renderer The [CanvasComplicationDrawable] to use for rendering. Note renderers
         *     should not be shared between complications.
         * @param supportedTypes The types of complication supported by this Complication. Passed
         *     into [ComplicationHelperActivity.createProviderChooserHelperIntent] during
         *     complication configuration. This list should be non-empty.
         * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
         *     the initial complication provider when the watch is first installed.
         * @param bounds The complication's [ComplicationBounds].
         */
        @JvmStatic
        public fun createRoundRectComplicationBuilder(
            id: Int,
            renderer: CanvasComplicationDrawable,
            supportedTypes: List<ComplicationType>,
            defaultProviderPolicy: DefaultComplicationProviderPolicy,
            bounds: ComplicationBounds
        ): Builder = Builder(
            id,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            ComplicationBoundsType.ROUND_RECT,
            bounds
        )

        /**
         * Constructs a [Builder] for a complication with bound type
         * [ComplicationBoundsType.BACKGROUND] whose bounds cover the entire screen. A background
         * complication is for watch faces that wish to have a full screen user selectable
         * backdrop. This sort of complication isn't clickable and at most one may be present in
         * the list of complications.
         *
         * @param id The watch face's ID for this complication. Can be any integer but should be
         *     unique within the watch face.
         * @param renderer The [CanvasComplicationDrawable] to use for rendering. Note renderers
         *     should not be shared between complications.
         * @param supportedTypes The types of complication supported by this Complication. Passed
         *     into [ComplicationHelperActivity.createProviderChooserHelperIntent] during
         *     complication configuration. This list should be non-empty.
         * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
         *     the initial complication provider when the watch is first installed.
         */
        @JvmStatic
        public fun createBackgroundComplicationBuilder(
            id: Int,
            renderer: CanvasComplicationDrawable,
            supportedTypes: List<ComplicationType>,
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

    /**
     * Builder for constructing [Complication]s.
     *
     * @param id The watch face's ID for this complication. Can be any integer but should be unique
     *     within the watch face.
     * @param renderer The [CanvasComplicationDrawable] to use for rendering. Note renderers should
     *     not be shared between complications.
     * @param supportedTypes The types of complication supported by this Complication. Passed into
     *     [ComplicationHelperActivity.createProviderChooserHelperIntent] during complication
     *     configuration. This list should be non-empty.
     * @param defaultProviderPolicy The [DefaultComplicationProviderPolicy] used to select
     *     the initial complication provider when the watch is first installed.
     * @param boundsType The [ComplicationBoundsType] of the complication.
     * @param bounds The complication's [ComplicationBounds].
     */
    public class Builder internal constructor(
        private val id: Int,
        private val renderer: CanvasComplicationDrawable,
        private val supportedTypes: List<ComplicationType>,
        private val defaultProviderPolicy: DefaultComplicationProviderPolicy,
        @ComplicationBoundsType private val boundsType: Int,
        private val bounds: ComplicationBounds
    ) {
        private var defaultProviderType = ComplicationType.NOT_CONFIGURED
        private var initiallyEnabled = true
        private var configExtras: Bundle = Bundle.EMPTY
        private var fixedComplicationProvider = false

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
         * be overridden by [ComplicationsUserStyleSetting].
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

        /** Constructs the [Complication]. */
        public fun build(): Complication = Complication(
            id,
            boundsType,
            bounds,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            defaultProviderType,
            initiallyEnabled,
            configExtras,
            fixedComplicationProvider
        )
    }

    init {
        renderer.onAttach(this)
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
    public var complicationBounds: ComplicationBounds = bounds
        @UiThread
        get
        @UiThread
        internal set(value) {
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

            // The caller might enable/disable a number of complications. For efficiency we need
            // to coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
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
        renderer.render(canvas, bounds, calendar, renderParameters, id)
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
    public fun computeBounds(screen: Rect): Rect {
        // Try the current type if there is one, otherwise fall back to the bounds for the default
        // provider type.
        val unitSquareBounds =
            renderer.getData()?.let {
                complicationBounds.perComplicationTypeBounds[it.type]
            } ?: complicationBounds.perComplicationTypeBounds[defaultProviderType]!!
        unitSquareBounds.intersect(unitSquare)
        return Rect(
            (unitSquareBounds.left * screen.width()).toInt(),
            (unitSquareBounds.top * screen.height()).toInt(),
            (unitSquareBounds.right * screen.width()).toInt(),
            (unitSquareBounds.bottom * screen.height()).toInt()
        )
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("Complication $id:")
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
        val bounds = complicationBounds.perComplicationTypeBounds.map {
            "${it.key} -> ${it.value}"
        }
        writer.println("bounds=[$bounds]")
        writer.decreaseIndent()
    }
}
