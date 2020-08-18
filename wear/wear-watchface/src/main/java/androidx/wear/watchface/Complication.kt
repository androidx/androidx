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

import android.content.ComponentName
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.icu.util.Calendar
import android.support.wearable.complications.ComplicationData
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.rendering.ComplicationDrawable

/**
 * Complications own a ComplicationBoundsProvider which is used to compute its bounds.
 */
interface ComplicationBoundsProvider {
    /**
     * Computes the screen space bounds of the complication. This can be animated.
     *
     * @param complication The {@link Complication} to compute bounds for
     * @param screen A {@link Rect} describing the bounds of the screen
     * @param calendar The current {@link Calendar}, required for animating complications
     */
    fun computeBounds(complication: Complication, screen: Rect, calendar: Calendar): Rect
}

/**
 * Some watch faces may wish to support an optional background image complication covering the
 * whole screen. This complication isn't clickable and at most one may be present in the list of
 * complications. If a BackgroundComplication is present then WatchFaceConfigActivity will
 * display a ListView asking the user to select between selecting a complication to configure
 * or the background complication.
 */
class BackgroundComplicationBounds : ComplicationBoundsProvider {
    /** {@inheritDoc} */
    override fun computeBounds(
        complication: Complication,
        screen: Rect,
        calendar: Calendar
    ) = screen // A BackgroundComplication covers the whole screen.
}

/** Helper for complications that don't animate. */
class FixedBounds(
    /**
     * Fractional bounds for the complication which get converted to screen space coordinates.
     */
    private val unitSquareBounds: RectF
) : ComplicationBoundsProvider {
    /** {@inheritDoc} */
    override fun computeBounds(complication: Complication, screen: Rect, calendar: Calendar) =
        Rect(
            (unitSquareBounds.left * screen.width()).toInt(),
            (unitSquareBounds.top * screen.height()).toInt(),
            (unitSquareBounds.right * screen.width()).toInt(),
            (unitSquareBounds.bottom * screen.height()).toInt()
        )
}

/** Common interface for rendering complications. */
interface ComplicationRenderer {
    /**
     * Called when the ComplicationRenderer attaches to a {@link Complication}.
     */
    fun onAttach()

    /**
     * Called when the ComplicationRenderer detaches from a {@link Complication}.
     */
    fun onDetach()

    /**
     * Draws the complication into the canvas with the specified bounds. This will usually be
     * called by user watch face drawing code, but the system may also call it for complication
     * selection UI rendering. The width and height will be the same as that computed by
     * computeBounds but the translation and canvas size may differ.
     *
     * @param canvas The {@link Canvas} to render into
     * @param bounds A {@link Rect} describing the bounds of the complication
     * @param calendar The current {@link Calendar}
     * @param drawMode The current {@link DrawMode}
     */
    fun onDraw(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        @DrawMode drawMode: Int
    )

    /**
     * Sets whether the complication should be drawn highlighted. This is to provide visual
     * feedback when the user taps on a complication.
     *
     * @param highlight Whether or not the complication should be drawn highlighted.
     */
    fun setIsHighlighted(highlight: Boolean)

    /**
     * Sets the current {@link ComplicationData}.
     *
     * @param data The {@link ComplicationData}
     */
    fun setComplicationData(data: ComplicationData?)

    /**
     * Returns the current ComplicationData associated with the ComplicationRenderer.
     */
    fun getComplicationData(): ComplicationData?

    interface InvalidateCallback {
        /** Requests redraw. */
        fun invalidate()
    }

    /**
     * Called by the {@link WatchFace}
     *
     * @param callback The {@link InvalidateCallback} to register
     */
    fun setInvalidateCallback(callback: InvalidateCallback)
}

/**
 * A complication rendered with ComplicationDrawable which does a lot of hard work for you. This
 * renderer can't be shared by multiple complications.
 */
open class ComplicationDrawableRenderer(
    /** The actual complication. */
    protected val drawable: ComplicationDrawable,

    private val systemState: SystemState
) : ComplicationRenderer {

    private inner class SystemStateListener : SystemState.Listener {
        override fun onAmbientModeChanged(isAmbient: Boolean) {
            drawable.setInAmbientMode(isAmbient)
        }
    }

    private val systemStateListener = SystemStateListener()

    private var complicationData: ComplicationData? = null

    /** {@inheritDoc} */
    override fun onAttach() {
        drawable.setInAmbientMode(systemState.isAmbient)
        drawable.setLowBitAmbient(systemState.hasLowBitAmbient)
        drawable.setBurnInProtection(systemState.hasBurnInProtection)

        systemState.addListener(systemStateListener)
    }

    /** {@inheritDoc} */
    override fun onDetach() {
        systemState.removeListener(systemStateListener)
    }

    /** {@inheritDoc} */
    override fun onDraw(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        @DrawMode drawMode: Int
    ) {
        drawable.bounds = bounds
        drawable.draw(
            canvas,
            calendar.timeInMillis
        )
    }

    /** {@inheritDoc} */
    override fun setIsHighlighted(highlight: Boolean) {
        drawable.highlighted = highlight
    }

    /** {@inheritDoc} */
    override fun setComplicationData(data: ComplicationData?) {
        drawable.setComplicationData(data)
        complicationData = data
    }

    /** {@inheritDoc} */
    override fun getComplicationData() = complicationData

    /** {@inheritDoc} */
    override fun setInvalidateCallback(callback: ComplicationRenderer.InvalidateCallback) {
        drawable.callback = object :
            Drawable.Callback {
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {}

            override fun invalidateDrawable(who: Drawable) {
                callback.invalidate()
            }

            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
        }
    }
}

