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

package androidx.xr.scenecore.impl.impress;

import androidx.annotation.RestrictTo;
import androidx.xr.scenecore.runtime.TextureResource;

import org.jspecify.annotations.NonNull;

/**
 * Texture class for the native Impress texture wrapper struct which is an implementation a
 * SceneCore TextureResource.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Texture extends BindingsResource implements TextureResource {
    private final ImpressApi mImpressApi;

    private Texture(Builder builder) {
        super(
                builder.mImpressapi.getBindingsResourceManager(),
                builder.mNativeTexture,
                (handle) -> builder.mImpressapi.destroyNativeObject(handle));
        mImpressApi = builder.mImpressapi;
    }

    @Override
    protected void releaseBindingsResource(long nativeHandle) {
        mImpressApi.destroyNativeObject(nativeHandle);
    }

    /** Use Builder to construct a Texture object instance. */
    public static class Builder {
        private ImpressApi mImpressapi;
        private long mNativeTexture = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            mImpressapi = impressApi;
            return this;
        }

        /** Sets the native texture. */
        @NonNull
        public Builder setNativeTexture(long nativeTexture) {
            mNativeTexture = nativeTexture;
            return this;
        }

        /** Builds the Texture. */
        @NonNull
        public Texture build() {
            if (mImpressapi == null || mNativeTexture == -1) {
                throw new IllegalStateException("Texture not built properly.");
            }
            return new Texture(this);
        }
    }
}
