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
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.xr.scenecore.runtime.ActivityPanelEntity
import androidx.xr.scenecore.runtime.PixelDimensions
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.space.ActivityPanel
import java.util.concurrent.ScheduledExecutorService

/** Implementation of [ActivityPanelEntity]. */
internal class ActivityPanelEntityImpl(
    context: Context,
    node: Node,
    name: String,
    extensions: XrExtensions,
    entityManager: EntityManager,
    private val activityPanel: ActivityPanel,
    windowBoundsPx: PixelDimensions,
    executor: ScheduledExecutorService,
) : BasePanelEntity(context, node, extensions, entityManager, executor), ActivityPanelEntity {

    // TODO(b/352630140): Add a static factory method and remove the business logic from
    // SpatialSceneRuntime.
    init {
        super.sizeInPixels = windowBoundsPx
        // We need to notify our base class of the pixelDimensions, even though the Extensions are
        // initialized in the factory method. (ext.ActivityPanel.setWindowBounds, etc.)
        mExtensions.createNodeTransaction().use { transaction ->
            transaction
                .setVisibility(activityPanel.node, true)
                .setName(activityPanel.node, name)
                .setCornerRadius(activityPanel.node, defaultCornerRadiusInMeters)
                .apply()
        }
        super.cornerRadiusValue = defaultCornerRadiusInMeters
    }

    override fun launchActivity(intent: Intent, bundle: Bundle?) {
        // Note that launching an Activity into the Panel doesn't actually update the size. The
        // application is expected to set the size of the ActivityPanel at construction time, before
        // launching an Activity into it. The Activity will then render into the size the
        // application specified, and the system will apply letterboxing if necessary.
        activityPanel.launchActivity(intent, bundle)
    }

    override fun moveActivity(activity: Activity) {
        // Note that moving an Activity into the Panel doesn't actually update the size. The
        // application should explicitly call setPixelDimensions() to update the size of an
        // ActivityPanel.
        activityPanel.moveActivity(activity)
    }

    override var sizeInPixels: PixelDimensions
        get() = super.sizeInPixels
        set(value) {
            // Avoid updating the bounds if we were called with the same values.
            if (super.sizeInPixels == value) {
                return
            }
            activityPanel.setWindowBounds(Rect(0, 0, value.width, value.height))
            super.sizeInPixels = value
        }

    /**
     * Disposes the ActivityPanelEntity.
     *
     * This will delete the ActivityPanel and destroy the embedded activity.
     */
    override fun dispose() {
        activityPanel.delete()
        super.dispose()
    }
}
