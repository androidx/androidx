/*
 * Copyright 2025 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.annotation.VisibleForTesting
import androidx.customview.poolingcontainer.isWithinPoolingContainer
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SharedUiAdapter
import kotlin.math.max

/**
 * [SharedUiContainer] is a [ViewGroup] that's designed to host 'shared UI', meaning it can handle
 * both client-owned and provider-owned UI (via [SandboxedSdkView]s). The container also allows to
 * register its child views as assets. Assets can be registered either before or after the session
 * is open.
 *
 * __Children__: The container should be used to host a single direct child view. However, it allows
 * adding more than one child view to it. All child views added to [SharedUiContainer] are placed in
 * the top left corner of the container plus padding.
 *
 * __Session management__: The container will maintain a session to communicate with a sandboxed SDK
 * for lifecycle management. To open a session, a [SharedUiAdapter] must be set using [setAdapter],
 * and the container must be attached to a visible window and have non-zero dimensions.
 *
 * When [setAdapter] is called, any existing session will be closed and all registered assets will
 * be released.
 *
 * __Provider-owned UI__: All registered child [SandboxedSdkView]s will maintain their own sessions
 * for provider-owned UI presentation. For all [SandboxedSdkView]s registered with
 * [SandboxedUiAdapter]s, [SharedUiContainer] will set corresponding adapters when a shared UI
 * session opens. For any [SandboxedSdkView]s registered when the session is open, adapters will be
 * set immediately. The container will close any open [SandboxedSdkView] sessions before closing its
 * own session.
 *
 * __Asset registration__: Client-owned views and [SandboxedSdkView]s can be registered as assets
 * using [registerSharedUiAsset] and unregistered using [unregisterSharedUiAsset].
 */
