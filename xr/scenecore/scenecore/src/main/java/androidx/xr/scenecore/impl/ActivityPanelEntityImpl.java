/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.xr.extensions.XrExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/** Implementation of {@link ActivityPanelEntity}. */
class ActivityPanelEntityImpl extends BasePanelEntity implements ActivityPanelEntity {
    private static final String TAG = ActivityPanelEntityImpl.class.getSimpleName();
    private final ActivityPanel mActivityPanel;

    // TODO(b/352630140): Add a static factory method and remove the business logic from
    //                    JxrPlatformAdapterAxr.

    ActivityPanelEntityImpl(
            Node node,
            String name,
            XrExtensions extensions,
            EntityManager entityManager,
            ActivityPanel activityPanel,
            PixelDimensions windowBoundsPx,
            ScheduledExecutorService executor) {
        super(node, extensions, entityManager, executor);
        // We need to notify our base class of the pixelDimensions, even though the Extensions are
        // initialized in the factory method. (ext.ActivityPanel.setWindowBounds, etc)
        super.setPixelDimensions(windowBoundsPx);
        float cornerRadius = getDefaultCornerRadiusInMeters();
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setVisibility(activityPanel.getNode(), true)
                    .setName(activityPanel.getNode(), name)
                    .setCornerRadius(activityPanel.getNode(), cornerRadius)
                    .apply();
        }
        mActivityPanel = activityPanel;
        super.setCornerRadiusValue(cornerRadius);
    }

    @Override
    public void launchActivity(Intent intent, @Nullable Bundle bundle) {
        // Note that launching an Activity into the Panel doesn't actually update the size. The
        // application is expected to set the size of the ActivityPanel at construction time, before
        // launching an Activity into it. The Activity will then render into the size the
        // application
        // specified, and the system will apply letterboxing if necessary.
        mActivityPanel.launchActivity(intent, bundle);
    }

    @Override
    public void moveActivity(Activity activity) {
        // Note that moving an Activity into the Panel doesn't actually update the size. The
        // application
        // should explicitly call setPixelDimensions() to update the size of an ActivityPanel.
        mActivityPanel.moveActivity(activity);
    }

    @Override
    public void setPixelDimensions(PixelDimensions dimensions) {
        PixelDimensions oldDimensions = mPixelDimensions;
        super.setPixelDimensions(dimensions);

        // Avoid updating the bounds if we were called with the same values.
        if (Objects.equals(oldDimensions, dimensions)) {
            Log.i(TAG, "setPixelDimensions called with same dimensions - " + dimensions);
            return;
        }

        mActivityPanel.setWindowBounds(new Rect(0, 0, dimensions.width, dimensions.height));
    }

    /**
     * Disposes the ActivityPanelEntity.
     *
     * <p>This will delete the ActivityPanel and destroy the embedded activity.
     */
    @Override
    public void dispose() {
        mActivityPanel.delete();
        super.dispose();
    }
}
