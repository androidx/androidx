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
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
     * being displayed.
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
class SandboxedSdkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private var adapter: SandboxedUiAdapter? = null
    private var client: Client? = null
    private var isZOrderOnTop = true
    private var contentView: View? = null
    private var requestedWidth = -1
    private var requestedHeight = -1
    private var isTransitionGroupSet = false
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

    fun setZOrderOnTopAndEnableUserInteraction(setOnTop: Boolean) {
        if (setOnTop == isZOrderOnTop) return
        this.isZOrderOnTop = setOnTop
        checkClientOpenSession()
        client?.notifyZOrderChanged(setOnTop)
    }

    private fun checkClientOpenSession() {
        val adapter = adapter
        if (client == null && adapter != null && isAttachedToWindow && width > 0 && height > 0) {
            stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Loading
            client = Client(this)
            adapter.openSession(
                context,
                width,
                height,
                isZOrderOnTop,
                handler::post,
                client!!
            )
        }
    }

    internal fun requestSize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        requestedWidth = width
        requestedHeight = height
        requestLayout()
    }

    internal fun removeContentView() {
        if (childCount == 1) {
            super.removeViewAt(0)
        }
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
        stateListenerManager.currentUiSessionState = SandboxedSdkUiSessionState.Active
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
        getChildAt(0)?.layout(left, top, right, bottom)
        checkClientOpenSession()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkClientOpenSession()
    }

    override fun onDetachedFromWindow() {
        client?.close()
        client = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        checkClientOpenSession()
        client?.notifyResized(width, height)
    }

    // TODO(b/270971893) Compare to old configuration before notifying of configuration change.
    override fun onConfigurationChanged(config: Configuration?) {
        requireNotNull(config) { "Config cannot be null" }
        super.onConfigurationChanged(config)
        checkClientOpenSession()
        client?.notifyConfigurationChanged(config)
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

        // pendingZOrderOnTop ensures visible and interactive provider UI as long as the UI is
        // unobstructed to the user.
        private var pendingZOrderOnTop: Boolean? = true
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
