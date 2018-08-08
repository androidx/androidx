package androidx.ui.engine.platform.io.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.accessibility.AccessibilityManager
import android.view.SurfaceHolder
import android.util.TypedValue
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.atomic.AtomicLong
import android.graphics.SurfaceTexture
import android.content.Intent
import android.content.BroadcastReceiver
import android.view.MotionEvent
import android.graphics.Bitmap
import android.view.WindowInsets
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.Surface
import androidx.ui.engine.geometry.Rect
import androidx.ui.services.raw_keyboard.RawKeyboard
import java.nio.ByteBuffer

/**
 * An Android view containing a Flutter app.
 */
class FlutterView(
    context: Context,
    attrs: AttributeSet?,
    nativeView: FlutterNativeView?
) : SurfaceView(context, attrs) {

    /**
     * Interface for those objects that maintain and expose a reference to a
     * {@code FlutterView} (such as a full-screen Flutter activity).
     *
     * <p>
     * This indirection is provided to support applications that use an activity
     * other than {@link io.flutter.app.FlutterActivity} (e.g. Android v4 support
     * library's {@code FragmentActivity}). It allows Flutter plugins to deal in
     * this interface and not require that the activity be a subclass of
     * {@code FlutterActivity}.
     * </p>
     */
    interface Provider {
        /**
         * Returns a reference to the Flutter view maintained by this object.
         * This may be `null`.
         */
        val flutterView: FlutterView
    }

    companion object {
        private val TAG = "FlutterView"

        private val ACTION_DISCOVER = "io.flutter.view.DISCOVER"
    }

    internal class ViewportMetrics {
        var devicePixelRatio = 1.0f
        var physicalWidth = 0
        var physicalHeight = 0
        var physicalPaddingTop = 0
        var physicalPaddingRight = 0
        var physicalPaddingBottom = 0
        var physicalPaddingLeft = 0
        var physicalViewInsetTop = 0
        var physicalViewInsetRight = 0
        var physicalViewInsetBottom = 0
        var physicalViewInsetLeft = 0
    }

    private val mImm: InputMethodManager? = null
//    private val mTextInputPlugin: TextInputPlugin? = null
    private var mSurfaceCallback: SurfaceHolder.Callback? = null
    private val mMetrics: ViewportMetrics = ViewportMetrics()
    private val mAccessibilityManager: AccessibilityManager =
            getContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
//    private val mFlutterLocalizationChannel: MethodChannel? = null
//    private val mFlutterNavigationChannel: MethodChannel? = null
//    private val mFlutterLifecycleChannel: BasicMessageChannel<String>? = null
//    private val mFlutterSystemChannel: BasicMessageChannel<Any>? = null
//    private val mFlutterSettingsChannel: BasicMessageChannel<Any>? = null
//    private val mDiscoveryReceiver: BroadcastReceiver? = null
//    private val mActivityLifecycleListeners: List<ActivityLifecycleListener>? = null
    private val mFirstFrameListeners: List<FirstFrameListener>? = null
    private val nextTextureId = AtomicLong(0L)
    private var mNativeView: FlutterNativeView? = null
    private val mIsSoftwareRenderingEnabled = false // using the software renderer or not
    private var mLastInputConnection: InputConnection? = null

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, null)

    constructor(context: Context): this(context, null)

    init {
        // mIsSoftwareRenderingEnabled = nativeGetIsSoftwareRenderingEnabled()

        mMetrics.devicePixelRatio = context.resources.displayMetrics.density
        isFocusable = true
        isFocusableInTouchMode = true

//        val activity = getContext() as Activity
//        if (nativeView == null) {
//            mNativeView = FlutterNativeView(activity.applicationContext)
//        } else {
//            mNativeView = nativeView
//        }
//        mNativeView.attachViewAndActivity(this, activity)

        var color = -0x1000000
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            color = typedValue.data
        }
        // TODO(abarth): Consider letting the developer override this color.
        val backgroundColor = color

//        mSurfaceCallback = object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                assertAttached()
//                nativeSurfaceCreated(mNativeView.get(), holder.surface, backgroundColor)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                assertAttached()
//                nativeSurfaceChanged(mNativeView.get(), width, height)
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                assertAttached()
//                nativeSurfaceDestroyed(mNativeView.get())
//            }
//        }
//        holder.addCallback(mSurfaceCallback)

