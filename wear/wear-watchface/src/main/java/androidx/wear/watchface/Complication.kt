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
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.Layer

/** Common interface for rendering complications onto a [Canvas]. */
public interface CanvasComplicationRenderer {
    /**
     * Called when the CanvasComplicationRenderer attaches to a [Complication].
     */
    @UiThread
    public fun onAttach(complication: Complication)

    /**
     * Called when the CanvasComplicationRenderer detaches from a [Complication].
     */
    @UiThread
    public fun onDetach()

    /**
     * Draws the complication into the canvas with the specified bounds. This will usually be
     * called by user watch face drawing code, but the system may also call it for complication
     * selection UI rendering. The width and height will be the same as that computed by
     * computeBounds but the translation and canvas size may differ.
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
     * Sets whether the complication should be drawn highlighted. This is to provide visual
     * feedback when the user taps on a complication.
     *
     * @param highlight Whether or not the complication should be drawn highlighted.
     */
    @UiThread
    public fun setIsHighlighted(highlight: Boolean)

    /**
     * Sets the current [ComplicationData].
     *
     * @param data The [ComplicationData]
     */
    @UiThread
    public fun setData(data: ComplicationData?)

    /**
     * Returns the current [ComplicationData] associated with the CanvasComplicationRenderer.
     */
    @UiThread
    public fun getData(): ComplicationData?

    public interface InvalidateCallback {
        /** Requests redraw. */
        @UiThread
        public fun onInvalidate()
    }

    /**
     * Called by the [WatchFace]
     *
     * @param callback The [InvalidateCallback] to register
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun setInvalidateCallback(callback: InvalidateCallback)
}

/**
 * A complication rendered with [ComplicationDrawable] which renders complications in a
 * material design style. This renderer can't be shared by multiple complications.
 */
public open class CanvasComplicationDrawableRenderer(
    /** The actual complication. */
    drawable: ComplicationDrawable,

    private val watchState: WatchState
) : CanvasComplicationRenderer {
    private var _drawable = drawable

    public var drawable: ComplicationDrawable
        get() = _drawable
        set(value) {
            _drawable = value
            _drawable.inAmbientMode = watchState.isAmbient.value
            _drawable.lowBitAmbient = watchState.hasLowBitAmbient
            _drawable.setBurnInProtection(watchState.hasBurnInProtection)

            attachedComplication?.scheduleUpdateComplications()
        }

    private val isAmbientObserver = Observer<Boolean> {
        drawable.inAmbientMode = it
    }

    private var attachedComplication: Complication? = null
    private var complicationData: ComplicationData? = null

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
                drawHighlight(canvas, bounds, calendar)
            }
            LayerMode.HIDE -> return
        }
    }

    /** Used (indirectly) by the editor, draws a highlight around the complication. */
    public open fun drawHighlight(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar
    ) {
        ComplicationOutlineRenderer.drawComplicationSelectOutline(canvas, bounds)
    }

    /** {@inheritDoc} */
    override fun setIsHighlighted(highlight: Boolean) {
        drawable.highlighted = highlight
    }

    /** {@inheritDoc} */
    override fun setData(data: ComplicationData?) {
        drawable.complicationData = data
        complicationData = data
    }

    /** {@inheritDoc} */
    override fun getData(): ComplicationData? = complicationData

    /** {@inheritDoc} */
    @SuppressLint("ExecutorRegistration")
    override fun setInvalidateCallback(callback: CanvasComplicationRenderer.InvalidateCallback) {
        drawable.callback = object :
            Drawable.Callback {
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {}

            override fun invalidateDrawable(who: Drawable) {
                callback.onInvalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
        }
    }
}

/**
 * Represents a individual complication on the screen. The number of complications is fixed
 * (see [ComplicationsManager]) but complications can be enabled or disabled as needed.
 */
