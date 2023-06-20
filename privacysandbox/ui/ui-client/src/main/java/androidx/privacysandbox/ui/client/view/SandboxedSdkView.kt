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
import android.graphics.Rect
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.AttributeSet
import android.view.SurfaceControl
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState.Active
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState.Idle
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState.Loading
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

/**
 * A listener for changes to the state of the UI session associated with SandboxedSdkView.
 */
fun interface SandboxedSdkUiSessionStateChangedListener {
    /**
     * Called when the state of the session for SandboxedSdkView is updated.
     */
    fun onStateChanged(state: SandboxedSdkUiSessionState)
}

/**
 * Represents the state of a UI session.
 *
 * A UI session refers to the session opened with a [SandboxedUiAdapter] to let the host display UI
 * from the UI provider. If the host has requested to open a session with its [SandboxedUiAdapter],
 * the state will be [Loading] until the session has been opened and the content has been displayed.
 * At this point, the state will become [Active]. If there is no active session and no session is
 * being loaded, the state is [Idle].
 */
sealed class SandboxedSdkUiSessionState private constructor() {
    /**
     * A UI session is currently attempting to be opened.
     *
     * This state occurs when the UI has requested to open a session with its [SandboxedUiAdapter].
     * No UI from the [SandboxedUiAdapter] will be shown during this state. When the session has
     * been successfully opened and the content has been displayed, the state will transition to
     * [Active].
     */
    object Loading : SandboxedSdkUiSessionState()

    /**
     * There is an open session with the supplied [SandboxedUiAdapter] and its UI is currently
     * being displayed. This state is set after the first draw event of the [SandboxedSdkView].
     */
    object Active : SandboxedSdkUiSessionState()

    /**
     * There is no currently open UI session and there is no operation in progress to open one.
     *
     * The UI provider may close the session at any point, which will result in the state becoming
     * [Idle] if the session is closed without an error. If there is an error that causes the
     * session to close, the state will be [Error].
     *
     * If a new [SandboxedUiAdapter] is set on a [SandboxedSdkView], the existing session will close
     * and the state will become [Idle].
     */
    object Idle : SandboxedSdkUiSessionState()

    /**
     * There was an error in the UI session.
     *
     * @param throwable The error that caused the session to end.
     */
    class Error(val throwable: Throwable) : SandboxedSdkUiSessionState()
}

