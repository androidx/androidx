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

package androidx.xr.scenecore.spatial.core;

import android.content.Context;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.SystemSpaceEntity;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A parentless system-controlled JXRCore Entity that defines its own coordinate space.
 *
 * <p>It is expected to be the soft root of its own parent-child entity hierarchy.
 */
abstract class SystemSpaceEntityImpl extends AndroidXrEntity implements SystemSpaceEntity {

    // Transform for this space's origin in OpenXR reference space.
    protected final AtomicReference<Matrix4> mOpenXrReferenceSpaceTransform =
            new AtomicReference<>(null);
    protected Vector3 mWorldSpaceScale = new Vector3(1f, 1f, 1f);
    // Visible for testing.
    Closeable mNodeTransformCloseable;
    private Runnable mSpaceUpdatedListener;
    private Executor mSpaceUpdatedExecutor;

    SystemSpaceEntityImpl(
            Context context,
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, node, extensions, entityManager, executor);

        // The underlying CPM node is always expected to be updated in response to changes to
        // the coordinate space represented by a SystemSpaceEntityImpl so we subscribe at
        // construction.
        subscribeToNodeTransform(node, executor);
    }

    /** Called when the underlying space has changed. */
    public void onSpaceUpdated() {
        if (mSpaceUpdatedListener != null) {
            mSpaceUpdatedExecutor.execute(() -> mSpaceUpdatedListener.run());
        }
    }

    /** Registers the SDK layer / application's listener for space updates. */
    @Override
    public void setOnSpaceUpdatedListener(
            @Nullable Runnable listener, @Nullable Executor executor) {
        mSpaceUpdatedListener = listener;
        mSpaceUpdatedExecutor = executor == null ? mExecutor : executor;
    }

    /**
     * Returns the pose relative to an OpenXR reference space.
     *
     * <p>The OpenXR reference space is the space returned by {@link
     * XrExtensions#getOpenXrWorldReferenceSpaceType()}
     */
    public Pose getPoseInOpenXrReferenceSpace() {
        if (mOpenXrReferenceSpaceTransform.get() == null) {
            return null;
        }
        return mOpenXrReferenceSpaceTransform.get().unscaled().getPose();
    }

    /**
     * Sets the pose and scale of the entity in an OpenXR reference space and should call the
     * onSpaceUpdated() callback to signal a change in the underlying space.
     *
     * @param openXrReferenceSpaceTransform 4x4 transformation matrix of the entity in an OpenXR
     *     reference space. The OpenXR reference space is of the type defined by the {@link
     *     XrExtensions#getOpenXrWorldReferenceSpaceType()} method.
     */
    protected void setOpenXrReferenceSpaceTransform(Matrix4 openXrReferenceSpaceTransform) {
        if (openXrReferenceSpaceTransform.equals(Matrix4.Zero)) {
            return;
        }
        mOpenXrReferenceSpaceTransform.set(openXrReferenceSpaceTransform);
        // TODO: b/353511649 - Make SystemSpaceEntityImpl thread safe.
        // Matrix4.scale returns either a positive or negative scale based on the rotation
        // matrix determinant, but we keep it positive for now to avoid any unexpected issues.
        // SpaceFlinger might apply a scale to the task node, for example if the user caused the
        // main panel to scale in Homespace mode.
        Vector3 actualScale = openXrReferenceSpaceTransform.getScale();
        // TODO: b/367780918 - Use the original scale, when the new matrix decomposition is tested
        // thoroughly.
        mWorldSpaceScale = Vector3.abs(actualScale);
        this.setScaleInternal(mWorldSpaceScale.copy());
        onSpaceUpdated();
    }

    /**
     * Subscribes to the node's transform update events and caches the pose by calling
     * setOpenXrReferenceSpacePose().
     *
     * @param node The node to subscribe to.
     * @param executor The executor to run the callback on.
     */
    private void subscribeToNodeTransform(Node node, ScheduledExecutorService executor) {
        mNodeTransformCloseable =
                node.subscribeToTransform(
                        executor,
                        (transform) ->
                                setOpenXrReferenceSpaceTransform(
                                        RuntimeUtils.getMatrix(transform.getTransform())));
    }

    @Override
    public @NonNull Vector3 getWorldSpaceScale() {
        return mWorldSpaceScale;
    }

    /** Unsubscribes from the node's transform update events. */
    private void unsubscribeFromNodeTransform() {
        try {
            mNodeTransformCloseable.close();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not close node transform subscription with error: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        unsubscribeFromNodeTransform();
        super.dispose();
    }
}