//        mActivityLifecycleListeners = ArrayList()
//        mFirstFrameListeners = ArrayList()

        // Configure the platform plugins and flutter channels.
//        mFlutterLocalizationChannel = MethodChannel(this, "flutter/localization", JSONMethodCodec.INSTANCE)
//        mFlutterNavigationChannel = MethodChannel(this, "flutter/navigation", JSONMethodCodec.INSTANCE)
//        mFlutterKeyEventChannel = BasicMessageChannel(this, "flutter/keyevent", JSONMessageCodec.INSTANCE)
//        mFlutterLifecycleChannel = BasicMessageChannel(this, "flutter/lifecycle", StringCodec.INSTANCE)
//        mFlutterSystemChannel = BasicMessageChannel(this, "flutter/system", JSONMessageCodec.INSTANCE)
//        mFlutterSettingsChannel = BasicMessageChannel(this, "flutter/settings", JSONMessageCodec.INSTANCE)

//        val platformPlugin = PlatformPlugin(activity)
//        val flutterPlatformChannel = MethodChannel(this, "flutter/platform", JSONMethodCodec.INSTANCE)
//        flutterPlatformChannel.setMethodCallHandler(platformPlugin)
//        addActivityLifecycleListener(platformPlugin)
//        mImm = getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        mTextInputPlugin = TextInputPlugin(this)
//
//        setLocale(resources.configuration.locale)
//        setUserSettings()
//
//        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE !== 0) {
//            mDiscoveryReceiver = DiscoveryReceiver()
//            context.registerReceiver(mDiscoveryReceiver, IntentFilter(ACTION_DISCOVER))
//        } else {
//            mDiscoveryReceiver = null
//        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!isAttached()) {
            return super.onKeyUp(keyCode, event)
        }

        RawKeyboard._handleKeyEvent("keyUp", event)
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isAttached()) {
            return super.onKeyDown(keyCode, event)
        }

        if (event.getDeviceId() !== KeyCharacterMap.VIRTUAL_KEYBOARD) {
            if (mLastInputConnection != null && mImm!!.isAcceptingText()) {
                mLastInputConnection!!.sendKeyEvent(event)
            }
        }

        RawKeyboard._handleKeyEvent("keyDown", event)
        return super.onKeyDown(keyCode, event)
    }

//    fun getFlutterNativeView(): FlutterNativeView? {
//        return mNativeView
//    }
//
//    fun getPluginRegistry(): FlutterPluginRegistry {
//        return mNativeView.getPluginRegistry()
//    }

//    fun getLookupKeyForAsset(asset: String): String {
//        return FlutterMain.getLookupKeyForAsset(asset)
//    }
//
//    fun getLookupKeyForAsset(asset: String, packageName: String): String {
//        return FlutterMain.getLookupKeyForAsset(asset, packageName)
//    }
//
//    fun addActivityLifecycleListener(listener: ActivityLifecycleListener) {
//        mActivityLifecycleListeners.add(listener)
//    }
//
//    fun onStart() {
//        mFlutterLifecycleChannel.send("AppLifecycleState.inactive")
//    }
//
//    fun onPause() {
//        mFlutterLifecycleChannel.send("AppLifecycleState.inactive")
//    }
//
//    fun onPostResume() {
//        updateAccessibilityFeatures()
//        for (listener in mActivityLifecycleListeners) {
//            listener.onPostResume()
//        }
//        mFlutterLifecycleChannel.send("AppLifecycleState.resumed")
//    }
//
//    fun onStop() {
//        mFlutterLifecycleChannel.send("AppLifecycleState.paused")
//    }
//
//    fun onMemoryPressure() {
//        val message = HashMap(1)
//        message.put("type", "memoryPressure")
//        mFlutterSystemChannel.send(message)
//    }