public class Complication internal constructor(
    internal val id: Int,
    @ComplicationBoundsType internal val boundsType: Int,
    unitSquareBounds: RectF,
    renderer: CanvasComplicationRenderer,
    supportedTypes: IntArray,
    defaultProviderPolicy: DefaultComplicationProviderPolicy,
    defaultProviderType: Int
) {
    /** @hide */
    private companion object {
        internal val unitSquare = RectF(0f, 0f, 1f, 1f)
    }

    public class Builder(
        /** The watch face's ID for this complication. */
        private val id: Int,

        /**
         * The renderer for this Complication. Renderers may not be sharable between complications.
         */
        private val renderer: CanvasComplicationRenderer,

        /**
         * The types of complication supported by this Complication. Passed into
         * [ComplicationHelperActivity.createProviderChooserHelperIntent] during complication
         * configuration.
         */
        private val supportedTypes: IntArray,

        /** Default complication provider. */
        private val defaultProviderPolicy: DefaultComplicationProviderPolicy
    ) {
        @ComplicationBoundsType
        private var boundsType: Int = ComplicationBoundsType.ROUND_RECT
        private lateinit var unitSquareBounds: RectF

        private var defaultProviderType: Int = WatchFace.DEFAULT_PROVIDER_TYPE_NONE

        /**
         * Sets the default complication provider data type. See [ComplicationData.ComplicationType]
         */
        public fun setDefaultProviderType(
            @ComplicationData.ComplicationType defaultProviderType: Int
        ): Builder {
            this.defaultProviderType = defaultProviderType
            return this
        }

        /**
         * Fractional bounds for the complication, clamped to the unit square [0..1], which get
         * converted to screen space coordinates. NB 0 and 1 are included in the unit square.
         */
        public fun setUnitSquareBounds(unitSquareBounds: RectF): Builder {
            boundsType = ComplicationBoundsType.ROUND_RECT

            this.unitSquareBounds = RectF().apply {
                setIntersect(
                    unitSquareBounds,
                    unitSquare
                )
            }
            return this
        }

        /**
         * A background complication is for watch faces that wish to have a full screen user
         * selectable backdrop. This sort of complication isn't clickable and at most one may be
         * present in the list of complications.
         */
        public fun setBackgroundComplication(): Builder {
            boundsType = ComplicationBoundsType.BACKGROUND
            this.unitSquareBounds = RectF(0f, 0f, 1f, 1f)
            return this
        }

        public fun build(): Complication = Complication(
            id,
            boundsType,
            unitSquareBounds,
            renderer,
            supportedTypes,
            defaultProviderPolicy,
            defaultProviderType
        )
    }

    init {
        renderer.onAttach(this)
    }

    private lateinit var complicationsManager: ComplicationsManager
    private lateinit var invalidateCallback: CanvasComplicationRenderer.InvalidateCallback

    private var _unitSquareBounds = unitSquareBounds
    internal var unitSquareBoundsDirty = true
    /**
     * The screen space unit-square bounds of the complication. This is converted to pixels during
     * rendering.
     */
    public var unitSquareBounds: RectF
        @UiThread
        get() = _unitSquareBounds
        @UiThread
        set(value) {
            if (_unitSquareBounds == value) {
                return
            }
            _unitSquareBounds = value
            unitSquareBoundsDirty = true

            // The caller might modify a number of complications. For efficiency we need to coalesce
            // these into one update task.
            complicationsManager.scheduleUpdate()
        }

    private var _enabled = true
    internal var enabledDirty = true
    /**
     * Whether or not the complication should be drawn and accept taps.
     */
    public var enabled: Boolean
        @JvmName("isEnabled")
        @UiThread
        get() = _enabled
        @UiThread
        set(value) {
            if (_enabled == value) {
                return
            }
            _enabled = value
            enabledDirty = true

            // The caller might enable/disable a number of complications. For efficiency we need
            // to coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    private var _renderer = renderer
    /**
     * The [CanvasComplicationRenderer] used to render the complication.
     */
    public var renderer: CanvasComplicationRenderer
        @UiThread
        get() = _renderer
        @UiThread
        set(value) {
            if (_renderer == value) {
                return
            }
            renderer.onDetach()
            value.setData(renderer.getData())
            _renderer = value
            value.onAttach(this)
            initRenderer()
        }

    private var _supportedTypes = supportedTypes
    internal var supportedTypesDirty = true
    /**
     * The types of complications the complication supports.
     */
    public var supportedTypes: IntArray
        @UiThread
        get() = _supportedTypes
        @UiThread
        set(value) {
            if (_supportedTypes == value) {
                return
            }
            _supportedTypes = value
            supportedTypesDirty = true

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    private var _defaultProviderPolicy = defaultProviderPolicy
    internal var defaultProviderPolicyDirty = true
    /**
     * The [DefaultComplicationProviderPolicy] which defines the default complications providers
     * selected when the user hasn't yet made a choice. See also [.defaultProviderType].
     */
    public var defaultProviderPolicy: DefaultComplicationProviderPolicy
        @UiThread
        get() = _defaultProviderPolicy
        @UiThread
        set(value) {
            if (_defaultProviderPolicy == value) {
                return
            }
            _defaultProviderPolicy = value
            defaultProviderPolicyDirty = true

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    private var _defaultProviderType = defaultProviderType
    internal var defaultProviderTypeDirty = true
    /**
     * The default [ComplicationData.ComplicationType] to use alongside [.defaultProviderPolicy].
     */
    public var defaultProviderType: Int
        @UiThread
        get() = _defaultProviderType
        @UiThread
        set(value) {
            if (_defaultProviderType == value) {
                return
            }
            _defaultProviderType = value
            defaultProviderTypeDirty = true

            // The caller might modify a number of complications. For efficiency we need to
            // coalesce these into one update task.
            if (this::complicationsManager.isInitialized) {
                complicationsManager.scheduleUpdate()
            }
        }

    internal var dataDirty = true

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
        renderer.setIsHighlighted(highlight)
    }

    private fun initRenderer() {
        // Renderers may register a user style listener during their initializer which can call
        // setComplicationRenderer() before complicationInvalidateCallback has been initialized.
        if (this::invalidateCallback.isInitialized) {
            renderer.setInvalidateCallback(invalidateCallback)
        }
    }

    internal fun init(
        complicationsManager: ComplicationsManager,
        invalidateCallback: CanvasComplicationRenderer.InvalidateCallback
    ) {
        this.complicationsManager = complicationsManager
        this.invalidateCallback = invalidateCallback
        initRenderer()
    }

    internal fun scheduleUpdateComplications() {
        // In tests this may not be initialized.
        if (this::complicationsManager.isInitialized) {
            // Update active complications to ensure accessibility data is up to date.
            complicationsManager.scheduleUpdate()
        }
    }

    /** Computes the bounds of the complication by converting the unitSquareBounds to pixels. */
    internal fun computeBounds(screen: Rect) =
        Rect(
            (unitSquareBounds.left * screen.width()).toInt(),
            (unitSquareBounds.top * screen.height()).toInt(),
            (unitSquareBounds.right * screen.width()).toInt(),
            (unitSquareBounds.bottom * screen.height()).toInt()
        )
}
