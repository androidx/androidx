/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.locationbutton

import android.annotation.SuppressLint
import android.app.Activity
import android.app.permissionui.LocationButtonClient
import android.app.permissionui.LocationButtonProvider
import android.app.permissionui.LocationButtonProviderFactory
import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Trace
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.core.content.res.use

/**
 * A widget that provides a session-based precise location permission button. It either draws the
 * button remotely rendered by System on supported platforms on and after
 * `Build.VERSION_CODES.CINNAMON_BUN`, or falls back to a locally rendered button on older
 * platforms. On older platforms, the button click delegates to
 * [LocationButtonListener.onRequestPermissions], allowing the app to handle the click (e.g., by
 * requesting permissions or performing actions).
 */
@SuppressLint("NewApi")
public class LocationButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.locationButtonStyle,
    defStyleRes: Int = R.style.Widget_AndroidXCore_LocationButton,
) : ViewGroup(context, attrs, defStyleAttr, defStyleRes) {
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TEXT_TYPE_NONE,
        TEXT_TYPE_PRECISE_LOCATION,
        TEXT_TYPE_USE_PRECISE_LOCATION,
        TEXT_TYPE_SHARE_PRECISE_LOCATION,
        TEXT_TYPE_NEAR_MY_PRECISE_LOCATION,
        TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION,
    )
    public annotation class LocationButtonTextType

    /** Remotely rendered location button is hosted in this surface view */
    private var surfaceView: SurfaceView? = null

    /**
     * Locally rendered location button, this button provides an alternate to remote location button
     * on older platforms. Clicking on this button will trigger existing permission request dialog.
     *
     * This button is also used in measuring the width for remote location button to help implement
     * wrap_content for SurfaceView.
     */
    private val localButtonView: LocalLocationButton

    /** Location button provider entry point for remote location button rendering */
    private var provider: LocationButtonProvider? = null

    /** Location button session handle to communicate to remote with remote rendering service. */
    private var session: LocationButtonSession? = null

    /** Client callback for location button events, provided by apps. */
    private var locationButtonListener: LocationButtonListener? = null

    /** Once initialized, can't add more views. */
    private var initialized = false

    // -- Style Attributes --
    private var textColor = 0
    private var backgroundColor = 0
    private var iconTint = 0
    private var cornerRadius = 0f
    private var pressedCornerRadius = 0f
    private var strokeColor = 0
    private var strokeWidth = 0
    private var textType = LocationButtonSession.TEXT_TYPE_PRECISE_LOCATION
    private var maxLines = -1
    private var textAllCaps = false
    private var includeFontPadding = true

    init {
        // Setup SurfaceView if remote rendering is supported
        if (isRemoteButtonSupported) {
            provider = LocationButtonProviderFactory.create(context)
            surfaceView =
                SurfaceView(context).apply {
                    holder.setFormat(PixelFormat.TRANSPARENT)
                    visibility = INVISIBLE
                }
            addView(surfaceView)
        }

        localButtonView = LocalLocationButton(context)
        if (!isRemoteButtonSupported) {
            localButtonView.setOnClickListener { locationButtonListener?.onRequestPermissions() }
        }
        addView(localButtonView)

        applyStyleAttributes(attrs, defStyleAttr, defStyleRes)
        initialized = true
    }

    private val openSessionRunnable = Runnable {
        @Suppress("DEPRECATION") val hostToken = surfaceView!!.hostToken ?: return@Runnable
        Trace.beginAsyncSection("LocationButton.openSession", 0)

        val request = createButtonRequest()

        val clientCallback =
            Api37Impl.createClient(
                onPermissionResultHandler = { isGranted ->
                    locationButtonListener?.onPermissionResult(isGranted)
                },
                onSessionErrorHandler = { t ->
                    closeSession()
                    locationButtonListener?.onSessionError(t)
                },
                onSessionOpenedHandler = { openedSession ->
                    try {
                        if (!isAttachedToWindow) {
                            openedSession.close()
                            return@createClient
                        }

                        session = openedSession
                        surfaceView!!.apply {
                            visibility = VISIBLE
                            setChildSurfacePackage(openedSession.surfacePackage)
                            setCompositionOrder(DEFAULT_COMPOSITION_ORDER)
                            invalidate()
                        }
                        localButtonView.visibility = INVISIBLE
                    } finally {
                        Trace.endAsyncSection("LocationButton.openSession", 0)
                    }
                },
            )

        provider?.openSession(
            findActivity(),
            hostToken,
            getDisplayId(),
            request,
            handler::post, // Force all client callbacks to the UI thread
            clientCallback,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isRemoteButtonSupported) {
            openSession()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(openSessionRunnable)
        closeSession()
    }

    private fun openSession() {
        post(openSessionRunnable)
    }

    private fun closeSession() {
        session?.close()
        session = null
        surfaceView?.clearChildSurfacePackage()
        surfaceView?.visibility = INVISIBLE
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top

        // SurfaceView gets the full width/height because the remote
        // rendering engine is handling the padding logic internally!
        if (isRemoteButtonSupported) {
            surfaceView!!.layout(0, 0, width, height)
        }

        // The local fallback view should fill the entire space,
        // as its internal padding is handled by setPadding calls.
        localButtonView.layout(0, 0, width, height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        var finalWidth = width
        var finalHeight = height
        var localMeasured = false

        // Only do the complex wrap_content calculation if we actually need to!
        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            val availableWidth = maxOf(0, width - safePaddingLeft - safePaddingRight)
            val availableHeight = maxOf(0, height - safePaddingTop - safePaddingBottom)

            val contentWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, widthMode)
            val contentHeightSpec = MeasureSpec.makeMeasureSpec(availableHeight, heightMode)

            // Measure the local button to get the true wrap_content bounds of the text/icon
            localButtonView.measure(contentWidthSpec, contentHeightSpec)
            localMeasured = true

            // Calculate total required dimensions including padding
            val desiredWidth = localButtonView.measuredWidth + safePaddingLeft + safePaddingRight
            val desiredHeight = localButtonView.measuredHeight + safePaddingTop + safePaddingBottom

            // Resolve the final dimensions against the parent's constraints
            finalWidth = resolveSize(desiredWidth, widthMeasureSpec)
            finalHeight = resolveSize(desiredHeight, heightMeasureSpec)
        }

        if (finalHeight > maxHeightPx) {
            finalHeight = maxHeightPx
        }
        setMeasuredDimension(finalWidth, finalHeight)

        val targetLocalWidth = maxOf(0, finalWidth - safePaddingLeft - safePaddingRight)
        val targetLocalHeight = maxOf(0, finalHeight - safePaddingTop - safePaddingBottom)
        if (
            !localMeasured ||
                localButtonView.measuredWidth != targetLocalWidth ||
                localButtonView.measuredHeight != targetLocalHeight
        ) {
            val localButtonWidthSpec =
                MeasureSpec.makeMeasureSpec(targetLocalWidth, MeasureSpec.EXACTLY)
            val localButtonHeightSpec =
                MeasureSpec.makeMeasureSpec(targetLocalHeight, MeasureSpec.EXACTLY)
            localButtonView.measure(localButtonWidthSpec, localButtonHeightSpec)
        }

        if (isRemoteButtonSupported) {
            val surfaceWidthSpec = MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY)
            val surfaceHeightSpec = MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
            surfaceView!!.measure(surfaceWidthSpec, surfaceHeightSpec)
        }
    }

    /**
     * Sets the listener to receive permission results and session error events.
     *
     * @param client The [LocationButtonListener] that will handle callbacks, or null to clear a
     *   previously set listener.
     */
    public fun setLocationButtonListener(client: LocationButtonListener?) {
        this.locationButtonListener = client
    }

    private fun applyStyleAttributes(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        context
            .obtainStyledAttributes(attrs, R.styleable.LocationButton, defStyleAttr, defStyleRes)
            .use { a ->
                backgroundColor = a.getColor(R.styleable.LocationButton_backgroundColor, 0)
                textColor = a.getColor(R.styleable.LocationButton_android_textColor, 0)
                iconTint = a.getColor(R.styleable.LocationButton_iconTint, textColor)
                strokeColor = a.getColor(R.styleable.LocationButton_strokeColor, 0)
                strokeWidth = a.getDimensionPixelSize(R.styleable.LocationButton_strokeWidth, 0)
                cornerRadius = a.getDimension(R.styleable.LocationButton_cornerRadius, 0f)
                pressedCornerRadius =
                    a.getDimension(R.styleable.LocationButton_pressedCornerRadius, 0f)
                textType =
                    a.getInt(
                        R.styleable.LocationButton_locationButtonTextType,
                        LocationButtonSession.TEXT_TYPE_PRECISE_LOCATION,
                    )
                maxLines = a.getInt(R.styleable.LocationButton_android_maxLines, -1)
                textAllCaps = a.getBoolean(R.styleable.LocationButton_android_textAllCaps, false)
                includeFontPadding =
                    a.getBoolean(R.styleable.LocationButton_android_includeFontPadding, true)
            }

        syncLocalButton()
    }

    private fun syncLocalButton() {
        localButtonView.configure(
            textType = textType,
            backgroundColor = backgroundColor,
            textColor = textColor,
            iconTint = iconTint,
            cornerRadius = cornerRadius,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            pressedCornerRadius = pressedCornerRadius,
            maxLines = maxLines,
            textAllCaps = textAllCaps,
            includeFontPadding = includeFontPadding,
        )
    }

    /**
     * Controls the composition order of the underlying SurfaceView.
     *
     * By default, this is set on Top in Z-order, as this button is a secure system component.
     * * For developers migrating from legacy SurfaceView APIs:
     * - `setZOrderOnTop(true)` is equivalent to passing `1`.
     * - `setZOrderOnTop(false)` is equivalent to passing `-2`.
     * * See also
     *   [SurfaceView.setCompositionOrder](https://developer.android.com/reference/android/view/SurfaceView#setCompositionOrder(int))
     *   for reference.
     *
     * @param order The exact Z-order integer. Default is 1 (on top of the app window).
     */
    public fun setCompositionOrder(order: Int) {
        surfaceView?.setCompositionOrder(order)
    }

    /**
     * Gets the composition order of the underlying SurfaceView.
     *
     * @return The exact Z-order integer.
     */
    public fun getCompositionOrder(): Int {
        return surfaceView?.getCompositionOrder() ?: DEFAULT_COMPOSITION_ORDER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if ((w == oldw && h == oldh) || w == 0 || h == 0) {
            return
        }
        session?.resize(w, h)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)

        session?.setPadding(safePaddingLeft, safePaddingTop, safePaddingRight, safePaddingBottom)
        syncLocalButton()
        localButtonView.setPadding(
            safePaddingLeft,
            safePaddingTop,
            safePaddingRight,
            safePaddingBottom,
        )
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)

        session?.setPadding(safePaddingLeft, safePaddingTop, safePaddingRight, safePaddingBottom)
        syncLocalButton()
        localButtonView.setPadding(
            safePaddingLeft,
            safePaddingTop,
            safePaddingRight,
            safePaddingBottom,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (newConfig != null) {
            session?.changeConfiguration(newConfig)
        }
    }

    /**
     * Sets the corner radius of the button background.
     *
     * @param radius The desired corner radius in pixels.
     */
    public fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        session?.setCornerRadius(radius)
        syncLocalButton()
    }

    /**
     * Sets the corner radius of the button background when the user presses it.
     *
     * @param radius The desired pressed-state corner radius in pixels.
     */
    public fun setPressedCornerRadius(radius: Float) {
        pressedCornerRadius = radius
        session?.setPressedCornerRadius(radius)
        syncLocalButton()
    }

    /**
     * Sets the color of the text displayed inside the button.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setTextColor(color: Int) {
        textColor = color
        session?.setTextColor(color)
        syncLocalButton()
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        session?.setBackgroundColor(color)
        syncLocalButton()
    }

    /**
     * Sets the color tint applied to the location icon.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setIconTint(color: Int) {
        iconTint = color
        session?.setIconTint(color)
        syncLocalButton()
    }

    /**
     * Sets the width of the button's outer stroke border.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setStrokeColor(color: Int) {
        strokeColor = color
        session?.setStrokeColor(color)
        syncLocalButton()
    }

    /**
     * Sets the width of the button's outer stroke border.
     *
     * @param strokeWidth The desired stroke width in pixels.
     */
    public fun setStrokeWidth(strokeWidth: Int) {
        this@LocationButton.strokeWidth = strokeWidth
        session?.setStrokeWidth(strokeWidth)
        syncLocalButton()
    }

    /**
     * Sets the text content displayed inside the button.
     *
     * @param textType The predefined [LocationButtonTextType] identifier.
     */
    // Suppress WrongConstant since we pass our layout XML-derived integer to the
    // @IntDef-guarded framework API.
    @SuppressLint("WrongConstant")
    public fun setTextType(@LocationButtonTextType textType: Int) {

        this.textType = textType
        session?.setTextType(textType)
        syncLocalButton()
        requestLayout()
    }

    private fun createButtonRequest(): LocationButtonRequest {
        return LocationButtonRequest.Builder(width, height, resources.configuration)
            .setPaddingLeft(safePaddingLeft)
            .setPaddingTop(safePaddingTop)
            .setPaddingRight(safePaddingRight)
            .setPaddingBottom(safePaddingBottom)
            .setBackgroundColor(backgroundColor)
            .setStrokeColor(strokeColor)
            .setStrokeWidth(strokeWidth)
            .setCornerRadius(cornerRadius)
            .setIconTint(iconTint)
            .setTextType(textType)
            .setTextColor(textColor)
            .setPressedCornerRadius(pressedCornerRadius)
            .build()
    }

    private fun getDisplayId(): Int {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        return displayManager?.displays?.firstOrNull()?.displayId
            ?: run {
                Log.w(LOG_TAG, "Display is null, defaulting to ID 0")
                0
            }
    }

    private fun findActivity(): Activity {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        throw IllegalStateException("LocationButton must be hosted within an Activity context")
    }

    // enforce restrictions on adding child views to this ViewGroup
    override fun addView(child: View?) {
        checkAddView()
        super.addView(child)
    }

    override fun addView(child: View?, index: Int) {
        checkAddView()
        super.addView(child, index)
    }

    override fun addView(child: View?, width: Int, height: Int) {
        checkAddView()
        super.addView(child, width, height)
    }

    override fun addView(child: View?, params: LayoutParams?) {
        checkAddView()
        super.addView(child, params)
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        checkAddView()
        super.addView(child, index, params)
    }

    override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        checkAddView()
        return super.addViewInLayout(child, index, params)
    }

    private fun checkAddView() {
        if (initialized) {
            throw UnsupportedOperationException(
                "Cannot add views to " +
                    "${javaClass.simpleName}; This View does not support additional children."
            )
        }
    }

    private val safePaddingLeft: Int
        get() = paddingLeft.coerceAtMost(maxPaddingPx)

    private val safePaddingTop: Int
        get() = paddingTop.coerceAtMost(maxPaddingPx)

    private val safePaddingRight: Int
        get() = paddingRight.coerceAtMost(maxPaddingPx)

    private val safePaddingBottom: Int
        get() = paddingBottom.coerceAtMost(maxPaddingPx)

    private val maxPaddingPx: Int
        get() = (MAX_PADDING_DP * resources.displayMetrics.density).toInt()

    private val maxHeightPx: Int
        get() = (MAX_HEIGHT_DP * resources.displayMetrics.density).toInt()

    /** Test-only hook to inject a fake provider. */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public fun setLocationButtonProviderForTesting(provider: LocationButtonProvider) {
        this.provider = provider
    }

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isRemoteSessionActive: Boolean
        get() = session != null

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isSurfaceViewVisible: Boolean
        get() = surfaceView?.visibility == View.VISIBLE

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isLocalButtonVisible: Boolean
        get() = localButtonView.visibility == View.VISIBLE

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public fun checkActivityContextForTesting() {
        findActivity()
    }

    @RequiresApi(37)
    private object Api37Impl {
        fun createClient(
            onPermissionResultHandler: (Boolean) -> Unit,
            onSessionErrorHandler: (Throwable) -> Unit,
            onSessionOpenedHandler: (LocationButtonSession) -> Unit,
        ): LocationButtonClient {
            return object : LocationButtonClient {
                override fun onPermissionResult(granted: Boolean) {
                    onPermissionResultHandler(granted)
                }

                override fun onSessionError(t: Throwable) {
                    onSessionErrorHandler(t)
                }

                override fun onSessionOpened(openedSession: LocationButtonSession) {
                    onSessionOpenedHandler(openedSession)
                }
            }
        }
    }

    public companion object {
        private const val LOG_TAG = "LocationButton"
        private const val DEBUG = false

        // Equivalent to android.view.WindowManagerPolicyConstants.APPLICATION_PANEL_SUBLAYER
        private const val DEFAULT_COMPOSITION_ORDER = 1
        private const val MAX_PADDING_DP = 8
        private const val MAX_HEIGHT_DP = 136

        public const val TEXT_TYPE_NONE: Int = LocationButtonSession.TEXT_TYPE_NONE
        public const val TEXT_TYPE_PRECISE_LOCATION: Int =
            LocationButtonSession.TEXT_TYPE_PRECISE_LOCATION
        public const val TEXT_TYPE_USE_PRECISE_LOCATION: Int =
            LocationButtonSession.TEXT_TYPE_USE_PRECISE_LOCATION
        public const val TEXT_TYPE_SHARE_PRECISE_LOCATION: Int =
            LocationButtonSession.TEXT_TYPE_SHARE_PRECISE_LOCATION
        public const val TEXT_TYPE_NEAR_MY_PRECISE_LOCATION: Int =
            LocationButtonSession.TEXT_TYPE_NEAR_MY_PRECISE_LOCATION
        public const val TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION: Int =
            LocationButtonSession.TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION

        /** Whether remote location button is supported or not. If not, SurfaceView isn't enabled */
        private val isRemoteButtonSupported =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
    }
}

/** Callbacks for location button events triggered by user interaction. */
public interface LocationButtonListener {
    /**
     * Triggered when the user interacts with the button and a permission decision is made. This
     * callback is only invoked on platforms on and after `Build.VERSION_CODES.CINNAMON_BUN`
     * supporting remote rendering, after the system-managed permission flow completes.
     *
     * @param isGranted True if the precise location permission was granted, false otherwise.
     */
    public fun onPermissionResult(isGranted: Boolean)

    /**
     * Triggered when a critical error occurs while establishing or maintaining the remote session.
     * This callback is only invoked on platforms on and after `Build.VERSION_CODES.CINNAMON_BUN`
     * supporting remote rendering. Apps typically do not need to handle this, as the library
     * automatically falls back to local rendering on failure.
     *
     * @param throwable The underlying exception that caused the session failure.
     */
    public fun onSessionError(throwable: Throwable) {}

    /**
     * Triggered when the button is clicked on platforms before `Build.VERSION_CODES.CINNAMON_BUN`
     * that do not support remote rendering.
     *
     * Developers should implement this to handle the button click. They should check if the
     * required permissions are already granted; if so, they can proceed with the location-based
     * action, otherwise they should request the permissions.
     */
    public fun onRequestPermissions()
}