//    /**
//     * Provide a listener that will be called once when the FlutterView renders its
//     * first frame to the underlaying SurfaceView.
//     */
//    fun addFirstFrameListener(listener: FirstFrameListener) {
//        mFirstFrameListeners.add(listener)
//    }
//
//    /**
//     * Remove an existing first frame listener.
//     */
//    fun removeFirstFrameListener(listener: FirstFrameListener) {
//        mFirstFrameListeners.remove(listener)
//    }
//
//    fun setInitialRoute(route: String) {
//        mFlutterNavigationChannel.invokeMethod("setInitialRoute", route)
//    }
//
//    fun pushRoute(route: String) {
//        mFlutterNavigationChannel.invokeMethod("pushRoute", route)
//    }
//
//    fun popRoute() {
//        mFlutterNavigationChannel.invokeMethod("popRoute", null)
//    }
//
//    private fun setUserSettings() {
//        val message = HashMap()
//        message.put("textScaleFactor", resources.configuration.fontScale)
//        message.put("alwaysUse24HourFormat", DateFormat.is24HourFormat(context))
//        mFlutterSettingsChannel.send(message)
//    }
//
//    private fun setLocale(locale: Locale) {
//        mFlutterLocalizationChannel.invokeMethod("setLocale", Arrays.asList(locale.getLanguage(), locale.getCountry()))
//    }
//
//    protected fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        setLocale(newConfig.locale)
//        setUserSettings()
//    }
//
//    fun getDevicePixelRatio(): Float {
//        return mMetrics.devicePixelRatio
//    }
//
//    fun detach(): FlutterNativeView? {
//        if (!isAttached())
//            return null
//        if (mDiscoveryReceiver != null) {
//            context.unregisterReceiver(mDiscoveryReceiver)
//        }
//        holder.removeCallback(mSurfaceCallback)
//        mNativeView.detach()
//
//        val view = mNativeView
//        mNativeView = null
//        return view
//    }
//
//    fun destroy() {
//        if (!isAttached())
//            return
//
//        if (mDiscoveryReceiver != null) {
//            context.unregisterReceiver(mDiscoveryReceiver)
//        }
//
//        holder.removeCallback(mSurfaceCallback)
//
//        mNativeView.destroy()
//        mNativeView = null
//    }
//
//    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
//        try {
//            mLastInputConnection = mTextInputPlugin.createInputConnection(this, outAttrs)
//            return mLastInputConnection
//        } catch (e: JSONException) {
//            Log.e(TAG, "Failed to create input connection", e)
//            return null
//        }
//
//    }

    // Must match the PointerChange enum in pointer.dart.
    private val kPointerChangeCancel = 0
    private val kPointerChangeAdd = 1
    private val kPointerChangeRemove = 2
    private val kPointerChangeHover = 3
    private val kPointerChangeDown = 4
    private val kPointerChangeMove = 5
    private val kPointerChangeUp = 6

    // Must match the PointerDeviceKind enum in pointer.dart.
    private val kPointerDeviceKindTouch = 0
    private val kPointerDeviceKindMouse = 1
    private val kPointerDeviceKindStylus = 2
    private val kPointerDeviceKindInvertedStylus = 3

    private fun getPointerChangeForAction(maskedAction: Int): Int {
        // Primary pointer:
        if (maskedAction == MotionEvent.ACTION_DOWN) {
            return kPointerChangeDown
        }
        if (maskedAction == MotionEvent.ACTION_UP) {
            return kPointerChangeUp
        }
        // Secondary pointer:
        if (maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
            return kPointerChangeDown
        }
        if (maskedAction == MotionEvent.ACTION_POINTER_UP) {
            return kPointerChangeUp
        }
        // All pointers:
        if (maskedAction == MotionEvent.ACTION_MOVE) {
            return kPointerChangeMove
        }
        return if (maskedAction == MotionEvent.ACTION_CANCEL) {
            kPointerChangeCancel
        } else -1
    }

    private fun getPointerDeviceTypeForToolType(toolType: Int): Int {
        when (toolType) {
            MotionEvent.TOOL_TYPE_FINGER -> return kPointerDeviceKindTouch
            MotionEvent.TOOL_TYPE_STYLUS -> return kPointerDeviceKindStylus
            MotionEvent.TOOL_TYPE_MOUSE -> return kPointerDeviceKindMouse
            else ->
                // MotionEvent.TOOL_TYPE_UNKNOWN will reach here.
                return -1
        }
    }

    private fun addPointerForIndex(event: MotionEvent, pointerIndex: Int, packet: ByteBuffer) {
//        val pointerChange = getPointerChangeForAction(event.actionMasked)
//        if (pointerChange == -1) {
//            return
//        }
//
//        val pointerKind = getPointerDeviceTypeForToolType(event.getToolType(pointerIndex))
//        if (pointerKind == -1) {
//            return
//        }
//
//        val timeStamp = event.eventTime * 1000 // Convert from milliseconds to microseconds.
//
//        packet.putLong(timeStamp) // time_stamp
//        packet.putLong(pointerChange) // change
//        packet.putLong(pointerKind) // kind
//        packet.putLong(event.getPointerId(pointerIndex)) // device
//        packet.putDouble(event.getX(pointerIndex)) // physical_x
//        packet.putDouble(event.getY(pointerIndex)) // physical_y
//
//        if (pointerKind == kPointerDeviceKindMouse) {
//            packet.putLong(event.buttonState and 0x1F) // buttons
//        } else if (pointerKind == kPointerDeviceKindStylus) {
//            packet.putLong(event.buttonState shr 4 and 0xF) // buttons
//        } else {
//            packet.putLong(0) // buttons
//        }
//
//        packet.putLong(0) // obscured
//
//        // TODO(eseidel): Could get the calibrated range if necessary:
//        // event.getDevice().getMotionRange(MotionEvent.AXIS_PRESSURE)
//        packet.putDouble(event.getPressure(pointerIndex)) // pressure
//        packet.putDouble(0.0) // pressure_min
//        packet.putDouble(1.0) // pressure_max
//
//        if (pointerKind == kPointerDeviceKindStylus) {
//            packet.putDouble(event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex)) // distance
//            packet.putDouble(0.0) // distance_max
//        } else {
//            packet.putDouble(0.0) // distance
//            packet.putDouble(0.0) // distance_max
//        }
//
//        packet.putDouble(event.getToolMajor(pointerIndex)) // radius_major
//        packet.putDouble(event.getToolMinor(pointerIndex)) // radius_minor
//
//        packet.putDouble(0.0) // radius_min
//        packet.putDouble(0.0) // radius_max
//
//        packet.putDouble(event.getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex)) // orientation
//
//        if (pointerKind == kPointerDeviceKindStylus) {
//            packet.putDouble(event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)) // tilt
//        } else {
//            packet.putDouble(0.0) // tilt
//        }
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (!isAttached()) {
//            return false
//        }
//
//        // TODO(abarth): This version check might not be effective in some
//        // versions of Android that statically compile code and will be upset
//        // at the lack of |requestUnbufferedDispatch|. Instead, we should factor
//        // version-dependent code into separate classes for each supported
//        // version and dispatch dynamically.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            requestUnbufferedDispatch(event)
//        }
//
//        // These values must match the unpacking code in hooks.dart.
//        val kPointerDataFieldCount = 19
//        val kBytePerField = 8
//
//        val pointerCount = event.pointerCount
//
//        val packet = ByteBuffer.allocateDirect(pointerCount * kPointerDataFieldCount * kBytePerField)
//        packet.order(ByteOrder.LITTLE_ENDIAN)
//
//        val maskedAction = event.actionMasked
//        // ACTION_UP, ACTION_POINTER_UP, ACTION_DOWN, and ACTION_POINTER_DOWN
//        // only apply to a single pointer, other events apply to all pointers.
//        if (maskedAction == MotionEvent.ACTION_UP || maskedAction == MotionEvent.ACTION_POINTER_UP
//                || maskedAction == MotionEvent.ACTION_DOWN || maskedAction == MotionEvent.ACTION_POINTER_DOWN) {
//            addPointerForIndex(event, event.actionIndex, packet)
//        } else {
//            // ACTION_MOVE may not actually mean all pointers have moved
//            // but it's the responsibility of a later part of the system to
//            // ignore 0-deltas if desired.
//            for (p in 0 until pointerCount) {
//                addPointerForIndex(event, p, packet)
//            }
//        }
//
//        assert(packet.position() % (kPointerDataFieldCount * kBytePerField) === 0)
//        nativeDispatchPointerDataPacket(mNativeView.get(), packet, packet.position())
//        return true
//    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (!isAttached()) {
            return false
        }

        TODO()
//        val handled = handleAccessibilityHoverEvent(event)
//        if (!handled) {
//            // TODO(ianh): Expose hover events to the platform,
//            // implementing ADD, REMOVE, etc.
//        }
//        return handled
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        mMetrics.physicalWidth = width
        mMetrics.physicalHeight = height
        updateViewportMetrics()
        super.onSizeChanged(width, height, oldWidth, oldHeight)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
//        // Status bar, left/right system insets partially obscure content (padding).
//        mMetrics.physicalPaddingTop = insets.systemWindowInsetTop
//        mMetrics.physicalPaddingRight = insets.systemWindowInsetRight
//        mMetrics.physicalPaddingBottom = 0
//        mMetrics.physicalPaddingLeft = insets.systemWindowInsetLeft
//
//        // Bottom system inset (keyboard) should adjust scrollable bottom edge (inset).
//        mMetrics.physicalViewInsetTop = 0
//        mMetrics.physicalViewInsetRight = 0
//        mMetrics.physicalViewInsetBottom = insets.systemWindowInsetBottom
//        mMetrics.physicalViewInsetLeft = 0
//        updateViewportMetrics()
        return super.onApplyWindowInsets(insets)
    }

    protected fun fitSystemWindows(insets: Rect): Boolean {
        TODO()
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
//            // Status bar, left/right system insets partially obscure content (padding).
//            mMetrics.physicalPaddingTop = insets.top
//            mMetrics.physicalPaddingRight = insets.right
//            mMetrics.physicalPaddingBottom = 0
//            mMetrics.physicalPaddingLeft = insets.left
//
//            // Bottom system inset (keyboard) should adjust scrollable bottom edge (inset).
//            mMetrics.physicalViewInsetTop = 0
//            mMetrics.physicalViewInsetRight = 0
//            mMetrics.physicalViewInsetBottom = insets.bottom
//            mMetrics.physicalViewInsetLeft = 0
//            updateViewportMetrics()
//            return true
//        } else {
//            return super.fitSystemWindows(insets)
//        }
    }

    private fun isAttached(): Boolean {
        TODO()
        // return mNativeView != null && mNativeView.isAttached()
    }

    fun assertAttached() {
        if (!isAttached())
            throw AssertionError("Platform view is not attached")
    }

    private fun preRun() {
        TODO()
        // resetAccessibilityTree()
    }

    private fun postRun() {}

    fun runFromBundle(bundlePath: String, snapshotOverride: String) {
        runFromBundle(bundlePath, snapshotOverride, "main", false)
    }

    fun runFromBundle(bundlePath: String, snapshotOverride: String, entrypoint: String) {
        runFromBundle(bundlePath, snapshotOverride, entrypoint, false)
    }

    fun runFromBundle(
        bundlePath: String,
        snapshotOverride: String,
        entrypoint: String,
        reuseRuntimeController: Boolean
    ) {
        TODO()
//        assertAttached()
//        preRun()
//        mNativeView.runFromBundle(bundlePath, snapshotOverride, entrypoint, reuseRuntimeController)
//        postRun()
    }

