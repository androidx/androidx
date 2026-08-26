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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.view.SurfaceControlViewHost
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.requiresApiLevel
import androidx.xr.scenecore.spatial.core.RuntimeUtils.getDefaultPixelsPerMeter
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledExecutorService

/**
 * Displays a [android.view.View] on a spatial panel.
 *
 * Back gesture handling requires API level 33 or higher.
 */
internal class PanelEntityImpl : BasePanelEntity, PanelEntity {
    private val surfaceControlViewHost: SurfaceControlViewHost
    private val panelCleanupAction: PanelEntityCleanupAction

    constructor(
        context: Context,
        node: Node,
        view: View,
        extensions: XrExtensions,
        sceneNodeRegistry: SceneNodeRegistry,
        surfaceDimensionsPx: PixelDimensions,
        name: String,
        executor: ScheduledExecutorService,
    ) : super(context, node, extensions, sceneNodeRegistry, executor) {
        val reparentedView = maybeReparentView(view, context)
        surfaceControlViewHost =
            requiresApiLevel(30) {
                SurfaceControlViewHost(
                    context,
                    checkNotNull(context.display) { "Context is not associated with a display." },
                    Binder(),
                )
            }
        setupSurfaceControlViewHostAndCornerRadius(reparentedView, surfaceDimensionsPx, name)
        panelCleanupAction =
            initCleanupAction(reparentedView, view, surfaceControlViewHost, executor)
    }

    constructor(
        context: Context,
        node: Node,
        view: View,
        extensions: XrExtensions,
        sceneNodeRegistry: SceneNodeRegistry,
        surfaceDimensions: Dimensions,
        name: String,
        executor: ScheduledExecutorService,
    ) : this(
        context,
        node,
        view,
        extensions,
        sceneNodeRegistry,
        PixelDimensions(
            (surfaceDimensions.width * getDefaultPixelsPerMeter(extensions)).toInt(),
            (surfaceDimensions.height * getDefaultPixelsPerMeter(extensions)).toInt(),
        ),
        name,
        executor,
    )

    private fun initCleanupAction(
        reparentedView: View,
        view: View,
        surfaceControlViewHost: SurfaceControlViewHost,
        executor: ScheduledExecutorService,
    ): PanelEntityCleanupAction {
        val registration = requiresApiLevel(33) { setDefaultOnBackInvokedCallback(view) }
        val wrapperRef =
            if (reparentedView !== view && reparentedView is ViewGroup) {
                WeakReference(reparentedView)
            } else {
                null
            }
        val childRef = if (reparentedView !== view) WeakReference(view) else null
        val cleanupAction =
            PanelEntityCleanupAction(
                surfaceControlViewHost,
                registration.dispatcher,
                registration.callback,
                wrapperRef,
                childRef,
            )
        registerCleanup(executor, cleanupAction)
        return cleanupAction
    }

