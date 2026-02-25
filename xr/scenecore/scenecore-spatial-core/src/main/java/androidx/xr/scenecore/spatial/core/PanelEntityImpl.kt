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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Binder
import android.view.Display
import android.view.SurfaceControlViewHost
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.Objects
import java.util.concurrent.ScheduledExecutorService

/**
 * Implementation of PanelEntity.
 *
 * (Requires API Level 30)
 *
 * This entity shows 2D view on spatial panel.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
internal class PanelEntityImpl : BasePanelEntity, PanelEntity {
    private val surfaceControlViewHost: SurfaceControlViewHost

    constructor(
        context: Context,
        node: Node,
        view: View,
        extensions: XrExtensions,
        entityManager: EntityManager,
        surfaceDimensionsPx: PixelDimensions,
        name: String,
        executor: ScheduledExecutorService,
    ) : super(context, node, extensions, entityManager, executor) {
        val reparentedView = maybeReparentView(view, context)
        surfaceControlViewHost =
            SurfaceControlViewHost(
                context,
                Objects.requireNonNull<Display>(context.display),
                Binder(),
            )
        setupSurfaceControlViewHostAndCornerRadius(reparentedView, surfaceDimensionsPx, name)
        setDefaultOnBackInvokedCallback(view)
    }

    constructor(
        context: Context,
        node: Node,
        view: View,
        extensions: XrExtensions,
        entityManager: EntityManager,
        surfaceDimensions: Dimensions,
        name: String,
        executor: ScheduledExecutorService,
    ) : super(context, node, extensions, entityManager, executor) {
        val surfaceDimensionsPx =
            PixelDimensions(
                (surfaceDimensions.width * defaultPixelDensity).toInt(),
                (surfaceDimensions.height * defaultPixelDensity).toInt(),
            )
        val reparentedView = maybeReparentView(view, context)
        surfaceControlViewHost =
            SurfaceControlViewHost(
                context,
                Objects.requireNonNull<Display>(context.display),
                Binder(),
            )
        setupSurfaceControlViewHostAndCornerRadius(reparentedView, surfaceDimensionsPx, name)
        setDefaultOnBackInvokedCallback(view)
    }

    // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
    private fun setupSurfaceControlViewHostAndCornerRadius(
        view: View,
        surfaceDimensionsPx: PixelDimensions,
        name: String,
    ) {
        surfaceControlViewHost.setView(view, surfaceDimensionsPx.width, surfaceDimensionsPx.height)

        val surfacePackage =
            Objects.requireNonNull<SurfaceControlViewHost.SurfacePackage>(
                surfaceControlViewHost.surfacePackage
            )

        // We need to manually inform our base class of the pixelDimensions, even though the
        // extensions are initialized in the factory method. (ext.setWindowBounds, etc.)
        super.sizeInPixels = surfaceDimensionsPx
        try {
            mExtensions.createNodeTransaction().use { transaction ->
                transaction
                    .setName(mNode, name)
                    .setSurfacePackage(mNode, surfacePackage)
                    .setWindowBounds(
                        surfacePackage,
                        surfaceDimensionsPx.width,
                        surfaceDimensionsPx.height,
                    )
                    .setVisibility(mNode, true)
                    .setCornerRadius(mNode, defaultCornerRadiusInMeters)
                    .apply()
            }
        } finally {
            surfacePackage.release()
        }
        super.cornerRadiusValue = defaultCornerRadiusInMeters
    }

    @Suppress("deprecation") // TODO: b/398052385 - Replace deprecate onBackPressed.
    private fun setDefaultOnBackInvokedCallback(view: View) {
        val onBackInvokedCallback = OnBackInvokedCallback {
            var context = view.context
            // The context is not necessarily an activity, we need to find the activity to forward
            // the onBackPressed()
            while (context is ContextWrapper) {
                if (context is Activity) {
                    context.onBackPressed()
                    return@OnBackInvokedCallback
                }
                context = context.baseContext
            }
        }
        val backDispatcher = view.findOnBackInvokedDispatcher()
        backDispatcher?.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            onBackInvokedCallback,
        )
    }

    override var sizeInPixels: PixelDimensions
        get() = super.sizeInPixels
        set(value) {
            if (super.sizeInPixels == value) return
            surfaceControlViewHost.relayout(value.width, value.height)
            val surfacePackage = surfaceControlViewHost.surfacePackage!!
            mExtensions.createNodeTransaction().use { transaction ->
                transaction.setWindowBounds(surfacePackage, value.width, value.height).apply()
            }
            surfacePackage.release()
            super.sizeInPixels = value
        }

    override fun dispose() {
        surfaceControlViewHost.release()
        super.dispose()
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