// TODO(b/268014171): Remove API requirements once S- support is added
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SandboxedSdkView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ViewGroup(context, attrs) {

    // TODO(b/284147223): Remove this logic in V+
    private val surfaceView = SurfaceView(context).apply {
        visibility = GONE
    }

    // This will only be invoked when the content view has been set and the window is attached.
    private val surfaceChangedCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(p0: SurfaceHolder) {
            setClippingBounds(true)
            viewTreeObserver.addOnGlobalLayoutListener(globalLayoutChangeListener)
        }

        override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
        }
    }

    // This will only be invoked when the content view has been set and the window is attached.
    private val globalLayoutChangeListener =
        ViewTreeObserver.OnGlobalLayoutListener { setClippingBounds() }

    private var adapter: SandboxedUiAdapter? = null
    private var client: Client? = null
    private var isZOrderOnTop = true
    private var contentView: View? = null
    private var requestedWidth = -1
    private var requestedHeight = -1
    private var isTransitionGroupSet = false
    private var windowInputToken: IBinder? = null
    private var currentClippingBounds = Rect()
    private var currentConfig = context.resources.configuration
    internal val stateListenerManager: StateListenerManager = StateListenerManager()

    /**
     * Adds a state change listener to the UI session and immediately reports the current
     * state.
     */
    fun addStateChangedListener(stateChangedListener: SandboxedSdkUiSessionStateChangedListener) {
        stateListenerManager.addStateChangedListener(stateChangedListener)
    }

    /**
     * Removes the specified state change listener from SandboxedSdkView.
     */
    fun removeStateChangedListener(
        stateChangedListener: SandboxedSdkUiSessionStateChangedListener
    ) {
        stateListenerManager.removeStateChangedListener(stateChangedListener)
    }

    fun setAdapter(sandboxedUiAdapter: SandboxedUiAdapter) {
        if (this.adapter === sandboxedUiAdapter) return
        client?.close()
        client = null
        this.adapter = sandboxedUiAdapter
        checkClientOpenSession()
    }

    /**
     * Sets the Z-ordering of the [SandboxedSdkView]'s surface, relative to its window.
     *
     * When [providerUiOnTop] is true, every [android.view.MotionEvent] on the [SandboxedSdkView]
     * will be sent to the UI provider. When [providerUiOnTop] is false, every
     * [android.view.MotionEvent] will be sent to the client. By default, motion events are sent to
     * the UI provider.
     */
    fun orderProviderUiAboveClientUi(providerUiOnTop: Boolean) {
        if (providerUiOnTop == isZOrderOnTop) return
        client?.notifyZOrderChanged(providerUiOnTop)
        isZOrderOnTop = providerUiOnTop
        checkClientOpenSession()
    }

    internal fun setClippingBounds(forceUpdate: Boolean = false) {
        checkNotNull(contentView)
        check(isAttachedToWindow)

        val updateRequired = getBoundingParent(currentClippingBounds) || forceUpdate
        if (!updateRequired) {
            return
        }

        val sv: SurfaceView = contentView as SurfaceView
        val attachedSurfaceControl = checkNotNull(sv.rootSurfaceControl) {
            "attachedSurfaceControl should be non-null if the window is attached"
        }
        val name = "clippingBounds-${System.currentTimeMillis()}"
        val clippingBoundsSurfaceControl =
            SurfaceControl.Builder().setName(name)
                .build()
        val reparentSurfaceControlTransaction = SurfaceControl.Transaction()
            .reparent(sv.surfaceControl, clippingBoundsSurfaceControl)

        val reparentClippingBoundsTransaction =
            checkNotNull(
                attachedSurfaceControl.buildReparentTransaction(clippingBoundsSurfaceControl)) {
                "Reparent transaction should be non-null if the window is attached"
            }
        reparentClippingBoundsTransaction.setCrop(
            clippingBoundsSurfaceControl, currentClippingBounds)
        reparentClippingBoundsTransaction.setVisibility(
            clippingBoundsSurfaceControl, true)
        reparentSurfaceControlTransaction.merge(reparentClippingBoundsTransaction)
        attachedSurfaceControl.applyTransactionOnDraw(reparentSurfaceControlTransaction)
    }

    /**
     * Computes the window space coordinates for the bounding parent of this view, and stores the
     * result in [rect].
     *
     * Returns true if the coordinates have changed, false otherwise.
     */
    @VisibleForTesting
    internal fun getBoundingParent(rect: Rect): Boolean {
        val prevBounds = Rect(rect)
        var viewParent: ViewParent? = parent
        while (viewParent != null && viewParent is View) {
            val v = viewParent as View
            if (v.isScrollContainer || v.id == android.R.id.content) {
                v.getGlobalVisibleRect(rect)
                return prevBounds != rect
            }
            viewParent = viewParent.getParent()
        }
        return false
    }

    private fun checkClientOpenSession() {
        val adapter = adapter
        if (client == null && adapter != null && windowInputToken != null &&
            width > 0 && height > 0) {
            stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Loading
            client = Client(this)
            adapter.openSession(
                context,
                windowInputToken!!,
                width,
                height,
                isZOrderOnTop,
                handler::post,
                client!!
            )
        }
    }

    /**
     * Attaches a temporary [SurfaceView] to the view hierarchy. This [SurfaceView] will be removed
     * once it has been attached to the window and its host token is non-null.
     *
     * TODO(b/284147223): Remove this logic in V+
     */
    private fun attachTemporarySurfaceView() {
        val onSurfaceViewAttachedListener =
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    view.removeOnAttachStateChangeListener(this)
                    removeSurfaceViewAndOpenSession()
                }

                override fun onViewDetachedFromWindow(view: View) {
                }
            }
        surfaceView.addOnAttachStateChangeListener(onSurfaceViewAttachedListener)
        super.addView(surfaceView, 0, generateDefaultLayoutParams())
    }

    internal fun removeSurfaceViewAndOpenSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            windowInputToken = surfaceView.hostToken
        } else {
            // Since there is no SdkSandbox, we don't need windowInputToken for creating SCVH
            windowInputToken = Binder()
        }
        super.removeView(surfaceView)
        checkClientOpenSession()
    }

    internal fun requestSize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        requestedWidth = width
        requestedHeight = height
        requestLayout()
    }

    private fun removeContentView() {
        removeCallbacks()
        if (childCount == 1) {
            super.removeViewAt(0)
        }
    }

    private fun removeCallbacks() {
        (contentView as? SurfaceView)?.holder?.removeCallback(surfaceChangedCallback)
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutChangeListener)
    }

    internal fun setContentView(contentView: View) {
        if (childCount > 1) {
            throw IllegalStateException("Number of children views must not exceed 1")
        }

        this.contentView = contentView
        removeContentView()

        if (contentView.layoutParams == null) {
            super.addView(contentView, 0, generateDefaultLayoutParams())
        } else {
            super.addView(contentView, 0, contentView.layoutParams)
        }
        // Wait for the next frame commit before sending an ACTIVE state change to listeners.
        viewTreeObserver.registerFrameCommitCallback {
            stateListenerManager.currentUiSessionState =
                SandboxedSdkUiSessionState.Active
        }

        if (contentView is SurfaceView) {
            contentView.holder.addCallback(surfaceChangedCallback)
        }
    }

    internal fun onClientClosedSession(error: Throwable? = null) {
        removeContentView()
        stateListenerManager.currentUiSessionState = if (error != null) {
            SandboxedSdkUiSessionState.Error(error)
        } else {
            SandboxedSdkUiSessionState.Idle
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
        setMeasuredDimension(newWidth, newHeight)
    }

    override fun isTransitionGroup(): Boolean = !isTransitionGroupSet || super.isTransitionGroup()

    override fun setTransitionGroup(isTransitionGroup: Boolean) {
        super.setTransitionGroup(isTransitionGroup)
        isTransitionGroupSet = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // Child needs to receive coordinates that are relative to the parent.
        getChildAt(0)?.layout(
            /* l = */ 0,
            /* t = */ 0,
            /* r = */ right - left,
            /* b = */ bottom - top)
        checkClientOpenSession()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachTemporarySurfaceView()
    }

    override fun onDetachedFromWindow() {
        client?.close()
        client = null
        windowInputToken = null
        removeCallbacks()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        client?.notifyResized(width, height)
        checkClientOpenSession()
    }

    override fun onConfigurationChanged(config: Configuration?) {
        requireNotNull(config) { "Config cannot be null" }
        if (config == currentConfig) {
            return
        }
        super.onConfigurationChanged(config)
        currentConfig = config
        client?.notifyConfigurationChanged(config)
        checkClientOpenSession()
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun addView(
        view: View?,
        index: Int,
        params: LayoutParams?
    ) {
        throw UnsupportedOperationException("Cannot add a view to SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeView(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewInLayout(view: View?) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewsInLayout(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViewAt(index: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeViews(start: Int, count: Int) {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeAllViews() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    /**
     * @throws UnsupportedOperationException when called
     */
    override fun removeAllViewsInLayout() {
        throw UnsupportedOperationException("Cannot remove a view from SandboxedSdkView")
    }

    internal class Client(private var sandboxedSdkView: SandboxedSdkView?) :
        SandboxedUiAdapter.SessionClient {

        private var session: SandboxedUiAdapter.Session? = null
        private var pendingWidth: Int? = null
        private var pendingHeight: Int? = null

        private var pendingZOrderOnTop: Boolean? = null
        private var pendingConfiguration: Configuration? = null

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
            session?.close()
            session = null
            sandboxedSdkView?.onClientClosedSession()
            sandboxedSdkView = null
        }

        override fun onSessionOpened(session: SandboxedUiAdapter.Session) {
            if (sandboxedSdkView == null) {
                session.close()
                return
            }
            sandboxedSdkView?.setContentView(session.view)
            this.session = session
            val width = pendingWidth
            val height = pendingHeight
            if ((width != null) && (height != null) && (width >= 0) && (height >= 0)) {
                session.notifyResized(width, height)
            }
            pendingConfiguration?.let {
                session.notifyConfigurationChanged(it)
            }
            pendingConfiguration = null
            pendingZOrderOnTop?.let {
                session.notifyZOrderChanged(it)
            }
            pendingZOrderOnTop = null
        }

        override fun onSessionError(throwable: Throwable) {
            if (sandboxedSdkView == null) return

            sandboxedSdkView?.onClientClosedSession(throwable)
        }

        override fun onResizeRequested(width: Int, height: Int) {
            if (sandboxedSdkView == null) return
            sandboxedSdkView?.requestSize(width, height)
        }
    }

    internal class StateListenerManager {
        internal var currentUiSessionState: SandboxedSdkUiSessionState =
            SandboxedSdkUiSessionState.Idle
            set(value) {
                if (field != value) {
                    field = value
                    for (listener in stateChangedListeners) {
                        listener.onStateChanged(currentUiSessionState)
                    }
                }
            }

        private var stateChangedListeners =
            CopyOnWriteArrayList<SandboxedSdkUiSessionStateChangedListener>()

        fun addStateChangedListener(listener: SandboxedSdkUiSessionStateChangedListener) {
            stateChangedListeners.add(listener)
            listener.onStateChanged(currentUiSessionState)
        }

        fun removeStateChangedListener(listener: SandboxedSdkUiSessionStateChangedListener) {
            stateChangedListeners.remove(listener)
        }
    }
}
