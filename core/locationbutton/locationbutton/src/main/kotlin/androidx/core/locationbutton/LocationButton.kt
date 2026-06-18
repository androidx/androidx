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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX
import androidx.core.content.ContextCompat
import androidx.core.content.res.use

/**
 * A widget that provides a session-based precise location permission button. It either draws the
 * button remotely rendered by the system on [Build.VERSION_CODES.CINNAMON_BUN] and above platforms,
 * or falls back to a locally rendered button on platforms before
 * [Build.VERSION_CODES.CINNAMON_BUN].
 *
 * On platforms before [Build.VERSION_CODES.CINNAMON_BUN], the button click delegates to
 * [OnRequestPermissionsListener] if provided, allowing the app to handle the click manually (e.g.,
 * by requesting permissions or displaying rationale). If no custom listener is provided, the
 * library automatically requests location permissions.
 */
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
    internal var surfaceView: SurfaceView? = null

    /**
     * Locally rendered location button, this button provides an alternate to remote location button
     * on platforms before [Build.VERSION_CODES.CINNAMON_BUN]. Clicking on this button will trigger
     * regular permission request flow.
     *
     * This button is also used in measuring the width for remote location button to help implement
     * wrap_content for SurfaceView.
     */
    internal val localButtonView: LocalLocationButton

    /**
     * Location button provider helper for remote rendering on [Build.VERSION_CODES.CINNAMON_BUN]
     * and above
     */
    internal var remoteDelegate: RemoteLocationButtonDelegate? = null

    internal var onPermissionResultListener: OnPermissionResultListener? = null
    internal var onRequestPermissionsListener: OnRequestPermissionsListener? = null
    internal var onErrorListener: OnErrorListener? = null

    internal var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    /**
     * The [Activity] that hosts this button.
     *
     * This activity is used to host the location button. If not explicitly set, the library will
     * attempt to resolve the hosting activity by traversing the [Context] wrapper chain of the
     * context passed to the constructor.
     */
    public var parentActivity: Activity? = null
        set(value) {
            if (!isAttachedToWindow) {
                field = value
                return
            }

            val oldActivity = findActivityOrNull()
            field = value
            val newActivity = findActivityOrNull()

            if (oldActivity == newActivity) {
                return
            }

            if (isRemoteButtonSupported) {
                if (isRemoteSessionActive) {
                    closeSession()
                }
                if (newActivity != null) {
                    openSession()
                }
            } else {
                if (newActivity != null) {
                    registerForPermissionResult()
                } else {
                    activityResultLauncher?.unregister()
                    activityResultLauncher = null
                }
            }
        }

    /** Once initialized, can't add more views. */
    private var initialized = false

    // -- Style Attributes --
    internal var textColor = 0
    internal var backgroundColor = 0
    internal var iconTint = 0
    internal var cornerRadius = -1f
    internal var pressedCornerRadius = -1f
    internal var strokeColor = 0
    internal var strokeWidth = 0
    internal var textType = TEXT_TYPE_PRECISE_LOCATION
    private var maxLines = -1
    private var textAllCaps = false
    private var includeFontPadding = true

    init {
        // Setup SurfaceView if remote rendering is supported
        if (isRemoteButtonSupported) {
            remoteDelegate = Api37Impl.create(this, context)
            surfaceView =
                SurfaceView(context).apply {
                    holder.setFormat(PixelFormat.TRANSPARENT)
                    visibility = INVISIBLE
                }
            addView(surfaceView)
        }

        localButtonView = LocalLocationButton(context)
        if (!isRemoteButtonSupported) {
            localButtonView.setOnClickListener {
                val requestListener = onRequestPermissionsListener
                if (requestListener != null) {
                    requestListener.onRequestPermissions()
                } else {
                    handleDefaultPermissionRequest()
                }
            }
        }
        addView(localButtonView)

        applyStyleAttributes(attrs, defStyleAttr, defStyleRes)
        initialized = true
    }

    private val openSessionRunnable = Runnable {
        val surfaceView = surfaceView ?: return@Runnable
        val delegate = remoteDelegate ?: return@Runnable

        delegate.openSession(requireActivity(), getDisplayId(), surfaceView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isRemoteButtonSupported) {
            openSession()
        } else {
            registerForPermissionResult()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(openSessionRunnable)
        closeSession()
        activityResultLauncher?.unregister()
        activityResultLauncher = null
    }

    private fun openSession() {
        post(openSessionRunnable)
    }

    private fun closeSession() {
        remoteDelegate?.closeSession(surfaceView)
    }

    private fun registerForPermissionResult() {
        activityResultLauncher?.unregister()
        activityResultLauncher = null
        val activity = findActivityOrNull() ?: return

        if (activity is ActivityResultRegistryOwner && id != View.NO_ID) {
            activityResultLauncher =
                activity.activityResultRegistry.register(
                    "androidx.core.locationbutton.PERMISSION_REQUEST_KEY_$id",
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { results ->
                    val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    onPermissionResultListener?.onPermissionResult(granted)
                }
        }
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
     * Sets the listener to receive permission results.
     *
     * **Note:** On platforms before [Build.VERSION_CODES.CINNAMON_BUN], if you rely on the default
     * permission request flow (i.e. you do not provide a custom [OnRequestPermissionsListener]),
     * the hosting [Activity] must implement [androidx.activity.result.ActivityResultRegistryOwner]
     * and the [LocationButton] must have an `android:id` to deliver the permission result to this
     * listener.
     *
     * @param listener The [OnPermissionResultListener] that will handle callbacks, or null to clear
     *   a previously set listener.
     */
    public fun setOnPermissionResultListener(listener: OnPermissionResultListener?) {
        onPermissionResultListener = listener
    }

    /**
     * Sets the listener to handle permission requests on platforms before
     * [Build.VERSION_CODES.CINNAMON_BUN].
     *
     * Provide this listener if you want to customize the permission request flow (e.g. to show
     * rationale) or if the hosting [Activity] does not implement
     * [androidx.activity.result.ActivityResultRegistryOwner] on platforms before
     * [Build.VERSION_CODES.CINNAMON_BUN].
     *
     * @param listener The [OnRequestPermissionsListener] that will handle callbacks, or null to
     *   clear a previously set listener.
     */
    public fun setOnRequestPermissionsListener(listener: OnRequestPermissionsListener?) {
        onRequestPermissionsListener = listener
    }

    /**
     * Sets the listener to receive remote session errors on [Build.VERSION_CODES.CINNAMON_BUN] and
     * above platforms.
     *
     * @param listener The [OnErrorListener] that will handle callbacks, or null to clear a
     *   previously set listener.
     */
    public fun setOnErrorListener(listener: OnErrorListener?) {
        onErrorListener = listener
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
                cornerRadius = a.getDimension(R.styleable.LocationButton_cornerRadius, -1f)
                pressedCornerRadius =
                    a.getDimension(R.styleable.LocationButton_pressedCornerRadius, -1f)
                textType =
                    a.getInt(
                        R.styleable.LocationButton_locationButtonTextType,
                        TEXT_TYPE_PRECISE_LOCATION,
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
        remoteDelegate?.setCompositionOrder(order)
    }

    /**
     * Gets the composition order of the underlying SurfaceView.
     *
     * @return The exact Z-order integer.
     */
    public fun getCompositionOrder(): Int {
        return remoteDelegate?.getCompositionOrder() ?: DEFAULT_COMPOSITION_ORDER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if ((w == oldw && h == oldh) || w == 0 || h == 0) {
            return
        }
        remoteDelegate?.onSizeChanged(w, h)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)

        remoteDelegate?.setPadding(
            safePaddingLeft,
            safePaddingTop,
            safePaddingRight,
            safePaddingBottom,
        )
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

        remoteDelegate?.setPadding(
            safePaddingLeft,
            safePaddingTop,
            safePaddingRight,
            safePaddingBottom,
        )
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
            remoteDelegate?.changeConfiguration(newConfig)
        }
    }

    /**
     * Sets the corner radius of the button background.
     *
     * @param radius The desired corner radius in pixels.
     */
    public fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        remoteDelegate?.setCornerRadius(radius)
        syncLocalButton()
    }

    /**
     * Sets the corner radius of the button background when the user presses it.
     *
     * @param radius The desired pressed-state corner radius in pixels.
     */
    public fun setPressedCornerRadius(radius: Float) {
        pressedCornerRadius = radius
        remoteDelegate?.setPressedCornerRadius(radius)
        syncLocalButton()
    }

    /**
     * Sets the color of the text displayed inside the button.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setTextColor(color: Int) {
        textColor = color
        remoteDelegate?.setTextColor(color)
        syncLocalButton()
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
        remoteDelegate?.setBackgroundColor(color)
        syncLocalButton()
    }

    /**
     * Sets the color tint applied to the location icon.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setIconTint(color: Int) {
        iconTint = color
        remoteDelegate?.setIconTint(color)
        syncLocalButton()
    }

    /**
     * Sets the width of the button's outer stroke border.
     *
     * @param color The resolved ARGB color integer.
     */
    public fun setStrokeColor(color: Int) {
        strokeColor = color
        remoteDelegate?.setStrokeColor(color)
        syncLocalButton()
    }

    /**
     * Sets the width of the button's outer stroke border.
     *
     * @param strokeWidth The desired stroke width in pixels.
     */
    public fun setStrokeWidth(strokeWidth: Int) {
        this@LocationButton.strokeWidth = strokeWidth
        remoteDelegate?.setStrokeWidth(strokeWidth)
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
        remoteDelegate?.setTextType(textType)
        syncLocalButton()
        requestLayout()
    }

    private fun getDisplayId(): Int {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        return displayManager?.displays?.firstOrNull()?.displayId
            ?: run {
                Log.w(LOG_TAG, "Display is null, defaulting to ID 0")
                0
            }
    }

    private fun requireActivity(): Activity =
        checkNotNull(findActivityOrNull()) {
            "LocationButton must be hosted within an Activity context"
        }

    private fun findActivityOrNull(): Activity? {
        parentActivity?.let {
            return it
        }
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    private fun handleDefaultPermissionRequest() {
        val launcher =
            checkNotNull(activityResultLauncher) {
                "Activity should support ActivityResultRegistry and LocationButton should have an `android:id`."
            }

        val hasFineLocation =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (hasFineLocation) {
            onPermissionResultListener?.onPermissionResult(true)
            return
        }

        val permissions =
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        launcher.launch(permissions)
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

    internal val safePaddingLeft: Int
        get() = getSafePadding(paddingLeft)

    internal val safePaddingTop: Int
        get() = getSafePadding(paddingTop)

    internal val safePaddingRight: Int
        get() = getSafePadding(paddingRight)

    internal val safePaddingBottom: Int
        get() = getSafePadding(paddingBottom)

    private fun getSafePadding(padding: Int): Int {
        val maxPadding = maxPaddingPx
        val minPadding = minPaddingPx
        return padding.coerceIn(minPadding, maxPadding)
    }

    private val maxPaddingPx: Int
        get() = (MAX_PADDING_DP * resources.displayMetrics.density).toInt()

    private val minPaddingPx: Int
        get() = (MIN_PADDING_DP * resources.displayMetrics.density).toInt()

    private val maxHeightPx: Int
        get() = (MAX_HEIGHT_DP * resources.displayMetrics.density).toInt()

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isRemoteSessionActive: Boolean
        get() = remoteDelegate?.isSessionActive() ?: false

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isSurfaceViewVisible: Boolean
        get() = surfaceView?.visibility == View.VISIBLE

    @get:RestrictTo(LIBRARY_GROUP_PREFIX)
    public val isLocalButtonVisible: Boolean
        get() = localButtonView.visibility == View.VISIBLE

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public fun resolveActivityForTesting(): Activity {
        return requireActivity()
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    internal object Api37Impl {
        fun create(view: LocationButton, context: Context): RemoteLocationButtonDelegate {
            return RemoteLocationButtonDelegateApi37(view, context)
        }
    }

    public companion object {
        private const val LOG_TAG = "LocationButton"
        private const val DEBUG = false

        // Equivalent to android.view.WindowManagerPolicyConstants.APPLICATION_PANEL_SUBLAYER
        internal const val DEFAULT_COMPOSITION_ORDER = 1
        private const val MAX_PADDING_DP = 8
        private const val MIN_PADDING_DP = 4
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

/** Callback for location button permission result. */
public fun interface OnPermissionResultListener {
    /**
     * Triggered after the user interacts with the system rendered location button and makes a
     * decision.
     *
     * @param isGranted True if the precise location permission was granted, false otherwise.
     */
    public fun onPermissionResult(isGranted: Boolean)
}

/**
 * Callback to handle permission requests on platforms before [Build.VERSION_CODES.CINNAMON_BUN].
 */
public fun interface OnRequestPermissionsListener {
    /**
     * Developers can override this to handle the location button click (e.g., by requesting
     * permissions or showing UI to explain why the permission is needed).
     */
    public fun onRequestPermissions()
}

/** Callback for remote session errors on [Build.VERSION_CODES.CINNAMON_BUN] and above platforms. */
public fun interface OnErrorListener {
    /**
     * Triggered when a critical error occurs while establishing or maintaining the remote session.
     *
     * @param throwable The underlying exception that caused the session failure.
     */
    public fun onError(throwable: Throwable)
}
