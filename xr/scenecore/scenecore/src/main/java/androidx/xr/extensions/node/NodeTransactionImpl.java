/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.node;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost.SurfacePackage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.asset.EnvironmentToken;
import androidx.xr.extensions.asset.GltfAnimation;
import androidx.xr.extensions.asset.GltfModelToken;
import androidx.xr.extensions.asset.SceneToken;
import androidx.xr.extensions.asset.TokenConverter;
import androidx.xr.extensions.passthrough.PassthroughState;
import androidx.xr.extensions.subspace.Subspace;
import androidx.xr.extensions.subspace.SubspaceTypeConverter;

class NodeTransactionImpl implements NodeTransaction {
    @NonNull final com.android.extensions.xr.node.NodeTransaction transaction;

    NodeTransactionImpl(@NonNull com.android.extensions.xr.node.NodeTransaction transaction) {
        requireNonNull(transaction);
        this.transaction = transaction;
    }

    @Override
    @NonNull
    public NodeTransaction setName(@NonNull Node node, @NonNull String name) {
        transaction.setName(toFramework(node), name);
        return this;
    }

    @Nullable
    private com.android.extensions.xr.node.Node toFramework(@Nullable Node node) {
        return NodeTypeConverter.toFramework(node);
    }

    @Override
    @NonNull
    public NodeTransaction setParent(@NonNull Node node, @Nullable Node parent) {
        transaction.setParent(toFramework(node), toFramework(parent));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setPosition(@NonNull Node node, float x, float y, float z) {
        transaction.setPosition(toFramework(node), x, y, z);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setOrientation(@NonNull Node node, float x, float y, float z, float w) {
        transaction.setOrientation(toFramework(node), x, y, z, w);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setScale(@NonNull Node node, float sx, float sy, float sz) {
        transaction.setScale(toFramework(node), sx, sy, sz);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setAlpha(@NonNull Node node, float value) {
        transaction.setAlpha(toFramework(node), value);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setVisibility(@NonNull Node node, boolean isVisible) {
        transaction.setVisibility(toFramework(node), isVisible);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setSurfaceControl(
            @Nullable Node node, @NonNull SurfaceControl surfaceControl) {
        transaction.setSurfaceControl(toFramework(node), surfaceControl);
        return this;
    }

    @Override
    @NonNull
    @SuppressLint("ObsoleteSdkInt")
    public NodeTransaction setSurfacePackage(
            @Nullable Node node, @NonNull SurfacePackage surfacePackage) {
        // This method has been deprecated in the platform side.
        if (Build.VERSION.SDK_INT >= 34) {
            requireNonNull(surfacePackage);
            return setSurfaceControl(node, surfacePackage.getSurfaceControl());
        } else {
            Log.e("NodeTransaction", "setSurfacePackage is not supported in SDK lower then 34");
            return this;
        }
    }

    @Override
    @NonNull
    public NodeTransaction setWindowBounds(
            @NonNull SurfaceControl surfaceControl, int widthPx, int heightPx) {
        transaction.setWindowBounds(surfaceControl, widthPx, heightPx);
        return this;
    }

    @Override
    @NonNull
    @SuppressLint("ObsoleteSdkInt")
    public NodeTransaction setWindowBounds(
            @NonNull SurfacePackage surfacePackage, int widthPx, int heightPx) {
        // This method has been deprecated in the platform side.
        if (Build.VERSION.SDK_INT >= 34) {
            requireNonNull(surfacePackage);
            return setWindowBounds(surfacePackage.getSurfaceControl(), widthPx, heightPx);
        } else {
            Log.e("NodeTransaction", "setSurfacePackage is not supported in SDK lower then 34");
            return this;
        }
    }

    @Override
    @NonNull
    @Deprecated
    public NodeTransaction setCurvature(@NonNull Node node, float radius) {
        transaction.setCurvature(toFramework(node), radius);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setPixelResolution(@NonNull Node node, float pixelsPerMeter) {
        transaction.setPixelResolution(toFramework(node), pixelsPerMeter);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setPixelPositioning(
            @NonNull Node node, @PixelPositionFlags int pixelPositionFlags) {
        transaction.setPixelPositioning(toFramework(node), pixelPositionFlags);
        return this;
    }

    @Override
    @NonNull
    @Deprecated
    public NodeTransaction setGltfModel(
            @NonNull Node node, @NonNull GltfModelToken gltfModelToken) {
        transaction.setGltfModel(toFramework(node), TokenConverter.toFramework(gltfModelToken));
        return this;
    }

    @Override
    @NonNull
    @Deprecated
    public NodeTransaction setEnvironment(
            @NonNull Node node, @NonNull EnvironmentToken environmentToken) {
        transaction.setEnvironment(toFramework(node), TokenConverter.toFramework(environmentToken));
        return this;
    }

    @Override
    @NonNull
    @Deprecated
    public NodeTransaction setImpressScene(@NonNull Node node, @NonNull SceneToken sceneToken) {
        transaction.setImpressScene(toFramework(node), TokenConverter.toFramework(sceneToken));
        return this;
    }

    @Override
    @NonNull
    @Deprecated
    public NodeTransaction setGltfAnimation(
            @NonNull Node node,
            @NonNull String gltfAnimationName,
            @NonNull GltfAnimation.State gltfAnimationState) {
        transaction.setGltfAnimation(
                toFramework(node),
                gltfAnimationName,
                TokenConverter.toFramework(gltfAnimationState));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setAnchorId(@NonNull Node node, @Nullable IBinder anchorId) {
        transaction.setAnchorId(toFramework(node), anchorId);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setSubspace(@NonNull Node node, @NonNull Subspace subspace) {
        transaction.setSubspace(toFramework(node), SubspaceTypeConverter.toFramework(subspace));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setPassthroughState(
            @NonNull Node node,
            float passthroughOpacity,
            @PassthroughState.Mode int passthroughMode) {
        transaction.setPassthroughState(toFramework(node), passthroughOpacity, passthroughMode);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction enableReform(@NonNull Node node, @NonNull ReformOptions options) {
        transaction.enableReform(toFramework(node), NodeTypeConverter.toFramework(options));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setReformSize(@NonNull Node node, @NonNull Vec3 reformSize) {
        transaction.setReformSize(
                toFramework(node),
                new com.android.extensions.xr.node.Vec3(reformSize.x, reformSize.y, reformSize.z));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction disableReform(@NonNull Node node) {
        transaction.disableReform(toFramework(node));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction setCornerRadius(@NonNull Node node, float cornerRadius) {
        transaction.setCornerRadius(toFramework(node), cornerRadius);
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction removeCornerRadius(@NonNull Node node) {
        transaction.removeCornerRadius(toFramework(node));
        return this;
    }

    @Override
    @NonNull
    public NodeTransaction merge(@NonNull NodeTransaction transaction) {
        this.transaction.merge(((NodeTransactionImpl) transaction).transaction);
        return this;
    }

    @Override
    public void apply() {
        transaction.apply();
    }

    @Override
    public void close() {
        transaction.close();
    }
}