// OptIn calling the experimental API SandboxedSdkView#orderProviderUiAboveClientUi
@OptIn(ExperimentalFeatures.ChangingContentUiZOrderApi::class)
@SuppressLint("NullAnnotationGroup")
@ExperimentalFeatures.SharedUiPresentationApi
public class SharedUiContainer
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    @VisibleForTesting internal val registeredAssets: MutableMap<View, SharedUiAsset> = HashMap()
    private val onAttachStateChangeListener =
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                if (isWithinPoolingContainer && containsView(v)) return
                unregisterSharedUiAsset(v)
            }
        }
    private val poolingContainerListenerDelegate = PoolingContainerListenerDelegate(this)

    private var sessionManager: SessionManager? = null

    /**
     * Sets [SharedUiAdapter] to maintain a session between [SharedUiContainer] and a sandboxed SDK.
     *
     * If there's already an adapter set, and it's different from [sharedUiAdapter], its shared UI
     * session is closed, assets registered using [registerSharedUiAsset] are unregistered, and UI
     * sessions for all registered [SandboxedSdkView] assets are closed.
     *
     * If [sharedUiAdapter] is the same as the previously set adapter, nothing happens.
     *
     * Passing 'null' value for [sharedUiAdapter] will release all resources and unregister all
     * children, without setting a new adapter.
     */
    public fun setAdapter(sharedUiAdapter: SharedUiAdapter?) {
        if (sharedUiAdapter === sessionManager?.sharedUiAdapter) return

        sessionManager?.closeClient()
        unregisterSharedUiAssets()

        sessionManager = if (sharedUiAdapter != null) SessionManager(sharedUiAdapter) else null
        sessionManager?.checkClientOpenSession()
    }

    /**
     * Registers a [SharedUiAsset], that represents a native ad asset, on [SharedUiContainer]. The
     * UI provider will receive information about the asset throughout the lifetime of the shared UI
     * session. Because of this, no user-sensitive information should be stored in [sharedUiAsset].
     *
     * [sharedUiAsset] must comprise a [View] and its asset ID. This view must be a child, direct or
     * indirect, of the container. If the view gets detached from window, it will be unregistered
     * from the container.
     *
     * One [View] can only be associated with one asset. If there's already another [SharedUiAsset]
     * registered for the [View], [sharedUiAsset] won't be registered, and the method will return
     * 'false'.
     *
     * Asset IDs will be used by the UI provider to identify an asset view and should be obtained
     * beforehand from the provider. Within the container, an asset ID doesn't have to be unique -
     * the container doesn't impose any restrictions in this regard, and the exact asset ID
     * structure should be a part of the contract between the UI client and provider.
     *
     * [SharedUiAsset] allows to provide a [SandboxedUiAdapter] for registered [SandboxedSdkView]s.
     * In this case, the container will handle session management of the view: [SandboxedUiAdapter]
     * will be set on the view once the container's session is open or, if it's already open, will
     * be set right away. However, the session will be closed by the container before the shared UI
     * session in any case.
     *
     * @return 'true' if [sharedUiAsset] was successfully registered, 'false' if there was another
     *   [SharedUiAsset] registered for the provided [View].
     * @throws [IllegalArgumentException] if a [View] is not a child (direct or indirect) of the
     *   container.
     */
    public fun registerSharedUiAsset(sharedUiAsset: SharedUiAsset): Boolean {
        val view = sharedUiAsset.view
        if (!containsView(view)) {
            throw IllegalArgumentException(
                "A view must be either attached to the container or any of its children to be registered by SharedUiContainer."
            )
        }

        if (registeredAssets.containsKey(view)) {
            return false
        }

        registeredAssets[view] = sharedUiAsset
        view.addOnAttachStateChangeListener(onAttachStateChangeListener)

        if (view is SandboxedSdkView && sharedUiAsset.sandboxedUiAdapter != null) {
            view.orderProviderUiAboveClientUi(false)
            // If the shared UI session is already open, the adapter should be set on the registered
            // view immediately.
            sessionManager?.trySetSandboxedSdkViewAdapter(view)
        }

        return true
    }

    /**
     * Unregisters a [SharedUiAsset] associated with a given [view] from the container. If the view
     * is a [SandboxedSdkView], its provider-owned UI session is closed.
     *
     * @return 'true' if the asset was unregistered successfully, 'false' otherwise (if there wasn't
     *   a [SharedUiAsset] associated with [view])
     */
    public fun unregisterSharedUiAsset(view: View): Boolean {
        if (!registeredAssets.containsKey(view)) return false

        if (view is SandboxedSdkView) {
            sessionManager?.closeSandboxedSdkViewSession(view)
        }
        registeredAssets.remove(view)
        view.removeOnAttachStateChangeListener(onAttachStateChangeListener)
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        maybeAttachPoolingContainerListener()
        sessionManager?.checkClientOpenSession()
    }

    override fun onDetachedFromWindow() {
        if (!isWithinPoolingContainer) {
            sessionManager?.closeClient()
        }
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            sessionManager?.checkClientOpenSession()
        }
    }

    /**
     * Lays out the container's children in the top left corner with their measured sizes. Takes
     * into account the container's padding settings.
     *
     * Child views that are [View.GONE] are ignored and don't take any space.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        maybeAttachPoolingContainerListener()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Ignore View.GONE views as these are not expected to take any space for layout
            // purposes.
            if (child.visibility == GONE) {
                continue
            }
            child.layout(
                paddingLeft,
                paddingTop,
                child.measuredWidth + paddingLeft,
                child.measuredHeight + paddingTop,
            )
        }
        sessionManager?.checkClientOpenSession()
    }

    /**
     * Measures the container and its children. The size of the container is determined by the size
     * of its largest child, container's padding and suggested dimensions, but only if they do not
     * exceed size restrictions imposed by the container's parent view. Child views are measured
     * with respect to the container's padding settings.
     *
     * [View.GONE] child views are not used for sizing and are not measured.
     */
    // TODO(b/373866405): extract out the code common across SharedUiContainer and SandboxedSdkView.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        var maxHeight = 0
        var childState = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Don't use View.GONE views for sizing and measurement as these are not expected to
            // take any space.
            if (child.visibility == GONE) {
                continue
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            maxWidth = max(maxWidth, child.measuredWidth)
            maxHeight = max(maxHeight, child.measuredHeight)
            childState = combineMeasuredStates(childState, child.measuredState)
        }

        maxWidth = max(maxWidth + paddingLeft + paddingRight, suggestedMinimumWidth)
        maxHeight = max(maxHeight + paddingTop + paddingBottom, suggestedMinimumHeight)

        setMeasuredDimension(
            resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
            resolveSizeAndState(
                maxHeight,
                heightMeasureSpec,
                childState shl MEASURED_HEIGHT_STATE_SHIFT,
            ),
        )
    }

    private fun unregisterSharedUiAssets() {
        sessionManager?.closeSandboxedSdkViewSessions()

        val iterator = registeredAssets.iterator()
        while (iterator.hasNext()) {
            val (view, _) = iterator.next().toPair()
            iterator.remove()
            view.removeOnAttachStateChangeListener(onAttachStateChangeListener)
        }
    }

    private fun containsView(view: View): Boolean {
        var currentView: View = view
        var parentView: ViewParent? = currentView.parent
        while (parentView != null && parentView is View) {
            if (parentView === this) {
                return true
            }
            currentView = parentView
            parentView = currentView.parent
        }
        return false
    }

    private fun maybeAttachPoolingContainerListener() {
        poolingContainerListenerDelegate.maybeAttachListener { sessionManager?.closeClient() }
    }

    private inner class SessionManager(val sharedUiAdapter: SharedUiAdapter) {
        private var client: Client? = null

        fun checkClientOpenSession() {
            val adapter = sharedUiAdapter
            if (
                client == null &&
                    isAttachedToWindow &&
                    width > 0 &&
                    height > 0 &&
                    windowVisibility == View.VISIBLE
            ) {
                client = Client(this)
                adapter.openSession(handler::post, client!!)
            }
        }

        fun closeClient() {
            if (client == null) return
            closeSandboxedSdkViewSessions()
            client?.close()
            client = null
        }

        /**
         * Sets adapters for all [SandboxedSdkView]s registered with [SandboxedUiAdapter]s. Adapters
         * are only set if there's an open shared UI session.
         *
         * Called by the container once the session is open, i.e. in
         * [SharedUiAdapter.SessionClient.onSessionOpened].
         */
        fun trySetSandboxedSdkViewAdapters() {
            if (client == null) return
            registeredAssets.forEach { (view, sharedUiAsset) ->
                if (view !is SandboxedSdkView) return@forEach
                val sandboxedUiAdapter = sharedUiAsset.sandboxedUiAdapter
                if (sandboxedUiAdapter != null) view.setAdapter(sandboxedUiAdapter)
            }
        }

        /**
         * Sets the adapter for an individual registered [sandboxedSdkView] if there's an adapter
         * provided for it and if there's an open shared UI session. Called by the container when
         * the view is registered as an asset.
         */
        fun trySetSandboxedSdkViewAdapter(sandboxedSdkView: SandboxedSdkView) {
            if (client == null) return
            val sandboxedUiAdapter = registeredAssets[sandboxedSdkView]?.sandboxedUiAdapter
            if (sandboxedUiAdapter != null) sandboxedSdkView.setAdapter(sandboxedUiAdapter)
        }

        fun closeSandboxedSdkViewSession(sandboxedSdkView: SandboxedSdkView) {
            sandboxedSdkView.closeClient()
        }

        /** Closes provider UI sessions for [SandboxedSdkView]s registered as assets. */
        fun closeSandboxedSdkViewSessions() {
            registeredAssets.forEach { (view, _) ->
                if (view is SandboxedSdkView) view.closeClient()
            }
        }

        private inner class Client(private var sessionManager: SessionManager?) :
            SharedUiAdapter.SessionClient {
            private var currentSession: SharedUiAdapter.Session? = null

            fun close() {
                currentSession?.close()
                currentSession = null
                sessionManager = null
            }

            override fun onSessionOpened(session: SharedUiAdapter.Session) {
                if (sessionManager == null) {
                    session.close()
                    return
                }
                // Once the shared UI session is open, adapters should be set on all previously
                // registered SandboxedSdkViews
                sessionManager?.trySetSandboxedSdkViewAdapters()
                currentSession = session
            }

            override fun onSessionError(throwable: Throwable) {
                if (sessionManager == null) return
                sessionManager?.closeClient()
                sessionManager = null
            }
        }
    }
}
