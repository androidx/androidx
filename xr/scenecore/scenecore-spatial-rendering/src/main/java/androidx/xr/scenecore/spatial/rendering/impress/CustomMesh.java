/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.spatial.rendering.impress;

import androidx.annotation.RestrictTo;
import androidx.xr.scenecore.runtime.CustomMeshResource;

import org.jspecify.annotations.NonNull;

/**
 * CustomMesh class for the native Impress custom mesh wrapper struct which is an implementation a
 * SceneCore CustomMeshResource.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CustomMesh extends BindingsResource implements CustomMeshResource {
    private final ImpressApi mImpressApi;

    private CustomMesh(Builder builder) {
        super(
                builder.mImpressapi.getBindingsResourceManager(),
                builder.mNativeCustomMesh,
                (handle) -> builder.mImpressapi.destroyCustomMesh(handle));
        mImpressApi = builder.mImpressapi;
    }

    @Override
    protected void releaseBindingsResource(long nativeHandle) {
        mImpressApi.destroyCustomMesh(nativeHandle);
    }

    /** Use Builder to construct a CustomMesh object instance. */
    public static class Builder {
        private ImpressApi mImpressapi;
        private long mNativeCustomMesh = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            mImpressapi = impressApi;
            return this;
        }

        /** Sets the native custom mesh. */
        @NonNull
        public Builder setNativeCustomMesh(long nativeCustomMesh) {
            mNativeCustomMesh = nativeCustomMesh;
            return this;
        }

        /** Builds the CustomMesh. */
        @NonNull
        public CustomMesh build() {
            if (mImpressapi == null || mNativeCustomMesh == -1) {
                throw new IllegalStateException("CustomMesh not built properly.");
            }
            return new CustomMesh(this);
        }
    }
}
