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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Binder;
import android.util.Log;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceControlViewHost.SurfacePackage;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.PanelEntity;
import androidx.xr.runtime.internal.PixelDimensions;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of PanelEntity.
 *
 * <p>(Requires API Level 30)
 *
 * <p>This entity shows 2D view on spatial panel.
 */
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
final class PanelEntityImpl extends BasePanelEntity implements PanelEntity {
    private static final String TAG = "PanelEntity";
    private final SurfaceControlViewHost mSurfaceControlViewHost;

    PanelEntityImpl(
            @NonNull Context context,
            Node node,
            @NonNull View view,
            XrExtensions extensions,
            EntityManager entityManager,
            PixelDimensions surfaceDimensionsPx,
            @NonNull String name,
            ScheduledExecutorService executor) {
        super(context, node, extensions, entityManager, executor);

        View reparentedView = maybeReparentView(view, name, context);
        mSurfaceControlViewHost =
                new SurfaceControlViewHost(
                        context, Objects.requireNonNull(context.getDisplay()), new Binder());
        setupSurfaceControlViewHostAndCornerRadius(reparentedView, surfaceDimensionsPx, name);
        setDefaultOnBackInvokedCallback(view);
    }

    PanelEntityImpl(
            @NonNull Context context,
            Node node,
            @NonNull View view,
            XrExtensions extensions,
            EntityManager entityManager,
            Dimensions surfaceDimensions,
            @NonNull String name,
            ScheduledExecutorService executor) {
        super(context, node, extensions, entityManager, executor);
        float unscaledPixelDensity = getDefaultPixelDensity();
        PixelDimensions surfaceDimensionsPx =
                new PixelDimensions(
                        (int) (surfaceDimensions.width * unscaledPixelDensity),
                        (int) (surfaceDimensions.height * unscaledPixelDensity));

        View reparentedView = maybeReparentView(view, name, context);
        mSurfaceControlViewHost =
                new SurfaceControlViewHost(
                        context, Objects.requireNonNull(context.getDisplay()), new Binder());
        setupSurfaceControlViewHostAndCornerRadius(reparentedView, surfaceDimensionsPx, name);
        setDefaultOnBackInvokedCallback(view);
    }

    // Adds a FrameLayout as a parent of the contentView if it doesn't already have one. Adding the
    // FrameLayout ensures compatibility with LayoutInspector without visually impacting the layout
    // of
    // the view.
    private static View maybeReparentView(View contentView, String name, Context context) {
        if (contentView instanceof FrameLayout) {
            return contentView;
        }
        if (contentView.getParent() != null) {
            Log.w(
                    TAG,
                    "Panel "
                            + name
                            + " already has a parent. LayoutInspector may not work properly for"
                            + " this panel.");
            return contentView;
        }
        try {
            FrameLayout frameLayout = new FrameLayout(context);
            LifecycleOwner contentLifecycleOwner = ViewTreeLifecycleOwner.get(contentView);
            if (contentLifecycleOwner != null) {
                ViewTreeLifecycleOwner.set(frameLayout, contentLifecycleOwner);
            }
            frameLayout.setLayoutParams(
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            frameLayout.addView(contentView);
            return frameLayout;
        } catch (Throwable t) {
            // This error only impacts the effectiveness of LayoutInspector, so we can just log it
            // and
            // return the original contentView rather than rethrowing.
            Log.e(
                    TAG,
                    "Could not set a new parent View for Panel "
                            + name
                            + ". LayoutInspector may not work properly for this panel.",
                    t);
        }

        return contentView;
    }

    // TODO(b/352827267): Enforce minSDK API strategy - go/androidx-api-guidelines#compat-newapi
    private void setupSurfaceControlViewHostAndCornerRadius(
            @NonNull View view,
            @NonNull PixelDimensions surfaceDimensionsPx,
            @NonNull String name) {
        mSurfaceControlViewHost.setView(
                view, surfaceDimensionsPx.width, surfaceDimensionsPx.height);

        SurfacePackage surfacePackage =
                Objects.requireNonNull(mSurfaceControlViewHost.getSurfacePackage());

        // We need to manually inform our base class of the pixelDimensions, even though the
        // Extensions
        // are initialized in the factory method. (ext.setWindowBounds, etc)
        super.setSizeInPixels(surfaceDimensionsPx);
        float cornerRadius = getDefaultCornerRadiusInMeters();
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setName(mNode, name)
                    .setSurfacePackage(mNode, surfacePackage)
                    .setWindowBounds(
                            surfacePackage, surfaceDimensionsPx.width, surfaceDimensionsPx.height)
                    .setVisibility(mNode, true)
                    .setCornerRadius(mNode, cornerRadius)
                    .apply();
        } finally {
            surfacePackage.release();
        }
        super.setCornerRadiusValue(cornerRadius);
    }

    @SuppressWarnings("deprecation") // TODO: b/398052385 - Replace deprecate onBackPressed.
    private void setDefaultOnBackInvokedCallback(View view) {
        OnBackInvokedCallback onBackInvokedCallback =
                () -> {
                    Context context = view.getContext();
                    // The context is not necessarily an activity, we need to find the activity
                    // to forward the onBackPressed()
                    while (context instanceof ContextWrapper) {
                        if (context instanceof Activity) {
                            ((Activity) context).onBackPressed();
                            return;
                        }
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                };
        OnBackInvokedDispatcher backDispatcher = view.findOnBackInvokedDispatcher();
        if (backDispatcher != null) {
            backDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback);
        }
    }

    @Override
    public void setSizeInPixels(@NonNull PixelDimensions dimensions) {
        super.setSizeInPixels(dimensions);

        SurfacePackage surfacePackage =
                Objects.requireNonNull(mSurfaceControlViewHost.getSurfacePackage());

        mSurfaceControlViewHost.relayout(dimensions.width, dimensions.height);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setWindowBounds(surfacePackage, dimensions.width, dimensions.height)
                    .apply();
        }
        surfacePackage.release();
    }

    @SuppressWarnings("ObjectToString")
    @Override
    public void dispose() {
        Log.i(TAG, "Disposing " + this);
        mSurfaceControlViewHost.release();
        super.dispose();
    }
}
