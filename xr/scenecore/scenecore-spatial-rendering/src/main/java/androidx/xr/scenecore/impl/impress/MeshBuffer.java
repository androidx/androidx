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

package androidx.xr.scenecore.impl.impress;

import androidx.annotation.RestrictTo;
import androidx.xr.scenecore.runtime.MeshBufferResource;

import org.jspecify.annotations.NonNull;

/**
 * MeshBuffer class for the native Impress mesh buffer wrapper struct which is an implementation a
 * SceneCore MeshBufferResource.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class MeshBuffer extends BindingsResource implements MeshBufferResource {
    private final ImpressApi mImpressApi;

    private MeshBuffer(Builder builder) {
        super(
                builder.mImpressapi.getBindingsResourceManager(),
                builder.mNativeMeshBuffer,
                (handle) -> builder.mImpressapi.destroyMeshBuffer(handle));
        mImpressApi = builder.mImpressapi;
    }

    @Override
    protected void releaseBindingsResource(long nativeHandle) {
        mImpressApi.destroyMeshBuffer(nativeHandle);
    }

    /** Use Builder to construct a MeshBuffer object instance. */
    public static class Builder {
        private ImpressApi mImpressapi;
        private long mNativeMeshBuffer = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            mImpressapi = impressApi;
            return this;
        }

        /** Sets the native mesh buffer. */
        @NonNull
        public Builder setNativeMeshBuffer(long nativeMeshBuffer) {
            mNativeMeshBuffer = nativeMeshBuffer;
            return this;
        }

        /** Builds the MeshBuffer. */
        @NonNull
        public MeshBuffer build() {
            if (mImpressapi == null || mNativeMeshBuffer == -1) {
                throw new IllegalStateException("MeshBuffer not built properly.");
            }
            return new MeshBuffer(this);
        }
    }
}
