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

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Space;
import androidx.xr.runtime.internal.SpaceValue;
import androidx.xr.runtime.internal.SubspaceNodeEntity;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;

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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class SubspaceNodeEntityImpl extends AndroidXrEntity implements SubspaceNodeEntity {
    private final Node mSubspaceNode;
    private Dimensions mSize;
    private Vector3 mScale = Vector3.One;

    SubspaceNodeEntityImpl(
            Context context,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor,
            Node subspaceNode,
            Dimensions size) {
        super(context, extensions.createNode(), extensions, entityManager, executor);
        this.mSubspaceNode = subspaceNode;
        setSize(size);
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
        mScale = super.getScale(Space.ACTIVITY);
        Dimensions size =
                new Dimensions(
                        mSize.width * mScale.getX(),
                        mSize.height * mScale.getY(),
                        mSize.depth * mScale.getZ());
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setScale(mSubspaceNode, size.width, size.height, size.depth).apply();
        }
    }

    @Override
    public void setAlpha(float alpha, @SpaceValue int relativeTo) {
        super.setAlpha(alpha, relativeTo);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAlpha(mSubspaceNode, alpha).apply();
        }
    }

    @Override
    public void setSize(@NonNull Dimensions size) {
        mSize = size;
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setScale(
                            mSubspaceNode,
                            size.width * mScale.getX(),
                            size.height * mScale.getY(),
                            size.depth * mScale.getZ())
                    .apply();
        }
    }

    @Override
    public @NonNull Dimensions getSize() {
        return mSize;
    }

    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setVisibility(mSubspaceNode, !hidden).apply();
        }
    }
}