//    /**
//     * Return the most recent frame as a bitmap.
//     *
//     * @return A bitmap.
//     */
//    fun getBitmap(): Bitmap {
//        assertAttached()
//        return nativeGetBitmap(mNativeView.get())
//    }

    private external fun nativeSurfaceCreated(
        nativePlatformViewAndroid: Long,
        surface: Surface,
        backgroundColor: Int
    )

    private external fun nativeSurfaceChanged(
        nativePlatformViewAndroid: Long,
        width: Int,
        height: Int
    )

    private external fun nativeSurfaceDestroyed(nativePlatformViewAndroid: Long)

    private external fun nativeSetViewportMetrics(
        nativePlatformViewAndroid: Long,
        devicePixelRatio: Float,
        physicalWidth: Int,
        physicalHeight: Int,
        physicalPaddingTop: Int,
        physicalPaddingRight: Int,
        physicalPaddingBottom: Int,
        physicalPaddingLeft: Int,
        physicalViewInsetTop: Int,
        physicalViewInsetRight: Int,
        physicalViewInsetBottom: Int,
        physicalViewInsetLeft: Int
    )

    private external fun nativeGetBitmap(nativePlatformViewAndroid: Long): Bitmap

    private external fun nativeDispatchPointerDataPacket(
        nativePlatformViewAndroid: Long,
        buffer: ByteBuffer,
        position: Int
    )

    private external fun nativeDispatchSemanticsAction(
        nativePlatformViewAndroid: Long,
        id: Int,
        action: Int,
        args: ByteBuffer?,
        argsPosition: Int
    )

    private external fun nativeSetSemanticsEnabled(
        nativePlatformViewAndroid: Long,
        enabled: Boolean
    )

    private external fun nativeSetAccessibilityFeatures(nativePlatformViewAndroid: Long, flags: Int)

    private external fun nativeGetIsSoftwareRenderingEnabled(): Boolean

    private external fun nativeRegisterTexture(
        nativePlatformViewAndroid: Long,
        textureId: Long,
        surfaceTexture: SurfaceTexture
    )

    private external fun nativeMarkTextureFrameAvailable(
        nativePlatformViewAndroid: Long,
        textureId: Long
    )

    private external fun nativeUnregisterTexture(nativePlatformViewAndroid: Long, textureId: Long)

    private fun updateViewportMetrics() {
        TODO()
//        if (!isAttached())
//            return
//        nativeSetViewportMetrics(mNativeView.get(), mMetrics.devicePixelRatio, mMetrics.physicalWidth,
//                mMetrics.physicalHeight, mMetrics.physicalPaddingTop, mMetrics.physicalPaddingRight,
//                mMetrics.physicalPaddingBottom, mMetrics.physicalPaddingLeft, mMetrics.physicalViewInsetTop,
//                mMetrics.physicalViewInsetRight, mMetrics.physicalViewInsetBottom, mMetrics.physicalViewInsetLeft)
//
//        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val fps = wm.defaultDisplay.refreshRate
//        VsyncWaiter.refreshPeriodNanos = (1000000000.0 / fps).toLong()
    }

    // Called by native to update the semantics/accessibility tree.
    fun updateSemantics(buffer: ByteBuffer, strings: Array<String>) {
        TODO()
//        try {
//            if (mAccessibilityNodeProvider != null) {
//                buffer.order(ByteOrder.LITTLE_ENDIAN)
//                mAccessibilityNodeProvider!!.updateSemantics(buffer, strings)
//            }
//        } catch (ex: Exception) {
//            Log.e(TAG, "Uncaught exception while updating semantics", ex)
//        }
    }

    fun updateCustomAccessibilityActions(buffer: ByteBuffer, strings: Array<String>) {
        TODO()
//        try {
//            if (mAccessibilityNodeProvider != null) {
//                buffer.order(ByteOrder.LITTLE_ENDIAN)
//                mAccessibilityNodeProvider!!.updateCustomAccessibilityActions(buffer, strings)
//            }
//        } catch (ex: Exception) {
//            Log.e(TAG, "Uncaught exception while updating local context actions", ex)
//        }
    }

    // Called by native to notify first Flutter frame rendered.
    fun onFirstFrame() {
        // Allow listeners to remove themselves when they are called.
        val listeners = ArrayList(mFirstFrameListeners)
        for (listener in listeners) {
            listener.onFirstFrame()
        }
    }

    // ACCESSIBILITY

