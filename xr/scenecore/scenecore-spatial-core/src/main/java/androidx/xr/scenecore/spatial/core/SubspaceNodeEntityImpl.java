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

import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Space;
import androidx.xr.scenecore.runtime.SpaceValue;
import androidx.xr.scenecore.runtime.SubspaceNodeEntity;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Represents an entity that manages a subspace node.
 *
 * <p>This class manages the pose, scale, alpha, size and visibility of the subspace node enclosed
 * by this entity, and allows the entity to be user interactable. This entity doesn't have access to
 * underlying impress nodes like the [SurfaceEntityImpl], so it treats the subspace node as sibling
 * disjointed from scene graph and applies all transformations to it explicitly.
 */
final class SubspaceNodeEntityImpl extends AndroidXrEntity implements SubspaceNodeEntity {
    private final Node mSubspaceNode;
    private Dimensions mSize;
    private Vector3 mInternalScale = Vector3.One;

    SubspaceNodeEntityImpl(
            Context context,
            XrExtensions extensions,
            Node subspaceNode,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(context, extensions.createNode(), extensions, entityManager, executor);
        mSubspaceNode = subspaceNode;
    }

    @Override
    public void setPose(@NonNull Pose pose, @SpaceValue int relativeTo) {
        super.setPose(pose, relativeTo);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setPosition(
                            mSubspaceNode,
                            pose.getTranslation().getX(),
                            pose.getTranslation().getY(),
                            pose.getTranslation().getZ())
                    .setOrientation(
                            mSubspaceNode,
                            pose.getRotation().getX(),
                            pose.getRotation().getY(),
                            pose.getRotation().getZ(),
                            pose.getRotation().getW())
                    .apply();
        }
    }

    @Override
    public void setScale(@NonNull Vector3 scale, @SpaceValue int relativeTo) {
        super.setScale(scale, relativeTo);
        mInternalScale = super.getScale(Space.ACTIVITY);
        Dimensions size =
                new Dimensions(
                        mSize.width * mInternalScale.getX(),
                        mSize.height * mInternalScale.getY(),
                        mSize.depth * mInternalScale.getZ());
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setScale(mSubspaceNode, size.width, size.height, size.depth).apply();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            // Get the final clamped alpha value from the inheritance chain (from BaseEntity).
            // This is then applied to mSubspaceNode to ensure its alpha is consistent with mNode's.
            transaction.setAlpha(mSubspaceNode, super.getAlpha()).apply();
        }
    }

    @Override
    public @NonNull Dimensions getSize() {
        return mSize;
    }

    @Override
    public void setSize(@NonNull Dimensions size) {
        mSize = size;
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setScale(
                            mSubspaceNode,
                            size.width * mInternalScale.getX(),
                            size.height * mInternalScale.getY(),
                            size.depth * mInternalScale.getZ())
                    .apply();
        }
    }

    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setVisibility(mSubspaceNode, !hidden).apply();
        }
    }
}
