/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.client.view

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.core.util.Consumer
import androidx.customview.poolingcontainer.isWithinPoolingContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SandboxedUiAdapter.SessionClient
import androidx.privacysandbox.ui.core.SandboxedUiAdapterSignalOptions
import androidx.privacysandbox.ui.core.SessionData
import androidx.tracing.trace
import kotlin.math.min

/** A listener for events relating to the SandboxedSdkView UI presentation. */
public interface SandboxedSdkViewEventListener {
    /**
     * Called when the UI is committed to the display. The UI might still not be visible to the user
     * at this point due to the SandboxedSdkView's properties. This is the point where the
     * SandboxedSdkView can be made visible to the user.
     */
    public fun onUiDisplayed()

    /**
     * Called when an error occurs in the [SandboxedSdkView]'s UI session. Use [error].getMessage()
     * to get the error message from the UI provider.
     */
    public fun onUiError(error: Throwable)

    /** Called when the UI session of the [SandboxedSdkView] is closed. */
    public fun onUiClosed()
}

/** A type of client that may get refresh requests (to re-establish a session) */
internal interface RefreshableSessionClient : SessionClient {
    /**
     * Called when the provider of content wants to refresh the ui session it holds.
     *
     * @param callback delivers success/failure of the refresh
     */
    fun onSessionRefreshRequested(callback: Consumer<Boolean>)
}