    internal class PanelEntityCleanupAction(
        private val surfaceControlViewHost: SurfaceControlViewHost,
        private val backDispatcher: OnBackInvokedDispatcher?,
        private val onBackInvokedCallback: OnBackInvokedCallback?,
        private val wrapperViewGroupRef: WeakReference<ViewGroup>? = null,
        private val childViewRef: WeakReference<View>? = null,
    ) :
        CleanupAction({
            requiresApiLevel(33) {
                if (backDispatcher != null && onBackInvokedCallback != null) {
                    backDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                }
            }
            requiresApiLevel(30) { surfaceControlViewHost.release() }
            val wrapper = wrapperViewGroupRef?.get()
            val child = childViewRef?.get()
            if (wrapper != null && child != null) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    wrapper.removeView(child)
                } else {
                    Handler(Looper.getMainLooper()).post { wrapper.removeView(child) }
                }
            }
        })

    private fun setupSurfaceControlViewHostAndCornerRadius(
        view: View,
        surfaceDimensionsPx: PixelDimensions,
        name: String,
    ) {
        requiresApiLevel(30) {
            surfaceControlViewHost.setView(
                view,
                surfaceDimensionsPx.width,
                surfaceDimensionsPx.height,
            )
        }

        val surfacePackage =
            checkNotNull(requiresApiLevel(30) { surfaceControlViewHost.surfacePackage }) {
                "SurfaceControlViewHost has no active SurfacePackage."
            }

        // We need to manually inform our base class of the pixelDimensions, even though the
        // extensions are initialized in the factory method. (ext.setWindowBounds, etc.)
        super.sizeInPixels = surfaceDimensionsPx
        try {
            extensions.createNodeTransaction().use { transaction ->
                transaction
                    .setName(node, name)
                    .setSurfacePackage(node, surfacePackage)
                    .setWindowBounds(
                        surfacePackage,
                        surfaceDimensionsPx.width,
                        surfaceDimensionsPx.height,
                    )
                    .setVisibility(node, true)
                    .setCornerRadius(node, defaultCornerRadiusInMeters)
                    .apply()
            }
        } finally {
            requiresApiLevel(30) { surfacePackage.release() }
        }
        super.cornerRadiusValue = defaultCornerRadiusInMeters
    }

    @RequiresApi(33)
    private data class BackInvokedRegistration(
        val dispatcher: OnBackInvokedDispatcher?,
        val callback: OnBackInvokedCallback?,
    )

    @RequiresApi(33)
    @Suppress("DEPRECATION") // TODO: b/398052385 - Replace deprecate onBackPressed.
    private fun setDefaultOnBackInvokedCallback(view: View): BackInvokedRegistration {
        val viewRef = WeakReference(view)
        val callback = OnBackInvokedCallback {
            val currentView = viewRef.get() ?: return@OnBackInvokedCallback
            var currentContext = currentView.context
            // The context is not necessarily an activity, we need to forward the
            // onBackPressed()
            while (currentContext is ContextWrapper) {
                if (currentContext is Activity) {
                    currentContext.onBackPressed()
                    return@OnBackInvokedCallback
                }
                currentContext = currentContext.baseContext
            }
        }
        val dispatcher = view.findOnBackInvokedDispatcher()
        dispatcher?.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback,
        )
        return BackInvokedRegistration(dispatcher, callback)
    }

    override var sizeInPixels: PixelDimensions
        get() = super.sizeInPixels
        set(value) {
            requiresApiLevel(30) {
                if (super.sizeInPixels == value) return
                surfaceControlViewHost.relayout(value.width, value.height)
                val surfacePackage = surfaceControlViewHost.surfacePackage ?: return
                try {
                    extensions.createNodeTransaction().use { transaction ->
                        transaction
                            .setWindowBounds(surfacePackage, value.width, value.height)
                            .apply()
                    }
                } finally {
                    surfacePackage.release()
                }
                super.sizeInPixels = value
            }
        }

    override var contentDescription: CharSequence = ""
        set(text) {
            field = text
            requiresApiLevel(30) {
                val view: View? = surfaceControlViewHost.view
                if (view != null) {
                    if (text.isNotEmpty()) {
                        view.isFocusable = true
                    }
                    view.contentDescription = text
                }
            }
        }

    companion object {
        // Adds a FrameLayout as a parent of the contentView if it doesn't already have one. Adding
        // the FrameLayout ensures compatibility with LayoutInspector without visually impacting the
        // layout of the view.
        private fun maybeReparentView(contentView: View, context: Context): View {
            if (contentView is FrameLayout) {
                return contentView
            }
            if (contentView.parent != null) {
                // Already has a parent. LayoutInspector may not work properly for this panel.
                return contentView
            }
            try {
                val frameLayout = FrameLayout(context)
                val contentLifecycleOwner = contentView.findViewTreeLifecycleOwner()
                if (contentLifecycleOwner != null) {
                    frameLayout.setViewTreeLifecycleOwner(contentLifecycleOwner)
                }
                frameLayout.setLayoutParams(
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )
                frameLayout.addView(contentView)
                return frameLayout
            } catch (_: Throwable) {
                // This error only impacts the effectiveness of LayoutInspector, don't rethrow it.
            }

            return contentView
        }
    }
}