//    private var mAccessibilityEnabled = false
//    private var mTouchExplorationEnabled = false
//    private var mAccessibilityFeatureFlags = 0
//    private var mTouchExplorationListener: TouchExplorationListener? = null
//
//    protected fun dispatchSemanticsAction(id: Int, action: AccessibilityBridge.Action) {
//        dispatchSemanticsAction(id, action, null)
//    }
//
//    protected fun dispatchSemanticsAction(id: Int, action: AccessibilityBridge.Action, args: Any?) {
//        if (!isAttached())
//            return
//        var encodedArgs: ByteBuffer? = null
//        var position = 0
//        if (args != null) {
//            encodedArgs = StandardMessageCodec.INSTANCE.encodeMessage(args)
//            position = encodedArgs!!.position()
//        }
//        nativeDispatchSemanticsAction(mNativeView.get(), id, action.value, encodedArgs, position)
//    }
//
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        mAccessibilityEnabled = mAccessibilityManager.isEnabled
//        mTouchExplorationEnabled = mAccessibilityManager.isTouchExplorationEnabled
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            val transitionUri = Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE)
//            context.contentResolver.registerContentObserver(transitionUri, false, mAnimationScaleObserver)
//        }
//
//        if (mAccessibilityEnabled || mTouchExplorationEnabled) {
//            ensureAccessibilityEnabled()
//        }
//        if (mTouchExplorationEnabled) {
//            mAccessibilityFeatureFlags = mAccessibilityFeatureFlags xor AccessibilityFeature.ACCESSIBLE_NAVIGATION.value
//        }
//        // Apply additional accessibility settings
//        updateAccessibilityFeatures()
//        resetWillNotDraw()
//        mAccessibilityManager.addAccessibilityStateChangeListener(this)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            if (mTouchExplorationListener == null) {
//                mTouchExplorationListener = TouchExplorationListener()
//            }
//            mAccessibilityManager.addTouchExplorationStateChangeListener(mTouchExplorationListener)
//        }
//    }
//
//    private fun updateAccessibilityFeatures() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            val transitionAnimationScale = Settings.Global.getString(context.contentResolver,
//                    Settings.Global.TRANSITION_ANIMATION_SCALE)
//            if (transitionAnimationScale != null && transitionAnimationScale == "0") {
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags xor AccessibilityFeature.DISABLE_ANIMATIONS.value
//            } else {
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags and AccessibilityFeature.DISABLE_ANIMATIONS.value.inv()
//            }
//        }
//        nativeSetAccessibilityFeatures(mNativeView.get(), mAccessibilityFeatureFlags)
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        context.contentResolver.unregisterContentObserver(mAnimationScaleObserver)
//        mAccessibilityManager.removeAccessibilityStateChangeListener(this)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            mAccessibilityManager.removeTouchExplorationStateChangeListener(mTouchExplorationListener)
//        }
//    }
//
//    private fun resetWillNotDraw() {
//        if (!mIsSoftwareRenderingEnabled) {
//            setWillNotDraw(!(mAccessibilityEnabled || mTouchExplorationEnabled))
//        } else {
//            setWillNotDraw(false)
//        }
//    }
//
//    fun onAccessibilityStateChanged(enabled: Boolean) {
//        if (enabled) {
//            ensureAccessibilityEnabled()
//        } else {
//            mAccessibilityEnabled = false
//            if (mAccessibilityNodeProvider != null) {
//                mAccessibilityNodeProvider!!.setAccessibilityEnabled(false)
//            }
//            nativeSetSemanticsEnabled(mNativeView.get(), false)
//        }
//        resetWillNotDraw()
//    }
//
//    /// Must match the enum defined in window.dart.
//    private enum class AccessibilityFeature private constructor(internal val value: Int) {
//        ACCESSIBLE_NAVIGATION(1 shl 0),
//        INVERT_COLORS(1 shl 1), // NOT SUPPORTED
//        DISABLE_ANIMATIONS(1 shl 2)
//    }
//
//    // Listens to the global TRANSITION_ANIMATION_SCALE property and notifies us so
//    // that we can disable animations in Flutter.
//    private inner class AnimationScaleObserver(handler: Handler) : ContentObserver(handler) {
//
//        override fun onChange(selfChange: Boolean) {
//            this.onChange(selfChange, null)
//        }
//
//        fun onChange(selfChange: Boolean, uri: Uri) {
//            val value = Settings.Global.getString(context.contentResolver,
//                    Settings.Global.TRANSITION_ANIMATION_SCALE)
//            if (value === "0") {
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags xor AccessibilityFeature.DISABLE_ANIMATIONS.value
//            } else {
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags and AccessibilityFeature.DISABLE_ANIMATIONS.value.inv()
//            }
//            nativeSetAccessibilityFeatures(mNativeView.get(), mAccessibilityFeatureFlags)
//        }
//    }
//
//    internal inner class TouchExplorationListener : AccessibilityManager.TouchExplorationStateChangeListener {
//        override fun onTouchExplorationStateChanged(enabled: Boolean) {
//            if (enabled) {
//                mTouchExplorationEnabled = true
//                ensureAccessibilityEnabled()
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags xor AccessibilityFeature.ACCESSIBLE_NAVIGATION.value
//                nativeSetAccessibilityFeatures(mNativeView.get(), mAccessibilityFeatureFlags)
//            } else {
//                mTouchExplorationEnabled = false
//                if (mAccessibilityNodeProvider != null) {
//                    mAccessibilityNodeProvider!!.handleTouchExplorationExit()
//                }
//                mAccessibilityFeatureFlags = mAccessibilityFeatureFlags and AccessibilityFeature.ACCESSIBLE_NAVIGATION.value.inv()
//                nativeSetAccessibilityFeatures(mNativeView.get(), mAccessibilityFeatureFlags)
//            }
//            resetWillNotDraw()
//        }
//    }
//
//    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider? {
//        return if (mAccessibilityEnabled) mAccessibilityNodeProvider else null
//        // TODO(goderbauer): when a11y is off this should return a one-off snapshot of
//        // the a11y
//        // tree.
//    }
//
//    private var mAccessibilityNodeProvider: AccessibilityBridge? = null
//
//    fun ensureAccessibilityEnabled() {
//        if (!isAttached())
//            return
//        mAccessibilityEnabled = true
//        if (mAccessibilityNodeProvider == null) {
//            mAccessibilityNodeProvider = AccessibilityBridge(this)
//        }
//        nativeSetSemanticsEnabled(mNativeView.get(), true)
//        mAccessibilityNodeProvider!!.setAccessibilityEnabled(true)
//    }
//
//    fun resetAccessibilityTree() {
//        if (mAccessibilityNodeProvider != null) {
//            mAccessibilityNodeProvider!!.reset()
//        }
//    }
//
//    private fun handleAccessibilityHoverEvent(event: MotionEvent): Boolean {
//        if (!mTouchExplorationEnabled) {
//            return false
//        }
//        if (event.action == MotionEvent.ACTION_HOVER_ENTER || event.action == MotionEvent.ACTION_HOVER_MOVE) {
//            mAccessibilityNodeProvider!!.handleTouchExploration(event.x, event.y)
//        } else if (event.action == MotionEvent.ACTION_HOVER_EXIT) {
//            mAccessibilityNodeProvider!!.handleTouchExplorationExit()
//        } else {
//            Log.d("flutter", "unexpected accessibility hover event: $event")
//            return false
//        }
//        return true
//    }
//
//    fun send(channel: String, message: ByteBuffer) {
//        send(channel, message, null)
//    }
//
//    fun send(channel: String, message: ByteBuffer, callback: BinaryReply?) {
//        if (!isAttached()) {
//            Log.d(TAG, "FlutterView.send called on a detached view, channel=$channel")
//            return
//        }
//        mNativeView.send(channel, message, callback)
//    }
//
//    fun setMessageHandler(channel: String, handler: BinaryMessageHandler) {
//        mNativeView.setMessageHandler(channel, handler)
//    }

    /**
     * Broadcast receiver used to discover active Flutter instances.
     *
     * This is used by the `flutter` tool to find the observatory ports for all the
     * active Flutter views. We dump the data to the logs and the tool scrapes the
     * log lines for the data.
     */
    private inner class DiscoveryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            TODO()