/**
 * Represents an individual complication.
 */
class Complication @JvmOverloads constructor(
    /** The watch face's ID for this complication. */
    val id: Int,

    /**
     * An interface that can provide the bounds for this complication. Helps avoid multiple
     * inheritance.
     */
    val boundsProvider: ComplicationBoundsProvider,

    /**
     * The renderer for this complication. Renderers may not be sharable between complications.
     */
    complicationRenderer: ComplicationRenderer,

    /**
     * Passed into ComplicationHelperActivity.createProviderChooserHelperIntent during
     * complication configuration.
     */
    internal val supportedComplicationDataTypes: IntArray,

    /** Default complication provider. */
    internal val defaultProvider: DefaultComplicationProvider,

    /** Default complication provider data type. */
    internal val defaultProviderType: Int = WatchFace.DEFAULT_PROVIDER_TYPE_NONE
) {
    init {
        complicationRenderer.onAttach()
    }

    /**
     * A watch face may wish to try and set one or more non-system providers as the default provider
     * for a complication. If a provider can't be used for some reason (e.g. it isn't installed or
     * it doesn't support the requested type, or the watch face lacks the necessary permission)
     * then the next one will be tried. A system provider acts as a final fallback in case no
     * non-system providers can be used.
     *
     * If the DefaultComplicationProvider is empty then no default is set.
     */
    class DefaultComplicationProvider(
        /** List of up to two non-system providers to be tried in turn. This may be empty. */
        val providers: List<ComponentName> = listOf(),

        /** Fallback in case none of the non-system providers could be used. */
        @SystemProviders.ProviderId val systemProviderFallback: Int = WatchFace.NO_DEFAULT_PROVIDER
    ) {
        constructor(systemProviderFallback: Int) : this(listOf(), systemProviderFallback)

        fun isEmpty() =
            providers.isEmpty() && systemProviderFallback == WatchFace.NO_DEFAULT_PROVIDER
    }

    private lateinit var complicationSlots: ComplicationSlots
    private lateinit var complicationInvalidateCallback: ComplicationRenderer.InvalidateCallback

    private var _complicationRenderer: ComplicationRenderer = complicationRenderer
    var complicationRenderer: ComplicationRenderer
        get() = _complicationRenderer
        set(value) {
            complicationRenderer.onDetach()
            _complicationRenderer = value
            complicationRenderer.onAttach()
            initRenderer()
        }

    private var _enabled = true

    var enabled: Boolean
        get() = _enabled
        set(value) {
            _enabled = value

            // The caller might enable/disable a number of complications. For efficiency we need
            // to coalesce these into one update task.
            complicationSlots.scheduleUpdateActiveComplications()
        }

    /** Any data for the complication. */
    private var _complicationData: ComplicationData? = null

    var complicationData: ComplicationData?
        get() = _complicationData
        internal set(value) {
            _complicationData = value
            complicationRenderer.setComplicationData(_complicationData)

            // In tests this may not be initialized.
            if (this::complicationSlots.isInitialized) {
                // Update active complications to ensure accessibility data is up to date.
                complicationSlots.scheduleUpdateActiveComplications()
            }
        }

    /**
     * Watch faces should use this method to render a complication. Note the system may call this.
     *
     * @param canvas The {@link Canvas} to render into
     * @param calendar The current {@link Calendar}
     * @param drawMode The current {@link DrawMode}
     */
    fun draw(
        canvas: Canvas,
        calendar: Calendar,
        @DrawMode drawMode: Int
    ) {
        val bounds = boundsProvider.computeBounds(
            this,
            Rect(0, 0, canvas.width, canvas.height), calendar
        )
        complicationRenderer.onDraw(canvas, bounds, calendar, drawMode)
    }

    /**
     * Returns {@code true} if the complication doesn't have any data or it's not configured.
     *
     * @return {@code true} if the complication doesn't have any data or it's not configured
     */
    fun isEmpty(): Boolean {
        val data = this.complicationData
        return data == null || data.type == ComplicationData.TYPE_EMPTY ||
                data.type == ComplicationData.TYPE_NOT_CONFIGURED
    }

    private fun initRenderer() {
        complicationRenderer.setComplicationData(complicationData)

        // Renderers may register a user style listener during their initializer which can call
        // setComplicationRenderer() before complicationInvalidateCallback has been initialized.
        if (this::complicationInvalidateCallback.isInitialized) {
            complicationRenderer.setInvalidateCallback(complicationInvalidateCallback)
        }
    }

    internal fun init(
        complicationSlots: ComplicationSlots,
        complicationInvalidateCallback: ComplicationRenderer.InvalidateCallback
    ) {
        this.complicationSlots = complicationSlots
        this.complicationInvalidateCallback = complicationInvalidateCallback
        initRenderer()
    }
}