public class SandboxedSdkView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
    private companion object {
        private const val TAG = "SandboxedSdkView"
    }

    private val scrollChangedListener =
        ViewTreeObserver.OnScrollChangedListener { signalMeasurer?.requestUpdatedSignals() }

    private var adapter: SandboxedUiAdapter? = null
    private var client: Client? = null
    private var clientSecondary: Client? = null
    private var isZOrderOnTop = false
    private var contentView: View? = null
    private var refreshCallback: Consumer<Boolean>? = null
    private var requestedWidth = -1
    private var requestedHeight = -1
    private var isTransitionGroupSet = false
    private var previousChildWidth = -1
    private var previousChildHeight = -1
    private var sessionData: SessionData? = null
    private var eventListener: SandboxedSdkViewEventListener? = null
    private val frameCommitCallback = Runnable { sendUiDisplayedEvents() }
    private var closeSessionOnWindowDetachment = true
    private val poolingContainerListenerDelegate = PoolingContainerListenerDelegate(this)
    internal var signalMeasurer: SandboxedSdkViewSignalMeasurer? = null

    // ONLY USE FOR TESTING.
    private val isSandboxProcess =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && android.os.Process.isSdkSandbox()

    /**
     * Sets an event listener to the [SandboxedSdkView] and starts reporting the new events. To
     * listen to all the events during the lifecycle of the SandboxedSdkView, the listener should be
     * set before calling [setAdapter].
     *
     * To remove the eventListener, set the eventListener as null.
     */
    public fun setEventListener(eventListener: SandboxedSdkViewEventListener?) {
        this.eventListener = eventListener
    }

    /**
     * Sets [SandboxedUiAdapter] to the SandboxedSdkView and tries to establish the session.
     *
     * @param sandboxedUiAdapter instance of [SandboxedUiAdapter]. If same instance of
     *   [SandboxedUiAdapter] is passed then it's no-op. If null value is passed then it closes any
     *   existing sessions.
     */
    public fun setAdapter(sandboxedUiAdapter: SandboxedUiAdapter?) {
        if (this.adapter === sandboxedUiAdapter) return
        client?.close()
        client = null
        signalMeasurer = null
        this.adapter = sandboxedUiAdapter
        checkClientOpenSession()
    }

    /**
     * Sets the Z-ordering of the [SandboxedSdkView]'s surface, relative to its window.
     *
     * When [providerUiOnTop] is true, every [android.view.MotionEvent] on the [SandboxedSdkView]
     * area will be sent to the UI provider. When [providerUiOnTop] is false, every
     * [android.view.MotionEvent] will be sent to the client and will also be transferred to the UI
     * provider. By default, [providerUiOnTop] is false.
     *
     * When [providerUiOnTop] is true, the UI provider's surface will be placed above the client's
     * window. In this case, none of the contents of the client's window beneath the provider's
     * surface will be visible.
     */
    @ExperimentalFeatures.ChangingContentUiZOrderApi
    public fun orderProviderUiAboveClientUi(providerUiOnTop: Boolean) {
        if (providerUiOnTop == isZOrderOnTop) return
        client?.notifyZOrderChanged(providerUiOnTop)
        isZOrderOnTop = providerUiOnTop
        checkClientOpenSession()
    }

    internal fun isProviderUiAboveClientUi(): Boolean {
        return isZOrderOnTop
    }

    /**
     * Modifies the behaviour of closing the session on window detachment. This method has no effect
     * when the SandboxedSdkView is parented by a PoolingContainer where session is closed only on
     * onRelease of PoolingContainerListener.
     *
     * @param preserveSessionOnWindowDetachment when true, session is not closed by SandboxedSdkView
     *   on window detachment. When false, it will follow the default behaviour that the session
     *   will be closed by SandboxedSdkView on window detachment.
     */
    public fun preserveSessionOnWindowDetachment(
        preserveSessionOnWindowDetachment: Boolean = true
    ) {
        this.closeSessionOnWindowDetachment = !preserveSessionOnWindowDetachment
    }

    private fun checkClientOpenSession(
        isSecondary: Boolean = false,
        callback: Consumer<Boolean>? = null,
    ) {
        val adapter = adapter
        if (
            adapter != null &&
                sessionData != null &&
                width > 0 &&
                height > 0 &&
                windowVisibility == View.VISIBLE
        ) {
            if (client == null && !isSecondary) {
                var tracePointName = "UiLib#checkClientOpenSession"
                if (isSandboxProcess) {
                    tracePointName = "UiLib#checkClientOpenSessionSandbox"
                }
                // PLEASE ASK BEFORE MOVING. Moving this may affect benchmark metrics.
                trace(tracePointName, {})
                client = Client(this)
                adapter.openSession(
                    context,
                    sessionData!!,
                    width,
                    height,
                    isZOrderOnTop,
                    handler::post,
                    client!!,
                )
            } else if (client != null && isSecondary) {
                clientSecondary = Client(this)
                this.refreshCallback = callback
                adapter.openSession(
                    context,
                    sessionData!!,
                    width,
                    height,
                    isZOrderOnTop,
                    handler::post,
                    clientSecondary!!,
                )
            }
        }
    }

    internal fun requestResize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        requestedWidth = width
        requestedHeight = height
        requestLayout()
    }

    private fun removeContentView() {
        if (childCount == 1) {
            super.removeViewAt(0)
        }
    }

    /**
     * Adds callbacks and listeners that are only valid while this view is attached to a window. All
     * callbacks and listeners added here will be removed in [removeCallbacksOnWindowDetachment].
     */
    private fun addCallbacksOnWindowAttachment() {
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
    }

    private fun removeCallbacksOnWindowDetachment() {
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        CompatImpl.unregisterFrameCommitCallback(viewTreeObserver, frameCommitCallback)
    }

    internal fun setContentView(contentView: View) {
        if (childCount > 0) {
            throw IllegalStateException("Number of children views must not exceed 1")
        }

        this.contentView = contentView
        removeContentView()

        super.addView(contentView, 0, generateDefaultLayoutParams())

        // Wait for the next frame commit before sending a UiDisplayed event to listeners.
        CompatImpl.registerFrameCommitCallback(viewTreeObserver, frameCommitCallback)
    }

    internal fun onClientClosedSession(error: Throwable? = null) {
        removeContentView()
        signalMeasurer?.stopMeasuring()
        signalMeasurer = null
        if (error != null) {
            eventListener?.onUiError(error)
        }
    }

    private fun calculateMeasuredDimension(requestedSize: Int, measureSpec: Int): Int {
        val measureSpecSize = MeasureSpec.getSize(measureSpec)

        when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> {
                return measureSpecSize
            }
            MeasureSpec.UNSPECIFIED -> {
                return if (requestedSize < 0) {
                    measureSpecSize
                } else {
                    requestedSize
                }
            }
            MeasureSpec.AT_MOST -> {
                return if (requestedSize >= 0) {
                    min(requestedSize, measureSpecSize)
                } else {
                    measureSpecSize
                }
            }
            else -> {
                return measureSpecSize
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val newWidth = calculateMeasuredDimension(requestedWidth, widthMeasureSpec)
        val newHeight = calculateMeasuredDimension(requestedHeight, heightMeasureSpec)
        requestedWidth = -1
        requestedHeight = -1
        setMeasuredDimension(newWidth, newHeight)
        if (childCount > 0) {
            measureChild(getChildAt(0), widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun isTransitionGroup(): Boolean = !isTransitionGroupSet || super.isTransitionGroup()

    override fun setTransitionGroup(isTransitionGroup: Boolean) {
        super.setTransitionGroup(isTransitionGroup)
        isTransitionGroupSet = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        maybeAttachPoolingContainerListener()

        val childView = getChildAt(0)
        if (childView != null) {
            val childWidth = Math.max(0, width - paddingLeft - paddingRight)
            val childHeight = Math.max(0, height - paddingTop - paddingBottom)
            // We will not call client?.notifyResized for the first onLayout call
            // and the case in which the width and the height remain unchanged.
            if (
                previousChildHeight != -1 &&
                    previousChildWidth != -1 &&
                    (childWidth != previousChildWidth || childHeight != previousChildHeight)
            ) {
                client?.notifyResized(childWidth, childHeight)
            } else {
                // Child needs to receive coordinates that are relative to the parent.
                childView.layout(
                    /* left = */ paddingLeft,
                    /* top = */ paddingTop,
                    /* right = */ paddingLeft + childWidth,
                    /* bottom = */ paddingTop + childHeight,
                )
            }
            previousChildHeight = childHeight
            previousChildWidth = childWidth
        }
        checkClientOpenSession()
        signalMeasurer?.requestUpdatedSignals(onLayoutEventOccurred = true)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            checkClientOpenSession()
        }
        signalMeasurer?.requestUpdatedSignals()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        signalMeasurer?.requestUpdatedSignals()
    }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        signalMeasurer?.requestUpdatedSignals()
        contentView?.alpha = alpha
    }

    internal fun closeClient() {
        client?.close()
        client = null
        sessionData = null
    }

    private fun maybeAttachPoolingContainerListener() {
        poolingContainerListenerDelegate.maybeAttachListener { closeClient() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addCallbacksOnWindowAttachment()
        maybeAttachPoolingContainerListener()
        if (client == null) {
            CompatImpl.deriveInputTokenAndOpenSession(context, this)
        }
        signalMeasurer?.resumeMeasuringIfNecessary()
    }

    override fun onDetachedFromWindow() {
        if (!this.isWithinPoolingContainer && closeSessionOnWindowDetachment) {
            closeClient()
        }
        signalMeasurer?.stopMeasuring()
        removeCallbacksOnWindowDetachment()
        super.onDetachedFromWindow()
    }

    // TODO(b/298658350): Cache previous config properly to avoid unnecessary binder calls
    override fun onConfigurationChanged(config: Configuration?) {
        requireNotNull(config) { "Config cannot be null" }
        super.onConfigurationChanged(config)
        client?.notifyConfigurationChanged(config)
        checkClientOpenSession()
    }

    /** @throws UnsupportedOperationException when called */
    override fun addView(view: View?, index: Int, params: LayoutParams?) {
        throw UnsupportedOperationException("Cannot add a view to SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeView(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeViewInLayout(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeViewsInLayout(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeViews(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeAllViews() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /** @throws UnsupportedOperationException when called */
    override fun removeAllViewsInLayout() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    private fun addTemporarySurfaceView(surfaceView: SurfaceView) {
        super.addView(surfaceView, 0, generateDefaultLayoutParams())
    }

    private fun removeTemporarySurfaceView(surfaceView: SurfaceView) {
        super.removeView(surfaceView)
    }

    /**
     * A SandboxedSdkView may have one active primary client and a secondary client with which a
     * session is being formed. Once [Client.onSessionOpened] is received on the secondaryClient we
     * close the session with the primary client and promote the secondary to the primary client.
     */
    internal class Client(private var sandboxedSdkView: SandboxedSdkView?) :
        RefreshableSessionClient {

        private var session: SandboxedUiAdapter.Session? = null
        private var pendingWidth: Int? = null
        private var pendingHeight: Int? = null

        private var pendingZOrderOnTop: Boolean? = null
        private var pendingConfiguration: Configuration? = null
        private val eventListener = sandboxedSdkView?.eventListener
        private var supportedSignalOptions =
            setOf(
                SandboxedUiAdapterSignalOptions.GEOMETRY,
                SandboxedUiAdapterSignalOptions.OBSTRUCTIONS,
            )

        // ONLY USE FOR TESTING.
        private val isSandboxProcess =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                android.os.Process.isSdkSandbox()

        fun notifyConfigurationChanged(configuration: Configuration) {
            val session = session
            if (session != null) {
                session.notifyConfigurationChanged(configuration)
            } else {
                pendingConfiguration = configuration
            }
        }

        fun notifyResized(width: Int, height: Int) {
            val session = session
            if (session != null) {
                session.notifyResized(width, height)
            } else {
                pendingWidth = width
                pendingHeight = height
            }
        }

        fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
            if (sandboxedSdkView?.isZOrderOnTop == isZOrderOnTop) return
            val session = session
            if (session != null) {
                session.notifyZOrderChanged(isZOrderOnTop)
            } else {
                pendingZOrderOnTop = isZOrderOnTop
            }
        }

        fun close() {
            eventListener?.onUiClosed()
            session?.close()
            session = null
            sandboxedSdkView?.onClientClosedSession()
            sandboxedSdkView = null
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            var tracePointName = "UiLib#ssvOnSessionOpened"
            if (isSandboxProcess) {
                tracePointName = "UiLib#ssvOnSessionOpenedSandbox"
            }
            // PLEASE ASK BEFORE MOVING. Moving this may affect benchmark metrics.
            trace(tracePointName, {})
            if (sandboxedSdkView == null) {
                close()
                return
            }
            val view = checkNotNull(sandboxedSdkView) { "SandboxedSdkView should not be null" }
            if (this === view.clientSecondary) {
                view.switchClient()
                view.refreshCallback?.accept(true)
            }
            this.session = session
            view.setContentView(session.view)
            val width = pendingWidth
            val height = pendingHeight
            if ((width != null) && (height != null) && (width >= 0) && (height >= 0)) {
                session.notifyResized(width, height)
            }
            pendingConfiguration?.let { session.notifyConfigurationChanged(it) }
            pendingConfiguration = null
            pendingZOrderOnTop?.let { session.notifyZOrderChanged(it) }
            pendingZOrderOnTop = null
            if (session.signalOptions.contains(SandboxedUiAdapterSignalOptions.GEOMETRY)) {
                view.signalMeasurer = SandboxedSdkViewSignalMeasurer(view, session)
            }
        }

        override fun onSessionError(throwable: Throwable) {
            sandboxedSdkView?.let { view ->
                if (this == view.clientSecondary) {
                    view.clientSecondary = null
                    Log.w(TAG, "Secondary client session error: $throwable")
                    view.refreshCallback?.accept(false)
                } else {
                    view.onClientClosedSession(throwable)
                }
            }
        }

        override fun onResizeRequested(width: Int, height: Int) {
            sandboxedSdkView?.requestResize(width, height)
        }

        override fun onSessionRefreshRequested(callback: Consumer<Boolean>) {
            // PLEASE ASK BEFORE MOVING. Moving this may affect benchmark metrics.
            trace("UiLib#onSessionRefreshRequested", {})
            sandboxedSdkView?.checkClientOpenSession(true, callback)
        }

        fun notifySessionRendered() {
            session?.notifySessionRendered(supportedSignalOptions)
        }
    }

    private fun switchClient() {
        if (this.clientSecondary == null) {
            throw java.lang.IllegalStateException("secondary client must be non null for switch")
        }
        // close session with primary client
        this.client?.close()
        this.client = this.clientSecondary
    }

    // Called when the first frame is displayed after a new session is opened.
    private fun sendUiDisplayedEvents() {
        eventListener?.onUiDisplayed()
        this.client?.notifySessionRendered()
    }

    /**
     * Provides backward compat support for APIs.
     *
     * If the API is available, it's called from a version-specific static inner class gated with
     * version check, otherwise a fallback action is taken depending on the situation.
     */
    private object CompatImpl {

        fun deriveInputTokenAndOpenSession(context: Context, sandboxedSdkView: SandboxedSdkView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Api35PlusImpl.setInputTransferTokenAndOpenSession(sandboxedSdkView)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34PlusImpl.attachTemporarySurfaceViewAndOpenSession(context, sandboxedSdkView)
            } else {
                sandboxedSdkView.sessionData = SessionData()
                sandboxedSdkView.checkClientOpenSession()
            }
        }

        fun registerFrameCommitCallback(observer: ViewTreeObserver, callback: Runnable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Api29PlusImpl.registerFrameCommitCallback(observer, callback)
            } else {
                callback.run()
            }
        }

        fun unregisterFrameCommitCallback(observer: ViewTreeObserver, callback: Runnable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Api29PlusImpl.unregisterFrameCommitCallback(observer, callback)
            }
        }

        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        private object Api35PlusImpl {
            @JvmStatic
            fun setInputTransferTokenAndOpenSession(sandboxedSdkView: SandboxedSdkView) {
                sandboxedSdkView.sessionData =
                    SessionData(
                        windowInputToken = null,
                        inputTransferToken = sandboxedSdkView.rootSurfaceControl?.inputTransferToken,
                    )
                sandboxedSdkView.checkClientOpenSession()
            }
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private object Api34PlusImpl {

            @JvmStatic
            fun attachTemporarySurfaceViewAndOpenSession(
                context: Context,
                sandboxedSdkView: SandboxedSdkView,
            ) {
                val surfaceView = SurfaceView(context).apply { visibility = GONE }
                val onSurfaceViewAttachedListener =
                    object : OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            view.removeOnAttachStateChangeListener(this)
                            @Suppress("DEPRECATION")
                            surfaceView.hostToken?.let {
                                sandboxedSdkView.sessionData =
                                    SessionData(windowInputToken = it, inputTransferToken = null)
                            }
                            sandboxedSdkView.removeTemporarySurfaceView(surfaceView)
                            sandboxedSdkView.checkClientOpenSession()
                        }

                        override fun onViewDetachedFromWindow(view: View) {}
                    }
                surfaceView.addOnAttachStateChangeListener(onSurfaceViewAttachedListener)
                sandboxedSdkView.addTemporarySurfaceView(surfaceView)
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private object Api29PlusImpl {

            @JvmStatic
            fun registerFrameCommitCallback(observer: ViewTreeObserver, callback: Runnable) {
                observer.registerFrameCommitCallback(callback)
            }

            @JvmStatic
            fun unregisterFrameCommitCallback(observer: ViewTreeObserver, callback: Runnable) {
                observer.unregisterFrameCommitCallback(callback)
            }
        }
    }
}