//            val observatoryUri = URI.create(FlutterNativeView.getObservatoryUri())
//            val discover = JSONObject()
//            try {
//                discover.put("id", getContext().packageName)
//                discover.put("observatoryPort", observatoryUri.getPort())
//                Log.i(TAG, "DISCOVER: $discover") // The tool looks for this data. See
//                // android_device.dart.
//            } catch (e: JSONException) {
//            }
        }
    }

    /**
     * Listener will be called on the Android UI thread once when Flutter renders
     * the first frame.
     */
    interface FirstFrameListener {
        fun onFirstFrame()
    }

//    fun createSurfaceTexture(): TextureRegistry.SurfaceTextureEntry {
//        val surfaceTexture = SurfaceTexture(0)
//        surfaceTexture.detachFromGLContext()
//        val entry = SurfaceTextureRegistryEntry(nextTextureId.getAndIncrement(),
//                surfaceTexture)
//        nativeRegisterTexture(mNativeView.get(), entry.id(), surfaceTexture)
//        return entry
//    }

//    internal inner class SurfaceTextureRegistryEntry(private val id: Long, private val surfaceTexture: SurfaceTexture) : TextureRegistry.SurfaceTextureEntry {
//        private var released: Boolean = false
//
//        init {
//            this.surfaceTexture.setOnFrameAvailableListener { nativeMarkTextureFrameAvailable(mNativeView.get(), this@SurfaceTextureRegistryEntry.id) }
//        }
//
//        fun surfaceTexture(): SurfaceTexture {
//            return surfaceTexture
//        }
//
//        fun id(): Long {
//            return id
//        }
//
//        fun release() {
//            if (released) {
//                return
//            }
//            released = true
//            nativeUnregisterTexture(mNativeView.get(), id)
//            surfaceTexture.release()
//        }
//    }
}