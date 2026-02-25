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
import android.graphics.Rect
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import com.android.extensions.xr.XrExtensionResult
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import java.util.concurrent.ScheduledExecutorService

/**
 * MainPanelEntity is a special instance of a PanelEntity that is backed by the WindowLeash CPM
 * node. The content of this PanelEntity is assumed to have been previously defined and associated
 * with the Window Leash Node.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
internal class MainPanelEntityImpl(
    activity: Activity,
    node: Node,
    extensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) : BasePanelEntity(activity, node, extensions, entityManager, executor), PanelEntity {
    // Note that we expect the Node supplied here to be the WindowLeash node.
    init {
        // Read the Pixel dimensions for the primary panel off the Activity's WindowManager. Note
        // that this requires MinAPI 30.
        // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
        super.sizeInPixels =
            PixelDimensions(boundsFromWindowManager.width(), boundsFromWindowManager.height())
        val cornerRadius = defaultCornerRadiusInMeters
        mExtensions.createNodeTransaction().use { transaction ->
            transaction.setCornerRadius(node, cornerRadius).apply()
        }
        super.cornerRadiusValue = cornerRadius
    }

    private val boundsFromWindowManager: Rect
        get() = activity!!.windowManager.currentWindowMetrics.bounds

    override var size: Dimensions
        get() {
            // The main panel bounds can change in HSM without JXRCore. Always read the bounds from
            // the WindowManager.
            return Dimensions(
                boundsFromWindowManager.width() / defaultPixelDensity,
                boundsFromWindowManager.height() / defaultPixelDensity,
                0f,
            )
        }
        set(value) {
            super.size = value
        }

    override var sizeInPixels: PixelDimensions
        get() {
            // The main panel bounds can change in HSM without JXRCore. Always read the bounds from
            // the WindowManager.
            return PixelDimensions(
                boundsFromWindowManager.width(),
                boundsFromWindowManager.height(),
            )
        }
        set(value) {
            // TODO: b/376126162 - Consider calling setPixelDimensions() either when
            // setMainWindowSize's callback is called, or when the next spatial state callback with
            // the expected size is called.
            super.sizeInPixels = value
            // TODO: b/376934871 - Check async results.
            mExtensions.setMainWindowSize(
                activity,
                value.width,
                value.height,
                { obj: Runnable -> obj.run() },
                { _: XrExtensionResult? -> },
            )
        }
}
